import os
import time

import tensorflow as tf
import numpy as np
import sklearn as sk
import semeval_data

from tensorflow.python.ops import seq2seq
from tensorflow.models.rnn import rnn
from sklearn import metrics

flags = tf.flags
logging = tf.logging

# define a command line argument (name, default value, message)
flags.DEFINE_string("data_path", None, "data_path")
flags.DEFINE_string(
        "model", "small",
        "A type of model. Possible options are: small, medium, large.")

FLAGS = flags.FLAGS

class SemevalModel(object):
  """The model."""

  def __init__(self, is_training, config):
    self.batch_size = batch_size = config.batch_size
    self.num_steps = num_steps = config.num_steps
    hidden_size = config.hidden_size
    self.vocab_size = vocab_size = config.vocab_size
    num_labels = config.labels_size

    self._input_data = inputs = tf.placeholder(tf.float32, [batch_size, num_steps, vocab_size])
    self._targets = tf.placeholder(tf.int32, [batch_size, num_steps])
    self._gold = tf.placeholder(tf.int32, [batch_size * num_steps])
    self._pred = tf.placeholder(tf.int32, [batch_size * num_steps])

    if is_training and config.keep_prob < 1:
      inputs = tf.nn.dropout(inputs, config.keep_prob)

    # Define Embedding Layer
    # define embedding parameters
    emb_w = tf.get_variable("emb_w", [vocab_size, hidden_size])
    emb_b = tf.get_variable("emb_b", [hidden_size])
    # calc embeddings: split the inputs along the timestep dimension into [batch_size, vocab_size] input matrices
    # and embed each input matrix
    embeddings = [tf.nn.relu_layer(tf.squeeze(input_,[1]), emb_w, emb_b)
                  for input_ in tf.split(1, num_steps, inputs)]

    # define the lstm cell
    lstm_cell = rnn.rnn_cell.BasicLSTMCell(hidden_size, forget_bias=1.0)
    if is_training and config.keep_prob < 1:
      lstm_cell = rnn.rnn_cell.DropoutWrapper(lstm_cell, output_keep_prob=config.keep_prob)

    #print(lstm_cell.output_size)
    #print(lstm_cell.input_size)

    # define lstm network
    cell = rnn.rnn_cell.MultiRNNCell([lstm_cell] * config.num_layers)
    self._initial_state = cell.zero_state(batch_size, tf.float32)
    outputs, state = rnn.rnn(cell, embeddings, initial_state=self._initial_state)

    # output = [batch_size * timestep, dimension]
    output = tf.reshape(tf.concat(1, outputs), [-1, hidden_size])
    softmax_w = tf.get_variable("softmax_w", [hidden_size, num_labels])
    softmax_b = tf.get_variable("softmax_b", [num_labels])
    logits = tf.matmul(output, softmax_w) + softmax_b

    loss = seq2seq.sequence_loss_by_example([logits],
                                            [tf.reshape(self._targets, [-1])],
                                            [tf.ones([batch_size * num_steps])],
                                            num_labels)

    # predictions = tf.equal(, tf.cast(tf.reshape(self._targets, [-1]), tf.int64))
    self._pred = tf.reshape(tf.argmax(logits, 1), [-1])

    self._cost = cost = tf.reduce_sum(loss) / batch_size
    self._final_state = state[-1]

    #self._accuracy = tf.reduce_mean(tf.cast(predictions, tf.float32))
    self._gold = tf.reshape(self._targets, [-1])

    if not is_training:
      return

    self._lr = tf.Variable(0.0, trainable=False)
    tvars = tf.trainable_variables()
    grads, _ = tf.clip_by_global_norm(tf.gradients(cost, tvars),
                                      config.max_grad_norm)
    optimizer = tf.train.GradientDescentOptimizer(self.lr)
    self._train_op = optimizer.apply_gradients(zip(grads, tvars))

  def assign_lr(self, session, lr_value):
    session.run(tf.assign(self.lr, lr_value))

  @property
  def input_data(self):
    return self._input_data

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
  def pred(self):
    return self._pred

  @property
  def gold(self):
    return self._gold

  @property
  def final_state(self):
    return self._final_state

  @property
  def lr(self):
    return self._lr

  @property
  def train_op(self):
    return self._train_op

class SmallConfig(object):
  """Small config."""
  init_scale = 0.1
  learning_rate = 1.0
  max_grad_norm = 5
  num_layers = 2
  num_steps = 20
  hidden_size = 100
  max_epoch = 4
  max_max_epoch = 13
  keep_prob = 1.0
  lr_decay = 0.5
  batch_size = 20
  vocab_size = 131119
  labels_size = 4
  embedding_size = 100

