import os
import time

import tensorflow as tf
import numpy as np
import sklearn as sk
import data_utils

from tensorflow.python.ops import seq2seq
from sklearn import metrics

class LstmModel(object):
  """The model."""

  def __init__(self, is_training, config):
    self.batch_size = batch_size = config.batch_size
    self.num_steps = num_steps = config.num_steps
    hidden_size = config.hidden_size
    self.vocab_size = vocab_size = config.vocab_size
    num_labels = config.labels_size

    self._input_data = inputs = tf.placeholder(tf.float32, [batch_size, num_steps, vocab_size], name='inputs')
    self._pred = tf.placeholder(tf.int32, [batch_size * num_steps], name='predictions')
    self.lengths = tf.placeholder(tf.int32, [batch_size], name='lengths')
    self._targets = tf.placeholder(tf.int32, [batch_size, num_steps], name='targets')
    self._gold = tf.placeholder(tf.int32, [batch_size * num_steps], name='gold')

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
    lstm_cell = tf.nn.rnn_cell.BasicLSTMCell(hidden_size, forget_bias=1.0)
    if is_training and config.keep_prob < 1:
      lstm_cell = tf.nn.rnn_cell.DropoutWrapper(lstm_cell, output_keep_prob=config.keep_prob)

    # define lstm network
    # stack the lstm num_layers times
    cell = tf.nn.rnn_cell.MultiRNNCell([lstm_cell] * config.num_layers)
    # define the initial state
    self._initial_state = cell.zero_state(batch_size, tf.float32)
    # define rnn
    outputs, state = tf.nn.rnn(cell, embeddings, initial_state=self._initial_state, sequence_length=self.lengths)

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

    # self._accuracy = tf.reduce_mean(tf.cast(predictions, tf.float32))
    self._gold = tf.reshape(self._targets, [-1])

    # back propagation
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
  def lr(self):
    return self._lr

  @property
  def train_op(self):
    return self._train_op

class ThymeConfig(object):
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
  labels_size = 2
  embedding_size = 100

class I2b2Config(object):
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
  vocab_size = 138075
  labels_size = 3
  embedding_size = 100

class I2b2IOConfig(object):
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
  vocab_size = 138075
  labels_size = 2
  embedding_size = 100


def run_epoch(session, model, data, eval_op, verbose=False):
  """Runs the model on the given data."""
  start_time = time.time()
  costs = 0.0
  iters = 0
  state = model.initial_state.eval()
  predictions = []
  gold_labels = []
  for step, (x, y, lengths) in enumerate(data_utils.seq_batch_iterator(data, model.num_steps, model.vocab_size, model.batch_size)):
    cost, _, pred, gold = session.run([model.cost, eval_op, model.pred, model.gold],
                                      {model.input_data: x,
                                       model.targets: y,
                                       model.initial_state: state,
                                       model.lengths: lengths})
    costs += cost
    iters += model.num_steps
    predictions.extend(pred)
    gold_labels.extend(gold)

    if step % 100 == 0:
      print("Step %d cost: %.3f speed: %.0f ips" %
            (step, np.exp(costs / iters),
             iters * model.batch_size / (time.time() - start_time)))

  labelset = np.unique(gold_labels)
  if len(labelset) == 2:
    print("using binary evaluation")
    pl = data_utils.seq_label_idxs['I']
    precision = sk.metrics.precision_score(gold_labels, predictions, pos_label=pl)
    recall = sk.metrics.recall_score(gold_labels, predictions, pos_label=pl)
    f1 = sk.metrics.f1_score(gold_labels, predictions, pos_label=pl)
    report = sk.metrics.classification_report(gold_labels, predictions)
    return np.exp(costs / iters), precision, recall, f1, f1, report
  else:
    precision = sk.metrics.precision_score(gold_labels, predictions, average=None, labels=labelset)
    recall = sk.metrics.recall_score(gold_labels, predictions, average=None, labels=labelset)
    f1 = sk.metrics.f1_score(gold_labels, predictions, average=None, labels=labelset)
    overall_f1 = sk.metrics.f1_score(gold_labels, predictions, average='micro', labels=labelset)
    report = sk.metrics.classification_report(gold_labels, predictions, labels=labelset)
    return np.exp(costs / iters), precision, recall, f1, overall_f1, report


