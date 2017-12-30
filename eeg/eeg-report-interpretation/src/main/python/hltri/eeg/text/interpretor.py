# coding=utf-8
from __future__ import absolute_import
from __future__ import print_function
from __future__ import division

from timeit import default_timer as timer
import os
import collections
import errno
import shutil
import sys

from tensorflow.python.platform import gfile

import numpy as np
import tensorflow as tf
from sklearn.metrics import classification_report

from tabulate import tabulate

import data
import interpretor_gan_model


tf.app.flags.DEFINE_float("learning_rate", 0.5, "Learning rate.")
tf.app.flags.DEFINE_float("learning_rate_decay_factor", 0.95, "Learning rate decays by this much.")
tf.app.flags.DEFINE_float("max_gradient_norm", 5.0, "Clip gradients to this norm.")
tf.app.flags.DEFINE_integer("batch_size", 4, "Batch size to use during training.")
tf.app.flags.DEFINE_integer("size", 200, "Size of each model layer.")
tf.app.flags.DEFINE_integer("vocab_size", 50000, "English vocabulary size.")
tf.app.flags.DEFINE_string("data_dir", "/home/travis/work/eegs/emnlp/data_v3_genia_raw", "Data directory")
tf.app.flags.DEFINE_string("working_dir", "/tmp", "Working directory")
tf.app.flags.DEFINE_integer("max_train_data_size", 0,
                            "Limit on the size of training data (0: no limit).")
tf.app.flags.DEFINE_integer("steps_per_checkpoint", 10,
                            "How many training steps to do per checkpoint.")
tf.app.flags.DEFINE_integer("steps_per_eval", 20,
                            "How many training steps to do before evaluating.")
tf.app.flags.DEFINE_boolean("use_coverage", False, "Set to True to use coverage-based attention")
tf.app.flags.DEFINE_boolean("clear", False,
                            "Clear any existing checkpoints")

tf.app.flags.DEFINE_integer("num_dev_eval_samples", 5, "number of samples to evaluate on the dev set for each bucket")

tf.app.flags.DEFINE_string("log_dir", tf.app.flags.FLAGS.working_dir + "/logs/", "Logging directory")
tf.app.flags.DEFINE_string("checkpoint_dir", tf.app.flags.FLAGS.working_dir + "/checkpoints/", "Checkpoint directory")
tf.app.flags.DEFINE_string("test_dir", tf.app.flags.FLAGS.working_dir + "/output/", "Output directory")

tf.app.flags.DEFINE_string("vocab_path", tf.app.flags.FLAGS.data_dir + "/" + "vocab.dat", "Vocabulary")


FLAGS = tf.app.flags.FLAGS


def _delete_dir_quiet(dir):
    try:
        shutil.rmtree(dir, ignore_errors=True)
    except OSError as exc:
        if exc.errno != errno.EEXIST:
            raise exc
        pass


def _make_dirs_quiet(dir):
    try:
        os.makedirs(dir)
    except OSError as exc:
        if exc.errno != errno.EEXIST:
            raise exc
        pass


def _simple_summary(tag, value):
    summary = tf.Summary()
    summary.value.add(tag='tag', simple_value=value)
    return summary

def _create_model(session, hps, vocab, batch_size):
    model = interpretor_gan_model.GANInterpretorModel(hps, vocab, batch_size)

    print("Building graph...")
    start_time = timer()
    model.build_graph()
    end_time = timer()
    print("Graph constructed in %6.3f seconds!" % (end_time - start_time))

    if FLAGS.clear:
        _delete_dir_quiet(FLAGS.checkpoint_dir)
    _make_dirs_quiet(FLAGS.checkpoint_dir)

    ckpt = tf.train.get_checkpoint_state(FLAGS.checkpoint_dir)
    if ckpt and gfile.Exists(ckpt.model_checkpoint_path):
        print("Reading model parameters from '%s'..." % ckpt.model_checkpoint_path)
        start_time = timer()
        model.saver.restore(session, ckpt.model_checkpoint_path)
        end_time = timer()
        print("Model restored in %6.3f seconds!" % (end_time - start_time))
    else:
        print("Creating model with fresh parameters...")
        start_time = timer()
        session.run(tf.global_variables_initializer())
        end_time = timer()
        print("Model initialized in %6.3f seconds!" % (end_time - start_time))
    return model


