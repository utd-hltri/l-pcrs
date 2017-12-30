from __future__ import absolute_import
from __future__ import division
from __future__ import print_function

from collections import namedtuple


import numpy as np
from six.moves import xrange  # pylint: disable=redefined-builtin
import tensorflow as tf

import data

def _clamp_as_probability(input):
    return tf.maximum(tf.minimum(input, .99), .01)


def _extract_argmax_and_embed(embedding, output_projection=None,
                              update_embedding=True):
    """Get a loop_function that extracts the previous symbol and embeds it.

    Args:
    embedding: embedding tensor for symbols.
    output_projection: None or a pair (W, B). If provided, each fed previous
      output will first be multiplied by W and added B.
    update_embedding: Boolean; if False, the gradients will not propagate
      through the embeddings.

    Returns:
    A loop function.
    """
    def loop_function(prev, _):
        """function that feed previous model output rather than ground truth."""
        if output_projection is not None:
          prev = tf.nn.xw_plus_b(
              prev, output_projection[0], output_projection[1])
        prev_symbol = tf.argmax(prev, 1)
        # Note that gradients will not propagate through the second parameter of
        # embedding_lookup.
        emb_prev = tf.nn.embedding_lookup(embedding, prev_symbol)
        if not update_embedding:
          emb_prev = tf.stop_gradient(emb_prev)
        return emb_prev
    return loop_function


HParams = namedtuple('HParams',
                     'enc_layers, enc_timesteps, dec_timesteps, emb_dim, num_hidden, d_min_lr, d_lr,'
                     'mu, sigma')


class GANInterpretorModel(object):


    def __init__(self, hps, vocab, batch_size, num_classes=2, class_importance=1):
        self._hps = hps
        self._vocab_size = len(vocab)
        self._batch_size = batch_size
        self._num_classes = num_classes
