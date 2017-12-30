from __future__ import absolute_import
from __future__ import division
from __future__ import print_function

# We disable pylint because we need python3 compatibility.
from six.moves import xrange  # pylint: disable=redefined-builtin
from six.moves import zip     # pylint: disable=redefined-builtin

from tensorflow.python.framework import dtypes
from tensorflow.python.framework import ops
from tensorflow.python.ops import array_ops
from tensorflow.python.ops import control_flow_ops
from tensorflow.python.ops import embedding_ops
from tensorflow.python.ops import math_ops
from tensorflow.python.ops import nn_ops
from tensorflow.python.ops import rnn
from tensorflow.python.ops import rnn_cell
from tensorflow.python.ops import variable_scope
from tensorflow.python.util import nest
from tensorflow.python.ops import init_ops

import tensorflow as tf

def linear(args, output_size, bias, bias_start=0.0, scope=None):
    """Linear map: sum_i(args[i] * W[i]), where W[i] is a variable.

    Args:
      args: a 2D Tensor or a list of 2D, batch x n, Tensors.
      output_size: int, second dimension of W[i].
      bias: boolean, whether to add a bias term or not.
      bias_start: starting value to initialize the bias; 0 by default.
      scope: VariableScope for the created subgraph; defaults to "Linear".

    Returns:
      A 2D Tensor with shape [batch x output_size] equal to
      sum_i(args[i] * W[i]), where W[i]s are newly created matrices.

    Raises:
      ValueError: if some of the arguments has unspecified or wrong shape.
    """
    if args is None or (nest.is_sequence(args) and not args):
        raise ValueError("`args` must be specified")
    if not nest.is_sequence(args):
        args = [args]

    # Calculate the total size of arguments on dimension 1.
    total_arg_size = 0
    shapes = [a.get_shape().as_list() for a in args]
    for shape in shapes:
        if len(shape) != 2:
            raise ValueError("Linear is expecting 2D arguments: %s" % str(shapes))
        if not shape[1]:
            raise ValueError("Linear expects shape[1] of arguments: %s" % str(shapes))
        else:
            total_arg_size += shape[1]

    dtype = [a.dtype for a in args][0]

    # Now the computation.
    with variable_scope.variable_scope(scope or "Linear"):
        matrix = variable_scope.get_variable(
            "Matrix", [total_arg_size, output_size], dtype=dtype)
        if len(args) == 1:
            res = math_ops.matmul(args[0], matrix)
        else:
            res = math_ops.matmul(array_ops.concat(1, args), matrix)
        if not bias:
            return res
        bias_term = variable_scope.get_variable(
            "Bias", [output_size],
            dtype=dtype,
            initializer=init_ops.constant_initializer(
                bias_start, dtype=dtype))
    return res + bias_term

def gather_cols(params, indices, name=None):
    """Gather columns of a 2D tensor.

    Args:
        params: A 2D tensor.
        indices: A 1D tensor. Must be one of the following types: ``int32``, ``int64``.
        name: A name for the operation (optional).

    Returns:
        A 2D Tensor. Has the same type as ``params``.
    """
    with tf.op_scope([params, indices], name, "gather_cols") as scope:
        # Check input
        params = tf.convert_to_tensor(params, name="params")
        indices = tf.convert_to_tensor(indices, name="indices")
        try:
            params.get_shape().assert_has_rank(2)
        except ValueError:
            raise ValueError('\'params\' must be 2D.')
        try:
            indices.get_shape().assert_has_rank(1)
        except ValueError:
            raise ValueError('\'params\' must be 1D.')

        # Define op
        p_shape = tf.shape(params)
        p_flat = tf.reshape(params, [-1])
        i_flat = tf.reshape(tf.reshape(tf.range(0, p_shape[0]) * p_shape[1],
                                       [-1, 1]) + indices, [-1])
        return tf.reshape(tf.gather(p_flat, i_flat),
                          [p_shape[0], -1])