def _train(train_set, devel_set, hps, batch_size, vocab, rev_vocab):

    def _evaluate_output_texts(writer, guess_texts, gold_texts):
        bleus = [[], [], []]
        rouges = [[], [], []]
        print('Guess texts:', guess_texts)
        print('Gold texts:', gold_texts)
        for j in range(FLAGS.batch_size):
            logits = [logit[j] for logit in texts_g]
            print('Logits:', logits)
            outputs = _logits_to_outputs(logits)
            print('Outputs:', outputs)
            guess = _outputs_to_words(_format_outputs(outputs), rev_vocab)
            print('Guess:', guess)
            gold = _outputs_to_words(_format_outputs([word[j] for word in z_output_texts]), rev_vocab)
            print('Gold:', gold)
            bleus[0].append(_get_precision_bleu(guess, gold, 1))
            bleus[1].append(_get_precision_bleu(guess, gold, 2))
            bleus[2].append(_get_precision_bleu(guess, gold, 3))
            rouges[0].append(_get_recall_rouge(guess, gold, 1))
            rouges[1].append(_get_recall_rouge(guess, gold, 2))
            rouges[2].append(_get_recall_rouge(guess, gold, 3))
        print('Bleus raw:', bleus)
        print('Rouges raw:', rouges)
        bleus = np.mean(bleus, axis=1)
        rouges = np.mean(rouges, axis=1)
        print('Bleus mean:', bleus)
        print('Rouges mean:', rouges)
        train_writer.add_summary(_simple_summary('BLEU-1', bleus[0].item()))
        train_writer.add_summary(_simple_summary('BLEU-2', bleus[1].item()))
        train_writer.add_summary(_simple_summary('BLEU-3', bleus[2].item()))
        train_writer.add_summary(_simple_summary('ROUGE-1', rouges[0].item()))
        train_writer.add_summary(_simple_summary('ROUGE-2', rouges[1].item()))
        train_writer.add_summary(_simple_summary('ROUGE-3', rouges[2].item()))
        return bleus, rouges


    def _print_evaluation_table(bleus, rouges):
        print('E| Bleus raw:', bleus)
        print('E| Rouges raw:', rouges)
        bleus = np.mean(bleus, axis=1)
        rouges = np.mean(rouges, axis=1)
        print('E| Bleus mean:', bleus)
        print('E| Rouges mean:', rouges)
        return [["Measure", "Unigram", "Bigram", "Trigram"],
                ["BLEU",  bleus[0], bleus[1], bleus[2]],
                ["ROUGE",  rouges[0], rouges[1], rouges[2]]]


    def _outputs_to_string(texts_g, rev_vocab):
        logits = [logit for logit in texts_g]
        print('O2S | Logits:', logits)
        outputs = _logits_to_outputs(logits)
        print('O2S | Outputs:', outputs)
        words = _outputs_to_words(_format_outputs(outputs), rev_vocab)
        print('O2S | Words:', words)
        return ' '.join(words)

    with tf.Session() as sess:
        # Create model
        model = _create_model(sess, hps, vocab, batch_size)

        # Create summary
        train_writer = tf.summary.FileWriter(FLAGS.log_dir + '/train', sess.graph)
        devel_writer = tf.summary.FileWriter(FLAGS.log_dir + '/devel', sess.graph)
        # test_writer = tf.summary.FileWriter(FLAGS.log_dir + '/test', sess.graph)

        # This is the training loop.
        current_step = 0

        print("Training the model...")
        avg_bleus = [[], [], []]
        avg_rouges = [[], [], []]
        avg_loss_d = []
        avg_loss_g = []
        classes_guess = []
        classes_gold = []
        avg_step_time = []
        while True:
            # Get a batch and make a step.
            start_time = timer()

            # Train descriminator
            x_input_texts, x_input_lens, \
                x_output_texts, x_output_lens, x_output_classes = train_set.get_batch()
            z_input_texts, z_input_lens, _, _, _ = train_set.get_batch()
            d1, d1_summaries, loss_d, step_d = model.run_train_d_step(sess,z_input_texts=z_input_texts, z_input_lens=z_input_lens,
                                                           x_input_texts=x_input_texts, x_input_lens=x_input_lens,
                                                           x_output_texts=x_output_texts, x_output_classes=x_output_classes)

            # Train generator
            z_input_texts, z_input_lens, \
                z_output_texts, z_output_lens, z_output_classes = train_set.get_batch()
            d2, d2_summaries, texts_g, classes_g, loss_g, step_g = model.run_train_g_step(sess,
                                                          z_input_texts=z_input_texts,
                                                          z_input_lens=z_input_lens)

            avg_loss_d.append(np.mean(loss_d))
            avg_loss_g.append(np.mean(loss_g))

            train_writer.add_summary(d1_summaries, step_d)
            train_writer.add_summary(d2_summaries, step_g)

            bleus, rouges = _evaluate_output_texts(train_writer, texts_g, z_output_texts)
            print('Bleus:', bleus)
            print('Bleus Expanded:', np.expand_dims(bleus, axis=1))
            print('Avg. Bleus:', avg_bleus)
            avg_bleus = np.concatenate((avg_bleus, np.expand_dims(bleus, axis=1)), axis=1)
            avg_rouges = np.concatenate((avg_rouges, np.expand_dims(rouges, axis=1)), axis=1)
            classes_guess.extend(classes_g.flatten())
            classes_gold.extend(z_output_classes.flatten())
            end_time = timer()
            avg_step_time.append(end_time - start_time)

            # Once in a while, we save checkpoint, print statistics, and run evals.
            if current_step % FLAGS.steps_per_checkpoint == 0:
                print("Current step: %d; Avg. Time per Step: %6.3fs" % (current_step + 1, np.mean(avg_step_time)))
                print("Average Loss - Descriminator: %6.3f" % np.mean(avg_loss_g))
                print("Average Loss - Generator: %6.3f" % np.mean(avg_loss_d))
                perf_table = _print_evaluation_table(avg_bleus, avg_rouges)
                print(tabulate(perf_table, headers="firstrow"))
                step_table = []
                for j in xrange(batch_size):
                    step_table.append(["Sample %d" % j, "Guess:", _outputs_to_string(texts_g[j], rev_vocab)])
                    step_table.append(["Sample %d" % j, "Gold:", _outputs_to_string(z_output_texts[j], rev_vocab)])
                print(tabulate(step_table, tablefmt="plain"))
                print("Classification report:")
                print(classification_report(classes_gold, classes_guess))
                sys.stdout.flush()

                # Save checkpoint
                checkpoint_path = os.path.join(FLAGS.checkpoint_dir, "interpretor.ckpt")
                model.saver.save(sess, checkpoint_path, global_step=model._step_g)

                if current_step % FLAGS.steps_per_eval == 0:
                    # Reset averages
                    avg_bleus = [[], [], []]
                    avg_rouges = [[], [], []]
                    classes_guess = []
                    classes_gold = []

                    # Run evals on development set and print their perplexity.
                    for j in range(FLAGS.num_dev_eval_samples):
                        z_input_texts, z_input_lens, \
                        z_output_texts, z_output_lens, z_output_classes = devel_set.get_batch()
                        texts_g, classes_g = model.run_eval_step(sess,
                                                                         z_input_texts=z_input_texts,
                                                                         z_input_lens=z_input_lens)
                        bleus, rouges = _evaluate_output_texts(devel_writer, texts_g, z_output_lens)
                        avg_bleus = np.concatenate((avg_bleus, np.expand_dims(bleus, axis=1)), axis=1)
                        avg_rouges = np.concatenate((avg_rouges, np.expand_dims(rouges, axis=1)), axis=1)
                        classes_guess.extend(classes_g.flatten())
                        classes_gold.extend(z_output_classes.flatten())
                    perf_table = _print_evaluation_table(avg_bleus, avg_rouges)
                    print(tabulate(perf_table, headers="firstrow"))
                    step_table = []
                    for j in xrange(batch_size):
                        step_table.append(["Sample %d" % j, "Guess:", _outputs_to_string(texts_g[j], rev_vocab)])
                        step_table.append(["Sample %d" % j, "Gold:", _outputs_to_string(z_output_texts[j], rev_vocab)])
                    print(tabulate(step_table, tablefmt="plain"))
                    print("Classification report:")
                    print(classification_report(classes_gold, classes_guess))
                    sys.stdout.flush()

                avg_bleus = [[], [], []]
                avg_rouges = [[], [], []]
                classes_guess = []
                classes_gold = []
                avg_step_time = []
                avg_loss_d = []
                avg_loss_g = []


