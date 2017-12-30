
import numpy as np
import tensorflow as tf


class SignalClassifierModel(object):
    """Wrapper for Tensorflow model graph for classifying EEG signals."""

    def __init__(self, params, num_gpus=0):
        """ Initialize new Signal Classifier using given hyper-parameters"""
        self._params = params
        self.seed = params.seed or 1337
        self._num_gpus = num_gpus
        self._cur_gpu = 0

    def _add_placeholders(self):
        params = self._params

        self._signals = tf.placeholder(tf.float32, [params.batch_size, params.timesteps],
                                    name='signals')
        self._classes = tf.placeholder(tf.int32, [params.batch_size], name='classes')
        self._signal_lens = tf.placeholder(tf.int32, [params.batch_size], name='signal_lens')

        if params.dropout:
            self._keep_prob = tf.placeholder(tf.float32)

    def _add_model(self):
        params = self._params

        with tf.variable_scope('signal_encoder'):
            encoder_inputs = tf.unpack(tf.transpose(self._signals))
            classes = tf.unpack(tf.transpose(self._classes))
            signal_lens = self._signal_lens

            with tf.variable_scope('convolution'):
                W = tf.Variable(tf.zeros([params.hidden_size, params.num_labels]), name='W')
                conv_encoder_inputs = [tf.nn.conv2d(encoder_inputs, W, strides=[1, 1, 1, 1], padding='SAME') for x in encoder_inputs]

            for layer_i in xrange(params.encoder_layers):
                with tf.variable_scope('encoder%d' % layer_i), tf.device(self._next_device()):
                    cell_fw = tf.nn.rnn_cell.LSTMCell(
                        params.hidden_size,
                        initializer = tf.random_uniform_initializer(-0.1, 0.1, seed=params.seed))
                    cell_bw = tf.nn.rnn_cell.LSTMCell(
                        params.hidden_size,
                        initializer = tf.random_uniform_initializer(-0.1, 0.1, seed=params.seed))
                    (conv_encoder_inputs, fw_state, _) = tf.nn.bidirectional_rnn(cell_fw, cell_bw,
                        conv_encoder_inputs, dtype=tf.float32, sequence_length=signal_lens)
            encoder_output = conv_encoder_inputs[-1]

            with tf.variable_scope('classifier'), tf.device(self._next_device()):
                W = tf.Variable(tf.zeros([params.hidden_size, params.num_labels]), name='W')
                b = tf.Variable(tf.zeros([params.num_labels]), name='b')
                if params.dropout:
                    encoder_output = tf.nn.dropout(encoder_output, self._keep_prob)
                logits = tf.matmul(encoder_output, W) + b

            with tf.variable_scope('loss'), tf.device(self._next_device()):
                self._loss = tf.nn.sparse_softmax_cross_entropy_with_logits(logits, classes, name='x-entropy')
                tf.scalar_summary('loss', self._loss)

    def _add_train_op(self):
        """Sets self._train_op, op to run for training."""
        params = self._params

        trainable_vars = tf.trainable_variables()
        with tf.device(self._get_gpu(self._num_gpus-1)):
            grads, global_norm = tf.clip_by_global_norm(tf.gradients(self._loss, trainable_vars),
                                                        params.max_grad_norm)
            tf.scalar_summary('global_norm', global_norm)
            optimizer = tf.train.AdamOptimizer()
            tf.scalar_summary = optimizer._lr
            self._train_op = optimizer.apply_gradients(zip(grads, trainable_vars),
                                                       global_step=self.global_step,
                                                       name='train_step')

    def _next_device(self):
        """Round robin the gpu device. (Reserve last gpu for expensive op)."""
        if self._num_gpus == 0:
            return ''
        dev = '/gpu:%d' % self._cur_gpu
        self._cur_gpu = (self._cur_gpu + 1) % (self._num_gpus-1)
        return dev

    def _get_gpu(self, gpu_id):
        if self._num_gpus <= 0 or gpu_id >= self._num_gpus:
            return ''
        return '/gpu:%d' % gpu_id

    def build_graph(self):
        self._add_placeholders()
        self._add_model()
        self.global_step = tf.Variable(0, name='global_step', trainable=False)
        if self._params.mode == 'train':
            self._add_train_op()
        self._summaries = tf.merge_all_summaries()

    def run_train_step(self, sess, signal_batch, targets, signal_lens):
        to_return = [self._train_op, self._summaries, self._loss, self.global_step]
        return sess.run(to_return,
                        feed_dict={self._signals : signal_batch,
                                   self._classes: targets,
                                   self._signal_lens: signal_lens})

    def run_eval_step(self, sess, signal_batch, targets, signal_lens):
        to_return = [self._summaries, self._loss, self.global_step]
        return sess.run(to_return,
                        feed_dict={self._signals: signal_batch,
                                   self._classes: targets,
                                   self._signal_lens: signal_lens})



