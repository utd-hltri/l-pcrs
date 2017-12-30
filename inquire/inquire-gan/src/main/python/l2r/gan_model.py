from __future__ import absolute_import
from __future__ import division
from __future__ import print_function

import numpy as np
from six.moves import xrange  # pylint: disable=redefined-builtin
import tensorflow as tf

from tensorflow.contrib import slim, rnn, legacy_seq2seq

from collections import namedtuple

HParams = namedtuple('HParams',
                     'g_relu1_dim, g_relu2_dim, ' +
                     'd_rnn_dim, d_relu_dim, d_min_lr, d_lr, d_ranking_dim, d_batch_norm, d_layer_norm, ' +
                     'mu, sigma, keep_prob')

# Designed for tensorflow v 1.0?


def _clamp_as_probability(tensor):
    return tf.maximum(tf.minimum(tensor, .99), .01)


def _leaky_relu(x, alpha=0.01):
    return tf.maximum(alpha * x, x)


def _smooth_sign(x, k=1e8):
    return (tf.tanh(k * (x - 1e-8)) + 1) / 2


def _add_ranking_network(scores):
    # Resize to [batch x length x 1]
    a = tf.expand_dims(scores, 2)
    # Resize to [batch x 1 x length]
    b = tf.expand_dims(scores, 1)
    # Use broadcast to compute pairwise differences in shape [batch x length x length]
    delta = (b - a)
    # Apply smooth sign to each difference and sum over last dimension, [batch x length]
    ranks = tf.reduce_sum(_smooth_sign(delta), axis=2)
    return ranks


