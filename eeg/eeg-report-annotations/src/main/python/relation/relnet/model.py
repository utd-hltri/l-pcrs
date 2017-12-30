"""
Define the recurrent entity network model.
"""
from __future__ import absolute_import
from __future__ import print_function
from __future__ import division

from functools import partial
import time

import tensorflow as tf
from tensorflow.contrib.learn import ModelFnOps, ModeKeys

from .dynamic_relational_memory_cell import DynamicRelationalMemoryCell
from .dynamic_memory_cell import DynamicMemoryCell, dmc_output_module
from .model_ops import cyclic_learning_rate, \
                       get_sequence_length, \
                       count_parameters, \
                       prelu

OPTIMIZER_SUMMARIES = [
    "learning_rate",
    "loss",
    "gradients",
    "gradient_norm",
]

DTYPE = tf.float32


def get_input_encoding(inputs, initializer=None, scope=None):
    """
    Implementation of the learned multiplicative mask from Section 2.1, Equation 1.
    This module is also described in [End-To-End Memory Networks](https://arxiv.org/abs/1502.01852)
    as Position Encoding (PE). The mask allows the ordering of words in a sentence to affect the
    encoding.
    """
    with tf.variable_scope(scope, 'Encoding', initializer=initializer):
        _, _, max_sentence_length, embedding_size = inputs.get_shape().as_list()
        positional_mask = tf.get_variable(
            name='positional_mask',
            shape=[max_sentence_length, embedding_size],
            dtype=DTYPE)
        encoded_input = tf.reduce_sum(inputs * positional_mask, axis=2)
        return encoded_input


def simple_output_module(
        last_state,    # ([batch_size, num_blocks * block_size], [batch_size, num_blocks, num_blocks, block_size]) tuple
        entity_indexes,  # [batch_size, num_blocks, num_blocks] mask tensor. 1 element of each num_blocks dim set
        num_blocks,
        output_size,   # number of labels
        activation=tf.nn.relu,
        initializer=None,
        scope=None):
    with tf.variable_scope(scope, 'Output', initializer=initializer):
        ent_state, rel_state = last_state
        # ent_state = [batch_size, num_blocks, block_size]
        ent_state = tf.stack(tf.split(ent_state, num_blocks, axis=1), axis=1)
        _, _, block_size = ent_state.get_shape().as_list()
        # C converts the concatenated [entity i memory, entity j memory, ij rel_memory] memory vectors into a single
        # block_size vector
        C = tf.get_variable('C', [3 * block_size, block_size], dtype=DTYPE)

        # rel_memories is a list of num_blocks lists of num_blocks tensors with shape [batch_size, block_size]
        # rel_memories[i][j] is a tensor with shape [batch_size, block_size] representing the relational memory r_ij
        rel_memories = [tf.unstack(rel_memory_i, num_blocks, axis=1)
                        for rel_memory_i in tf.unstack(rel_state, num_blocks, axis=1)]
        full_memory = []
        # ent_memories is a list of num_blocks tensors with shape [batch_size, block_size]
        ent_memories = tf.unstack(ent_state, num_blocks, axis=1)
        for i, memory_i in enumerate(ent_memories):
            full_memory_i = []
            for j, memory_j in enumerate(ent_memories):
                full_memory_i.append(tf.matmul(tf.concat([memory_i, memory_j, rel_memories[i][j]], 1), C))
            full_memory.append(tf.stack(full_memory_i, axis=1))
        # full_memory = p = [batch_size, num_blocks, num_blocks, block_size] tensor
        full_memory = tf.stack(full_memory, axis=1)

        # select relational memory for two related entities
        # [batch_size, block_size]
        query = tf.boolean_mask(full_memory, tf.cast(entity_indexes, tf.bool))
        print('query: %s' % query)

        Z = tf.get_variable('Z', [block_size, output_size], dtype=DTYPE)
        y = tf.nn.relu(tf.matmul(query, Z, name='logits'))
        return y