def _format_outputs(outputs):
    if data.GO_ID in outputs:
        outputs = outputs[outputs.index(data.GO_ID) + 1:]
    if data.EOS_ID in outputs:
        outputs = outputs[:outputs.index(data.EOS_ID)]
    return outputs

def _logits_to_outputs(outputs):
    #print("Output Logits:", output_logits)
    #outputs = [int(np.argmax(logits)) for logits in output_logits]
    if data.EOS_ID in outputs:
        outputs = outputs[:outputs.index(data.EOS_ID)]
    return outputs

def _outputs_to_words(outputs, rev_vocab):
    #print("Outputs:", outputs)
    return [rev_vocab[output] for output in outputs]

def _words_to_ngrams(words, n=2):
    #print("Words:", words)
    ngrams = ["_".join(tuple) for tuple in zip(*(words[i:] for i in range(n)))]
    #print("Words:", words)
    #print("%d-grams:", n, ngrams)
    return ngrams


def _get_precision_bleu(guess, gold, n=2):
    #print("Guess:", guess)
    guess=_words_to_ngrams(guess, n)
    gold=_words_to_ngrams(gold, n)
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


def _get_recall_rouge(guess, gold, n=2):
    #print("Guess:", guess)
    guess=_words_to_ngrams(guess, n)
    gold=_words_to_ngrams(gold, n)
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


