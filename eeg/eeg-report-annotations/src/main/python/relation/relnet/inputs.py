"""
Module responsible for input data.
"""
from __future__ import absolute_import
from __future__ import print_function
from __future__ import division

import tensorflow as tf
from tensorflow.contrib.learn import read_batch_record_features


def read_and_decode_labels(tfrecord_filename, metadata, attr_data=False):
    labels = []
    filename_queue = tf.train.string_input_producer([tfrecord_filename], num_epochs=1)
    _, test_y = decode_tfrecords(filename_queue, metadata, 1, attr_data)
    init_op = tf.group(tf.global_variables_initializer(), tf.local_variables_initializer())
    with tf.Session() as sess:
        sess.run(init_op)
        coord = tf.train.Coordinator()
        threads = tf.train.start_queue_runners(sess=sess, coord=coord)
        try:
            while not coord.should_stop():
                labels.extend(sess.run(test_y))
        except tf.errors.OutOfRangeError:
            print('Done!')
        finally:
            coord.request_stop()
        coord.join(threads)
    print('Read %s labels from tfrecords in %s' % (len(labels), tfrecord_filename))
    return labels


def decode_tfrecords(filename_queue, metadata, batch_size, attr_data=False):
    max_story_length = metadata['max_story_length']
    max_sentence_length = metadata['max_sentence_length']
    num_blocks = metadata['num_blocks']
    num_attrs = metadata['num_attrs'] + 1
    with tf.device('/cpu:0'):
        reader = tf.TFRecordReader()
        _, tfrecord = reader.read(filename_queue)
        feats = {'story': tf.FixedLenFeature(shape=[max_story_length, max_sentence_length], dtype=tf.int64),
                 'entity_indexes': tf.FixedLenFeature(shape=[num_blocks, num_blocks], dtype=tf.int64),
                 'keys': tf.FixedLenFeature(shape=[num_blocks], dtype=tf.int64),
                 'label': tf.FixedLenFeature(shape=[], dtype=tf.int64)}
        if attr_data:
            feats['story'] = tf.FixedLenFeature(shape=[max_story_length, max_sentence_length, num_attrs],
                                                dtype=tf.int64)
            feats['keys'] = tf.FixedLenFeature(shape=[num_blocks, num_attrs], dtype=tf.int64)
        record_features = tf.parse_single_example(tfrecord, features=feats)
        tensors = tf.train.batch(record_features, batch_size)
        labels = tensors['label']
        return tensors, labels


def generate_input_fn(filename, metadata, batch_size, num_epochs=None, shuffle=False):
    "Return _input_fn for use with Experiment."
    def _input_fn():
        max_story_length = metadata['max_story_length']
        max_sentence_length = metadata['max_sentence_length']
        num_blocks = metadata['num_blocks']

        with tf.device('/cpu:0'):
            features = {
                'story': tf.FixedLenFeature(shape=[max_story_length, max_sentence_length], dtype=tf.int64),
                'entity_indexes': tf.FixedLenFeature(shape=[num_blocks], dtype=tf.int64),
                'keys': tf.FixedLenFeature(shape=[num_blocks], dtype=tf.int64),
                'label': tf.FixedLenFeature(shape=[], dtype=tf.int64)
            }

            record_features = read_batch_record_features(
                file_pattern=filename,
                features=features,
                batch_size=batch_size,
                randomize_input=shuffle,
                num_epochs=num_epochs)

            story = record_features['story']
            entity_indexes = record_features['entity_indexes']
            keys = record_features['keys']
            label = record_features['label']

            features = {
                'story': story,
                'entity_indexes': entity_indexes,
                'keys': keys
            }

            return features, label

    return _input_fn


def generate_input_fn_attr(filename, metadata, batch_size, num_epochs=None, shuffle=False):
    "Return _input_fn for use with Experiment."
    def _input_fn():
        max_story_length = metadata['max_story_length']
        max_sentence_length = metadata['max_sentence_length']
        num_blocks = metadata['num_blocks']
        num_attrs = metadata['num_attrs'] + 1

        with tf.device('/cpu:0'):
            features = {
                'story': tf.FixedLenFeature(shape=[max_story_length, max_sentence_length, num_attrs], dtype=tf.int64),
                'entity_indexes': tf.FixedLenFeature(shape=[num_blocks, num_blocks], dtype=tf.int64),
                'keys': tf.FixedLenFeature(shape=[num_blocks, num_attrs], dtype=tf.int64),
                'label': tf.FixedLenFeature(shape=[], dtype=tf.int64)
            }

            record_features = read_batch_record_features(
                file_pattern=filename,
                features=features,
                batch_size=batch_size,
                randomize_input=shuffle,
                num_epochs=num_epochs)

            story = record_features['story']
            entity_indexes = record_features['entity_indexes']
            keys = record_features['keys']
            label = record_features['label']

            features = {
                'story': story,
                'entity_indexes': entity_indexes,
                'keys': keys
            }

            return features, label

    return _input_fn