def get_output_module(
        last_state,    # ([batch_size, num_blocks * block_size], [batch_size, num_blocks, num_blocks, block_size]) tuple
        entity_indexes,  # [batch_size, num_blocks] mask tensor. Exactly two elements of each batch should be 1.
        num_blocks,
        output_size,   # number of labels
        activation=tf.nn.relu,
        initializer=None,
        scope=None):
    """
    Implementation of Output Module in section 2 of the RelNet paper.
    """
    with tf.variable_scope(scope, 'Output', initializer=initializer):
        ent_state, rel_state = last_state
        # ent_state = [batch_size, num_blocks, block_size]
        ent_state = tf.stack(tf.split(ent_state, num_blocks, axis=1), axis=1)
        _, _, block_size = ent_state.get_shape().as_list()
        # C converts the concatenated [entity i memory, entity j memory, ij rel_memory] memory vectors into a single
        # block_size vector
        C = tf.get_variable('C', [3 * block_size, block_size], dtype=DTYPE)

        # rel_memories is a list of num_blocks lists of num_blocks tensors with shape [batch_size, block_size]
        # rel_memories[i][j] is a tensor with shape [batch_size, block_size] representing the relational memory r_ij
        rel_memories = [tf.unstack(rel_memory_i, num_blocks, axis=1)
                        for rel_memory_i in tf.unstack(rel_state, num_blocks, axis=1)]
        full_memory = []
        # ent_memories is a list of num_blocks tensors with shape [batch_size, block_size]
        ent_memories = tf.unstack(ent_state, num_blocks, axis=1)
        for i, memory_i in enumerate(ent_memories):
            full_memory_i = []
            for j, memory_j in enumerate(ent_memories):
                full_memory_i.append(tf.matmul(tf.concat([memory_i, memory_j, rel_memories[i][j]], 1), C))
            full_memory.append(tf.stack(full_memory_i, axis=1))
        # full_memory = p = [batch_size, num_blocks, num_blocks, block_size] tensor
        full_memory = tf.stack(full_memory, axis=1)

        # select the two related entities
        query = tf.boolean_mask(ent_state, tf.cast(entity_indexes, tf.bool))
        query = tf.stack(tf.split(query, 2, axis=0, num=2), axis=1)
        # query = ent_state * tf.expand_dims(tf.cast(entity_indexes, tf.float32), axis=-1)

        # average the two remaining entity memories together
        # query = [batch_size, 1, block_size]
        query = tf.reduce_sum(query, axis=1, keep_dims=True) / 2.0
        # TODO: learn a combination of the two remaining entity memories together
        # # query = [batch_size, 1, block_size]
        # Q = tf.get_variable('Q', [])

        # Use the encoded_query to attend over memories
        # (hidden states of dynamic last_state cell blocks)
        # alignments = [batch_size, num_blocks, num_blocks]
        alignments = tf.reduce_sum(full_memory * tf.expand_dims(query, axis=1), axis=-1)

        # Subtract max for numerical stability (softmax is shift invariant)
        alignments_max = tf.reduce_max(alignments, axis=-1, keep_dims=True)
        alignments = tf.nn.softmax(alignments - alignments_max)
        # alignments = [batch_size, num_blocks, num_blocks, 1]
        alignments = tf.expand_dims(alignments, axis=-1)

        # Weight memories by attention vectors
        # attention = u = [batch_size, block_size]
        attention = tf.reduce_sum(tf.reduce_sum(full_memory * alignments, axis=2), axis=1)

        # Z acts as the decoder matrix to convert from internal state to the output vocabulary size
        Z = tf.get_variable('Z', [block_size, output_size], dtype=DTYPE)
        H = tf.get_variable('H', [block_size, block_size], dtype=DTYPE)

        q = tf.squeeze(query, axis=1)
        y = tf.matmul(activation(q + tf.matmul(attention, H)), Z)
        return y


