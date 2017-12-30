from __future__ import absolute_import
from __future__ import division
from __future__ import print_function

import random

import numpy as np
from six.moves import xrange  # pylint: disable=redefined-builtin
import tensorflow as tf

import coverage_seq2seq
import eeg_data_utils

class EegInterpretorModel(object):
    def __init__(self, desc_vocab_size, impr_vocab_size, buckets,
                 num_units, num_layers, max_gradient_norm, batch_size,
                 learning_rate, learning_rate_decay_factor,
                 use_coverage=False,
                 use_lstm=False,
                 num_samples=512, forward_only=False):
        self.desc_vocab_size = desc_vocab_size
        self.impr_vocab_size = impr_vocab_size
        self.buckets = buckets
        self.batch_size = batch_size
        self.learning_rate = tf.Variable(float(learning_rate), trainable=False)
        self.learning_rate_decay_op = self.learning_rate.assign(self.learning_rate * learning_rate_decay_factor)
        self.global_step = tf.Variable(0, trainable=False)

        # If we use sampled softmax, we need an output projection.
        output_projection = None
        softmax_loss_function = None
        # Sampled softmax only makes sense if we sample less than vocabulary size.
        if 0 < num_samples < self.impr_vocab_size:
            print("- Creating output projection")
            with tf.device("/cpu:0"):
                w = tf.get_variable("proj_w", [num_units, self.impr_vocab_size])
                w_t = tf.transpose(w)
                b = tf.get_variable("proj_b", [self.impr_vocab_size])
            output_projection = (w, b)

            print("- Defining loss")

            def sampled_loss(inputs, labels):
                with tf.device("/cpu:0"):
                    labels = tf.reshape(labels, [-1, 1])
                    return tf.nn.sampled_softmax_loss(w_t, b, inputs, labels, num_samples,
                                                      self.impr_vocab_size)

            softmax_loss_function = sampled_loss

            # Create the internal multi-layer cell for our RNN.
            print("- Building RNN")
            single_cell = tf.nn.rnn_cell.GRUCell(num_units)
            if use_lstm:
                single_cell = tf.nn.rnn_cell.BasicLSTMCell(num_units)
            cell = single_cell
            if num_layers > 1:
                cell = tf.nn.rnn_cell.MultiRNNCell([single_cell] * num_layers)

            if use_coverage:
                print("-- Using coverage-based attention")
                def seq2seq_f(encoder_inputs, decoder_inputs, do_decode):
                    return coverage_seq2seq.embedding_coverage_attention_seq2seq(
                        encoder_inputs, decoder_inputs, cell,
                        desc_vocab_size, impr_vocab_size,
                        num_units,
                        output_projection=output_projection,
                        feed_previous=do_decode)
            else:
                print("-- Using standard attention")
                def seq2seq_f(encoder_inputs, decoder_inputs, do_decode):
                    return tf.nn.seq2seq.embedding_attention_seq2seq(
                        encoder_inputs, decoder_inputs, cell,
                        desc_vocab_size, impr_vocab_size,
                        num_units,
                        output_projection=output_projection,
                        feed_previous=do_decode)

            # Feeds for inputs.
            print("- Constructing input and output feeds")
            self.encoder_inputs = []
            self.decoder_inputs = []
            self.target_weights = []
            for i in xrange(buckets[-1][0]):  # Last bucket is the biggest one.
                self.encoder_inputs.append(tf.placeholder(tf.int32, shape=[None],
                                                          name="encoder{0}".format(i)))
            for i in xrange(buckets[-1][1] + 1):
                self.decoder_inputs.append(tf.placeholder(tf.int32, shape=[None],
                                                          name="decoder{0}".format(i)))
                self.target_weights.append(tf.placeholder(tf.float32, shape=[None],
                                                          name="weight{0}".format(i)))
            # Our targets are decoder inputs shifted by one.
            targets = [self.decoder_inputs[i + 1]
                       for i in xrange(len(self.decoder_inputs) - 1)]

            # Outputs and losses.
            if forward_only:
                print("- Setting testing outputs & losses")
                self.outputs, self.losses = tf.nn.seq2seq.model_with_buckets(
                    self.encoder_inputs, self.decoder_inputs, targets,
                    self.target_weights, buckets,
                    seq2seq=lambda x, y: seq2seq_f(x, y, True),
                    softmax_loss_function=softmax_loss_function)
                # If we use output projection, we need to project outputs for decoding.
                if output_projection is not None:
                    for b in xrange(len(buckets)):
                        self.outputs[b] = [tf.matmul(output, output_projection[0]) +
                                           output_projection[1]
                                           for output in self.outputs[b]]
            else:
                print("- Setting training outputs & losses")
                self.outputs, self.losses = tf.nn.seq2seq.model_with_buckets(
                    self.encoder_inputs, self.decoder_inputs, targets,
                    self.target_weights, buckets,
                    seq2seq=lambda x, y: seq2seq_f(x, y, False),
                    softmax_loss_function=softmax_loss_function)

            # Gradients and SGD update operation for training the model.
            print("- Defining gradients & updates")
            params = tf.trainable_variables()
            if not forward_only:
                self.gradient_norms = []
                self.updates = []
                opt = tf.train.AdamOptimizer()
                for b in xrange(len(buckets)):
                    print("-- Defining gradients for bucket", b)
                    gradients = tf.gradients(self.losses[b], params)

                    print("-- Clipping gradients for bucket", b)
                    clipped_gradients, norm = tf.clip_by_global_norm(gradients,
                                                                     max_gradient_norm)
                    self.gradient_norms.append(norm)

                    print("-- Creating update operation for bucket", b)
                    self.updates.append(opt.apply_gradients(
                        zip(clipped_gradients, params), global_step=self.global_step))

            self.saver = tf.train.Saver(tf.all_variables())
            print("- Model defined!")

            #self.summary = tf.merge_all_summaries()

    def step(self, session, encoder_inputs, decoder_inputs, target_weights,
             bucket_id, forward_only):
        """Run a step of the model feeding the given inputs.
        Args:
          session: tensorflow session to use.
          encoder_inputs: list of numpy int vectors to feed as encoder inputs.
          decoder_inputs: list of numpy int vectors to feed as decoder inputs.
          target_weights: list of numpy float vectors to feed as target weights.
          bucket_id: which bucket of the model to use.
          forward_only: whether to do the backward step or only forward.
        Returns:
          A triple consisting of gradient norm (or None if we did not do backward),
          average perplexity, and the outputs.
        Raises:
          ValueError: if length of encoder_inputs, decoder_inputs, or
            target_weights disagrees with bucket size for the specified bucket_id.
        """
        # Check if the sizes match.
        encoder_size, decoder_size = self.buckets[bucket_id]
        if len(encoder_inputs) != encoder_size:
            raise ValueError("Encoder length must be equal to the one in bucket,"
                             " %d != %d." % (len(encoder_inputs), encoder_size))
        if len(decoder_inputs) != decoder_size:
            raise ValueError("Decoder length must be equal to the one in bucket,"
                             " %d != %d." % (len(decoder_inputs), decoder_size))
        if len(target_weights) != decoder_size:
            raise ValueError("Weights length must be equal to the one in bucket,"
                             " %d != %d." % (len(target_weights), decoder_size))
        # Input feed: encoder inputs, decoder inputs, target_weights, as provided.
        input_feed = {}
        for l in xrange(encoder_size):
            input_feed[self.encoder_inputs[l].name] = encoder_inputs[l]
        for l in xrange(decoder_size):
            input_feed[self.decoder_inputs[l].name] = decoder_inputs[l]
            input_feed[self.target_weights[l].name] = target_weights[l]
        # Since our targets are decoder inputs shifted by one, we need one more.
        last_target = self.decoder_inputs[decoder_size].name
        input_feed[last_target] = np.zeros([self.batch_size], dtype=np.int32)
        # Output feed: depends on whether we do a backward step or not.
        if not forward_only:
            output_feed = [self.updates[bucket_id],  # Update Op that does SGD.
                           self.gradient_norms[bucket_id],  # Gradient norm.
                           self.losses[bucket_id]]  # Loss for this batch.
                           #self.summary]
        else:
            output_feed = [self.losses[bucket_id]]  # Loss for this batch.
                           #self.summary]
        for l in xrange(decoder_size):  # Output logits.
            output_feed.append(self.outputs[bucket_id][l])
        outputs = session.run(output_feed, input_feed)
        if not forward_only:
            return outputs[1], outputs[2], outputs[3:]  # Gradient norm, loss, no outputs.
        else:
            return None, outputs[0], outputs[2:] #, outputs[1] # No gradient norm, loss, outputs.

    def get_batch(self, data, bucket_id):
        """Get a random batch of data from the specified bucket, prepare for step.
        To feed data in step(..) it must be a list of batch-major vectors, while
        data here contains single length-major cases. So the main logic of this
        function is to re-index data cases to be in the proper format for feeding.
        Args:
          data: a tuple of size len(self.buckets) in which each element contains
            lists of pairs of input and output data that we use to create a batch.
          bucket_id: integer, which bucket to get the batch for.
        Returns:
          The triple (encoder_inputs, decoder_inputs, target_weights) for
          the constructed batch that has the proper format to call step(...) later.
        """
        encoder_size, decoder_size = self.buckets[bucket_id]
        encoder_inputs, decoder_inputs = [], []
        # Get a random batch of encoder and decoder inputs from data,
        # pad them if needed, reverse encoder inputs and add GO to decoder.
        for _ in xrange(self.batch_size):
            encoder_input, _, decoder_input, _ = random.choice(data[bucket_id])
            # print("Sampled input:", encoder_input, decoder_input)
            # Encoder inputs are padded and then reversed.
            encoder_pad = [eeg_data_utils.PAD_ID] * (encoder_size - len(encoder_input))
            encoder_inputs.append(list(reversed(encoder_input + encoder_pad)))
            # Decoder inputs get an extra "GO" symbol, and are padded then.
            decoder_pad_size = decoder_size - len(decoder_input) - 1
            decoder_inputs.append([eeg_data_utils.GO_ID] + decoder_input +
                                  [eeg_data_utils.PAD_ID] * decoder_pad_size)
            # print("Encoder inputs", encoder_inputs[-1])
            # print("Decoder inputs", decoder_inputs[-1])
        # Now we create batch-major vectors from the data selected above.
        batch_encoder_inputs, batch_decoder_inputs, batch_weights = [], [], []
        # Batch encoder inputs are just re-indexed encoder_inputs.
        for length_idx in xrange(encoder_size):
            batch_encoder_inputs.append(
                np.array([encoder_inputs[batch_idx][length_idx]
                          for batch_idx in xrange(self.batch_size)], dtype=np.int32))
            # print("batch_encoder_inputs", batch_encoder_inputs[-1])
        # Batch decoder inputs are re-indexed decoder_inputs, we create weights.
        for length_idx in xrange(decoder_size):
            batch_decoder_inputs.append(
                np.array([decoder_inputs[batch_idx][length_idx]
                          for batch_idx in xrange(self.batch_size)], dtype=np.int32))
            # print("batch_decoder_inputs", batch_encoder_inputs[-1])
            # Create target_weights to be 0 for targets that are padding.
            batch_weight = np.ones(self.batch_size, dtype=np.float32)
            for batch_idx in xrange(self.batch_size):
                # We set weight to 0 if the corresponding target is a PAD symbol.
                # The corresponding target is decoder_input shifted by 1 forward.
                if length_idx < decoder_size - 1:
                    target = decoder_inputs[batch_idx][length_idx + 1]
                if length_idx == decoder_size - 1 or target == eeg_data_utils.PAD_ID:
                    batch_weight[batch_idx] = 0.0
            batch_weights.append(batch_weight)
            # print("batch_weight", batch_weights[-1])

        # print("Batch inputs:", batch_encoder_inputs, batch_decoder_inputs, batch_weencoder_inputsights)
        return batch_encoder_inputs, batch_decoder_inputs, batch_weights