def coverage_attention_decoder(decoder_inputs,
                      initial_state,
                      encoder_output_states,
                      cell,
                      output_size=None,
                      num_heads=1,
                      loop_function=None,
                      dtype=dtypes.float32,
                      scope=None,
                      initial_state_attention=False):
    """RNN decoder with attention for the sequence-to-sequence model.
    In this context "attention" means that, during decoding, the RNN can look up
    information in the additional tensor attention_states, and it does this by
    focusing on a few entries from the tensor. This model has proven to yield
    especially good results in a number of sequence-to-sequence tasks. This
    implementation is based on http://arxiv.org/abs/1412.7449 (see below for
    details). It is recommended for complex sequence-to-sequence tasks.
    Args:
      decoder_inputs: A list of 2D Tensors [batch_size x input_size].
      initial_state: 2D Tensor [batch_size x cell.state_size].
      encoder_output_states: 3D Tensor [batch_size x attn_length x attn_size].
      cell: rnn_cell.RNNCell defining the cell function and size.
      output_size: Size of the output vectors; if None, we use cell.output_size.
      num_heads: Number of attention heads that read from attention_states.
      loop_function: If not None, this function will be applied to i-th output
        in order to generate i+1-th input, and decoder_inputs will be ignored,
        except for the first element ("GO" symbol). This can be used for decoding,
        but also for training to emulate http://arxiv.org/abs/1506.03099.
        Signature -- loop_function(prev, i) = next
          * prev is a 2D Tensor of shape [batch_size x output_size],
          * i is an integer, the step number (when advanced control is needed),
          * next is a 2D Tensor of shape [batch_size x input_size].
      dtype: The dtype to use for the RNN initial state (default: tf.float32).
      scope: VariableScope for the created subgraph; default: "attention_decoder".
      initial_state_attention: If False (default), initial attentions are zero.
        If True, initialize the attentions from the initial state and attention
        states -- useful when we wish to resume decoding from a previously
        stored decoder state and attention states.
    Returns:
      A tuple of the form (outputs, state), where:
        outputs: A list of the same length as decoder_inputs of 2D Tensors of
          shape [batch_size x output_size]. These represent the generated outputs.
          Output i is computed from input i (which is either the i-th element
          of decoder_inputs or loop_function(output {i-1}, i)) as follows.
          First, we run the cell on a combination of the input and previous
          attention masks:
            cell_output, new_state = cell(linear(input, prev_attn), prev_state).
          Then, we calculate new attention masks:
            new_attn = softmax(V^T * tanh(W * attention_states + U * new_state))
          and then we calculate the output:
            output = linear(cell_output, new_attn).
        state: The state of each decoder cell the final time-step.
          It is a 2D Tensor of shape [batch_size x cell.state_size].
    Raises:
      ValueError: when num_heads is not positive, there are no inputs, shapes
        of attention_states are not set, or input size cannot be inferred
        from the input.
    """
    if not decoder_inputs:
        raise ValueError("Must provide at least 1 input to attention decoder.")
    if num_heads < 1:
        raise ValueError("With less than 1 heads, use a non-attention decoder.")
    if not encoder_output_states.get_shape()[1:2].is_fully_defined():
        raise ValueError("Shape[1] and [2] of encoder_output_states must be known: %s"
                         % encoder_output_states.get_shape())
    if output_size is None:
        output_size = cell.output_size

    with variable_scope.variable_scope(
                    scope or "coverage_attention_decoder") as scope:
        batch_size = array_ops.shape(decoder_inputs[0])[0]  # Needed for reshaping.
        attn_length = encoder_output_states.get_shape()[1].value
        attn_size = encoder_output_states.get_shape()[2].value

        #print("Batch Size:", batch_size)
        #print("Attn. Length:", attn_length)
        #print("Attn./Coverage Vector Size:", attn_size)
        #print("Output Size:", output_size)

        attention_vec_size = attn_size  # Size of query vectors for attention.
        coverage_vec_size = attn_size
        # coverage is an RNN!
        # C[i][j] = GRU(C[i-1][j] .. a[i][j] .. t[i-1] .. h[j])
        # h[j] = encoder output j
        # a[i][j] = attention between decoder output i and encoder output j
        # t[i - 1] = previous decoder state = hidden_features[a]




        # To calculate W1 * h_t we use a 1-by-1 convolution, need to reshape before.
        hidden = array_ops.reshape(
            encoder_output_states, [-1, attn_length, 1, attn_size])
        hidden_features = []
        coverage_grus = []
        v = []
        for head in xrange(num_heads):
            k = variable_scope.get_variable("AttnW_%d" % head,
                                            [1, 1, attention_vec_size, attention_vec_size])
            hidden_features.append(nn_ops.conv2d(hidden, k, [1, 1, 1, 1], "SAME"))
            v.append(
                variable_scope.get_variable("AttnV_%d" % head, [attention_vec_size]))
            with variable_scope.variable_scope("Attention_%d" % head):
                coverage_grus.append(rnn_cell.GRUCell(coverage_vec_size))

        decoder_state = initial_state

        def attention(query, prev_coverage, prev_output, i):
            #print("Query Tensor:",  query)
            #print("Prev. Output Tensor:",  prev_output)
            #print("Prev. Coverage Tensor:",  prev_coverage)
            # query == h_i
            """Put attention masks on hidden using hidden_features and query."""
            ds = []  # Results of attention reads will be stored here.
            coverages = [] # Results of coverage computation will be stored here.
            if nest.is_sequence(query):  # If the query is a tuple, flatten it.
                query_list = nest.flatten(query)
                for q in query_list:  # Check that ndims == 2 if specified.
                    ndims = q.get_shape().ndims
                    if ndims:
                        assert ndims == 2
                query = array_ops.concat(1, query_list)
            for head in xrange(num_heads):
                l = variable_scope.get_variable("CvrgW_%d" % head,
                                                [1, 1, coverage_vec_size, coverage_vec_size])
                with variable_scope.variable_scope("Attention_%d" % head):
                    y = linear(query, attention_vec_size, True)
                    y = array_ops.reshape(y, [-1, 1, 1, attention_vec_size])
                    hidden_prev_coverage = array_ops.reshape(
                        prev_coverage[head], [-1, attn_length, 1, coverage_vec_size])
                    coverage_features = nn_ops.conv2d(hidden_prev_coverage, l, [1, 1, 1, 1], "SAME")

                    # Attention mask is a softmax of v^T * tanh(...).h
                    t1 = hidden_features[head] + y + coverage_features
                    #tf.Print(t1, [t1], "Attention: tanh input:")
                    #print("Head + y + prev_coverage:", hidden_features[head], y, coverage_features, t1)
                    s = math_ops.reduce_sum(
                        # v[a] * math_ops.tanh(hidden_features[a] + y), [2, 3])
                        v[head] * math_ops.tanh(t1), [2, 3])
                    # a^t
                    a = nn_ops.softmax(s)
                    #tf.Print(a, [a], "Attention: attention vector:")
                    #print("Attention Tensor:",  a)

                    #print(array_ops.expand_dims(a, -1))
                    #print(encoder_output_states)
                    #print(prev_output)

                    #tf.histogram_summary("Attention_%d" % i, a)

                    # old coverage was an RNN over the input sequence -- waf
                    # A = [ batch x input length]
                    # A = [ batch x input length x 1]
                    a_3d = array_ops.expand_dims(a, -1)
                    tf.stop_gradient(a_3d)
                    prev_3d = tf.pack([prev_output for _ in xrange(attn_length)], axis=1)
                    tf.stop_gradient(prev_3d)
                    gru_inputs = tf.concat(2, [a_3d, encoder_output_states, prev_3d])
                    tf.stop_gradient(gru_inputs)
                    time_order_gru_inputs = tf.transpose(gru_inputs, [1, 0, 2])
                    tf.stop_gradient(time_order_gru_inputs)
                    time_order_prev_coverage = tf.transpose(prev_coverage[head], [1, 0, 2])


                    # a = [ batch x encoder length ]
                    # prev_output = [ batch x output size ]
                    # encoder_output_states = [ batch x encoder length x encoder size ]

                    #head_coverages, _ = coverage_grus[head](tf.concat(1, [a, prev_output, encoder_flat]), prev_coverage[head])