def predict_lstm(config):
  config.val_prob = -1
  config.type = None
  assert config.data_filename is not None
  input, _ = data_utils.raw_data_boundary(config, name=config.data_filename)

  config.batch_size = min(len(input), config.batch_size)
  with tf.Graph().as_default(), tf.Session() as session:
    with tf.variable_scope(config.type, reuse=None):
      model = LstmModel(is_training=False, config=config)
    saver = tf.train.Saver()
    ckpt_dir = config.model_dir
    ckpt = tf.train.get_checkpoint_state(ckpt_dir, config.checkpoint)
    print('loading model from ', ckpt)
    if ckpt and ckpt.model_checkpoint_path:
      saver.restore(session, ckpt.model_checkpoint_path)
    state = model.initial_state.eval()
    predictions = []
    print("raw inputs: %s" % input)
    for step, (x, y, lengths, names) in enumerate(
        data_utils.seq_batch_iterator(input, model.num_steps, model.vocab_size, model.batch_size, True)):
      pred = session.run([model.pred], {model.input_data: x, model.lengths: lengths, model.initial_state: state})
      pred = np.reshape(pred[0], [model.batch_size, model.num_steps])
      idx = 0
      pstart = len(predictions)
      for (length, p) in zip(lengths, pred):
        predictions.extend(zip(names[idx:idx + length], p[:length]))
        idx += length
      print('names: %s, sum(lengths): %s, new preds: %s' % (len(names), sum(lengths), len(predictions) - pstart))
  print('writing %s predictions to %s' % (len(predictions), config.pred_file))
  idx2label = {v:k for (k,v) in data_utils.seq_label_idxs.iteritems()}
  with open(os.path.join(config.data_dir, config.pred_file), "w+") as outfile:
    for pred in predictions:
      outfile.write('%s %s\n' % (pred[0], idx2label[pred[1]]))


def train_lstm(config):
  train_data, valid_data = data_utils.raw_data_boundary(config)

  with tf.Graph().as_default(), tf.Session() as session:
    initializer = tf.random_uniform_initializer(-config.init_scale,
                                                config.init_scale)
    with tf.variable_scope(config.type, reuse=None, initializer=initializer):
      model = LstmModel(is_training=True, config=config)
    with tf.variable_scope(config.type, reuse=True, initializer=initializer):
      mvalid = LstmModel(is_training=False, config=config)

    tf.initialize_all_variables().run()
    saver = tf.train.Saver()

    reports = []
    for i in range(config.max_max_epoch):
      lr_decay = config.learning_rate_decay_factor ** max(i - config.max_epoch, 0.0)
      model.assign_lr(session, config.learning_rate * lr_decay)

      step = i + 1
      print("Epoch: %d Learning rate: %.3f" % (step, session.run(model.lr)))
      train_perplexity, train_precision, train_recall, train_f1, total_train_f1, report = run_epoch(
          session, model, train_data, model.train_op, verbose=True)
      print("Epoch: %d Train Perplexity: %.3f" % (step, train_perplexity))
      print("Epoch: %d Train Precision:" % step)
      print(train_precision)
      print("Epoch: %d Train Recall:" % step)
      print(train_recall)
      print("Epoch: %d Train F1:" % step)
      print(train_f1)
      print("Report: ")
      print(report)
      print("Epoch: %d Overall Train F1: %.3f" % (step, total_train_f1))

      valid_perplexity, val_precision, val_recall, val_f1, total_val_f1, report = run_epoch(
          session, mvalid, valid_data, tf.no_op())
      print("Epoch: %d Valid Perplexity: %.3f" % (step, valid_perplexity))
      print("Epoch: %d Valid Precision:" % step)
      print(val_precision)
      print("Epoch: %d Valid Recall:" % step)
      print(val_recall)
      print("Epoch: %d Valid F1:" % step)
      print(val_f1)
      print("Report: ")
      print(report)
      print("Epoch: %d Overall Valid F1: %.3f" % (step, total_val_f1))
      save_path = saver.save(session, os.path.join(config.data_dir, config.type, "model.ckpt"), global_step=step)
      print('saved model loadJson %s' % (save_path))
      print("--------------------------------")
      reports.append("Epoch %d" % i)
      reports.append(report)
    with open(os.path.join(config.data_dir, config.type, "reports.txt"), "w+") as outfile:
      outfile.writelines(reports)