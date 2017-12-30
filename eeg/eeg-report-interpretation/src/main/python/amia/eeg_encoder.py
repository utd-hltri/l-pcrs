import time
from itertools import izip

import numpy as np
import tensorflow as tf

import eeg_data

flags = tf.flags
logging = tf.logging

# define a command line argument (name, default value, message)
flags.DEFINE_string("data_path", None, "path to training data")
flags.DEFINE_string("model_dir", None, "path to save model")

FLAGS = flags.FLAGS

class EegModel(object):
    """ EEG Auto-Encoder """

    def __init__(self, is_training, config):
        # Define model parameters
        self.batch_size = batch_size = config.batch_size
        self.num_steps = num_steps = config.num_steps
        self.signal_dim = signal_dim = config.signal_dim
        self.memory_dim = memory_dim =  config.memory_dim
        self.num_layers = config.num_layers

        # Define the input & output variables
        self._input_data = inputs = tf.placeholder(tf.float32, [batch_size, num_steps, signal_dim], name="inputd")
        self._target_data = target_data = tf.placeholder(tf.float32, [batch_size, num_steps, signal_dim], name="targetd")

        # Define the LSTM cell template
        lstm_cell = tf.nn.rnn_cell.BasicLSTMCell(memory_dim, forget_bias=1.0)
        if is_training and config.keep_prob < 1:
            lstm_cell = tf.nn.rnn_cell.DropoutWrapper(lstm_cell, output_keep_prob=config.keep_prob)

        # Define the stacked LSTM network
        cell = tf.nn.rnn_cell.MultiRNNCell([lstm_cell] * config.num_layers)
        self._initial_state = cell.zero_state(batch_size, tf.float32)

        if is_training and config.keep_prob < 1:
            inputs = tf.nn.dropout(inputs, config.keep_prob)

        # Define the RNN connections from input to the LSTM stack to output
        inputs = [tf.squeeze(input_, [1])
                  for input_ in tf.split(1, num_steps, inputs)]
        targets = [tf.squeeze(target_, [1])
                   for target_ in tf.split(1, num_steps, target_data)]

        outputs, state = tf.nn.rnn(cell, inputs, initial_state=self._initial_state)

        # Softplus layer
        softplus_w = tf.get_variable("softplus_w", [memory_dim, signal_dim])
        softplus_b = tf.get_variable("softplus_b", [signal_dim])

        # Compute cosine distance between each state & the correct input
        dists = []
        for o, t in izip(outputs, targets):
            ow = tf.matmul(o, softplus_w)
            owb = tf.add(ow, softplus_b)
            owb_norm = tf.nn.l2_normalize(owb, dim=0)
            t_norm = tf.nn.l2_normalize(t, dim=0)
            dist = tf.matmul(owb_norm, t_norm, transpose_b=True)
            dists.append(tf.pack([dist[i, i] for i in xrange(batch_size)]))

        self._cost = cost = tf.reduce_sum(tf.add_n(dists, name="tdist")) // batch_size
        self._final_state = state[-1]

        if not is_training:
            return

        # Calculate the gradient of the loss function
        # Create an optimizer.
        opt = tf.train.AdamOptimizer()

        # Compute the gradients for a list of variables.
        tvars = tf.trainable_variables()
        grads_and_vars = opt.compute_gradients(cost, tvars)

        # Ask the optimizer to apply the capped gradients.
        self._train_op = opt.apply_gradients(grads_and_vars)

    def assign_lr(self, session, lr_value):
        session.run(tf.assign(self.lr, lr_value))

    @property
    def input_data(self):
        return self._input_data

    @property
    def target_data(self):
        return self._target_data

    @property
    def inputs(self):
        return self._inputs

    @property
    def targets(self):
        return self._targets

    @property
    def initial_state(self):
        return self._initial_state

    @property
    def cost(self):
        return self._cost

    @property
    def final_state(self):
        return self._final_state

    @property
    def lr(self):
        return self._lr

    @property
    def train_op(self):
        return self._train_op

class StandardConfig(object):
    signal_dim = 31
    batch_size = 1
    num_layers = 2
    memory_dim = 200
    num_steps = 20
    keep_prob = 1
    init_scale = 0.1
    learning_rate = 1.0
    max_grad_norm = 5
    max_epoch = 4
    max_max_epoch = 13
    lr_decay = 1 / 2


def run_epoch(session, model, data, eval_op, verbose=False):
    """Runs the model on the given data."""
    start_time = time.time()
    costs = 0.0
    iters = 0
    state = model.initial_state.eval()
    lastn = 0
    for step, (x, y, n) in enumerate(data):
        _, cost, state = session.run([eval_op, model.cost, model.final_state],
                                    {model.input_data: x,
                                     model.target_data: y,
                                     model.initial_state: state})
        costs += cost
        iters += model.num_steps

        if n != lastn:
            state = model.initial_state.eval()
            lastn = n

        if step % 100 == 0:
            print("%.3f cost: %.3f speed: %.0f ips" %
              (step * 1.0, np.exp(costs / iters),
               iters * model.batch_size / (time.time() - start_time)))

    return np.exp(costs / iters)


def main(unused_args):
    config = StandardConfig()
    # eval_config = StandardConfig()
    # eval_config.batch_size = 1
    # eval_config.num_steps = 1

    train_data = eeg_data.edf_iterator(FLAGS.data_path, config.batch_size, config.num_steps, config.signal_dim)
    with tf.Graph().as_default(), tf.Session() as session:


        initializer = tf.random_uniform_initializer(-config.init_scale,
                                                    config.init_scale)
        with tf.variable_scope("model", reuse=None, initializer=initializer):
            model = EegModel(is_training=True, config=config)
        # with tf.variable_scope("model", reuse=True, initializer=initializer):
        #     mvalid = EegModel(is_training=False, config=config)

        tf.initialize_all_variables().run()

        saver = tf.train.Saver()
        for i in range(config.max_max_epoch):
            lr_decay = config.lr_decay ** max(i - config.max_epoch, 0.0)
            model.assign_lr(session, config.learning_rate * lr_decay)

            print("Epoch: %d Learning rate: %.3f" % (i + 1, session.run(model.lr)))
            train_cost = run_epoch(session, model, train_data, model.train_op, verbose=True)
            print("Epoch: %d Train Cost: %.3f" % (i + 1, train_cost))
            # train_cost = run_epoch(session, mvalid, valid_data, tf.no_op())
            # print("Epoch: %d Valid Cost: %.3f" % (i + 1, valid_cost))
            print("--------------------------------")
            saver.save(session, FLAGS.model_dir, global_step=i)


if __name__ == "__main__":
    tf.app.run()
