import os
import pprint
import tensorflow as tf
import math
import data, model
import random
import time
import numpy as np


pp = pprint.PrettyPrinter()

flags = tf.app.flags
flags.DEFINE_string("data_dir", "/home/rmm120030/working/eeg/knowledge_graph/relations_names", "data directory")
flags.DEFINE_string("checkpoint_dir_name", "checkpoints", "checkpoint directory")
flags.DEFINE_string("mode", "train", "Mode [train]")
flags.DEFINE_string("optimizer", "gd", "optimizer [gd, adam]")
flags.DEFINE_string("energy", "l2", "energy function(d) [l2, l1]")
flags.DEFINE_float("lr", 0.01, "learning rate [0.1, 0.01, 0.001]")
flags.DEFINE_float("init", None, "embeddings will be initialized in [-init, init]")
flags.DEFINE_float("max_grad_norm", 50, "clip gradients to this norm [50]")
flags.DEFINE_float("max_ent_norm", 1.0, "clip gradients to this norm [50]")
flags.DEFINE_float("gamma", 1.0, "gamma [1, 2, 10]")
flags.DEFINE_float("val_proportion", 0.05, "Proportion of data to hold out for validation.")
flags.DEFINE_integer("nepoch", 1000, "number of epoch to use during training [100]")
flags.DEFINE_integer("batch_size", 128, "batch size to use during training [128]")
flags.DEFINE_integer("val_batch_size", 4, "batch size to use during validation [4]")
flags.DEFINE_integer("emb_size", 50, "Embedding size [20, 50]")
flags.DEFINE_integer("ignore_entity_threshold", 1,
                     "Ignore triples involving entities that occur this many times or less.")
flags.DEFINE_integer("max_num_batches", 100000, "The maximum number of batches per training epoch.")
flags.DEFINE_integer("max_val_batches", 100, "The maximum number of validation batches per epoch.")
flags.DEFINE_integer("validate_every_x_epochs", 5, "Only run validation every x epochs.")
flags.DEFINE_boolean("personal_space", True, "Get out of my personal space.")
flags.DEFINE_boolean("ignore_uncommon_entities", False, "Ignore triples involving entities that are uncommon.")
flags.DEFINE_boolean("show", True, "Show the progress bar")
flags.DEFINE_boolean("clip_grads", False, "Clip the gradients?")
flags.DEFINE_boolean("validate_on_train", False, "Sample from the training documents for both training and validation?")

FLAGS = flags.FLAGS


def main(_):
    FLAGS.checkpoint_dir = os.path.join(FLAGS.data_dir, FLAGS.checkpoint_dir_name)
    if not os.path.exists(FLAGS.checkpoint_dir):
        os.mkdir(FLAGS.checkpoint_dir)
    FLAGS.init = 6.0 / math.sqrt(FLAGS.emb_size)

    all_triples, ent2idx = data.load_all_data(FLAGS)
    random.shuffle(all_triples)
    num_val = int(FLAGS.val_proportion * len(all_triples))
    if FLAGS.validate_on_train:
      train_data = val_data = all_triples
    else:
      train_data = all_triples[num_val:]
      val_data = all_triples[:num_val]

    FLAGS.num_relations = len(data.rel2idx)
    FLAGS.num_entities = len(ent2idx)
    FLAGS.num_batches = min(FLAGS.max_num_batches, len(train_data) / FLAGS.batch_size)
    pp.pprint(flags.FLAGS.__flags)

    run(train_data, val_data, {v: k for (k, v) in ent2idx.iteritems()}, FLAGS)


def run(train_data, val_data, idx2name, config):
  with tf.Session() as session:
    start = time.time()
    with tf.variable_scope("model", reuse=None):
      train_model = model.TransEModel(config=config, is_training=True)
    with tf.variable_scope("model", reuse=True):
      val_model = model.TransEModel(config=config, is_training=False)

    tf.initialize_all_variables().run()
    saver = tf.train.Saver()
    print("Took %s seconds to initialize models." % (time.time() - start))

    if config.show: from utils import ProgressBar
    # entity_embeddings = np.random.uniform(-config.init, config.init, [config.num_entities, config.emb_size])
    for epoch in xrange(config.nepoch):
      epoch += 1
      print("Begin Epoch %s" % epoch)
      start = time.time()
      if config.show: bar = ProgressBar("TRAIN", max=config.num_batches)
      train_loss = 0.0
      for batch in xrange(config.num_batches):
        tl = train_model.train_batch(train_data, session)
        train_loss += tl
        if config.show: bar.next()
      if config.show: bar.finish()
      train_loss = train_loss / config.num_batches
      train_end = time.time()
      print("[Epoch %s] Training took %s seconds. Loss: %s" %
            (epoch, train_end - start, train_loss))

      if epoch % config.validate_every_x_epochs == 0:
        num_val_batches = min(config.max_val_batches, int(math.ceil(float(len(val_data)) / config.val_batch_size)))
        print("[Epoch %s] Begin %s validation batches." % (epoch, num_val_batches))
        if config.show: bar = ProgressBar("VALIDATION", max=num_val_batches)
        rankings = []
        for batch in xrange(num_val_batches):
          rankings.extend(val_model.val_batch(random.sample(val_data, config.val_batch_size), session))
          if config.show: bar.next()
        if config.show: bar.finish()
        mean_rank = np.mean(rankings)
        print("[Epoch %s] Validation mean rank: %s" % (epoch, mean_rank))
        hat = float(len([r for r in rankings if r <= 10])) / len(rankings)
        print("[Epoch %s] Validation hits at 10: %s" % (epoch, hat))
        hah = float(len([r for r in rankings if r <= 100])) / len(rankings)
        print("[Epoch %s] Validation hits at 100: %s" % (epoch, hah))
        print("[Epoch %s] Validation took %s seconds. Epoch took %s seconds" %
              (epoch, time.time() - train_end, time.time() - start))
      saver.save(session, os.path.join(config.checkpoint_dir, "TransE.model"), global_step=epoch)


if __name__ == '__main__':
    tf.app.run()
