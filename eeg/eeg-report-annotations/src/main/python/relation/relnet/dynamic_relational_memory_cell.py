"Define a dynamic memory cell."
from __future__ import absolute_import
from __future__ import print_function
from __future__ import division

from .model_ops import safe_norm, skip_gradient

import tensorflow as tf
from tensorflow.contrib.rnn import RNNCell


class DynamicRelationalMemoryCell(RNNCell):
    """
    Implementation of a dynamic memory cell as a gated recurrent network.
    The cell's hidden state is divided into blocks and each block's weights are tied.
    """

    def __init__(self,
                 num_blocks,
                 num_units_per_block,
                 keys,
                 initializer=None,
                 recurrent_initializer=None,
                 activation=tf.nn.relu,
                 rel_activation=None,
                 dtype=tf.float32,
                 do_norm=True):
        self._num_blocks = num_blocks  # M
        self._num_units_per_block = num_units_per_block  # d
        self._keys = keys  # list of [?, hidden_size] tensors where ? will be batch_size
        # self._relational_memory = relational_memory
        self._activation = activation  # \phi
        self._rel_activation = activation if rel_activation is None else rel_activation
        self._initializer = initializer
        self._recurrent_initializer = recurrent_initializer
        self._dtype = dtype
        self._do_norm = do_norm

    @property
    def state_size(self):
        "Return the total state size of the cell, across all blocks."
        return (self._num_blocks * self._num_units_per_block,
                self._num_blocks * self._num_blocks * self._num_units_per_block)

    @property
    def output_size(self):
        "Return the total output size of the cell, across all blocks."
        # return tf.TensorShape([self._num_blocks, self._num_units_per_block])
        return self._num_blocks * self._num_units_per_block

    def zero_state(self, batch_size, dtype):
        # Initialize the memory to the key values.
        zero_state_batch = tf.concat(self._keys, axis=1, name='init_state')
        # Initialize relational memory to average of the two keys
        zero_state_rel = []
        # key_i = [?, hidden_size]
        for i, key_i in enumerate(self._keys):
            state_i = []
            for j, key_j in enumerate(self._keys):
                state_ij = (key_i + key_j) / 2.0
                state_i.append(state_ij)
            zero_state_rel.append(tf.stack(state_i, axis=1))
        zero_state_rel_batch = tf.stack(zero_state_rel, axis=1, name='init_rel_state')
        return (zero_state_batch,       # [?, num_blocks, block_size]
                zero_state_rel_batch)   # [?, num_blocks, num_blocks, block_size]

    ############################## Entity Network Methods ##############################
    def get_gate(self, state_j, key_j, inputs):
        """
        Implements the gate (scalar for each block). Equation E2:

        g_j <- \sigma(s_t^T h_j + s_t^T w_j)

        :param state_j: [batch_size, block_size] tensor (value memory)
        :param key_j: [batch_size, block_size] tensor (key)
        :param inputs:  [batch_size, block_size] tensor (inputs)
        :return: [batch_size] tensor (g_j)
        """
        b = tf.reduce_sum(inputs * key_j, axis=1)
        a = tf.reduce_sum(inputs * state_j, axis=1)
        return tf.sigmoid(a + b, name="gate")

    def get_candidate(self, state_j, key_j, inputs, U, V, W, U_bias):
        """
        Represents the new memory candidate that will be weighted by the
        gate value and combined with the existing memory. Equation E3:

        h_j^~ <- \phi(U h_j + V w_j + W s_t)

        :param state_j: [batch_size, block_size] tensor (value memory)
        :param key_j: [batch_size, block_size] tensor (key)
        :param inputs: [batch_size, block_size] tensor (inputs)
        :param U: [block_size, block_size] weight tensor
        :param V: [block_size, block_size] weight tensor
        :param W: [block_size, block_size] weight tensor
        :param U_bias: [block_size] bias weight tensor
        :return: [batch_size, block_size] tensor (candidate value memory)
        """
        key_V = tf.matmul(key_j, V)
        state_U = tf.matmul(state_j, U) + U_bias
        inputs_W = tf.matmul(inputs, W)
        return self._activation(state_U * inputs_W * key_V)

    ############################## RelNet Methods ##############################
    def get_gate_rel(self, gate_mi, gate_mj, rel_state_ij, inputs):
        """
        Implements the gate (scalar for each block). Equation R3:

        g^r_ij <- g^m_i g^m_j \sigma(s_t^T r_ij)

        :param gate_mi: [batch_size] tensor
        :param gate_mj: [batch_size] tensor
        :param rel_state_ij: [batch_size, block_size] tensor
        :param inputs: [batch_size, block_size] tensor
        :return: [batch_size] tensor
        """
        a = tf.reduce_sum(inputs * rel_state_ij, axis=1)
        return tf.multiply(gate_mi, gate_mj * tf.sigmoid(a), name='gate_rel')

    def get_candidate_rel(self, rel_state_ij, inputs, A, B):
        """
        Represents the new memory candidate that will be weighted by the
        gate value and combined with the existing memory. Equation R4.1:

        r_ij^~ <- \phi(A r_ij + B s_t)

        :param rel_state_ij: [batch_size, block_size] tensor
        :param inputs: [batch_size, block_size] tensor
        :param A: [block_size, block_size] weight tensor
        :param B: [block_size, block_size] weight tensor
        :return: [batch_size, block_size] tensor
        """
        state_A = tf.matmul(rel_state_ij, A)
        inputs_B = tf.matmul(inputs, B)
        return self._rel_activation(state_A * inputs_B)

    def __call__(self, inputs_tuple, state_tuple, scope=None):
        """
        Updates the hidden states of the cell with the inputs tuple.
        :param inputs_tuple: ([batch_size, hidden_size], [batch_size, hidden_size]) tensor tuple
        :param state_tuple: ([batch_size, num_blocks, block_size], [batch_size, num_blocks, num_blocks, block_size])
                            tensor tuple
        :param scope: scope
        :return: a tuple of (cell_output, hidden_state) where hidden_state is a (entity_memory, rel_memory) tuple
                entity_memory is a [batch_size, num_blocks, block_size] tensor
                rel_memory is a [batch_size, num_blocks, num_blocks, block_size] tensor
                cell_output is the same tensor as entity_memory (similar to a GRU)
        """
        # ([batch_size, hidden_size], [batch_size, hidden_size]) tensor tuple
        inputs, rel_inputs = inputs_tuple
        # block_size = hidden_size = self._num_units_per_block
        # ([batch_size, num_blocks, block_size], [batch_size, num_blocks, num_blocks, block_size]) tensor tuple
        state, rel_state = state_tuple
        with tf.variable_scope(scope or type(self).__name__, initializer=self._initializer):
            U = tf.get_variable('U', [self._num_units_per_block, self._num_units_per_block],
                                initializer=self._recurrent_initializer, dtype=self._dtype)
            U_bias = tf.get_variable('U_bias', [self._num_units_per_block], dtype=self._dtype)
            V = tf.get_variable('V', [self._num_units_per_block, self._num_units_per_block],
                                initializer=self._recurrent_initializer, dtype=self._dtype)
            W = tf.get_variable('W', [self._num_units_per_block, self._num_units_per_block],
                                initializer=self._recurrent_initializer, dtype=self._dtype)
            A = tf.get_variable('A', [self._num_units_per_block, self._num_units_per_block],
                                initializer=self._recurrent_initializer, dtype=self._dtype)
            B = tf.get_variable('B', [self._num_units_per_block, self._num_units_per_block],
                                initializer=self._recurrent_initializer, dtype=self._dtype)

            # Split the hidden state into blocks (each U, V, W, A, B are shared across blocks).
            # state = list of num_blocks tensors with shape [batch_size, block_size]
            state = tf.split(state, self._num_blocks, axis=1, name="hidden_state")

            next_states = []
            gates = []
            for j, state_j in enumerate(state):  # Hidden State (j)
                # key_j = [batch_size, block_size]
                key_j = self._keys[j]
                # gate_j = [batch_size]
                gate_j = self.get_gate(state_j, key_j, inputs)
                gates.append(gate_j)
                # candidate_j = [batch_size, block_size]
                candidate_j = self.get_candidate(state_j, key_j, inputs, U, V, W, U_bias)

                # Equation E4: h_j <- h_j + g_j * h_j^~
                # Perform an update of the hidden state (memory).
                # state_j_next = [batch_size, block_size]
                state_j_next = state_j + tf.expand_dims(gate_j, -1) * candidate_j

                if self._do_norm:
                    # Equation E5: h_j <- h_j / \norm{h_j}
                    # Forget previous memories by normalization.
                    state_j_next_norm = safe_norm(state_j_next, epsilon=1e-6, axis=-1, keep_dims=True)
                    # state_j_next = skip_gradient(state_j_next / state_j_next_norm, state_j_next)
                    state_j_next = state_j_next / state_j_next_norm

                next_states.append(state_j_next)

            # update relational memories
            # rel_states is a list of num_blocks tensors with shape [batch_size, num_blocks, block_size]
            rel_states = tf.unstack(rel_state, self._num_blocks, axis=1, name='rel_hidden_state')
            next_rel_states = []
            for i, gate_i in enumerate(gates):
                # rel_states_i is a list of num_blocks tensors with shape [batch_size, block_size]
                rel_states_i = tf.unstack(rel_states[i], self._num_blocks, axis=1)
                next_rel_states_i = []
                for j, gate_j in enumerate(gates):
                    # rel_state_ij = [batch_size, block_size]
                    rel_state_ij = rel_states_i[j]
                    # gate_ij = [batch_size]
                    gate_ij = self.get_gate_rel(gate_i, gate_j, rel_state_ij, rel_inputs)
                    # candidate_ij = [batch_size, block_size]
                    candidate_ij = self.get_candidate_rel(rel_state_ij, rel_inputs, A, B)

                    # Equation R4.2: r_ij = r_ij + g^r_ij * r_ij^~
                    # rel_state_ij_next = [batch_size, block_size]
                    rel_state_ij_next = rel_state_ij + tf.expand_dims(gate_ij, -1) * candidate_ij

                    if self._do_norm:
                        # normalize
                        rel_state_ij_next_norm = safe_norm(rel_state_ij_next, epsilon=1e-6, axis=-1, keep_dims=True)
                        # rel_state_ij_next = skip_gradient(rel_state_ij_next / rel_state_ij_next_norm, rel_state_ij_next)
                        rel_state_ij_next = rel_state_ij_next / rel_state_ij_next_norm

                    next_rel_states_i.append(rel_state_ij_next)
                next_rel_states.append(tf.stack(next_rel_states_i, axis=1))

            state_next = tf.concat(next_states, axis=1, name='hidden_state')
            rel_state_next = tf.stack(next_rel_states, axis=1, name='rel_hidden_state')

        return state_next, (state_next, rel_state_next)
