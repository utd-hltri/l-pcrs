# coding=utf-8
from __future__ import absolute_import
from __future__ import print_function
from __future__ import division

import csv
import os
import collections
import math
import time
import re
import errno
import shutil

import tensorflow.python.platform
from tensorflow.python.platform import gfile

import numpy as np
from six.moves import xrange
import tensorflow as tf

from scipy.stats.mstats import gmean

from tabulate import tabulate

import sys
import codecs

import eeg_data_utils
import eeg_interpretor_model

sys.stdout = codecs.getwriter('utf8')(sys.stdout)
sys.stderr = codecs.getwriter('utf8')(sys.stderr)


tf.app.flags.DEFINE_float("learning_rate", 0.5, "Learning rate.")
tf.app.flags.DEFINE_float("learning_rate_decay_factor", 0.95, "Learning rate decays by this much.")
tf.app.flags.DEFINE_float("max_gradient_norm", 5.0, "Clip gradients to this norm.")
tf.app.flags.DEFINE_integer("batch_size", 4, "Batch size to use during training.")
tf.app.flags.DEFINE_integer("size", 200, "Size of each model layer.")
tf.app.flags.DEFINE_integer("decoder_rnn_layers", 2, "Number of layers in the model.")
tf.app.flags.DEFINE_integer("decoder_rnn_units", 200, "Number of layers in the model.")
tf.app.flags.DEFINE_integer("desc_vocab_size", 100000, "English vocabulary size.")
tf.app.flags.DEFINE_integer("impr_vocab_size", 100000, "French vocabulary size.")
tf.app.flags.DEFINE_string("data_dir", "/home/travis/work/eegs/emnlp/data_v3_genia_raw", "Data directory")
tf.app.flags.DEFINE_string("working_dir", "/tmp", "Working directory")
tf.app.flags.DEFINE_integer("max_train_data_size", 0,
                            "Limit on the size of training data (0: no limit).")
tf.app.flags.DEFINE_integer("steps_per_checkpoint", 10,
                            "How many training steps to do per checkpoint.")
tf.app.flags.DEFINE_integer("steps_per_eval", 20,
                            "How many training steps to do before evaluating.")
tf.app.flags.DEFINE_boolean("use_coverage", False, "Set to True to use coverage-based attention")
tf.app.flags.DEFINE_boolean("decode", False,
                            "Set to True for interactive decoding.")
tf.app.flags.DEFINE_boolean("self_test", False,
                            "Run a self-test if this is set to True.")
tf.app.flags.DEFINE_boolean("lstm", False,
                            "Use LSTM memory units instead of GRU.")
tf.app.flags.DEFINE_boolean("clear", False,
                            "Clear any existing checkpoints")

tf.app.flags.DEFINE_integer("num_dev_eval_samples", 5, "number of samples to evaluate on the dev set for each bucket")

tf.app.flags.DEFINE_string("log_dir", tf.app.flags.FLAGS.working_dir + "/logs/", "Logging directory")
tf.app.flags.DEFINE_string("checkpoint_dir", tf.app.flags.FLAGS.working_dir + "/checkpoints/", "Checkpoint directory")
tf.app.flags.DEFINE_string("test_dir", tf.app.flags.FLAGS.working_dir + "/output/", "Output directory")

tf.app.flags.DEFINE_string("desc_vocab_path", tf.app.flags.FLAGS.data_dir + "/" + "desc.vocab", "English vocabulary size.")
tf.app.flags.DEFINE_string("impr_vocab_path", tf.app.flags.FLAGS.data_dir + "/" + "impr.vocab", "English vocabulary size.")


FLAGS = tf.app.flags.FLAGS

_buckets = [(100, 50)] #(60, 10), (80, 30), (100, 50)]

def create_model(session, forward_only):
    model = eeg_interpretor_model.EegInterpretorModel(FLAGS.desc_vocab_size, FLAGS.impr_vocab_size,
                                  _buckets,
                                  FLAGS.decoder_rnn_units, FLAGS.decoder_rnn_layers,
                                  FLAGS.max_gradient_norm,
                                  FLAGS.batch_size,
                                  FLAGS.learning_rate, FLAGS.learning_rate_decay_factor,
                                  use_lstm=FLAGS.lstm,
                                  use_coverage=FLAGS.use_coverage,
                                  forward_only=forward_only)

    if FLAGS.clear:
        try:
            shutil.rmtree(FLAGS.checkpoint_dir, ignore_errors=True)
        except OSError as exc:
            if exc.errno != errno.EEXIST:
                raise exc
            pass

    try:
        os.makedirs(FLAGS.checkpoint_dir)
    except OSError as exc:
        if exc.errno != errno.EEXIST:
            raise exc
        pass

    ckpt = tf.train.get_checkpoint_state(FLAGS.checkpoint_dir)
    if ckpt and gfile.Exists(ckpt.model_checkpoint_path):
        print("Reading model parameters from %s" % ckpt.model_checkpoint_path)
        model.saver.restore(session, ckpt.model_checkpoint_path)
    else:
        print("Creating model with fresh parameters.")
        session.run(tf.initialize_all_variables())
    return model

