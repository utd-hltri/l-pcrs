import math, os, time, pickle
import numpy as np
import sklearn as sk
from sklearn import metrics
import tensorflow as tf
from LstmModel import train_lstm

import AttrModel
import data, Config


def main(unused_args):
  config = Config.flags

  attributes = data.activity_attributes if config.type == "activity" else data.event_attributes
  if config.mode == 'train':
    train(get_model_dir(config), config, attributes)
  elif config.mode == 'eval':
    eval(get_model_dir(config), config)
  elif config.mode == 'boundary':
    train_lstm(config)
  elif config.mode == 'cv':
    cross_validation(get_model_dir(config), config, attributes)
  else:
    raise ValueError("invalid mode: %s" % config.mode)


def get_model_dir(config):
  model_dir = config.model_dir if config.model_dir != None else '%s/%s-%s' % (
  config.data_dir, config.model, config.model_dir_name)
  if not os.path.exists(model_dir):
    os.mkdir(model_dir)
  return model_dir
  
  
def eval(model_dir, config):
  """
  Need the following FLAGS:
  model
  feature_size
  mode
  reportfile
  :param model_dir:
  :param config:
  :return:
  """
  attrs = data.activity_attributes if config.type == "activity" else data.event_attributes
  with tf.Session() as session:
    scope = "model"
    with tf.variable_scope(scope):
      if config.model == 'deep-relu':
        print 'Using deep relu model'
        model = AttrModel.AttributeModel(config, False, attrs, config.feature_size)
      elif config.model == 'highway':
        print 'Using highway model'
        model = AttrModel.HighwayAttributeModel(config, False, attrs, config.feature_size)
      elif config.model == 'single':
        print 'Using single attribute model with attr %s' % config.attr
        assert config.attr in attrs
        model = AttrModel.SingleAttributeModel(config, False, config.attr, config.feature_size)
      saver = tf.train.Saver()
      ckpt = tf.train.get_checkpoint_state(model_dir)
      if ckpt and ckpt.model_checkpoint_path:
        saver.restore(session, ckpt.model_checkpoint_path)
      else:
        raise ValueError("no saved model at %s" % model_dir)
      val_data = pickle.load(open('%s/val.pkl' % model_dir))
      labels = pickle.load(open('%s/labels.pkl' % model_dir))
      generator = data.attr_batch_generator(val_data, labels, config, config.feature_size)
      results = {}
      for x, y_map, aids in generator:
        feed_dict = {model.inputs: x}
        for attr in attrs:
          feed_dict[('%s/label-%s:0' % (scope, attr))] = y_map[attr]
        fetches = ['%s/logits-%s:0' % (scope, attr) for attr in attrs]
        fetched = session.run(fetches, feed_dict)
        logits_map = {attr: logit for (attr, logit) in zip(attrs, fetched)}
        predictions = {attr: logits_map[attr] for attr in attrs}
        confidences = {attr: shannon_entropy(logits_map[attr]) for attr in attrs}
        gold_labels = {attr: y_map[attr] if attr == 'LOCATION' else np.argmax(y_map[attr], axis=1) for attr in attrs}
        for i, aid in enumerate(aids):
          results[aid] = {}
          for attr in attrs:
            results[aid][attr] = (confidences[attr][i], predictions[attr][i], gold_labels[attr][i])
  with open(config.reportfile, 'w') as outfile:
    for aid, result_map in results.items():
      for attr in attrs:
        result = result_map[attr]
        # if attr != "LOCATION":
        #   print attr, ' logits sum: ', np.sum(result[1]), ', ', np.array_str
        outfile.write('%s,%s,%f,%s,%s,%s\n' % (aid, attr, result[0],
                                               'na' if attr == "LOCATION" else np.argmax(result[1]),
                                               result[2].astype(int),
                                               np.array_str(result[1], max_line_width=9999999)))
  
  
