from __future__ import absolute_import
from __future__ import print_function
from __future__ import division

import os
import json
import tensorflow as tf
from tensorflow.contrib import learn, metrics
from sklearn import metrics as skmetrics
import pprint

from .hooks import EarlyStoppingHook
from .inputs import generate_input_fn, generate_input_fn_attr, read_and_decode_labels
from .serving import generate_serving_input_fn, generate_serving_input_fn_attr
from .model import model_fn


def generate_experiment_fn(data_dir,
                           dataset_id,
                           num_epochs,
                           learning_rate_min,
                           learning_rate_max,
                           learning_rate_step_size,
                           gradient_noise_scale,
                           batch_size,
                           embedding_size,
                           clip_grads,
                           num_labels,
                           ent_net,
                           do_norm,
                           use_attrs,
                           sum_attrs,
                           eval):
    "Return _experiment_fn for use with learn_runner."
    def _experiment_fn(output_dir):
        pp = pprint.PrettyPrinter(indent=2)

        metadata_path = os.path.join(data_dir, '{}.json'.format(dataset_id))
        with tf.gfile.Open(metadata_path) as metadata_file:
            metadata = json.load(metadata_file)

        print('Creating Experiment with metadata:')
        pp.pprint({k: v for (k, v) in metadata.iteritems() if k != 'vocab'})

        train_filename = os.path.join(data_dir, '{}_{}.tfrecords'.format(dataset_id, 'test'))
        eval_filename = eval if eval else os.path.join(data_dir, '{}_{}.tfrecords'.format(dataset_id, 'train'))

        num_blocks = metadata['max_entities_length']
        metadata['num_blocks'] = num_blocks
        train_input_fn = generate_input_fn_attr(
            filename=train_filename,
            metadata=metadata,
            batch_size=batch_size,
            num_epochs=num_epochs,
            shuffle=True) if use_attrs else\
            generate_input_fn(
            filename=train_filename,
            metadata=metadata,
            batch_size=batch_size,
            num_epochs=num_epochs,
            shuffle=True)

        eval_input_fn = generate_input_fn_attr(
            filename=eval_filename,
            metadata=metadata,
            batch_size=batch_size,
            num_epochs=1,
            shuffle=False) if use_attrs else \
            generate_input_fn(
            filename=eval_filename,
            metadata=metadata,
            batch_size=batch_size,
            num_epochs=1,
            shuffle=False)

        vocab_size = metadata['vocab_size']
        task_size = metadata['task_size']
        train_steps_per_epoch = task_size // batch_size

        run_config = learn.RunConfig(
            save_summary_steps=1e20,
            save_checkpoints_steps=train_steps_per_epoch,
            save_checkpoints_secs=None)

        params = {
            'vocab_size': vocab_size,
            'embedding_size': embedding_size,
            'num_blocks': num_blocks,
            'num_labels': num_labels,
            'story_len': metadata['max_story_length'],
            'learning_rate_min': learning_rate_min,
            'learning_rate_max': learning_rate_max,
            'learning_rate_step_size': learning_rate_step_size * train_steps_per_epoch,
            'clip_gradients': clip_grads,
            'gradient_noise_scale': gradient_noise_scale,
            'ent_net': ent_net,
            'do_norm': do_norm,
            'num_attrs': metadata['num_attrs'],
            'use_attrs': use_attrs,
            'sum_attrs': sum_attrs
        }
        print("Params:")
        pp.pprint(params)

        estimator = learn.Estimator(
            model_dir=output_dir,
            model_fn=model_fn,
            config=run_config,
            params=params)

        validation_labels = read_and_decode_labels(eval_filename, metadata, use_attrs)

        assert validation_labels
        if eval:
            predictions = estimator.predict(input_fn=eval_input_fn)
            print('Evaluating data at %s' % eval)
            if not isinstance(predictions, list):
                predictions = list(predictions)
            print(skmetrics.classification_report(validation_labels, predictions))
        else:
            eval_metrics = {
                'accuracy': learn.MetricSpec(
                    metric_fn=metrics.streaming_accuracy),
                # 'precision': learn.MetricSpec(
                #     metric_fn=create_multiclass_precision_metric(num_labels)),
                # 'recall': learn.MetricSpec(
                #     metric_fn=create_multiclass_recall_metric(num_labels))
            }

            train_monitors = [
                EarlyStoppingHook(
                    input_fn=eval_input_fn,
                    estimator=estimator,
                    metrics=eval_metrics,
                    metric_name='accuracy',
                    every_steps=100,
                    max_patience=50 * train_steps_per_epoch,
                    minimize=False,
                    numpy_val_data=validation_labels)
            ]

            serving_input_fn = generate_serving_input_fn_attr(metadata) if use_attrs else generate_serving_input_fn(metadata)
            export_strategy = learn.utils.make_export_strategy(serving_input_fn)

            experiment = learn.Experiment(
                estimator=estimator,
                train_input_fn=train_input_fn,
                eval_input_fn=eval_input_fn,
                eval_metrics=eval_metrics,
                train_monitors=train_monitors,
                train_steps=None,
                eval_steps=None,
                export_strategies=[export_strategy],
                min_eval_frequency=100)
            return experiment

    return _experiment_fn
