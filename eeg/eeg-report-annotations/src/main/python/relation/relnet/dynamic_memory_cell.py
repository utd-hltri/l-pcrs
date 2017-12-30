"Define a dynamic memory cell."
from __future__ import absolute_import
from __future__ import print_function
from __future__ import division

from . import model_ops

import tensorflow as tf
from tensorflow.contrib.rnn import RNNCell


class DynamicMemoryCell(RNNCell):
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
                 dtype=tf.float32):
        self._num_blocks = num_blocks  # M
        self._num_units_per_block = num_units_per_block  # d
        self._keys = keys  # list of [?, hidden_size] tensors where ? will be batch_size
        # self._relational_memory = relational_memory
        self._activation = activation  # \phi
        self._initializer = initializer
        self._recurrent_initializer = recurrent_initializer
        self._dtype = dtype

    @property
    def state_size(self):
        "Return the total state size of the cell, across all blocks."
        return self._num_blocks * self._num_units_per_block

    @property
    def output_size(self):
        "Return the total output size of the cell, across all blocks."
        # return tf.TensorShape([self._num_blocks, self._num_units_per_block])
        return self._num_blocks * self._num_units_per_block

    def zero_state(self, batch_size, dtype=None):
        # Initialize the memory to the key values.
        zero_state_batch = tf.concat(self._keys, axis=1, name='init_state')
        return zero_state_batch

    def get_gate(self, state_j, key_j, inputs):
        """
        Implements the gate (scalar for each block). Equation E2:

        g_j <- \sigma(s_t^T h_j + s_t^T w_j)

        :param state_j: [batch_size, block_size] tensor (value memory)
        :param key_j: [batch_size, block_size] tensor (key)
        :param inputs:  [batch_size, block_size] tensor (inputs)
        :return: [batch_size] tensor (g_j)
        """
        b = tf.reduce_sum(inputs * key_j, axis=1)    # [batch_size]
        a = tf.reduce_sum(inputs * state_j, axis=1)  # [batch_size]
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
        print('inputs')
        print(inputs_tuple)
        print('states')
        print(state_tuple)
        inputs = inputs_tuple
        # block_size = hidden_size = self._num_units_per_block
        # ([batch_size, num_blocks, block_size], [batch_size, num_blocks, num_blocks, block_size]) tensor tuple
        state = state_tuple
        with tf.variable_scope(scope or type(self).__name__, initializer=self._initializer):
            U = tf.get_variable('U', [self._num_units_per_block, self._num_units_per_block],
                                initializer=self._recurrent_initializer, dtype=self._dtype)
            U_bias = tf.get_variable('U_bias', [self._num_units_per_block], dtype=self._dtype)
            V = tf.get_variable('V', [self._num_units_per_block, self._num_units_per_block],
                                initializer=self._recurrent_initializer, dtype=self._dtype)
            W = tf.get_variable('W', [self._num_units_per_block, self._num_units_per_block],
                                initializer=self._recurrent_initializer, dtype=self._dtype)

            # Split the hidden state into blocks (each U, V, W, A, B are shared across blocks).
            # state = list of num_blocks tensors with shape [batch_size, block_size]
            print('States: %s' % state)
            state = tf.split(state, self._num_blocks, axis=1, name="hidden_state")

            next_states = []
            for j, state_j in enumerate(state):  # Hidden State (j)
                # print('state: %s' % state_j)
                # key_j = [batch_size, block_size]
                key_j = self._keys[j]
                # gate_j = [batch_size]
                gate_j = self.get_gate(state_j, key_j, inputs)
                # print('gate: %s' % gate_j)
                # candidate_j = [batch_size, block_size]
                candidate_j = self.get_candidate(state_j, key_j, inputs, U, V, W, U_bias)
                # print('candidate: %s' % candidate_j)

                # Equation E4: h_j <- h_j + g_j * h_j^~
                # Perform an update of the hidden state (memory).
                # state_j_next = [batch_size, block_size]
                state_j_next = state_j + tf.expand_dims(gate_j, -1) * candidate_j
                # print('next_state: %s' % state_j_next)

                # Equation E5: h_j <- h_j / \norm{h_j}
                # Forget previous memories by normalization.
                # state_j_next_norm = tf.norm(
                #     tensor=state_j_next,
                #     ord='euclidean',
                #     axis=-1,
                #     keep_dims=True)
                state_j_next_norm = model_ops.safe_norm(state_j_next, axis=-1, keep_dims=True)
                state_j_next_norm = model_ops.is_nan(state_j_next_norm, "NaN norm")
                # state_j_next_norm = tf.where(
                #     tf.greater(state_j_next_norm, 0.0001),
                #     state_j_next_norm,
                #     tf.ones_like(state_j_next_norm))
                state_j_next = state_j_next / state_j_next_norm
                state_j_next = model_ops.is_nan(state_j_next, 'NaN normed next state')
                # print('next_state_normed: %s' % state_j_next)

                next_states.append(state_j_next)

            state_next = tf.concat(next_states, axis=1, name='hidden_state')
            # print('final state: %s' % state_next)
            # outputs = tf.concat(next_states, axis=1, name='outputs')

        return state_next, state_next


def dmc_output_module(
    last_state,  # ([batch_size, num_blocks * block_size], [batch_size, num_blocks, num_blocks, block_size]) tuple
    entity_indexes,  # [batch_size, num_blocks, num_blocks] mask tensor. 1 element of each num_blocks dim set
    num_blocks,
    output_size,  # number of labels
    activation=tf.nn.relu,
    initializer=None,
    scope=None,
    dtype=tf.float32):
    with tf.variable_scope(scope, 'Output', initializer=initializer):
        ent_state = last_state
        # ent_state = [batch_size, num_blocks, block_size]
        ent_state = tf.stack(tf.split(ent_state, num_blocks, axis=1), axis=1)
        _, _, block_size = ent_state.get_shape().as_list()
        # C converts the concatenated [entity i memory, entity j memory, ij rel_memory] memory vectors into a single
        # block_size vector
        C = tf.get_variable('C', [2 * block_size, block_size], dtype=dtype)

        full_memory = []
        # ent_memories is a list of num_blocks tensors with shape [batch_size, block_size]
        ent_memories = tf.unstack(ent_state, num_blocks, axis=1)
        for i, memory_i in enumerate(ent_memories):
            full_memory_i = []
            for j, memory_j in enumerate(ent_memories):
                full_memory_i.append(tf.matmul(tf.concat([memory_i, memory_j], 1), C))
            full_memory.append(tf.stack(full_memory_i, axis=1))
        # full_memory = p = [batch_size, num_blocks, num_blocks, block_size] tensor
        full_memory = tf.stack(full_memory, axis=1)

        # select relational memory for two related entities
        # [batch_size, block_size]
        query = tf.boolean_mask(full_memory, tf.cast(entity_indexes, tf.bool))
        print('query: %s' % query)

        Z = tf.get_variable('Z', [block_size, output_size], dtype=dtype)
        y = tf.nn.relu(tf.matmul(query, Z, name='logits'))
        return y