#                    print("GRU INPUTS")
#                    print(time_order_gru_inputs)
#                    print("PREV. COVERAGE")
#                    print(time_order_prev_coverage)

                    def get_coverage(t):
                        coverage, _ = coverage_grus[head](t[0], t[1])
                        return coverage

                    head_coverages = tf.map_fn(get_coverage, (time_order_gru_inputs,
                                                              time_order_prev_coverage),
                                               swap_memory=True,
                                               dtype=tf.float32)

                    print("Head Coverages:")
                    print(head_coverages)
                    head_coverages = tf.transpose(head_coverages, [1, 0, 2])

                    #_, head_coverages = coverage_grus[head](gru_inputs, prev_coverage[head])
                    #                                    dtype=tf.float32,
                    #                                    parallel_iterations=1)

                    #tf.histogram_summary("Coverage_%d" % i, head_coverages)

                    #def get_coverage((input, state)):
                    #    input = tf.concat(1, [input, prev_output])
                    #    cvg, _ = coverage_grus[head](input, state)
                    #    return (i, cvg)

                    #gru_inputs = ? x L x (200 + 1)
                    #gru_inputs = tf.transpose(tf.concat(2, [array_ops.expand_dims(a, -1),
                    #                                        encoder_output_states]), perm=[1, 0, 2])
                    #gru_state = tf.transpose(prev_coverage[head], perm=[1, 0, 2])

                    #with variable_scope.variable_scope(variable_scope.get_variable_scope(),
                    #                                   reuse=None):
                    #    initial_coverage, _ = coverage_grus[head](tf.concat(1, [gru_inputs[0, :, :],
                    #                                                            prev_output]),
                    #                                              gru_state[0, :, :])

                    #initial_coverage = tf.expand_dims(initial_coverage, 0)
                    #print("GRU Inputs:", gru_inputs)
                    #print("GRU State:", gru_state)
                    #print("Initial Coverage:", initial_coverage)
                    #with variable_scope.variable_scope(variable_scope.get_variable_scope(),
                    #                                   reuse=True):
                    #    head_coverages = tf.map_fn(get_coverage, (gru_inputs[1:attn_length, :, :],
                    #                                              gru_state[1:attn_length, :, :]),
                    #                               dtype=tf.float32, back_prop=False)
                    #print("Tail Coverages:", head_coverages)
                    #head_coverages = tf.concat(0, [initial_coverage, head_coverages])
                    #print("Head Coverages:", head_coverages)
                    #head_coverages = tf.transpose(head_coverages, perm=[1, 0, 2])
                    #print("Final Head Coverages:", head_coverages)

