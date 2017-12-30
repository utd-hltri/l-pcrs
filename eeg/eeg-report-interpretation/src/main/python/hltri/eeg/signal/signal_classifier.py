import sys
import time

import numpy as np
import tensorflow as tf

import signal_data_utils as data
import signal_classifier_model as model

FLAGS = tf.app.flags.FLAGS
# define a command line argument (name, default value, message)
tf.flags.DEFINE_string("data_path", None, "path to training data")
tf.flags.DEFINE_string("model_dir", None, "path to save model")


logging = tf.logging

def _RunningAvgLoss(loss, running_avg_loss, summary_writer, step, decay=0.999):
    """Calculate the running average of losses."""
    if running_avg_loss == 0:
        running_avg_loss = loss
    else:
        running_avg_loss = running_avg_loss * decay + (1 - decay) * loss
    running_avg_loss = min(running_avg_loss, 12)
    loss_sum = tf.Summary()
    loss_sum.value.add(tag='running_avg_loss', simple_value=running_avg_loss)
    summary_writer.add_summary(loss_sum, step)
    sys.stdout.write('running_avg_loss: %f\n' % running_avg_loss)
    return running_avg_loss

def _Train(model, data):
    """Runs model training."""
    with tf.device('/cpu:0'):
        model.build_graph()
        saver = tf.train.Saver()
        # Train dir is different from log_root to avoid summary directory
        # conflict with Supervisor.
        summary_writer = tf.train.SummaryWriter(FLAGS.train_dir)
        sv = tf.train.Supervisor(logdir=FLAGS.log_root,
                                 is_chief=True,
                                 saver=saver,
                                 summary_op=None,
                                 save_summaries_secs=60,
                                 save_model_secs=FLAGS.checkpoint_secs,
                                 global_step=model.global_step)
        sess = sv.prepare_or_wait_for_session()
        running_avg_loss = 0
        step = 0
        while not sv.should_stop() and step < FLAGS.max_run_steps:
            (signal_batch, classes, signal_lens) = data.edf_iterator()
            (_, summaries, loss, train_step) = model.run_train_step(
                sess, signal_batch, classes, signal_lens)

            summary_writer.add_summary(summaries, train_step)
            running_avg_loss = _RunningAvgLoss(
                running_avg_loss, loss, summary_writer, train_step)
            step += 1
            if step % 100 == 0:
                summary_writer.flush()
        sv.Stop()
        return running_avg_loss

def _Eval(model, data):
    """Runs model eval."""
    model.build_graph()
    saver = tf.train.Saver()
    summary_writer = tf.train.SummaryWriter(FLAGS.eval_dir)
    sess = tf.Session(config=tf.ConfigProto(allow_soft_placement=True))
    running_avg_loss = 0
    step = 0
    while True:
        time.sleep(FLAGS.eval_interval_secs)
        try:
            ckpt_state = tf.train.get_checkpoint_state(FLAGS.log_root)
        except tf.errors.OutOfRangeError as e:
            tf.logging.error('Cannot restore checkpoint: %s', e)
            continue

        if not (ckpt_state and ckpt_state.model_checkpoint_path):
            tf.logging.info('No model to eval yet at %s', FLAGS.train_dir)
            continue

        tf.logging.info('Loading checkpoint %s', ckpt_state.model_checkpoint_path)
        saver.restore(sess, ckpt_state.model_checkpoint_path)

        (signal_batch, classes, signal_lens) = data.NextBatch()
        (summaries, loss, train_step) = model.run_eval_step(
            sess, signal_batch, classes, signal_lens)

        summary_writer.add_summary(summaries, train_step)
        running_avg_loss = _RunningAvgLoss(
            running_avg_loss, loss, summary_writer, train_step)
        if step % 100 == 0:
            summary_writer.flush()

def main(unused_argv):
    batch_size = 4

    params = model.HParams(
        mode=FLAGS.mode,  # train, eval, test
        batch_size=batch_size,
        enc_layers=4,
        enc_timesteps=120,
        min_input_len=2,  # discard articles/summaries < than this
        num_hidden=256,  # for rnn cell
        emb_dim=128,  # If 0, don't use embedding
        max_grad_norm=2)  # If 0, no sampled softmax.

    batcher = data.Batcher(
        FLAGS.data_path, params,
        bucketing=FLAGS.use_bucketing,
        truncate_input=FLAGS.truncate_input)
    tf.set_random_seed(FLAGS.random_seed)

    if params.mode == 'train':
        model = signal_classifier_model.SignalClassifierModel(
            params, num_gpus=FLAGS.num_gpus)
        _Train(model, batcher)
    elif params.mode == 'eval':
        model = seq2seq_attention_model.SignalClassifierModel(
            params, num_gpus=FLAGS.num_gpus)
        _Eval(model, batcher)

if __name__ == '__main__':
    tf.app.run()