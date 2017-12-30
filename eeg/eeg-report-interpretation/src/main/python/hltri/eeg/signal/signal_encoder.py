from __future__ import division
from __future__ import print_function

import time
from timeit import default_timer as timer

import os
import logging
import shutil
import errno

import numpy as np

import tensorflow as tf
from tensorflow.python.platform import gfile


import signal_encoder_model
import signal_data_utils

log = logging.getLogger("SignalEncoder")
log.setLevel(logging.DEBUG)

flags = tf.flags

# define a command line argument (name, default value, message)
flags.DEFINE_string("data_path", None, "path to training data")
flags.DEFINE_string("model_dir", None, "path to save model")
flags.DEFINE_string("fingerprint_dir", '/shared/aifiles/disk1/travis/data/corpora/tuh_eeg/fingerprints',
                    "path to save fingerprints")
flags.DEFINE_string("fingerprint_fn", 'vectors.txt', "filename for file to save fingerprints to")
flags.DEFINE_string("checkpoint", 'checkpoint', "checkpoint")
flags.DEFINE_integer("signal_dim", 31, '')
flags.DEFINE_integer("batch_size", 1, '')
flags.DEFINE_integer("num_layers", 2, '')
flags.DEFINE_integer("memory_dim", 200, '')
flags.DEFINE_integer("num_steps", 20, '')
flags.DEFINE_integer("max_grad_norm", 5, '')
flags.DEFINE_integer("max_epoch", 4, '')
flags.DEFINE_integer("max_max_epoch", 13, '')
flags.DEFINE_float("keep_prob", 1.0, '')
flags.DEFINE_float("init_scale", 0.1, '')
flags.DEFINE_float("learning_rate", 1.0, '')
flags.DEFINE_float("lr_decay", 0.5, '')
flags.DEFINE_string("mode", "TRAIN", "mode: TRAIN or ENCODE")
flags.DEFINE_boolean("clear", False, "delete previous models")

FLAGS = flags.FLAGS


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


def _run_epoch(sess, model, data, global_step=0):
    """Runs the model on the given data."""
    start_time = time.time()
    state = sess.run(model.initial_state)
    lastn = 0
    total_loss = 0.0
    iters = 0
    step = 0
    for step, (x, y, n, l) in enumerate(data):
        loss, state = model.run_train_step(sess, x, l, state)
        total_loss += loss
        iters += model.num_steps

        if n != lastn:
            state = sess.run(model.initial_state)
            lastn = n

        if step % 100 == 0:
            print("[Step %d] Avg. Loss: [%6.3f] Speed: %.0f ips" %
                  (step, total_loss / iters,
                   iters * model.batch_size / (time.time() - start_time)))

        if step % 1000 == 0:
            checkpoint_path = os.path.join(FLAGS.model_dir, "model")
            print('Saving model to', checkpoint_path, '...')
            model.saver.save(sess, checkpoint_path, global_step=step + global_step)

    return total_loss / iters, step + global_step


def _delete_dir_quiet(directory):
    try:
        shutil.rmtree(directory, ignore_errors=True)
    except OSError as exc:
        if exc.errno != errno.EEXIST:
            raise exc
        pass


def _make_dirs_quiet(directory):
    try:
        os.makedirs(directory)
    except OSError as exc:
        if exc.errno != errno.EEXIST:
            raise exc
        pass


def _create_model(session, config):
    model = signal_encoder_model.SignalEncoderModel(FLAGS.mode.lower() == 'train', config)

    logging.info("Building graph...")
    start_time = timer()
    model.build_graph()
    end_time = timer()
    log.debug("Graph constructed in %6.3f seconds!", end_time - start_time)

    if FLAGS.clear:
        log.debug("Clearing previous checkpoints...")
        _delete_dir_quiet(FLAGS.model_dir)
    _make_dirs_quiet(FLAGS.model_dir)

    ckpt = tf.train.get_checkpoint_state(FLAGS.model_dir)
    if ckpt and gfile.Exists(ckpt.model_checkpoint_path + '.meta'):
        log.info("Reading model parameters from '%s'...", ckpt.model_checkpoint_path)
        start_time = timer()
        model.saver.restore(session, ckpt.model_checkpoint_path)
        end_time = timer()
        log.debug("Model restored in %6.3f seconds!", end_time - start_time)
    else:
        log.info("Creating model with fresh parameters...")
        start_time = timer()
        tf.initialize_all_variables().run()
        end_time = timer()
        log.debug("Model initialized in %6.3f seconds!",  end_time - start_time)
    return model


def train(config):
    print('training with config: %s' % config)
    with tf.Session() as sess:
        model = _create_model(sess, config)
        step = 0
        for i in range(config.max_max_epoch):
            train_data = signal_data_utils.edf_iterator(FLAGS.data_path,
                                                        config.batch_size,
                                                        config.num_steps,
                                                        config.signal_dim)
            epoch_start = time.time()
            cost, step = _run_epoch(sess, model, train_data, step)
            print("Epoch: %d | Loss: %.3f. | Took %.2f minutes." % (i + 1,
                                                                    cost,
                                                                    (time.time() - epoch_start) / 60.0))
            # train_cost = run_epoch(session, mvalid, valid_data, tf.no_op())
            # print("Epoch: %d Valid Cost: %.3f" % (i + 1, valid_cost))
            print("--------------------------------")


def encode(config):
    print('encoding with config: %s' % config)
    data = signal_data_utils.edf_iterator_with_names(config.data_path, config.batch_size, config.num_steps,
                                                     config.signal_dim)
    footprints = {}
    with tf.Session() as sess:
        model = _create_model(sess, config)
        start = time.time()
        for (batched_signal, name, n, l) in data:
            if len(batched_signal) > 1:
                print('Signal for %s has length %s split into %s batches' % (name, sum(l), len(batched_signal)))
            # states_dict is initialized from model.initial_state
            state = sess.run(model.initial_state)
            # print('Signal %s has %s batches...' % (name, len(batched_signal)))
            for (batch_idx, input_batch) in enumerate(batched_signal):
                state = model.run_eval_step(sess, input_batch, [l[batch_idx]], state)
            footprints[name] = np.squeeze(state[-1])
            if n % 100 == 0:
                secs = float(time.time() - start)
                print('[%d] Made footprint for %s. %.2f per second. %.2f min so far' %
                      (n, name, n / secs, secs / 60.0))

        with open(os.path.join(config.fingerprint_dir, config.fingerprint_fn), 'w+') as f:
            # noinspection PyCompatibility
            for (name, footprint) in footprints.iteritems():
                line = name
                for _f in footprint:
                    line += (' %.8f' % _f)
                line += '\n'
                f.write(line)


# noinspection PyUnusedLocal
def main(unused_args):
    config = FLAGS
    # eval_config = StandardConfig()
    # eval_config.batch_size = 1
    # eval_config.num_steps = 1
    if config.mode.lower() == 'encode':
        encode(config)
    elif config.mode.lower() == 'train':
        train(config)


if __name__ == "__main__":
    tf.app.run()