#        self._class_importance=class_importance
        self._B = class_importance * class_importance
        # self._learning_rate = tf.Variable(float(hps.learning_rate), trainable=False)
        # self._learning_rate_decay_op = self._learning_rate.assign(self._learning_rate * hps.learning_rate_decay_factor)
        self._step_g = tf.Variable(0, trainable=False)
        self._step_d = tf.Variable(0, trainable=False)
        self._embedding = tf.get_variable(
            'embedding', [self._vocab_size, hps.emb_dim], dtype=tf.float32,
            initializer=tf.truncated_normal_initializer(stddev=1e-4))


    def _add_placeholders(self):
        """Inputs to be fed to the graph."""
        hps = self._hps
        self._z_input_texts = tf.placeholder(tf.int32,
                                             [self._batch_size, hps.enc_timesteps],
                                             name='z_input_texts')
        self._z_input_lens = tf.placeholder(tf.int32, [self._batch_size],
                                            name='z_input_lens')
        self._z_seeds = tf.placeholder(tf.float32, [self._batch_size, 1],
                                       'z_seeds')

        self._x_input_texts = tf.placeholder(tf.int32,
                                             [self._batch_size, hps.enc_timesteps],
                                             name='x_input_texts')
        self._x_output_classes = tf.placeholder(tf.int32,
                                                [self._batch_size],
                                                name='x_output_classes')
        self._x_output_texts = tf.placeholder(tf.int32,
                                              [self._batch_size, hps.dec_timesteps],
                                              name='x_output_texts')
        self._x_input_lens = tf.placeholder(tf.int32, [self._batch_size],
                                            name='x_input_lens')


    def _add_generator(self):
        with tf.variable_scope('G') as scope:
            self._add_seq2seq(self._z_input_texts, self._z_input_lens, self._z_seeds)
            self._theta_g = [v for v in tf.global_variables() if v.name.startswith(scope.name)]
            #self._summaries_g = tf.summary.merge([s for s in tf.get_collection(tf.GraphKeys.SUMMARIES) if s.name.startswith(scope.name)])


    def _add_descriminator(self):
        with tf.variable_scope('D') as scope:
            emb_decoder_inputs = []
            for x in tf.unpack(tf.transpose(self._x_output_texts)):
                emb_decoder_inputs.append(tf.nn.embedding_lookup(self._embedding, x))

            print("Output Texts:", self._x_output_texts)
            print("Embedded Output Texts:", emb_decoder_inputs)

            self._d1 = self._add_seq_vs_seq(self._x_input_texts, emb_decoder_inputs,
                                            self._x_input_lens, self._x_output_classes)
            tf.summary.scalar('d1', tf.reduce_mean(self._d1))
            scope.reuse_variables()
            self._d2 = self._add_seq_vs_seq(self._z_input_texts, self._synthetic_output_text_embeddings,
                                            self._z_input_lens,  self._synthetic_output_classes)
            tf.summary.scalar('d2', tf.reduce_mean(self._d2))
            self._theta_d = [v for v in tf.global_variables() if v.name.startswith(scope.name)]
            self._summaries_d = tf.summary.merge([s for s in tf.get_collection(tf.GraphKeys.SUMMARIES) if s.name.startswith(scope.name)])



    def _add_seq2seq(self, input_texts, input_lens, z_seeds):
        vsize = self._vocab_size
        hps = self._hps

        with tf.variable_scope('seq2seq'):
            # encoder_inputs = tf.unpack(tf.transpose(input_texts))

            with tf.variable_scope('embedding'), tf.device('/cpu:0'):
                # emb_encoder_inputs = [tf.nn.embedding_lookup(self._embedding, x)
                #                       for x in encoder_inputs]
                emb_encoder_inputs = tf.nn.embedding_lookup(self._embedding, input_texts)
                print('Emb. Enc. Inputs:', emb_encoder_inputs)
                pad_embedding = tf.nn.embedding_lookup(self._embedding, tf.constant(data.PAD_ID, shape=[self._batch_size]))
                end_embedding = tf.nn.embedding_lookup(self._embedding, tf.constant(data.EOS_ID, shape=[self._batch_size]))
                go_embedding = tf.nn.embedding_lookup(self._embedding, tf.constant(data.GO_ID, shape=[self._batch_size]))
                emb_decoder_inputs = [pad_embedding for _ in xrange(hps.dec_timesteps - 2)]
                emb_decoder_inputs.insert(0, go_embedding)
                emb_decoder_inputs.insert(1, end_embedding)

#            for layer_i in xrange(hps.enc_layers):
 #               with tf.variable_scope('encoder%d' % layer_i):
            with tf.variable_scope('encoder'):
                cell_fw = tf.nn.rnn_cell.LSTMCell(
                    hps.num_hidden,
                    initializer=tf.random_uniform_initializer(-0.1, 0.1),
                    state_is_tuple=True)
                cell_bw = tf.nn.rnn_cell.LSTMCell(
                    hps.num_hidden,
                    initializer=tf.random_uniform_initializer(-0.1, 0.1),
                    state_is_tuple=True)
                (outputs, states) = tf.nn.bidirectional_dynamic_rnn(
                    cell_fw, cell_bw, emb_encoder_inputs, dtype=tf.float32,
                    sequence_length=self._z_input_lens)
                outputs_fw, outputs_bw = outputs
                states_fw, states_bw = states

            print('Forward outputs:', outputs_fw)
            print('Backwards outputs:', outputs_bw)
            encoder_outputs = tf.concat(2, outputs)
            print('Encoder outputs:', encoder_outputs)
            print('Forward states:', states_fw)
            print('Backward states:', states_bw)


            with tf.variable_scope('vocabulary_projection'):
                w_text = tf.get_variable(
                    'w_text', [hps.num_hidden, vsize], dtype=tf.float32,
                    initializer=tf.truncated_normal_initializer(stddev=1e-4))
                v_text = tf.get_variable(
                    'v_text', [vsize], dtype=tf.float32,
                    initializer=tf.truncated_normal_initializer(stddev=1e-4))
            #
            # with tf.variable_scope('embedding_projection'):
            #     w_emb = tf.get_variable(
            #         'w_emb', [hps.num_hidden, hps.emb_dim], dtype=tf.float32,
            #         initializer=tf.truncated_normal_initializer(stddev=1e-4))
            #     v_emb = tf.get_variable(
            #         'v_emb', [hps.emb_dim], dtype=tf.float32,
            #         initializer=tf.truncated_normal_initializer(stddev=1e-4))

            with tf.variable_scope('class_projection'):
                w_class = tf.get_variable(
                    'w_class', [2 * hps.num_hidden + 1, self._num_classes], dtype=tf.float32,
                    initializer=tf.truncated_normal_initializer(stddev=1e-4))
                b_class = tf.get_variable(
                    'b_class', [self._num_classes], dtype=tf.float32,
                    initializer=tf.truncated_normal_initializer(stddev=1e-4))

            with tf.variable_scope('decoder'):
                loop_function = _extract_argmax_and_embed(
                        self._embedding, (w_text, v_text), update_embedding=False)

                cell = tf.nn.rnn_cell.LSTMCell(
                    hps.num_hidden,
                    initializer=tf.random_uniform_initializer(-0.1, 0.1),
                    state_is_tuple=False)

