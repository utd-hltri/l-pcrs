"""
Serving input function definition.
"""
from __future__ import absolute_import
from __future__ import print_function
from __future__ import division

import tensorflow as tf
from tensorflow.contrib.learn import utils


def generate_serving_input_fn(metadata):
    "Returns _serving_input_fn for use with an export strategy."
    max_story_length = metadata['max_story_length']
    max_sentence_length = metadata['max_sentence_length']
    num_blocks = metadata['max_entities_length']

    def _serving_input_fn():
        story_placeholder = tf.placeholder(
            shape=[max_story_length, max_sentence_length],
            dtype=tf.int64,
            name='story')
        entity_index_placeholder = tf.placeholder(
            shape=[num_blocks],
            dtype=tf.int64,
            name='entity_indexes')
        keys_placeholder = tf.placeholder(
            shape=[num_blocks],
            dtype=tf.int64,
            name='key_indexes')

        feature_placeholders = {
            'story': story_placeholder,
            'entity_indexes': entity_index_placeholder,
            'keys': keys_placeholder
        }

        features = {
            key: tf.expand_dims(tensor, axis=0)
            for key, tensor in feature_placeholders.items()
        }

        input_fn_ops = utils.input_fn_utils.InputFnOps(
            features=features,
            labels=None,
            default_inputs=feature_placeholders)

        return input_fn_ops

    return _serving_input_fn


def generate_serving_input_fn_attr(metadata):
    "Returns _serving_input_fn for use with an export strategy."
    max_story_length = metadata['max_story_length']
    max_sentence_length = metadata['max_sentence_length']
    num_blocks = metadata['max_entities_length']
    num_attrs = metadata['num_attrs'] + 1

    def _serving_input_fn():
        story_placeholder = tf.placeholder(
            shape=[max_story_length, max_sentence_length, num_attrs],
            dtype=tf.int64,
            name='story')
        entity_index_placeholder = tf.placeholder(
            shape=[num_blocks, num_blocks],
            dtype=tf.int64,
            name='entity_indexes')
        keys_placeholder = tf.placeholder(
            shape=[num_blocks, num_attrs],
            dtype=tf.int64,
            name='key_indexes')

        feature_placeholders = {
            'story': story_placeholder,
            'entity_indexes': entity_index_placeholder,
            'keys': keys_placeholder
        }

        features = {
            key: tf.expand_dims(tensor, axis=0)
            for key, tensor in feature_placeholders.items()
        }

        input_fn_ops = utils.input_fn_utils.InputFnOps(
            features=features,
            labels=None,
            default_inputs=feature_placeholders)

        return input_fn_ops

    return _serving_input_fn