def variable_summaries(var, name):
    """Attach a lot of summaries to a Tensor."""
    with tf.name_scope('summaries'):
        mean = tf.reduce_mean(var)
        tf.scalar_summary('mean/' + name, mean)
        with tf.name_scope('stddev'):
            stddev = tf.sqrt(tf.reduce_sum(tf.square(var - mean)))
        tf.scalar_summary('sttdev/' + name, stddev)
        tf.scalar_summary('max/' + name, tf.reduce_max(var))
        tf.scalar_summary('min/' + name, tf.reduce_min(var))
        tf.histogram_summary(name, var)


def train():
    """Train a descr -> impr model using TUH EEG data."""
    # Prepare TUH EEG data.
    print("Preparing TUH EEG data in %s" % FLAGS.data_dir)
    train_set, dev_set, _ = eeg_data_utils.build_data(FLAGS.data_dir,
                                                     FLAGS.desc_vocab_path,
                                                     FLAGS.impr_vocab_path,
                                                     FLAGS.desc_vocab_size,
                                                     FLAGS.impr_vocab_size, _buckets)

    _, rev_desc_vocab = eeg_data_utils.initialize_vocabulary(FLAGS.desc_vocab_path)
    _, rev_impr_vocab = eeg_data_utils.initialize_vocabulary(FLAGS.impr_vocab_path)


    # nsamples = len(data)
    # nsplit = nsamples // 5
    # train_set = data[0:nsplit * 3]
    # dev_set = data[nsplit * 3 + 1:nsplit * 4]

    with tf.Session() as sess:
        # Create model
        print("Creating %d layers of %d GRU units." % (FLAGS.decoder_rnn_layers, FLAGS.decoder_rnn_units))
        model = create_model(sess, False)

        # Create summary
        #train_writer = tf.train.SummaryWriter(FLAGS.log_dir + '/train', sess.graph)
        #devel_writer = tf.train.SummaryWriter(FLAGS.log_dir + '/devel', sess.graph)

        # Read data into buckets
        print("Reading data into buckets")
        train_bucket_sizes = [len(train_set[b]) for b in xrange(len(_buckets))]
        train_total_size = float(sum(train_bucket_sizes))
        # A bucket scale is a list of increasing numbers from 0 to 1 that we'll use
        # to select a bucket. Length of [scale[i], scale[i+1]] is proportional to
        # the size if i-th training bucket, as used later.
        train_buckets_scale = [sum(train_bucket_sizes[:i + 1]) / train_total_size
                               for i in xrange(len(train_bucket_sizes))]

        # This is the training loop.
        step_time, loss = 0.0, 0.0
        current_step = 0
        previous_losses = []
        print("Training the model...")
        bleus = [[], [], []]
        rouges = [[], [], []]
        while True:
            # Choose a bucket according to data dispersal. We pick a random number
            # in [0, 1] and use the corresponding interval in train_buckets_scale.
            random_number_01 = np.random.random_sample()
            bucket_id = min([i for i in xrange(len(train_buckets_scale))
                             if train_buckets_scale[i] > random_number_01])

            # Get a batch and make a step.
            start_time = time.time()
            encoder_inputs, decoder_inputs, target_weights = model.get_batch(
                                                             train_set, bucket_id)

            #print("Session:", sess)

            #print("Encoder Inputs:", ([[" ".join(outputs2words([word[k] for word in encoder_inputs], rev_desc_vocab))]
            #                           for k in range(FLAGS.batch_size)]))
            #print("Decoder Inputs:", ([[" ".join(outputs2words([word[k] for word in decoder_inputs], rev_impr_vocab))]
            #                           for k in range(FLAGS.batch_size)]))
            #print("Target Weights:", target_weights)
            #print("Bucket:", bucket_id)

            _, step_loss, batch_logits = model.step(sess, encoder_inputs, decoder_inputs,
                                         target_weights, bucket_id, False)
            #print(batch_logits)
            step_time += (time.time() - start_time) / FLAGS.steps_per_checkpoint
            loss += step_loss / FLAGS.steps_per_checkpoint
            current_step += 1
            for j in range(FLAGS.batch_size):
                #print("Logits:", [logit[j] for logit in batch_logits])
                outputs = logits2outputs([logit[j] for logit in batch_logits])
                #print("Outputs:", outputs)
                guess = outputs2words(format_outputs(outputs), rev_impr_vocab)
                #print("Guess:", guess)
                gold = outputs2words(format_outputs([word[j] for word in decoder_inputs]), rev_impr_vocab)
                bleu = get_precision_bleu(guess, gold)
                rouge = get_recall_rouge(guess, gold)
                # print("    STEP", current_step, "Perplexity: %.4f" % math.exp(loss) if loss < 300 else float('inf'))
                # print("    STEP", current_step, "BLEU-2: %.4f" % bleu)
                # print("    STEP", current_step, "ROUGE-2: %.4f" % rouge)
                bleus[0].append(get_precision_bleu(guess, gold, 1))
                bleus[1].append(get_precision_bleu(guess, gold, 2))
                bleus[2].append(get_precision_bleu(guess, gold, 3))
                rouges[0].append(get_recall_rouge(guess, gold, 1))
                rouges[1].append(get_recall_rouge(guess, gold, 2))
                rouges[2].append(get_recall_rouge(guess, gold, 3))

            # Once in a while, we save checkpoint, print statistics, and run evals.
            if current_step % FLAGS.steps_per_checkpoint == 0:
                # Print statistics for the previous epoch.
                perplexity = math.exp(loss) if loss < 300 else float('inf')
                print("Global step", model.global_step.eval(), u": η = %.4f;  Avg. TPS %.2f" % (model.learning_rate.eval(), step_time))
                perf_table = [["Measure", "Unigram", "Bigram", "Trigram"],
                              ["BLEU",  np.mean(bleus[0]), np.mean(bleus[1]), np.mean(bleus[2])],
                              ["ROUGE",  np.mean(rouges[0]), np.mean(rouges[1]), np.mean(rouges[2])]]
                print("Training Performance (Perplexity = %.4f)" % perplexity)
                print(tabulate(perf_table, headers="firstrow"))
                step_table = [["Step %d" % current_step, "Guess:", (u" ".join(guess))],
                              ["Step %d" % current_step, "Gold:", (u" ".join(gold))]]
                print(tabulate(step_table, tablefmt="plain"))
                sys.stdout.flush()
                #bleus = [[], [], []]
                #rouges = [[], [], []]

                # Decrease learning rate if no improvement was seen over last 3 times.
                if len(previous_losses) > 2 and loss > max(previous_losses[-3:]):
                    sess.run(model.learning_rate_decay_op)
                previous_losses.append(loss)
                # Save checkpoint and zero timer and loss.
                checkpoint_path = os.path.join(FLAGS.checkpoint_dir, "interpretor.ckpt")
                model.saver.save(sess, checkpoint_path, global_step=model.global_step)
                step_time, loss = 0.0, 0.0

                #if current_step % FLAGS.steps_per_eval == 0:
                # Run evals on development set and print their perplexity.
                ppx = 0
                bleus = [[], [], []]
                rouges = [[], [], []]
                for j in range(FLAGS.num_dev_eval_samples):
                    random_number_01 = np.random.random_sample()
                    bucket_id = min([i for i in xrange(len(train_buckets_scale))
                             if train_buckets_scale[i] > random_number_01])
                    encoder_inputs, decoder_inputs, target_weights = model.get_batch(
                        dev_set, bucket_id)
                    _, eval_loss, batch_logits = model.step(sess, encoder_inputs, decoder_inputs,
                                                     target_weights, bucket_id, True)
                    eval_ppx = math.exp(eval_loss) if eval_loss < 300 else float('inf')
                    ppx += eval_ppx
                    for k in range(FLAGS.batch_size):
                        outputs = logits2outputs([logit[k] for logit in batch_logits])
                        guess = outputs2words(format_outputs(outputs), rev_impr_vocab)
                        gold = outputs2words(format_outputs([word[k] for word in decoder_inputs]), rev_impr_vocab)
                        bleus[0].append(get_precision_bleu(guess, gold, 1))
                        bleus[1].append(get_precision_bleu(guess, gold, 2))
                        bleus[2].append(get_precision_bleu(guess, gold, 3))
                        rouges[0].append(get_recall_rouge(guess, gold, 1))
                        rouges[1].append(get_recall_rouge(guess, gold, 2))
                        rouges[2].append(get_recall_rouge(guess, gold, 3))
                ppx /= (FLAGS.num_dev_eval_samples)
                print("Devel Performance (Perplexity = %.4f)" % ppx)
                perf_table = [["Measure", "Unigram", "Bigram", "Trigram"],
                              ["BLEU",  np.mean(bleus[0]), np.mean(bleus[1]), np.mean(bleus[2])],
                              ["ROUGE",  np.mean(rouges[0]), np.mean(rouges[1]), np.mean(rouges[2])]]
                print(tabulate(perf_table, headers="firstrow"))
                step_table = [["Step %d" % current_step, "Guess:", (u" ".join(guess))],
                              ["Step %d" % current_step, "Gold:", (u" ".join(gold))]]
                print(tabulate(step_table, tablefmt="plain"))
                sys.stdout.flush()
                bleus = [[], [], []]
                rouges = [[], [], []]