class GANModel(object):
    def __init__(self,
                 max_score,
                 feature_vector_dim,
                 max_list_len,
                 batch_size,
                 hps):
        self._max_score = max_score
        self._feature_vector_dim = feature_vector_dim
        self._max_list_len = max_list_len
        self._batch_size = batch_size
        self._hps = hps
        self._g_step = tf.Variable(0, trainable=False)
        self._d_step = tf.Variable(0, trainable=False)
        self._keep_prob = tf.constant(hps.keep_prob)

    def _add_placeholders(self):
        batch_size = self._batch_size
        max_list_len = self._max_list_len
        feature_vector_dim = self._feature_vector_dim

        self._z_seeds = tf.placeholder(tf.float32, [batch_size, 1], name='z_seeds')
        self._z_vectors_stacked = tf.placeholder(tf.float32,
                                                 [batch_size, max_list_len, feature_vector_dim],
                                                 name='z_vectors_stacked')
        self._z_lens = tf.placeholder(tf.int32, [batch_size], name='z_lens')

        self._x_vectors_stacked = tf.placeholder(tf.float32,
                                                 [batch_size, max_list_len, feature_vector_dim],
                                                 name='x_vectors_stacked')
        self._x_lens = tf.placeholder(tf.int32, [batch_size], name='x_lens')
        self._x_ranks_stacked = tf.placeholder(tf.float32,
                                               [batch_size, max_list_len],
                                               name='x_ranks_stacked')

        self._is_training = tf.placeholder(tf.bool, [], name='is_training')

    def _add_pairwise_scorer(self, vectors, lens):
        num_feats = vectors.shape[2]

        # Resize to [batch x length x 1 x feats]
        a = tf.expand_dims(vectors, 2)
        # Resize to [batch x 1 x length x feats)
        b = tf.expand_dims(vectors, 1)
        # Use broadcast to compute pairwise differences in shape [batch x length x length x feats]
        delta = (b - a)

        pairwise_vectors = tf.reshape(delta, [self._batch_size * self._max_list_len * self._max_list_len, -1])
        pairwse_scores = slim.fully_connected(pairwise_vectors, 1)

        vector_pairwise_scores = tf.reshape(pairwse_scores, [-1, self._max_list_len])
        vector_scores = tf.reduce_mean(vector_pairwise_scores, axis=1)

        return tf.reshape(vector_scores, [self._batch_size, self._max_list_len])




    def _add_generator(self):
        hps = self._hps
        max_score = self._max_score

        with tf.variable_scope('generator') as scope:
            # Flatten to [(BATCH * LENGTH) x FEATURES]
            # Tile seeds from [BATCH x 1] to [BATCH x LENGTH]]
            seeds = tf.tile(self._z_seeds, [1, self._max_list_len])
            # Expand seeds to [BATCH x LENGTH x 1]
            seeds = tf.expand_dims(seeds, axis=2)


            # Append batch seed to each vector: [BATCH x LENGTH x (FEATURES + 1)]
            vectors = tf.concat([self._z_vectors_stacked, seeds], axis=2)

            self._z_scores_stacked = self._add_pairwise_scorer(vectors, self._z_lens)

            #
            # # Flatten vectors to [(BATCH * LENGTH) x (FEATURES + 1)]
            # vectors = tf.reshape(vectors, [-1, self._feature_vector_dim + 1])
            #
            # # Setup leaky relu layers & batch norm
            # with tf.contrib.slim.arg_scope([tf.contrib.slim.fully_connected],
            #                                biases_initializer=tf.constant_initializer(0.01),
            #                                activation_fn=tf.nn.relu):
            #                                 # normalizer_fn=tf.contrib.slim.batch_norm,
            #                                 # normalizer_params={'is_training': self._is_training}):
            #     linear = tf.contrib.slim.fully_connected(vectors, 1, scope='g_layer')
            # scores = tf.maximum(linear, 1e-8) # + tf.random_uniform([self._batch_size * self._max_list_len, 1], minval=1e-8, maxval=1e-5)

            # Reshape scores to [BATCH x LEN]
            # self._z_scores_stacked = tf.reshape(scores, [self._batch_size, self._max_list_len])

            # Store generator's trainable variables
            self._theta_g = [v for v in tf.trainable_variables() if v.name.startswith(scope.name)]

            # Compute ranks
            self._z_ranks_stacked = _add_ranking_network(self._z_scores_stacked)

    def _add_rnn_cell(self):
        hps = self._hps
        if self._hps.d_layer_norm:
            print('Creating Layer-Normalizing LSTM Cell...')
            return tf.contrib.rnn.LayerNormBasicLSTMCell(hps.d_rnn_dim)
        else:
            return tf.contrib.rnn.GRUCell(hps.d_rnn_dim)


    def _add_dan(self, vectors_stacked, vectors_lens):
        # flat_vectors = flat_vectors = tf.reshape(vectors_stacked, [-1, self._feature_vector_dim])

        with tf.variable_scope('dan'):
            with slim.arg_scope([tf.contrib.slim.fully_connected],
                                biases_initializer=tf.constant_initializer(0.01),
                                activation_fn=_leaky_relu):
                                           # normalizer_fn=tf.contrib.slim.batch_norm,
                                           # normalizer_params={'is_training': self._is_training}):
                # l1 = slim.fully_connected(flat_vectors, self._feature_vector_dim, scope='layer_1')
                # l2 = slim.fully_connected(l1, self._feature_vector_dim, scope='layer_2')
                mask = tf.expand_dims(tf.sequence_mask(vectors_lens, self._max_list_len, dtype=tf.float32), axis=2)
                # vectors = tf.reshape(l2, [self._batch_size, self._max_list_len, self._feature_vector_dim])
                avg = tf.reduce_sum(vectors_stacked * mask, axis=1) / tf.cast(tf.expand_dims(vectors_lens, axis=1), tf.float32)
                # l3 = slim.fully_connected(avg, self._feature_vector_dim, scope='layer_3')
                # l4 = slim.fully_connected(l3, self._feature_vector_dim, scope='layer_4')
        return avg




    def _add_energy_discriminator(self, vectors_stacked, vectors_ranks, vectors_lens):
        # Flatten vectors to [(BATCH * LENGTH) x FEATURES]
        avg_vectors = tf.tile(self._add_dan(vectors_stacked, vectors_lens), (self._max_list_len, 1))
        flat_vectors = tf.reshape(vectors_stacked, [-1, self._feature_vector_dim])

        with slim.arg_scope([tf.contrib.slim.fully_connected],
                            biases_initializer=tf.constant_initializer(0.01),
                            activation_fn=tf.nn.tanh):
                                       # normalizer_fn=tf.contrib.slim.batch_norm,
                                       # normalizer_params={'is_training': self._is_training}):
            l1 = slim.fully_connected(tf.concat([flat_vectors, avg_vectors], axis=1), self._hps.d_relu_dim, scope='layer_1')
            l2 = slim.fully_connected(l1, self._hps.d_relu_dim, scope='layer_2')
            scores = slim.fully_connected(l2, 1, activation_fn=tf.nn.sigmoid, scope='layer_3')

        # Reshape to [BATCH x LENGTH]
        stacked_scores = tf.reshape(scores, [self._batch_size, self._max_list_len])

        # ranked_scores = stacked_scores / (1 + tf.log(vectors_ranks)) * length_mask
        # total_scores = tf.reduce_sum(ranked_scores, axis=1, keep_dims=True)
        # score = tf.squeeze(slim.fully_connected(total_scores, 1, activation_fn=tf.nn.sigmoid, scope='d_output'))
        # return _clamp_as_probability(score)

        length_mask = tf.sequence_mask(vectors_lens, self._max_list_len, dtype=tf.float32)
        energy = -stacked_scores * length_mask

        permutation_total = tf.expand_dims(1 + tf.reduce_sum((1 / tf.log(vectors_ranks)) * length_mask, axis=1), axis=1)
        permutation_prob = 1 / (tf.log(vectors_ranks + 1e-8) * permutation_total)

        total_energy = tf.reduce_sum(energy * length_mask, axis=1, keep_dims=True)

        list_energy = total_energy - tf.reduce_sum(energy * vectors_ranks * permutation_prob, axis=1, keep_dims=True)

        # Compute score as sigmoid of total score
        score = tf.squeeze(slim.fully_connected(list_energy, 1, activation_fn=tf.nn.sigmoid, scope='d_output'))
        return _clamp_as_probability(score)

    def _add_memory_discriminator(self, vectors_stacked, vectors_ranks, vectors_lens):
        # Compute length mask
        length_mask = tf.expand_dims(tf.sequence_mask(vectors_lens, self._max_list_len, dtype=tf.float32), axis=2)

        num_hidden = 64
        batch_size = self._batch_size
        emb_dim = self._feature_vector_dim
        num_heads = 1

        enc_layers = 3

        rank_coefficients = tf.expand_dims(1 / (1 + (tf.log(vectors_ranks + 1e-8))), axis=2)

        pro_cell = tf.contrib.rnn.LSTMCell(num_hidden, state_is_tuple=False)

        self._pro_att_states = [
            tf.reshape(x, [batch_size, 1, emb_dim])
            for x in tf.unstack(vectors_stacked, axis=1)
        ]
        self._pro_att_states = tf.concat(self._pro_att_states, 1)

        # Encoder does not get any input. We give a bias term to define length
        encoder_inputs = [
            tf.ones([batch_size, 1], tf.float32)
            for x in range(enc_layers)
        ]
        pro_initial_state = tf.ones([batch_size, 2 * num_hidden],
                                    tf.float32)

        # We misuse the attention decoder to read the input embeddings in
        # arbitrary order (attention defines order)
        encoder_outputs, self._pro_out_state = (
            legacy_seq2seq.attention_decoder(
                encoder_inputs,
                pro_initial_state,
                self._pro_att_states,
                pro_cell,
                num_heads=num_heads,
                loop_function=None,
                initial_state_attention=False))

        encoder_outputs = encoder_outputs[-1]


        tiled_output = tf.tile(tf.expand_dims(encoder_outputs, axis=1), [1, self._max_list_len, 1])
        feature_vectors = tf.concat([tiled_output, (vectors_stacked * rank_coefficients),
                                     tf.expand_dims(vectors_ranks, axis=2)], axis=2) * length_mask
        flat_vectors = tf.reshape(feature_vectors, [self._batch_size * self._max_list_len, -1])
        total_scores = slim.fully_connected(flat_vectors, 1, activation_fn=tf.nn.sigmoid, scope='d_layer_1')
        stacked_scores = tf.reshape(total_scores, [self._batch_size, self._max_list_len])
        total_scores = tf.squeeze(slim.fully_connected(stacked_scores, 1, activation_fn=tf.nn.sigmoid,
                                                       scope='d_layer_2'))
        return _clamp_as_probability(total_scores)


    def _add_discriminator_copy(self, vectors_stacked, vectors_ranks, vectors_lens):
        return self._add_energy_discriminator(vectors_stacked, vectors_ranks, vectors_lens)
        # return self._add_memory_discriminator(vectors_stacked, vectors_ranks, vectors_lens)

    def _add_discriminator(self):
        batch_size = self._batch_size

        with tf.variable_scope('discriminator') as scope:
                self._d1 = self._add_discriminator_copy(self._x_vectors_stacked, self._x_ranks_stacked, self._x_lens)
                scope.reuse_variables()
                self._d2 = self._add_discriminator_copy(self._z_vectors_stacked, self._z_ranks_stacked, self._z_lens)

        with tf.variable_scope('discriminator'):
            self._theta_d = [v for v in tf.trainable_variables() if v.name.startswith('discriminator')]

            # Discriminator pre-training
            print('Adding discriminator pre-training loss...')
            with tf.variable_scope('pretraining'):
                self._d_pre_step = tf.Variable(0, trainable=False)
                self._x_labels_stacked = tf.placeholder(tf.float32, [batch_size], name='x_labels_stacked')
                self._d_pre_loss = tf.losses.mean_squared_error(self._x_labels_stacked, self._d1)
                self._d_pre_opt = tf.train.AdamOptimizer().minimize(self._d_pre_loss,
                                                                    global_step=self._d_pre_step,
                                                                    var_list=self._theta_d)

    def _add_train_ops(self):
        hps = self._hps
        self._obj_d = tf.reduce_mean(tf.log(self._d1) + tf.log(1 - self._d2))
        self._loss_d = 1 - self._obj_d
        # self._lr_rate_d = tf.maximum(
        #     hps.d_min_lr,  # min_lr_rate.
        #     tf.train.exponential_decay(hps.d_lr, self._d_step, 30000, 0.98))
        # self._opt_d = tf.train.GradientDescentOptimizer(self._lr_rate_d).minimize(self._loss_d,
        #                                                                global_step=self._d_step,
        #                                                                var_list=self._theta_d)
        self._opt_d = tf.train.AdamOptimizer().minimize(self._loss_d, global_step=self._d_step, var_list=self._theta_d)
        self._obj_g = tf.reduce_mean(tf.log(self._d2))
        # In GAN papers, the loss function to optimize G is min (log 1-D),
        # but in practice folks practically use max log D
        # because the first formulation has vanishing gradients early on
        # Goodfellow et. al (2014)
        # from: https://github.com/soumith/ganhacks
        self._loss_g = -self._obj_g
        self._opt_g = tf.train.AdamOptimizer().minimize(self._loss_g,
                                                        global_step=self._g_step,
                                                        var_list=self._theta_g)

    def build_graph(self):
        self._add_placeholders()
        self._add_generator()
        self._add_discriminator()
        self._add_train_ops()

    def run_pretrain_step(self, sess, x_vectors, x_ranks, x_lens):
        batch_size = self._batch_size
        z_vectors = np.zeros((self._batch_size, self._max_list_len, self._feature_vector_dim))
        z_ranks = np.zeros((batch_size, self._max_list_len))
        z_seeds = np.zeros((batch_size, 1))
        z_lens = np.zeros(batch_size)

        x_ranks_pre = []
        x_labels_pre = []
        for i in xrange(batch_size):
            if i % 2 == 0:
                x_ranks_pre.append(np.random.permutation(x_ranks[i]))
                x_labels_pre.append(0)
            else:
                x_ranks_pre.append(x_ranks[i])
                x_labels_pre.append(1)
        _, d1, loss_d, step_d = sess.run(
            [self._d_pre_opt, self._d1, self._d_pre_loss, self._d_pre_step],
            feed_dict={self._is_training: True,
                       self._x_vectors_stacked: x_vectors,
                       self._x_ranks_stacked: np.asarray(x_ranks_pre),
                       self._x_lens: x_lens,
                       self._z_vectors_stacked: z_vectors,
                       self._z_ranks_stacked: z_ranks,
                       self._z_seeds: z_seeds,
                       self._z_lens: z_lens,
                       self._x_labels_stacked: np.asarray(x_labels_pre)})
        return d1, loss_d, step_d

    def run_train_d_step(self, sess, x_vectors, x_ranks, x_lens, z_vectors, z_lens):
        hps = self._hps
        batch_size = self._batch_size
        x_labels = np.zeros(batch_size)
        z_seeds = np.random.normal(hps.mu, hps.sigma, (batch_size, 1))
        _, d1, obj_d, loss_d, step_d = sess.run(
            [self._opt_d, self._d1, self._obj_d, self._loss_d, self._d_step],
            feed_dict={self._is_training: True,
                       self._x_vectors_stacked: x_vectors,
                       self._x_ranks_stacked: x_ranks,
                       self._x_lens: x_lens,
                       self._z_vectors_stacked: z_vectors,
                       self._z_seeds: z_seeds,
                       self._z_lens: z_lens,
                       self._x_labels_stacked: x_labels})
        return d1, obj_d, loss_d, step_d

    def run_train_g_step(self, sess, z_vectors, z_lens):
        hps = self._hps
        batch_size = self._batch_size
        x_labels = np.zeros(batch_size)
        x_vectors = np.zeros((self._batch_size, self._max_list_len, self._feature_vector_dim))
        x_ranks = np.zeros((self._batch_size, self._max_list_len))
        z_seeds = np.random.normal(hps.mu, hps.sigma, (batch_size, 1))
        _, d2, z_ranks, z_scores, obj_g, loss_g, step_g = sess.run(
            [self._opt_g, self._d2, self._z_ranks_stacked, self._z_scores_stacked, self._obj_g, self._loss_g, self._g_step],
            feed_dict={
                self._is_training: True,
                self._x_vectors_stacked: x_vectors,
                self._x_ranks_stacked: x_ranks,
                self._x_lens: np.zeros(batch_size),
                self._z_vectors_stacked: z_vectors,
                self._z_seeds: z_seeds,
                self._z_lens: z_lens,
                self._x_labels_stacked: x_labels})
        return d2, z_ranks, z_scores, obj_g, loss_g, step_g

    def run_train_step(self, sess, x_vectors1, x_ranks1, x_lens1, z_vectors1, z_lens1, z_vectors2, z_lens2):
        d1, obj_d, loss_d, step_d = self.run_train_d_step(sess, x_vectors1, x_ranks1, x_lens1, z_vectors1, z_lens1)
        d2, z_scores, obj_g, loss_g, step_g = self.run_train_g_step(sess, z_vectors2, z_lens2)
        return d1, d2, loss_d, loss_g, z_scores, step_d, step_g

    def run_eval_step(self, sess, z_vectors, z_lens):
        hps = self._hps
        batch_size = self._batch_size
        z_seeds = np.random.normal(hps.mu, hps.sigma, (batch_size, 1))
        # The generator doesn't use x_vectors or x_scores, but Tensorflow requires
        # all placeholders to be provided, so we give blank tensors
        x_vectors = np.zeros((self._batch_size, self._max_list_len, self._feature_vector_dim))
        x_ranks = np.zeros((self._batch_size, self._max_list_len))
        x_lens = np.zeros(self._batch_size)
        return sess.run([self._z_ranks_stacked, self._z_scores_stacked],
                        feed_dict={self._is_training: False,
                                   self._x_vectors_stacked: x_vectors,
                                   self._x_ranks_stacked: x_ranks,
                                   self._x_lens: x_lens,
                                   self._z_vectors_stacked: z_vectors,
                                   self._z_lens: z_lens,
                                   self._z_seeds: z_seeds})