#                encoder_outputs = [tf.reshape(x, [self._batch_size, 1, 2 * hps.num_hidden])
#                                   for x in encoder_outputs]
                enc_top_states = encoder_outputs #tf.concat(1, encoder_outputs)
                # noinspection PyUnboundLocalVariable
                dec_in_state = tf.concat(1, [states_fw[0], states_bw[0], z_seeds])

                # During decoding, follow up _dec_in_state are fed from beam_search.
                # dec_out_state are stored by beam_search for next step feeding.
                initial_state_attention = False #(hps.mode == 'decode')
                decoder_outputs, dec_out_state = tf.nn.seq2seq.attention_decoder(
                    emb_decoder_inputs, dec_in_state, enc_top_states,
                    cell, num_heads=1, loop_function=loop_function,
                    initial_state_attention=initial_state_attention)

                with tf.variable_scope('output'):
                    model_outputs = []
                    model_output_embeddings = []
                    for i in xrange(len(decoder_outputs)):
                        if i > 0:
                            tf.get_variable_scope().reuse_variables()
                        v_logits = tf.nn.xw_plus_b(decoder_outputs[i], w_text, v_text)
                        output = tf.argmax(v_logits, axis=1)
                        model_outputs.append(output)

                        instance_embeddings = []
                        for v_logit in  tf.unstack(v_logits, axis=0):
                            v_logits_max = tf.reduce_max(v_logits)
                            v_one_hot = tf.maximum(v_logit - v_logits_max, 0)
                            v_embedded = tf.matmul(tf.expand_dims(v_one_hot, 0), self._embedding)
                            instance_embeddings.append(tf.squeeze(v_embedded))
                        model_output_embeddings.append(tf.stack(instance_embeddings, axis=0))


                        #v_one_hot = tf.maximum(tf.stack([v_logit - v_logits_max for v_logit in ], axis=1), 0)
                        #model_output_embeddings.append(tf.batch_matmul(v_one_hot, self._embedding))
                        #model_output_embeddings.append(tf.nn.xw_plus_b(decoder_outputs[i], w_emb, v_emb))

                    self._synthetic_output_texts = tf.stack(model_outputs, 1)
                    self._synthetic_output_text_embeddings = model_output_embeddings
                    print("Model outputs:", model_outputs)
                    print("Model output embeddings:", model_output_embeddings)
                    softmax_classes = tf.nn.softmax(tf.nn.xw_plus_b(dec_in_state, w_class, b_class))
                    # tf.summary.histogram('P(class)', softmax_classes)
                    self._synthetic_output_classes = tf.cast(tf.argmax(softmax_classes, axis=1), tf.int32)


    def _add_seq_vs_seq(self, input_texts, output_text_embeddings, input_lens, output_classes):
        vsize = self._vocab_size
        hps = self._hps

        with tf.variable_scope('seq_vs_seq'):
            emb_decoder_inputs = output_text_embeddings
            input_lens = input_lens

            with tf.variable_scope('embedding'), tf.device('/cpu:0'):
                emb_encoder_inputs = tf.nn.embedding_lookup(self._embedding, input_texts)

            with tf.variable_scope('encoder'):
                cell_fw = tf.nn.rnn_cell.LSTMCell(
                    hps.num_hidden,
                    initializer=tf.random_uniform_initializer(-0.1, 0.1),
                    state_is_tuple=True)
                cell_bw = tf.nn.rnn_cell.LSTMCell(
                    hps.num_hidden,
                    initializer=tf.random_uniform_initializer(-0.1, 0.1),
                    state_is_tuple=True)
                (outputs, states) = tf.nn.bidirectional_dynamic_rnn(
                    cell_fw, cell_bw, emb_encoder_inputs, dtype=tf.float32,
                    sequence_length=input_lens)
                states_fw, states_bw = states

            encoder_outputs = tf.concat(2, outputs)

            with tf.variable_scope('decoder'):
                cell = tf.nn.rnn_cell.LSTMCell(
                    hps.num_hidden,
                    initializer=tf.random_uniform_initializer(-0.1, 0.1, seed=113),
                    state_is_tuple=False)

                #encoder_outputs = [tf.reshape(x, [self._batch_size, 1, 2 * hps.num_hidden])
                #                   for x in encoder_outputs]
                enc_top_states = encoder_outputs #tf.concat(1, encoder_outputs)
                # noinspection PyUnboundLocalVariable
                dec_in_state = tf.concat(1, [states_fw[0], states_bw[0]])
                # During decoding, follow up _dec_in_state are fed from beam_search.
                # dec_out_state are stored by beam_search for next step feeding.
                print("S2S Attn. Decoder Inputs: ", emb_decoder_inputs)
                decoder_outputs, dec_out_state = tf.nn.seq2seq.attention_decoder(
                    emb_decoder_inputs, dec_in_state, enc_top_states,
                    cell, num_heads=1, initial_state_attention=False)


                w_class = tf.get_variable("w_class_d", [hps.num_hidden * 2, self._num_classes],
                                    dtype=tf.float32,
                                    initializer=tf.truncated_normal_initializer(stddev=1e-4))
                b_class = tf.get_variable("b_class_d", [self._num_classes],
                                    dtype=tf.float32,
                                    initializer=tf.truncated_normal_initializer(stddev=1e-4))
                class_predictions = tf.expand_dims(tf.cast(tf.cast(tf.argmax(tf.nn.softmax(tf.batch_matmul(dec_in_state, w_class) + b_class), axis=1), tf.int32) - output_classes, tf.float32), -1)

                # Decoder outputs = [time] x [batch_size x features]
                output = tf.concat(1, [decoder_outputs[-1], class_predictions]) # tf.stack([decoder_outputs[output_lens[i]][i, :] for i in xrange(self._batch_size)])
                w_d = tf.get_variable("w_d", [hps.num_hidden + 1, 1],
                                    dtype=tf.float32,
                                    initializer=tf.truncated_normal_initializer(stddev=1e-4))
                b_d = tf.get_variable("b_d", [1],
                                    dtype=tf.float32,
                                    initializer=tf.truncated_normal_initializer(stddev=1e-4))

                return _clamp_as_probability(tf.sigmoid(tf.batch_matmul(output, w_d) + b_d))


    def _add_train_ops(self):
        hps = self._hps
        self._loss_d = tf.reduce_mean(-tf.log(self._d1) -tf.log(1 - self._d2))
        tf.summary.scalar('D loss', self._loss_d)
        self._lr_rate_d = tf.maximum(
            hps.d_min_lr,  # min_lr_rate.
            tf.train.exponential_decay(hps.d_lr, self._step_d, 30000, 0.98))
        tf.summary.scalar('D learning rate', self._lr_rate_d)
        self._opt_d = tf.train.GradientDescentOptimizer(0.01).minimize(self._loss_d,
                                                                       global_step=self._step_d,
                                                                       var_list=self._theta_d)

        # In GAN papers, the loss function to optimize G is min (log 1-D),
        # but in practice folks practically use max log D
        # because the first formulation has vanishing gradients early on
        # Goodfellow et. al (2014)
        # from: https://github.com/soumith/ganhacks
        self._loss_g = tf.reduce_mean(tf.log(-self._d2))
        tf.summary.scalar('G loss', self._loss_g)
        adam = tf.train.AdamOptimizer()
        tf.summary.scalar('G learning rate', adam._lr)
        self._opt_g = adam.minimize(self._loss_g,
                                                        global_step=self._step_g,
                                                        var_list=self._theta_g)


    def build_graph(self):
        print('Adding placeholders...')
        self._add_placeholders()
        print('Adding generator...')
        self._add_generator()
        print('Adding descriminator...')
        self._add_descriminator()
        print('Adding train ops...')
        self._add_train_ops()
        self.saver = tf.train.Saver(tf.global_variables())


    def _get_z_seeds(self):
        hps = self._hps
        batch_size = self._batch_size
        return np.random.normal(hps.mu, hps.sigma, (batch_size, 1))


    def run_train_d_step(self, sess, z_input_texts, z_input_lens,
                                     x_input_texts, x_input_lens, x_output_texts, x_output_classes):
        _, summaries, d1, loss_d, step_d = sess.run(
            [self._opt_d, self._summaries_d, self._d1, self._loss_d, self._step_d],
            feed_dict={self._z_input_texts: z_input_texts,
                       self._z_input_lens: z_input_lens,
                       self._z_seeds: self._get_z_seeds(),
                       self._x_input_texts: x_input_texts,
                       self._x_input_lens: x_input_lens,
                       self._x_output_texts: x_output_texts,
                       self._x_output_classes: x_output_classes})
        return d1, summaries, loss_d, step_d


    def run_train_g_step(self, sess, z_input_texts, z_input_lens):
        hps = self._hps
        _, summaries, d2, texts, classes, loss_g, step_g = sess.run(
            [self._opt_g, self._summaries_d, self._d2,
             self._synthetic_output_texts, self._synthetic_output_classes,
             self._loss_g, self._step_g],
            feed_dict={
                self._z_input_texts: z_input_texts,
                self._z_input_lens: z_input_lens,
                self._z_seeds: self._get_z_seeds(),
                self._x_input_texts: np.zeros([self._batch_size, hps.enc_timesteps]),
                self._x_input_lens: np.zeros([self._batch_size]),
                self._x_output_texts: np.zeros([self._batch_size, hps.dec_timesteps]),
                self._x_output_classes: np.zeros([self._batch_size])})
        return d2, summaries, texts, classes, loss_g, step_g

    # self._x_input_texts = tf.placeholder(tf.int32,
    #                                      [self._batch_size, hps.enc_timesteps],
    #                                      name='x_input_texts')
    # self._x_output_classes = tf.placeholder(tf.int32,
    #                                         [self._batch_size],
    #                                         name='x_output_classes')
    # self._x_output_texts = tf.placeholder(tf.int32,
    #                                       [self._batch_size, hps.dec_timesteps],
    #                                       name='x_output_texts')
    # self._x_input_lens = tf.placeholder(tf.int32, [self._batch_size],
    #                                     name='x_input_lens')


    def run_eval_step(self, sess,  z_input_texts, z_input_lens):
        texts, classes = sess.run(
            [self._synthetic_output_texts, self._synthetic_output_classes],
            feed_dict={self._z_input_texts: z_input_texts,
                       self._z_input_lens: z_input_lens,
                       self._z_seeds: self._get_z_seeds()})
        return texts, classes