def decode():
    with tf.Session() as sess:
        # Create model and loadJson parameters.
        model = create_model(sess, True)
        model.batch_size = 1  # We decode one sentence at a time.

        # Load vocabularies.
        descr_vocab, _ = eeg_data_utils.initialize_vocabulary(FLAGS.desc_vocab_path)
        _, rev_impr_vocab = eeg_data_utils.initialize_vocabulary(FLAGS.impr_vocab_path)

        # Decode from standard input.
        sys.stdout.write("> ")
        sys.stdout.flush()
        sentence = sys.stdin.readline()
        while sentence:
            # Get token-ids for the input sentence.
            token_ids = eeg_data_utils.seq_to_token_ids(sentence, descr_vocab)
            # Which bucket does it belong to?
            bucket_id = min([b for b in xrange(len(_buckets))
                             if _buckets[b][0] > len(token_ids)])
            # Get a 1-element batch to feed the sentence to the model.
            encoder_inputs, decoder_inputs, target_weights = model.get_batch(
                {bucket_id: [(token_ids, [])]}, bucket_id)

            # Get output logits for the sentence.
            _, _, output_logits, _ = model.step(sess, encoder_inputs, decoder_inputs,
                                             target_weights, bucket_id, True)
            # This is a greedy decoder - outputs are just argmaxes of output_logits.
            outputs = [int(np.argmax(logit, axis=1)) for logit in output_logits]
            # If there is an EOS symbol in outputs, cut them at that point.
            if eeg_data_utils.EOS_ID in outputs:
                outputs = outputs[:outputs.index(eeg_data_utils.EOS_ID)]
            # Print out French sentence corresponding to outputs.
            print(" ".join([rev_impr_vocab[output] for output in outputs]))
            print("> ", end="")
            sys.stdout.flush()
            sentence = sys.stdin.readline()