def shannon_entropy(logits):
  # q = np.exp(logits) / np.reshape(np.sum(np.exp(logits), axis=1), (logits.shape[0], 1))
  # zero-vectors in logits should have uniform distributions in q
  q = np.ma.divide(logits, np.reshape(np.sum(logits, axis=1), (logits.shape[0], 1))).filled(1.0 / logits.shape[1])
  logq = np.ma.log(q).filled(0)   # replace NaNs resulting from log(0) with 0 since we assume -x*logx = 0 if x = 0
  if np.sum(logits) > 10.1:
    print "logits: ", logits
    print "logit sums:", np.sum(logits, axis=1)
    print "q: ", q
    print "logq: ", logq
    print "H: ", np.sum(-q * logq, axis=1)
  return np.ma.sum(-q * logq, axis=1)


def get_models(config, attributes):
  feature_size = config.feature_size
  initializer = tf.random_uniform_initializer(0, config.init_scale)
  with tf.variable_scope("model", reuse=None, initializer=initializer):
    if config.model == 'deep-relu':
      print 'Using deep relu model'
      train_model = AttrModel.AttributeModel(config, True, attributes, feature_size)
    elif config.model == 'highway':
      print 'Using highway model'
      train_model = AttrModel.HighwayAttributeModel(config, True, attributes, feature_size)
    elif config.model == 'highway-svm':
      print 'Using highway model with svm loss'
      train_model = AttrModel.HighwayAttributeModelSvmLoss(config, True, attributes, feature_size)
    elif config.model == 'single':
      print 'Using single attribute model with attr %s' % config.attr
      assert config.attr in attributes
      attributes = [config.attr]
      train_model = AttrModel.SingleAttributeModel(config, True, config.attr, feature_size)
    elif config.model == 'single-svm':
      print 'Using single attribute model with svm loss with attr %s' % config.attr
      assert config.attr in attributes
      attributes = [config.attr]
      train_model = AttrModel.SingleAttributeSvmLossModel(config, True, config.attr, feature_size)
    else:
      raise (ValueError("invalid model: %s" % config.model))
  with tf.variable_scope("model", reuse=True, initializer=initializer):
    if config.model == 'deep-relu':
      val_model = AttrModel.AttributeModel(config, False, attributes, feature_size)
    elif config.model == 'highway':
      val_model = AttrModel.HighwayAttributeModel(config, False, attributes, feature_size)
    elif config.model == 'highway-svm':
      val_model = AttrModel.HighwayAttributeModelSvmLoss(config, False, attributes, feature_size)
    elif config.model == 'single':
      assert config.attr in data.activity_attributes
      val_model = AttrModel.SingleAttributeModel(config, False, config.attr, feature_size)
    elif config.model == 'single-svm':
      assert config.attr in data.activity_attributes
      val_model = AttrModel.SingleAttributeSvmLossModel(config, False, config.attr, feature_size)
  return train_model, val_model


