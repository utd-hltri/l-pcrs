from itertools import izip

import tensorflow as tf


def _clamp_as_cos(tensor):
    return tf.maximum(tf.minimum(tensor, .99), -.99)


class SignalEncoderModel(object):
    """ EEG Auto-Encoder """

    def _make_rnn_cell(self):
        lstm_cell = tf.nn.rnn_cell.BasicLSTMCell(self.memory_dim, forget_bias=1.0, state_is_tuple=False)
        if self.is_training and self.keep_prob < 1:
            lstm_cell = tf.nn.rnn_cell.DropoutWrapper(lstm_cell, output_keep_prob=self.keep_prob)

        cell = tf.nn.rnn_cell.MultiRNNCell([lstm_cell] * self.num_layers, state_is_tuple=False)
        return cell

    def __init__(self, is_training, config):
        # Define model parameters
        self.batch_size = config.batch_size
        self.num_steps = config.num_steps
        self.signal_dim = config.signal_dim
        self.memory_dim = config.memory_dim
        self.num_layers = config.num_layers
        self.keep_prob = config.keep_prob
        self.is_training = is_training

    def _add_placeholders(self):
        # Define the input & output variables
        self._inputs = tf.placeholder(tf.float32, [self.batch_size, self.num_steps, self.signal_dim],
                                      name="inputs")
        self._lengths = tf.placeholder(tf.int32, [self.batch_size], name="lengths")

    def _add_encoder(self):
        with tf.variable_scope("fw_encoder"):
            # Define the LSTM cell template
            fw_cell = self._make_rnn_cell()
            self.initial_state = fw_cell.zero_state(self.batch_size, tf.float32)

            inputs = self._inputs
            if self.is_training and self.keep_prob < 1:
                inputs = tf.nn.dropout(self._inputs, self.keep_prob)

            # Define the RNN connections from input to the LSTM stack to output
            self._inputs_list = [tf.squeeze(input_, [1])
                                 for input_ in tf.split(1, self.num_steps, inputs)]

            outputs, state = tf.nn.rnn(fw_cell, self._inputs_list, initial_state=self.initial_state,
                                       sequence_length=self._lengths)

            self._final_state = state

    def _add_decoder(self):
        with tf.variable_scope("bw_decoer"):
            bw_cell = self._make_rnn_cell()

            decoder_inputs = [tf.zeros([self.batch_size, self.signal_dim], tf.float32)] + self._inputs_list[:-1]

            outputs, _ = tf.nn.rnn(bw_cell, decoder_inputs, initial_state=self._final_state,
                                   sequence_length=self._lengths)

            self._outputs = outputs

    def _add_loss(self):
        # Softplus layer
        softplus_w = tf.get_variable("softplus_w", [self.memory_dim, self.signal_dim])
        softplus_b = tf.get_variable("softplus_b", [self.signal_dim])

        dists = []
        for o, t in izip(self._outputs, self._inputs_list):
            # project output into signal space
            ow = tf.matmul(o, softplus_w)
            owb = tf.add(ow, softplus_b)
            # compute cosine distance
            owb_norm = tf.nn.l2_normalize(owb, dim=0)
            t_norm = tf.nn.l2_normalize(t, dim=0)
            cos_sim = tf.reduce_sum(tf.matmul(owb_norm, t_norm, transpose_b=True), reduction_indices=1)
            # compute angular distance
            ang_dist = tf.acos(_clamp_as_cos(cos_sim))
            dists.append(ang_dist)

        self._loss = tf.reduce_mean(tf.reduce_mean(dists, name="cost", reduction_indices=1))

    def _add_train_opt(self):
        # Calculate the gradient of the loss function
        self._lr = tf.Variable(0.0, trainable=False)
        tvars = tf.trainable_variables()
        # grads, _ = tf.clip_by_global_norm(tf.gradients(self._loss, tvars), self._max_grad_norm)
        grads = tf.gradients(self._loss, tvars)
        optimizer = tf.train.AdamOptimizer()
        self._train_op = optimizer.apply_gradients(zip(grads, tvars))

    def build_graph(self):
        self._add_placeholders()
        self._add_encoder()
        self._add_decoder()
        self._add_loss()
        if self.is_training:
            self._add_train_opt()
        # noinspection PyAttributeOutsideInit
        self.saver = tf.train.Saver(tf.all_variables())

    def run_train_step(self, session, inputs, lengths, state):
        _, loss, output = session.run(
            [self._train_op, self._loss, self._final_state],
            feed_dict={self._inputs: inputs,
                       self._lengths: lengths,
                       self.initial_state: state})
        return loss, output

    def run_eval_step(self, session, inputs, lengths, state):
        output = session.run(
            [self._final_state],
            feed_dict={self._inputs: inputs,
                       self._lengths: lengths,
                       self.initial_state: state})
        return output[0]