def format_outputs(outputs):
    if eeg_data_utils.GO_ID in outputs:
        outputs = outputs[outputs.index(eeg_data_utils.GO_ID) + 1:]
    if eeg_data_utils.EOS_ID in outputs:
        outputs = outputs[:outputs.index(eeg_data_utils.EOS_ID)]
    return outputs

def logits2outputs(output_logits):
    #print("Output Logits:", output_logits)
    outputs = [int(np.argmax(logits)) for logits in output_logits]
    if eeg_data_utils.EOS_ID in outputs:
        outputs = outputs[:outputs.index(eeg_data_utils.EOS_ID)]
    return outputs

def outputs2words(outputs, rev_impr_vocab):
    #print("Outputs:", outputs)
    return [rev_impr_vocab[output] for output in outputs]

def words2ngrams(words, n=2):
    #print("Words:", words)
    ngrams = ["_".join(tuple) for tuple in zip(*(words[i:] for i in range(n)))]
    #print("Words:", words)
    #print("%d-grams:", n, ngrams)
    return ngrams

def get_precision_bleu(guess, gold, n=2):
    #print("Guess:", guess)
    guess=words2ngrams(guess, n)
    gold=words2ngrams(gold, n)
    #print("Guess %d-grams:" % n, guess)
    #print("Gold %d-grams:" % n, gold)
    f_guess = collections.Counter(guess)
    f_gold = collections.Counter(gold)
    sum = 0.0
    for ngram in f_guess.iterkeys():
        #print("Count[%s] = Gold: %d; Guess: %d" % (ngram, f_gold[ngram], f_guess[ngram]))
        sum += min(f_guess[ngram], f_gold[ngram])
    if (len(f_guess.keys()) == 0):
        return 0
    else:
        return np.float32(sum) / np.float32(len(f_guess.keys()))

