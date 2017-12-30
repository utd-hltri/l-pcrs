"Training task script."
from __future__ import absolute_import
from __future__ import print_function
from __future__ import division

import os
import json
import pprint
import argparse
import tensorflow as tf
import tflearn
from tensorflow.contrib.learn.python.learn import learn_runner
from tensorflow.contrib import learn
from sklearn import metrics

from .experiment import generate_experiment_fn
from .inputs import generate_input_fn, read_and_decode_labels, generate_input_fn_attr
from .serving import generate_serving_input_fn, generate_serving_input_fn_attr
from .model import model_fn


def main(args):
    pp = pprint.PrettyPrinter(indent=2)
    metadata_path = os.path.join(args.data_dir, '{}.json'.format(args.dataset_id))
    with tf.gfile.Open(metadata_path) as metadata_file:
        metadata = json.load(metadata_file)
    num_blocks = metadata['max_entities_length']
    metadata['num_blocks'] = num_blocks

    print('Creating Experiment with metadata:')
    pp.pprint({k: v for (k, v) in metadata.iteritems() if k != 'vocab'})

    use_attrs = not args.no_attrs
    vocab_size = metadata['vocab_size']
    task_size = metadata['task_size']
    train_steps_per_epoch = task_size // args.batch_size
    run_config = tf.contrib.learn.RunConfig(
        save_summary_steps=train_steps_per_epoch,
        save_checkpoints_steps=5 * train_steps_per_epoch,
        save_checkpoints_secs=None)
    params = {
        'vocab_size': vocab_size,
        'embedding_size': args.embedding_size,
        'num_blocks': num_blocks,
        'num_labels': args.num_labels,
        'story_len': metadata['max_story_length'],
        'learning_rate_min': args.lr_min,
        'learning_rate_max': args.lr_max,
        'learning_rate_step_size': args.lr_step_size * train_steps_per_epoch,
        'clip_gradients': args.clip_grads,
        'gradient_noise_scale': args.grad_noise,
        'ent_net': args.ent_net,
        'do_norm': (not args.no_norm),
        'num_attrs': metadata['num_attrs'],
        'use_attrs': use_attrs,
        'sum_attrs': args.sum_attrs
    }
    print("Params:")
    pp.pprint(params)
    model = learn.Estimator(model_fn=model_fn,
                            model_dir=args.job_dir,
                            config=run_config,
                            params=params)
    tflearn.DNN()

    train_filename = os.path.join(args.data_dir, '{}_{}.tfrecords'.format(args.dataset_id, 'train'))
    eval_filename = os.path.join(args.data_dir, '{}_{}.tfrecords'.format(args.dataset_id, 'test'))

    train_input_fn = generate_input_fn_attr(
        filename=train_filename,
        metadata=metadata,
        batch_size=args.batch_size,
        num_epochs=1,
        shuffle=True) if use_attrs else \
        generate_input_fn(
        filename=train_filename,
        metadata=metadata,
        batch_size=args.batch_size,
        num_epochs=1,
        shuffle=True)
    eval_input_fn = generate_input_fn_attr(
        filename=eval_filename,
        metadata=metadata,
        batch_size=1,
        shuffle=False) if use_attrs else \
        generate_input_fn(
        filename=eval_filename,
        metadata=metadata,
        batch_size=1,
        shuffle=False)

    y_true = read_and_decode_labels(eval_filename, metadata, use_attrs)
    steps = 100
    for epoch in xrange(args.num_epochs):
        print('Training for %d steps...' % steps)
        model.fit(input_fn=train_input_fn, steps=steps)
        print('Done! Saving checkpoint...')
        model.export_savedmodel(args.job_dir,
                                generate_serving_input_fn_attr(metadata) if use_attrs else
                                generate_serving_input_fn(metadata))
        predictions = model.predict(input_fn=eval_input_fn)
        print("Validation Metrics for Epoch %d" % (epoch + 1))
        print(metrics.classification_report(y_true, predictions))
        print("Overall Accuracy: %s" % metrics.accuracy_score(y_true, predictions))


def punkass_main(args):
    "Entrypoint for training."
    tf.logging.set_verbosity(tf.logging.INFO)

    print(args)
    experiment_fn = generate_experiment_fn(
        data_dir=args.data_dir,
        dataset_id=args.dataset_id,
        num_epochs=args.num_epochs,
        learning_rate_min=args.lr_min,
        learning_rate_max=args.lr_max,
        learning_rate_step_size=args.lr_step_size,
        gradient_noise_scale=args.grad_noise,
        batch_size=args.batch_size,
        embedding_size=args.embedding_size,
        clip_grads=args.clip_grads,
        num_labels=args.num_labels,
        ent_net=args.ent_net,
        do_norm=(not args.no_norm),
        use_attrs=(not args.no_attrs),
        sum_attrs=args.sum_attrs,
        eval=args.eval)

    learn_runner.run(experiment_fn, args.job_dir)


if __name__ == '__main__':
    parser = argparse.ArgumentParser()
    parser.add_argument(
        '--data-dir',
        help='Directory containing data',
        default='/home/rmm120030/working/eeg/relnet/data/attr-feature')
    parser.add_argument(
        '--dataset-id',
        help='Unique id identifying dataset',
        required=True)
    parser.add_argument(
        '--job-dir',
        help='Location to write checkpoints, summaries, and export models',
        required=True)
    parser.add_argument(
        '--num-epochs',
        help='Maximum number of epochs on which to train',
        default=200,
        type=int)
    parser.add_argument(
        '--lr-min',
        help='Minimum learning rate',
        default=2e-4,
        type=float)
    parser.add_argument(
        '--lr-max',
        help='Maximum learning rate',
        default=1e-2,
        type=float)
    parser.add_argument(
        '--lr-step-size',
        help='Learning rate step size (in epochs)',
        default=10,
        type=int)
    parser.add_argument(
        '--grad-noise',
        help='Gradient noise scale',
        default=0.005,
        type=float)
    parser.add_argument(
        '--batch-size',
        help='Batch size',
        default=32,
        type=int)
    parser.add_argument(
        '--embedding-size',
        help='Embedding/Hidden size',
        default=100,
        type=int)
    parser.add_argument(
        '--clip-grads',
        help='Clip gradients to this value',
        default=40.0,
        type=float)
    parser.add_argument(
        '--num-labels',
        help='Number of labels',
        default=4,
        type=int)
    parser.add_argument(
        '--ent-net',
        help='Use EntNet instead of RelNet?',
        default=False,
        type=bool)
    parser.add_argument(
        '--no-norm',
        help='Use EntNet instead of RelNet?',
        default=False,
        type=bool)
    parser.add_argument(
        '--no-attrs',
        help='Do not use attributes?',
        default=False,
        type=bool)
    parser.add_argument(
        '--sum-attrs',
        help='Sum the attribute embeddings together?',
        default=False,
        type=bool)
    parser.add_argument(
        '--eval',
        help='If not None, do eval on this passed tfrecord file instead of training.',
        default=None)

    arguments = parser.parse_args()
    punkass_main(arguments)