def train(model_dir, config, attributes):
  train_f1s = {}
  train_costs = []
  val_f1s = {}
  val_costs = []
  raw_data, raw_labels, feature_size = data.raw_data(config.data_dir, config)
  config.feature_size = feature_size
  train_data = {k:v for k,v in raw_data.items()[:int(len(raw_data) * (1 - config.val_proportion))]}
  val_data = {k:v for k,v in raw_data.items()[int(len(raw_data) * (1 - config.val_proportion)):]}
  pickle.dump(val_data, open('%s/val.pkl' % model_dir, 'w+'))
  pickle.dump(raw_labels, open('%s/labels.pkl' % model_dir, 'w+'))
  print "\n\n%d total data points split into %d training and %d validation\n\n" % (len(raw_data), len(train_data), len(val_data))
  with tf.Graph().as_default(), tf.Session() as session:
    train_model, val_model = get_models(config, attributes)
    saver = tf.train.Saver()
    tf.initialize_all_variables().run()
    
    for i in range(config.max_max_epoch):
      step = i+1
      if (i % 10 == 0):
        lr_decay = config.learning_rate_decay_factor ** max(math.floor(i / 10), 0.0)
        lr = config.learning_rate * lr_decay
        train_model.assign_lr(session, lr)
      
      print '------------------------------'
      print("Epoch %d Learning rate: %.3f" % (step, lr))
      print '------------------------------'
      print 'begin training...'
      train_generator = data.boolean_attr_batch_generator(train_data, raw_labels, config, feature_size, attributes)\
          if config.model == 'single-svm' else data.attr_batch_generator(train_data, raw_labels, config, feature_size)
      val_generator = data.boolean_attr_batch_generator(val_data, raw_labels, config, feature_size, attributes)\
          if config.model == 'single-svm' else data.attr_batch_generator(val_data, raw_labels, config, feature_size)
      
      train_reports, f1s, tcosts = do_batch_svm(train_generator, session, train_model, config, 'model', attributes, True)\
          if config.model == 'single-svm' else do_batch(train_generator, session, train_model, config, 'model', attributes)
      train_costs.append(tcosts)
      add_to_map(f1s, train_f1s)
      print 'Epoch %d TRAIN Results (lr=%f):' % (step, lr)
      
      for attr in attributes:
        if attr == 'LOCATION':
          print 'LOCATION: ', train_reports[attr]
        else:
          print '%s: %f' % (attr, f1s[attr])
          if config.verbose:
            print train_reports[attr]
      print 'Epoch %d TRAIN cost: %f' % (step, tcosts)
      print 'previous 10 costs: %s' % train_costs[max(len(train_costs) - 10, 0):]
      save_path = saver.save(session, os.path.join(model_dir, "model.ckpt"), global_step=step)
      
      reports, f1s, costs = do_batch_svm(val_generator, session, val_model, config, 'model', attributes, False)\
          if config.model == 'single-svm' else do_batch(val_generator, session, val_model, config, 'model_1', attributes)
      val_costs.append(costs)
      add_to_map(f1s, val_f1s)
      

      print 'Epoch %d VALIDATION Results:' % step
      for attr in attributes:
        if attr == 'LOCATION':
          print 'LOCATION: ', reports[attr]
        else :
          print '%s (F1=%f):' % (attr, f1s[attr])
          if config.verbose:
            print reports[attr]
      print 'Epoch %d VAL cost: %f' % (step, costs)
      print 'Done epoch %d.' % step
      if tcosts < 1e-8:
        print 'Converged after %d epochs' % i
        break
  print 'last train f1s: %s' % {attr:l[len(l)-1] for attr, l in train_f1s.items()}
  # print 'all train costs: %s' % train_costs
  print 'last val f1s: %s' % {attr:l[len(l)-1] for attr, l in val_f1s.items()}
  # print 'all val costs: %s' % val_costs
  with open(config.reportfile, "w+") as outfile:
    outfile.write("train_costs,")
    for cost in get_every_x(train_costs, 10):
      outfile.write("%f," % cost)
    outfile.write("\nval_costs,")
    for cost in get_every_x(val_costs, 10):
      outfile.write("%f," % cost)
    outfile.write("\nvalidation f1s\n")
    for attr in attributes:
      if attr != 'LOCATION':
        outfile.write("%s," % attr)
        for f1 in get_every_x(val_f1s[attr], 10):
          outfile.write("%s," % f1)
        outfile.write("\n%s\n" % train_reports[attr])
        outfile.write("\n%s\n" % reports[attr])