def get_outputs(inputs, params):
    """
    Return the outputs from the model which will be used in the loss function.
    :param inputs:
    :param params:
    :return:
    """
    use_attrs = params['use_attrs']
    embedding_size = params['embedding_size']
    num_blocks = params['num_blocks']
    vocab_size = params['vocab_size']
    num_labels = params['num_labels']
    use_full_network = not params['ent_net']
    do_norm = params['do_norm']
    sum_attrs = params['sum_attrs']

    story = inputs['story']  # [?, num_sentences, max_sentence_len]
    entity_indexes = inputs['entity_indexes']  # [?, num_blocks, num_blocks]
    key_indexes = inputs['keys']  # [?, num_blocks]

    batch_size = tf.shape(story)[0]

    normal_initializer = tf.random_normal_initializer(stddev=0.1)
    ones_initializer = tf.constant_initializer(1.0, dtype=DTYPE)

    # Extend the vocab to include keys for the dynamic memory cell,
    # allowing the initialization of the memory to be learned.
    vocab_size = vocab_size + num_blocks

    with tf.variable_scope('RelNet', initializer=normal_initializer):
        # PReLU activations have their alpha parameters initialized to 1
        # so they may be identity before training.
        alpha = tf.get_variable(
            name='alpha',
            shape=embedding_size,
            initializer=ones_initializer)
        activation = partial(prelu, alpha=alpha)

        # Embeddings
        embedding_params = tf.get_variable(
            name='embedding_params',
            shape=[vocab_size, embedding_size],
            dtype=DTYPE)

        # The embedding mask forces the special "pad" embedding to zeros.
        embedding_mask = tf.constant(
            value=[0 if i == 0 else 1 for i in range(vocab_size)],
            shape=[vocab_size, 1],
            dtype=DTYPE)
        embedding_params_masked = embedding_params * embedding_mask

        # [?, num_sentences, max_sentence_len, embedding_size]
        story_embedding = tf.nn.embedding_lookup(embedding_params_masked, story)
        if use_attrs:
            # in this case, story embedding is [?, num_sentences, max_sentence_len, num_attrs+1, embedding_size]
            if sum_attrs:
                # sum over the attr dimension to get [?, num_sentences, max_sentence_len, embedding_size]
                story_embedding = tf.reduce_sum(story_embedding, axis=3)
            else:
                # after the concat operation it is [?, num_sentences, max_sentence_len, (num_attrs+1) * embedding_size]
                story_embedding = tf.concat(tf.unstack(story_embedding, axis=-1), axis=-1)
                # finally, we have [?, num_sentences, max_sentence_len, embedding_size]
                story_embedding = tf.layers.dense(story_embedding, embedding_size, activation=None,
                                                  name='attr_embedding')
        print('story: %s', story_embedding)

        # Input Module - [?, num_sentences, embeddings_size]
        encoded_story = get_input_encoding(
            inputs=story_embedding,
            initializer=ones_initializer,
            scope='StoryEncoding')
        if use_full_network:
            rel_encoded_story = get_input_encoding(
                inputs=story_embedding,
                initializer=ones_initializer,
                scope='RelStoryEncoding')
            print('enc stories: %s, %s' % (encoded_story, rel_encoded_story))

        # Memory Module
        keys = tf.nn.embedding_lookup(embedding_params_masked, key_indexes)  # [?, num_blocks, block_size]
        if use_attrs:
            # in this case, keys is [?, num_blocks, num_attrs+1, block_size]
            if sum_attrs:
                keys = tf.reduce_sum(keys, axis=2)
            else:
                # after the concat operation it is [?, num_blocks, (num_attrs+1) * block_size]
                keys = tf.concat(tf.unstack(keys, axis=-1), axis=-1)
                # finally, we have [?, num_blocks, block_size]
                keys = tf.layers.dense(keys, embedding_size, activation=None, name='attr_embedding', reuse=True)
        print('keys: %s' % keys)
        keys = tf.unstack(keys, num_blocks, axis=1)
        # keys = tf.split(keys, num_blocks, axis=0)
        # keys = [tf.squeeze(key, axis=0) for key in keys]

        if use_full_network:
            cell = DynamicRelationalMemoryCell(
                num_blocks=num_blocks,
                num_units_per_block=embedding_size,
                keys=keys,
                initializer=normal_initializer,
                recurrent_initializer=normal_initializer,
                activation=activation,
                rel_activation=tf.nn.relu,
                dtype=DTYPE,
                do_norm=do_norm)
        else:
            cell = DynamicMemoryCell(
                num_blocks=num_blocks,
                num_units_per_block=embedding_size,
                keys=keys,
                initializer=normal_initializer,
                recurrent_initializer=normal_initializer,
                activation=activation,
                dtype=DTYPE)

        # Recurrence
        initial_state = cell.zero_state(batch_size, DTYPE)

        # with drnn
        inputs = (encoded_story, rel_encoded_story) if use_full_network else encoded_story
        sequence_length = get_sequence_length(encoded_story)
        _, final_state = tf.nn.dynamic_rnn(
            cell=cell,
            inputs=inputs,
            sequence_length=sequence_length,
            initial_state=initial_state)

        # Output Module
        if use_full_network:
            outputs = simple_output_module(
                last_state=final_state,
                entity_indexes=entity_indexes,
                num_blocks=num_blocks,
                output_size=num_labels,
                initializer=normal_initializer,
                activation=activation)
        else:
            outputs = dmc_output_module(
                last_state=final_state,
                entity_indexes=entity_indexes,
                num_blocks=num_blocks,
                output_size=num_labels,
                initializer=normal_initializer,
                activation=activation)
        parameters = count_parameters()
        print('Parameters: {}'.format(parameters))

        return outputs


def get_predictions(outputs):
    """Return the actual predictions for use with evaluation metrics or TF Serving."""
    predictions = tf.argmax(outputs, axis=-1)
    return predictions


def get_loss(outputs, labels, mode):
    """Return the loss function which will be used with an optimizer."""

    loss = None
    if mode == ModeKeys.INFER:
        return loss

    loss = tf.losses.sparse_softmax_cross_entropy(
        logits=outputs,
        labels=labels)

    return loss


def get_train_op(loss, params, mode):
    """Return the training operation which will be used to train the model."""

    train_op = None
    if mode != ModeKeys.TRAIN:
        return train_op

    global_step = tf.contrib.framework.get_or_create_global_step()

    learning_rate = cyclic_learning_rate(
        learning_rate_min=params['learning_rate_min'],
        learning_rate_max=params['learning_rate_max'],
        step_size=params['learning_rate_step_size'],
        mode='triangular',
        global_step=global_step)
    tf.summary.scalar('learning_rate', learning_rate)

    train_op = tf.contrib.layers.optimize_loss(
        loss=loss,
        global_step=global_step,
        learning_rate=learning_rate,
        optimizer='Adam',
        clip_gradients=params['clip_gradients'],
        gradient_noise_scale=params['gradient_noise_scale'],
        summaries=OPTIMIZER_SUMMARIES)

    return train_op


def model_fn(features, labels, mode, params):
    """Return ModelFnOps for use with Estimator."""

    start = time.time()
    outputs = get_outputs(features, params)
    predictions = get_predictions(outputs)
    loss = get_loss(outputs, labels, mode)
    train_op = get_train_op(loss, params, mode)
    print('Model built. Took %s seconds' % (time.time() - start))

    return ModelFnOps(
        predictions=predictions,
        loss=loss,
        train_op=train_op,
        mode=mode)
