"Utilities for model construction."
from __future__ import absolute_import
from __future__ import print_function
from __future__ import division

import numpy as np
import tensorflow as tf
from tensorflow.contrib import metrics


def count_parameters():
    "Count the number of parameters listed under TRAINABLE_VARIABLES."
    num_parameters = sum([np.prod(tvar.get_shape().as_list())
                          for tvar in tf.trainable_variables()])
    return num_parameters


def get_sequence_length(sequence, scope=None):
    """
    Determine the length of a sequence that has been padded with zeros.
    :param sequence: [..., max_story_len, embedding_dim] tensor
    :param scope: scope
    :return: [...] tensor of int story lengths
    """
    with tf.variable_scope(scope, 'SequenceLength'):
        used = tf.sign(tf.reduce_max(tf.abs(sequence), reduction_indices=[-1]))
        length = tf.cast(tf.reduce_sum(used, reduction_indices=[-1]), tf.int32)
        return length


def cyclic_learning_rate(
        learning_rate_min,  # 0.0002
        learning_rate_max,  # 0.01
        step_size,
        global_step,
        mode='triangular',
        scope=None):
    with tf.variable_scope(scope, 'CyclicLearningRate'):
        cycle = tf.floor(1 + tf.to_float(global_step) / (2 * step_size))

        if mode == 'triangular':
            scale = 1
        elif mode == 'triangular2':
            scale = 2**(cycle - 1)
        else:
            raise ValueError('Unrecognized mode: {}'.format(mode))

        x = tf.abs(tf.to_float(global_step) / step_size - 2 * cycle + 1)
        lr = learning_rate_min + (learning_rate_max - learning_rate_min) * \
            tf.maximum(0.0, 1 - x) / scale

        # tf.assert_greater_equal(lr, learning_rate_min, data=[lr], message="LR too small")
        # tf.assert_less_equal(lr, learning_rate_max, data=[lr], message="LR too big")
        return lr


def prelu(features, alpha, scope=None):
    """
    Implementation of [Parametric ReLU](https://arxiv.org/abs/1502.01852) borrowed from Keras.
    """
    with tf.variable_scope(scope, 'PReLU'):
        pos = tf.nn.relu(features)
        neg = alpha * (features - tf.abs(features)) * 0.5
        return tf.add(pos, neg)


def prelu6(features, alpha, scope=None):
    """
    Implementation of [Parametric ReLU](https://arxiv.org/abs/1502.01852) borrowed from Keras.
    """
    with tf.variable_scope(scope, 'PReLU'):
        pos = tf.nn.relu6(features)
        neg = tf.maximum(-6 * tf.ones_like(features), alpha * (features - tf.abs(features)) * 0.5)
        return tf.add(pos, neg)


def is_nan(x, msg):
    with tf.control_dependencies(
      [tf.assert_equal(0, tf.reduce_sum(tf.cast(tf.is_nan(x), tf.int32)), message=msg)]):
        return x


def safe_norm(x, epsilon=1e-12, axis=None, keep_dims=False):
    return tf.sqrt(tf.reduce_sum(x ** 2, axis=axis, keep_dims=keep_dims) + epsilon)


def skip_gradient(forward, backward):
    return backward + tf.stop_gradient(forward + backward)


def create_multiclass_precision_metric(num_labels):
    def multiclass_streaming_precision(predictions, labels, weights=None,
                                       metrics_collections=None, updates_collections=None,
                                       name=None):
        return metrics.streaming_precision(predictions=tf.one_hot(predictions, num_labels),
                                           labels=tf.one_hot(labels, num_labels),
                                           weights=weights,
                                           metrics_collections=metrics_collections,
                                           updates_collections=updates_collections,
                                           name=name)
    return multiclass_streaming_precision


def create_multiclass_recall_metric(num_labels):
    def multiclass_streaming_recall(predictions, labels, weights=None,
                                    metrics_collections=None, updates_collections=None,
                                    name=None):
        return metrics.streaming_recall(predictions=tf.one_hot(predictions, num_labels),
                                        labels=tf.one_hot(labels, num_labels),
                                        weights=weights,
                                        metrics_collections=metrics_collections,
                                        updates_collections=updates_collections,
                                        name=name)
    return multiclass_streaming_recall