#                    # a = (? x L)
                    # h = (? x L x 200)
                    # prev = (? x L x 200)
                    # Compute coverage against each encoder output j
                    #head_coverages = []
                    #for j in xrange(attn_length):
                        #h = encoder_output_states[:, j, :]
                        #print("H_j:", h)
                        #a_j = tf.reshape(a[:, j], [batch_size, 1]) #gather_cols(a, [j])
                        #print("a_ij:", a_j, a_j.get_shape())
                        #with variable_scope.variable_scope(variable_scope.get_variable_scope(),
                        #                                   reuse=True if j > 0 else None):
                            #gru_input = tf.concat(1, [a_j, h, prev_output])
                            #print("GRU Input:", gru_input)
                            #coverage, _ = coverage_grus[head](gru_inputs[j], prev_coverage[head][:, j, :])
                            #print("Coverage[%d]:" % j, coverage)
                            #head_coverages.append(coverage)

                    # Goal: axis 0 = batch, axis 1 = time, axis 2 = dimension
                    #head_coverages = tf.pack(head_coverages, axis=1)
                    #tf.Print(head_coverages, [head_coverages], "Attention: coverage:")
                    coverages.append(head_coverages)

                    # Now calculate coverage_i
                    #gru_input = tf.concat(1, [a_i, query, prev_output])
                    #print("GRU Input:", gru_input)
                    #print("GRU State:", prev_coverage[head])
                    #print("GRU Units:", coverage_size)
                    #coverage, _ = coverage_grus[head](gru_input, prev_coverage[head])

                    #print("Coverage:", coverage)
                    # Now calculate the attention-weighted vector d_t.
                    d = math_ops.reduce_sum(
                        array_ops.reshape(a, [-1, attn_length, 1, 1]) * hidden,
                        [1, 2])
                    #print("d:", d)
                    ds.append(array_ops.reshape(d, [-1, attn_size]))
                    #print("ds:", ds)
            return ds, coverages

        initial_output = array_ops.zeros([batch_size, output_size], dtype=dtype)
        outputs = []
        prev = None
        batch_attn_size = array_ops.pack([batch_size, attention_vec_size])
        attns = [array_ops.zeros(batch_attn_size, dtype=dtype)
                 for _ in xrange(num_heads)]
        cvrgs = [array_ops.zeros([batch_size, attn_length, coverage_vec_size], dtype=dtype)
                 for _ in xrange(num_heads)]
        for a in attns:  # Ensure the second shape of attention vectors is set.
            a.set_shape([None, attention_vec_size])
        for c in cvrgs:
            c.set_shape([None, attn_length, coverage_vec_size])
        if initial_state_attention:
            attns = attention(initial_state, cvrgs, initial_output)
        for i, decoder_input in enumerate(decoder_inputs):
            if i > 0:
                variable_scope.get_variable_scope().reuse_variables()
            # If loop_function is set, we use it instead of decoder_inputs.
            if loop_function is not None and prev is not None:
                with variable_scope.variable_scope("loop_function", reuse=True):
                    decoder_input = loop_function(prev, i)
            # Merge input and previous attentions into one vector of the right size.
            input_size = decoder_input.get_shape().with_rank(2)[1]
            if input_size.value is None:
                raise ValueError("Could not infer input size from input: %s" % decoder_input.name)
            x = linear([decoder_input] + attns, input_size, True)

            # Run the RNN.
            decoder_output, decoder_state = cell(x, decoder_state)

            # Run the attention mechanism.
            if i == 0:
                if initial_state_attention:
                    with variable_scope.variable_scope(variable_scope.get_variable_scope(),
                                                       reuse=True):
                        attns, cvrgs = attention(decoder_state, cvrgs, initial_output, i)
                else:
                    attns, cvrgs = attention(decoder_state, cvrgs, initial_output, i)
            else:
                attns, cvrgs = attention(decoder_state, cvrgs, outputs[i - 1], i)

            with variable_scope.variable_scope("AttnOutputProjection"):
                output = linear([decoder_output] + attns, output_size, True)
            if loop_function is not None:
                prev = output