# class MediumConfig(object):
#   """Medium config."""
#   init_scale = 0.05
#   learning_rate = 1.0
#   max_grad_norm = 5
#   num_layers = 2
#   num_steps = 35
#   hidden_size = 650
#   max_epoch = 6
#   max_max_epoch = 39
#   keep_prob = 0.5
#   lr_decay = 0.8
#   batch_size = 35
#   vocab_size = 10000
#   labels_size = 4
#   embedding_size = 200
#
#
# class LargeConfig(object):
#   """Large config."""
#   init_scale = 0.04
#   learning_rate = 1.0
#   max_grad_norm = 10
#   num_layers = 2
#   num_steps = 35
#   hidden_size = 1500
#   max_epoch = 14
#   max_max_epoch = 55
#   keep_prob = 0.35
#   lr_decay = 1 / 1.15
#   batch_size = 35
#   vocab_size = 10000
#   labels_size = 4
#   embedding_size = 1500


def run_epoch(session, model, data, eval_op, verbose=False):
  """Runs the model on the given data."""
  epoch_size = ((len(data) // model.batch_size) - 1) // model.num_steps
  start_time = time.time()
  costs = 0.0
  iters = 0
  state = model.initial_state.eval()
  predictions = []
  gold_labels = []
  for step, (x, y) in enumerate(
          semeval_data.batch_iterator(data, model.num_steps, model.vocab_size, model.batch_size)):
    cost, _, pred, gold = session.run([model.cost, eval_op, model.pred, model.gold],
                                   {model.input_data: x,
                                    model.targets: y,
                                    model.initial_state: state})
    costs += cost
    iters += model.num_steps
    predictions.extend(pred)
    gold_labels.extend(gold)

    if step % 100 == 0:
      print("%.3f cost: %.3f speed: %.0f ips" %
            (step * 1.0, np.exp(costs / iters),
             iters * model.batch_size / (time.time() - start_time)))

  labelset = np.unique(gold_labels)
  precision = sk.metrics.precision_score(gold_labels, predictions, average=None, labels=labelset)
  recall = sk.metrics.recall_score(gold_labels, predictions, average=None, labels=labelset)
  f1 = sk.metrics.f1_score(gold_labels, predictions, average=None, labels=labelset)
  return np.exp(costs / iters), precision, recall, f1


def get_config():
  if FLAGS.model == "small":
    return SmallConfig()
  elif FLAGS.model == "medium":
    return MediumConfig()
  elif FLAGS.model == "large":
    return LargeConfig()
  else:
    raise ValueError("Invalid model: %s", FLAGS.model)


def main(unused_args):
  if not FLAGS.data_path:
    raise ValueError("Must set --data_path to labeled sparse vector file")
  if not FLAGS.model:
    raise ValueError("Must set --model to one of (small, medium, large)")

  config = get_config()
  eval_config = get_config()
  eval_config.batch_size = 1
  eval_config.num_steps = 1

  train_data, valid_data = semeval_data.raw_data(FLAGS.data_path)

  with tf.Graph().as_default(), tf.Session() as session:
    initializer = tf.random_uniform_initializer(-config.init_scale,
                                                config.init_scale)
    with tf.variable_scope("model", reuse=None, initializer=initializer):
      model = SemevalModel(is_training=True, config=config)
    with tf.variable_scope("model", reuse=True, initializer=initializer):
      mvalid = SemevalModel(is_training=False, config=config)

    tf.initialize_all_variables().run()
    saver = tf.train.Saver()

    for i in range(config.max_max_epoch):
      lr_decay = config.lr_decay ** max(i - config.max_epoch, 0.0)
      model.assign_lr(session, config.learning_rate * lr_decay)

      step = i + 1
      print("Epoch: %d Learning rate: %.3f" % (step, session.run(model.lr)))
      train_perplexity, train_precision, train_recall, train_f1 = run_epoch(session, model, train_data, model.train_op, verbose=True)
      print("Epoch: %d Train Perplexity: %.3f" % (step, train_perplexity))
      print("Epoch: %d Train Precision:" % step)
      print(train_precision)
      print("Epoch: %d Train Recall:" % step)
      print(train_recall)
      print("Epoch: %d Train F1:" % step)
      print(train_f1)
      valid_perplexity, val_precision, val_recall, val_f1 = run_epoch(session, mvalid, valid_data, tf.no_op())
      print("Epoch: %d Valid Perplexity: %.3f" % (step, valid_perplexity))
      print("Epoch: %d Valid Precision:" % step)
      print(val_precision)
      print("Epoch: %d Valid Recall:" % step)
      print(val_recall)
      print("Epoch: %d Valid F1:" % step)
      print(val_f1)
      print("--------------------------------")
      save_path = saver.save(session, os.path.join(FLAGS.data_path, "model.ckpt"), global_step=step)
      print('saved model at %s' % (save_path))

if __name__ == "__main__":
  tf.app.run()