def get_recall_rouge(guess, gold, n=2):
    #print("Guess:", guess)
    guess=words2ngrams(guess, n)
    gold=words2ngrams(gold, n)
    #print("Guess %d-grams:" % n, guess)
    #print("Gold %d-grams:" % n, gold)
    f_guess = collections.Counter(guess)
    f_gold = collections.Counter(gold)
    sum = 0.0
    for ngram in f_gold.iterkeys():
        #print("Count[%s] = Gold: %d; Guess: %d" % (ngram, f_gold[ngram], f_guess[ngram]))
        sum += min(f_guess[ngram], f_gold[ngram])
    if (len(f_gold.keys()) == 0):
        return 0
    else:
        return np.float32(sum) / np.float32(len(f_gold.keys()))


def self_test():
    """Test the translation model."""
    with tf.Session() as sess:
        _, _, test_set = eeg_data_utils.build_data(FLAGS.data_dir,
                                         FLAGS.desc_vocab_path,
                                         FLAGS.impr_vocab_path,
                                         FLAGS.desc_vocab_size,
                                         FLAGS.impr_vocab_size, _buckets)

        # Create model and loadJson parameters.
        model = create_model(sess, True)
        model.batch_size = 1  # We decode one sentence at a time.

        # Load vocabularies.
        _, rev_desc_vocab = eeg_data_utils.initialize_vocabulary(FLAGS.desc_vocab_path)
        _, rev_impr_vocab = eeg_data_utils.initialize_vocabulary(FLAGS.impr_vocab_path)

        try:
            os.makedirs(FLAGS.test_dir)
        except OSError:
            if not os.path.isdir(FLAGS.test_dir):
                raise

        try:
            os.makedirs(FLAGS.test_dir + "/guess")
        except OSError:
            if not os.path.isdir(FLAGS.test_dir + "/guess"):
                raise

        try:
            os.makedirs(FLAGS.test_dir + "/gold")
        except OSError:
            if not os.path.isdir(FLAGS.test_dir + "/gold"):
                raise

        with gfile.GFile(FLAGS.test_dir + "/conclusions.tsv", mode="w") as c_file:
            for eeg in test_set[-1]:
                print(eeg)
                bucket_id = min([b for b in xrange(len(_buckets))
                                 if _buckets[b][0] > len(eeg[0])])
                encoder_inputs, decoder_inputs, target_weights = model.get_batch(
                    {bucket_id: [(eeg[0], [], [], eeg[3])]}, bucket_id)

                print("Session:", sess)
                print("Encoder Inputs:", encoder_inputs)
                print("Decoder Inputs:", decoder_inputs)
                print("Target Weights:", target_weights)
                print("Bucket:", bucket_id)

                _, _, output_logits = model.step(sess, encoder_inputs, decoder_inputs,
                                                 target_weights, bucket_id, True)

                outputs = [int(np.argmax(logit, axis=1)) for logit in output_logits]
                if eeg_data_utils.EOS_ID in outputs:
                    outputs = outputs[:outputs.index(eeg_data_utils.EOS_ID)]

                j_generated = " ".join([rev_impr_vocab[output] for output in outputs])
                j_gold = " ".join([rev_impr_vocab[int(output)] for output in eeg[2]])

                id = eeg[3]

                with gfile.GFile(FLAGS.test_dir + "/guess/" + id + ".txt", mode="w") as g_file:
                    g_file.write(j_generated)

                with gfile.GFile(FLAGS.test_dir + "/gold/" + id + ".txt", mode="w") as g_file:
                    g_file.write(j_gold)

                if "ABNORMAL" in [rev_impr_vocab[output].upper() for output in outputs]:
                    c_generated = "ABNORMAL"
                else:
                    c_generated = "NORMAL"
                c_gold = eeg[1]

                c_file.write(id + '\t' + c_generated + '\t' + c_gold + '\n')

def main(unused_argv):
    if FLAGS.self_test:
        self_test()
    elif FLAGS.decode:
        decode()
    else:
        print('hi2')
        train()


if __name__ == "__main__":
    print('hi')
    tf.app.run()