#            tf.histogram_summary("Output_%d" % i, output)
            tf.Print(output, [output], "Output %i" % i)
            outputs.append(output)
    return outputs, decoder_state

def embedding_coverage_attention_seq2seq(encoder_inputs, decoder_inputs, cell,
                                num_encoder_symbols, num_decoder_symbols,
                                embedding_size,
                                num_heads=1, output_projection=None,
                                feed_previous=False, dtype=dtypes.float32,
                                scope=None, initial_state_attention=False,
                                         ):
    """Embedding sequence-to-sequence model with attention.

    This model first embeds encoder_inputs by a newly created embedding (of shape
    [num_encoder_symbols x input_size]). Then it runs an RNN to encode
    embedded encoder_inputs into a state vector. It keeps the outputs of this
    RNN at every step to use for attention later. Next, it embeds decoder_inputs
    by another newly created embedding (of shape [num_decoder_symbols x
    input_size]). Then it runs attention decoder, initialized with the last
    encoder state, on embedded decoder_inputs and attending to encoder outputs.

    Args:
      encoder_inputs: A list of 1D int32 Tensors of shape [batch_size].
      decoder_inputs: A list of 1D int32 Tensors of shape [batch_size].
      cell: rnn_cell.RNNCell defining the cell function and size.
      num_encoder_symbols: Integer; number of symbols on the encoder side.
      num_decoder_symbols: Integer; number of symbols on the decoder side.
      embedding_size: Integer, the length of the embedding vector for each symbol.
      num_heads: Number of attention heads that read from attention_states.
      output_projection: None or a pair (W, B) of output projection weights and
        biases; W has shape [output_size x num_decoder_symbols] and B has
        shape [num_decoder_symbols]; if provided and feed_previous=True, each
        fed previous output will first be multiplied by W and added B.
      feed_previous: Boolean or scalar Boolean Tensor; if True, only the first
        of decoder_inputs will be used (the "GO" symbol), and all other decoder
        inputs will be taken from previous outputs (as in embedding_rnn_decoder).
        If False, decoder_inputs are used as given (the standard decoder case).
      dtype: The dtype of the initial RNN state (default: tf.float32).
      scope: VariableScope for the created subgraph; defaults to
        "embedding_attention_seq2seq".
      initial_state_attention: If False (default), initial attentions are zero.
        If True, initialize the attentions from the initial state and attention
        states.

    Returns:
      A tuple of the form (outputs, state), where:
        outputs: A list of the same length as decoder_inputs of 2D Tensors with
          shape [batch_size x num_decoder_symbols] containing the generated
          outputs.
        state: The state of each decoder cell at the final time-step.
          It is a 2D Tensor of shape [batch_size x cell.state_size].
    """
    with variable_scope.variable_scope(scope or "embedding_coverage_attention_seq2seq"):
        # Encoder.
        encoder_cell = rnn_cell.EmbeddingWrapper(
            cell, embedding_classes=num_encoder_symbols,
            embedding_size=embedding_size)
        encoder_outputs, encoder_state = rnn.rnn(
            encoder_cell, encoder_inputs, dtype=dtype)

        # First calculate a concatenation of encoder outputs to put attention on.
        top_states = [array_ops.reshape(e, [-1, 1, cell.output_size])
                      for e in encoder_outputs]
        attention_states = array_ops.concat(1, top_states)

        # Decoder.
        output_size = None
        if output_projection is None:
            cell = rnn_cell.OutputProjectionWrapper(cell, num_decoder_symbols)
            output_size = num_decoder_symbols

        if isinstance(feed_previous, bool):
            return embedding_coverage_attention_decoder(
                decoder_inputs, encoder_state, attention_states, cell,
                num_decoder_symbols, embedding_size, num_heads=num_heads,
                output_size=output_size, output_projection=output_projection,
                feed_previous=feed_previous,
                initial_state_attention=initial_state_attention)

        # If feed_previous is a Tensor, we construct 2 graphs and use cond.
        def decoder(feed_previous_bool):
            reuse = None if feed_previous_bool else True
            with variable_scope.variable_scope(variable_scope.get_variable_scope(),
                                               reuse=reuse):
                outputs, state = embedding_coverage_attention_decoder(
                    decoder_inputs, encoder_state, attention_states, cell,
                    num_decoder_symbols, embedding_size, num_heads=num_heads,
                    output_size=output_size, output_projection=output_projection,
                    feed_previous=feed_previous_bool,
                    update_embedding_for_previous=False,
                    initial_state_attention=initial_state_attention)
                state_list = [state]
                if nest.is_sequence(state):
                    state_list = nest.flatten(state)
                return outputs + state_list

        outputs_and_state = control_flow_ops.cond(feed_previous,
                                                  lambda: decoder(True),
                                                  lambda: decoder(False))
        outputs_len = len(decoder_inputs)  # Outputs length same as decoder inputs.
        state_list = outputs_and_state[outputs_len:]
        state = state_list[0]
        if nest.is_sequence(encoder_state):
            state = nest.pack_sequence_as(structure=encoder_state,
                                          flat_sequence=state_list)
        return outputs_and_state[:outputs_len], state