def main(unused_argv):

    batch_size = 4

    hps = interpretor_gan_model.HParams(
        enc_layers=1,#2,
        enc_timesteps=20,#200,
        dec_timesteps=10,#100,
        num_hidden=10,#256,
        emb_dim=128,
        d_min_lr=0.01,
        d_lr=0.15,
        mu=0,
        sigma=1)

    print("Preparing data from %s..." % FLAGS.data_dir)

    train_input_texts, train_output_texts, train_output_classes, _ = data.read_eeg_data(FLAGS.data_dir + '/train')
    print(train_input_texts[0])
    train_words = [word for text in train_input_texts+train_output_texts for word in text]
    print(train_words[0])
    data.create_vocabulary(FLAGS.vocab_path, train_words, FLAGS.vocab_size)
    vocab, rev_vocab = data.initialize_vocabulary(FLAGS.vocab_path)

    train_set = data.Batcher(train_input_texts, train_output_texts, train_output_classes,
                             vocab, hps.enc_timesteps, hps.dec_timesteps, batch_size)

    devel_input_texts, devel_output_texts, devel_output_classes, _ = data.read_eeg_data(FLAGS.data_dir + '/dev')
    devel_set = data.Batcher(devel_input_texts, devel_output_texts, devel_output_classes,
                             vocab, hps.enc_timesteps, hps.dec_timesteps, batch_size)

    _train(train_set, devel_set, hps=hps, batch_size=batch_size, vocab=vocab, rev_vocab=rev_vocab)


if __name__ == "__main__":
    tf.app.run()