def cross_validation(model_dir, config, attributes):
  raw_data, raw_labels, feature_size = data.raw_data(config.data_dir, config)
  p = int(float(len(raw_data)) / config.num_splits)
  reports = []
  for split in xrange(config.num_splits):
    config.feature_size = feature_size
    train_data = {}
    val_data = {}
    for i, (k,v) in enumerate(raw_data.items()):
      if i >= split*p and i < (split+1)*p:
        val_data[k] = v
      else:
        train_data[k] = v
    print "\n\n%d total data points split into %d training and %d validation\n\n" %\
          (len(raw_data), len(train_data), len(val_data))

    with tf.Graph().as_default(), tf.Session() as session:
      train_model, val_model = get_models(config, attributes)
      saver = tf.train.Saver()
      tf.initialize_all_variables().run()

      for i in range(config.max_max_epoch):
        step = i + 1
        if (i % 10 == 0):
          lr_decay = config.learning_rate_decay_factor ** max(math.floor(i / 10), 0.0)
          lr = config.learning_rate * lr_decay
          train_model.assign_lr(session, lr)

        print '------------------------------'
        print("Split %d of %d: Epoch %d Learning rate: %.3f" % (split+1, config.num_splits, step, lr))
        print '------------------------------'
        print 'begin training...'
        train_generator = data.boolean_attr_batch_generator(train_data, raw_labels, config, feature_size, attributes) \
          if config.model == 'single-svm' else data.attr_batch_generator(train_data, raw_labels, config, feature_size)
        val_generator = data.boolean_attr_batch_generator(val_data, raw_labels, config, feature_size, attributes) \
          if config.model == 'single-svm' else data.attr_batch_generator(val_data, raw_labels, config, feature_size)

        train_reports, f1s, tcosts = do_batch_svm(train_generator, session, train_model, config, 'model', attributes, True) \
          if config.model == 'single-svm' else do_batch(train_generator, session, train_model, config, 'model',
                                                        attributes)
        print 'Epoch %d TRAIN Results (lr=%f):' % (step, lr)

        for attr in attributes:
          print '%s: %f' % (attr, f1s[attr])
          if config.verbose:
            print train_reports[attr]
        print 'Epoch %d TRAIN cost: %f' % (step, tcosts)
        save_path = saver.save(session, os.path.join(model_dir, "model.ckpt"), global_step=step)

        report, f1s, costs = do_batch_svm(val_generator, session, val_model, config, 'model', attributes, False) \
          if config.model == 'single-svm' else do_batch(val_generator, session, val_model, config, 'model_1',
                                                        attributes)
        print 'Epoch %d VAL Results' % (step)
        for attr in attributes:
          print '%s: %f' % (attr, f1s[attr])
          if config.verbose:
            print report[attr]
      reports.append("Run %s" % split)
      for attr in attributes:
        reports.append("\n%s\n" % attr)
        reports.append(report[attr])
  with open(config.reportfile, "w+") as outfile:
    outfile.writelines(reports)
    

def get_every_x(lst, x):
  return [c for i, c in enumerate(lst) if i % x == 0]


def add_to_map(lil_map, big_map):
  for key in lil_map.keys():
    l = big_map[key] if key in big_map.keys() else []
    l.append(lil_map[key])
    big_map[key] = l


def do_batch(generator, session, model, config, scope, attrs):
  start_time = time.time()
  costs = 0.0
  iters = 0
  predictions = {}
  gold_labels = {}
  is_training = scope == 'model'
  
  for step, (x, y_map, _) in enumerate(generator):
    # print 'x.shape: %s, num hot: %s' % (x.shape, [np.count_nonzero(x_i) for x_i in x])
    feed_dict = {model.inputs: x}
    for a in attrs:
      feed_dict[('%s/label-%s:0' % (scope, a))] = y_map[a]
    if is_training:
      fetches = [model.cost, model.train_op]
    else:
      fetches = [model.cost]
    for attr in attrs:
      fetches.append('%s/logits-%s:0' % (scope, attr))
    fetched = session.run(fetches, feed_dict)
    costs += fetched[0]
    iters += config.batch_size
    logits = {attr: logit for (attr, logit) in zip(attrs, fetched[len(fetched) - len(attrs):])}
    # preds = {attr: np.argmax(logits[attr], axis=1) for attr in attrs}
    for attr in attrs:
      gold_vec = y_map[attr]
      plist = predictions[attr] if attr in predictions.keys() else []
      plist.extend(logits[attr] if attr == 'LOCATION' else np.argmax(logits[attr], axis=1))
      
      glist = gold_labels[attr] if attr in gold_labels.keys() else []
      glist.extend(gold_vec if attr == 'LOCATION' else np.argmax(gold_vec, axis=1))
      
      predictions[attr] = plist
      gold_labels[attr] = glist
    
    if config.verbose and step % 10 == 0:
      print'Step %d cost: %.3f speed: %.0f ips.' % (step, fetched[0],
                                                    iters * config.batch_size / (time.time() - start_time))
      # print 'fetches: %s' % {k:v for k, v in zip(fetches, fetched)}
      # print 'gold - %s' % y_map
      # print 'logits - %s' % logits
      # if is_training:
      #   print 'train_op - %s' % fetched[1]
      
  reports = {}
  f1s = {}
  for attr in attrs:
    if attr == 'LOCATION':
      reports[attr] = {#'Coverage Error': sk.metrics.coverage_error(gold_labels[attr], predictions[attr]),
                       'Label Ranking Avg Prec': sk.metrics.label_ranking_average_precision_score(gold_labels[attr], predictions[attr]),
                       'Ranking Loss': sk.metrics.label_ranking_loss(gold_labels[attr], predictions[attr])}
    else:
      reports[attr] = sk.metrics.classification_report(gold_labels[attr], predictions[attr], labels=range(
        data.label_sizes[attr]))
      if data.label_sizes[attr] == 2:
        f1s[attr] = sk.metrics.f1_score(gold_labels[attr], predictions[attr], average='binary',
                                        pos_label=(0 if attr == 'POLARITY' else 1))
      else:
        f1s[attr] = sk.metrics.f1_score(gold_labels[attr], predictions[attr], labels=range(data.label_sizes[attr]),
                                        average='micro')
  return (reports, f1s, costs)