def embedding_coverage_attention_decoder(decoder_inputs, initial_state, attention_states,
                                cell, num_symbols, embedding_size, num_heads=1,
                                output_size=None, output_projection=None,
                                feed_previous=False,
                                update_embedding_for_previous=True,
                                dtype=dtypes.float32, scope=None,
                                initial_state_attention=False):
    """RNN decoder with embedding and attention and a pure-decoding option.

    Args:
      decoder_inputs: A list of 1D batch-sized int32 Tensors (decoder inputs).
      initial_state: 2D Tensor [batch_size x cell.state_size].
      attention_states: 3D Tensor [batch_size x attn_length x attn_size].
      cell: rnn_cell.RNNCell defining the cell function.
      num_symbols: Integer, how many symbols come into the embedding.
      embedding_size: Integer, the length of the embedding vector for each symbol.
      num_heads: Number of attention heads that read from attention_states.
      output_size: Size of the output vectors; if None, use output_size.
      output_projection: None or a pair (W, B) of output projection weights and
        biases; W has shape [output_size x num_symbols] and B has shape
        [num_symbols]; if provided and feed_previous=True, each fed previous
        output will first be multiplied by W and added B.
      feed_previous: Boolean; if True, only the first of decoder_inputs will be
        used (the "GO" symbol), and all other decoder inputs will be generated by:
          next = embedding_lookup(embedding, argmax(previous_output)),
        In effect, this implements a greedy decoder. It can also be used
        during training to emulate http://arxiv.org/abs/1506.03099.
        If False, decoder_inputs are used as given (the standard decoder case).
      update_embedding_for_previous: Boolean; if False and feed_previous=True,
        only the embedding for the first symbol of decoder_inputs (the "GO"
        symbol) will be updated by back propagation. Embeddings for the symbols
        generated from the decoder itself remain unchanged. This parameter has
        no effect if feed_previous=False.
      dtype: The dtype to use for the RNN initial states (default: tf.float32).
      scope: VariableScope for the created subgraph; defaults to
        "embedding_attention_decoder".
      initial_state_attention: If False (default), initial attentions are zero.
        If True, initialize the attentions from the initial state and attention
        states -- useful when we wish to resume decoding from a previously
        stored decoder state and attention states.

    Returns:
      A tuple of the form (outputs, state), where:
        outputs: A list of the same length as decoder_inputs of 2D Tensors with
          shape [batch_size x output_size] containing the generated outputs.
        state: The state of each decoder cell at the final time-step.
          It is a 2D Tensor of shape [batch_size x cell.state_size].

    Raises:
      ValueError: When output_projection has the wrong shape.
    """
    if output_size is None:
        output_size = cell.output_size
    if output_projection is not None:
        proj_biases = ops.convert_to_tensor(output_projection[1], dtype=dtype)
        proj_biases.get_shape().assert_is_compatible_with([num_symbols])

    with variable_scope.variable_scope(scope or "embedding_attention_decoder"):
        embedding = variable_scope.get_variable("embedding",
                                                [num_symbols, embedding_size])
        loop_function = tf.nn.seq2seq._extract_argmax_and_embed(
            embedding, output_projection,
            update_embedding_for_previous) if feed_previous else None
        emb_inp = [
            embedding_ops.embedding_lookup(embedding, i) for i in decoder_inputs]
        return coverage_attention_decoder(
            emb_inp, initial_state, attention_states, cell, output_size=output_size,
            num_heads=num_heads, loop_function=loop_function,
            initial_state_attention=initial_state_attention)