def do_batch_svm(generator, session, model, config, scope, attrs, is_training):
  start_time = time.time()
  costs = 0.0
  iters = 0
  predictions = {}
  gold_labels = {}

  for step, (x, y_map, _) in enumerate(generator):
    # print 'x.shape: %s, num hot: %s, y: %s' % (x.shape, [np.count_nonzero(x_i) for x_i in x], y_map['RECURRENCE'])
    feed_dict = {('%s/inputs:0' % scope): x, model.inputs: x}
    for a in attrs:
      feed_dict[('%s/label-%s:0' % (scope, a))] = y_map[a]
      feed_dict[('model_1/label-%s:0' % (a))] = y_map[a]
    if is_training:
      fetches = [model.cost, model.train_op]
    else:
      fetches = [model.cost]
    for attr in attrs:
      fetches.append('%s/predictions-%s:0' % (scope, attr))
    fetched = session.run(fetches, feed_dict)
    costs += fetched[0]
    iters += config.batch_size
    pred = {attr: logit for (attr, logit) in zip(attrs, fetched[len(fetched) - len(attrs):])}
    # preds = {attr: np.argmax(logits[attr], axis=1) for attr in attrs}
    for attr in attrs:
      plist = predictions[attr] if attr in predictions.keys() else []
      plist.extend(pred[attr])
      predictions[attr] = plist

      glist = gold_labels[attr] if attr in gold_labels.keys() else []
      glist.extend(y_map[attr])
      gold_labels[attr] = glist

    if config.verbose and step % 10 == 0:
      print'Step %d cost: %.3f speed: %.0f ips.' % (step, fetched[0],
                                                    iters * config.batch_size / (time.time() - start_time))
      # print 'fetches: %s' % {k:v for k, v in zip(fetches, fetched)}
      # print 'gold - %s' % y_map
      # print 'logits - %s' % logits
      # if is_training:
      #   print 'train_op - %s' % fetched[1]

  reports = {}
  f1s = {}
  for attr in attrs:
    if attr == 'LOCATION':
      reports[attr] = {  # 'Coverage Error': sk.metrics.coverage_error(gold_labels[attr], predictions[attr]),
        'Label Ranking Avg Prec': sk.metrics.label_ranking_average_precision_score(gold_labels[attr],
                                                                                   predictions[attr]),
        'Ranking Loss': sk.metrics.label_ranking_loss(gold_labels[attr], predictions[attr])}
    else:
      y = gold_labels[attr]
      x = predictions[attr]
      reports[attr] = sk.metrics.classification_report(y, x, labels=[-1,1])
      y = [0 if y_ < 0 else 1 for y_ in y]
      x = [0 if x_ < 0 else 1 for x_ in x]
      print y
      print x
      f1s[attr] = sk.metrics.f1_score(y, x, average='binary', pos_label=1)
  return (reports, f1s, costs)
    

if __name__ == "__main__":
  tf.app.run()
