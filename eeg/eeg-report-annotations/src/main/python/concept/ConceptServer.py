# Let's get this party started!
import sys
import falcon
import numpy as np
import os
import tensorflow as tf
import LstmModel, Config, data_utils
from wsgiref import simple_server


# Falcon follows the REST architectural style, meaning (among
# other things) that you think in terms of resources and state
# transitions, which map to HTTP verbs.
class Resource(object):
  def on_post(self, req, resp):
    lines = req.bounded_stream.read()
    # print('Post request: %s' % lines)
    data, _ = data_utils.read_raw_data(config, lines.split('\n'))
    print('Received %d sequences with %d tokens for prediction' % (len(data), sum([len(s) for s in data])))
    apredictions = self.predict(data, activityModel)
    epredictions = self.predict(data, eventModel)
    pstring = ''
    for name in apredictions:
      pstring += '%s %s %s\n' % (name, apredictions[name], epredictions[name])
    print('Returning %d predictions' % len(apredictions))
    resp.body = pstring
    resp.status = falcon.HTTP_200

  def predict(self, data, model):
    state = model.initial_state.eval()
    predictions = {}
    for step, (x, y, lengths, names) in enumerate(data_utils.seq_batch_iterator(data,
                                                                                model.num_steps,
                                                                                model.vocab_size,
                                                                                model.batch_size,
                                                                                True)):
      print('lengths: ', lengths)
      pred = session.run([model.pred], {model.input_data: x,
                                        model.lengths: lengths,
                                        model.initial_state: state})
      pred = np.reshape(pred[0], [model.batch_size, model.num_steps])
      idx = 0
      for (length, p) in zip(lengths, pred):
        if idx + length <= len(names):
          for (name, lbl) in zip(names[idx:idx + length], p[:length]):
            predictions[name] = idx2label[lbl]
          idx += length
    return predictions

  def on_get(self, req, resp):
    """Handles GET requests"""
    resp.status = falcon.HTTP_200  # This is the default status
    resp.body = ('\nTest Success!\n')


# falcon.API instances are callable WSGI apps
app = falcon.API()

# Resources are represented by long-lived class instances
r = Resource()

# things will handle all requests to the '/things' URL path
app.add_route('/boundary', r)
app.add_route('/test', r)

if __name__ == '__main__':
  config = Config.flags
  if len(sys.argv) > 1 and sys.argv[1] == 'hermes':
    config.data_dir = '/home/hermes/models/concept-server'
    config.model_dir = '/home/rmm120030/working/eeg/vec/boundary/tf4'
  else:
    config.data_dir = '/home/rmm120030/working/eeg/vec/boundary/tf4'
    config.model_dir = '/home/rmm120030/working/eeg/vec/boundary/tf4'
  config.val_prob = -1

  idx2label = {v: k for (k, v) in data_utils.seq_label_idxs.iteritems()}
  with tf.Graph().as_default(), tf.Session() as session:
    with tf.variable_scope("activity", reuse=None):
      activityModel = LstmModel.LstmModel(is_training=False, config=config)
    with tf.variable_scope("event", reuse=None):
      eventModel = LstmModel.LstmModel(is_training=False, config=config)
    all_vars = tf.all_variables()

    avars = [k for k in all_vars if k.name.startswith("activity")]
    asaver = tf.train.Saver(avars)
    ackpt_dir = os.path.join(config.model_dir, "activity")
    ackpt = tf.train.get_checkpoint_state(ackpt_dir, config.checkpoint)
    print('loading activity model from ', ackpt)
    if ackpt and ackpt.model_checkpoint_path:
      asaver.restore(session, ackpt.model_checkpoint_path)

    evars = [k for k in all_vars if k.name.startswith("event")]
    esaver = tf.train.Saver(evars)
    eckpt_dir = os.path.join(config.model_dir, "event")
    eckpt = tf.train.get_checkpoint_state(eckpt_dir, config.checkpoint)
    print('loading event model from ', eckpt)
    if eckpt and eckpt.model_checkpoint_path:
      esaver.restore(session, eckpt.model_checkpoint_path)
    httpd = simple_server.make_server('0.0.0.0', 8050, app)
    httpd.serve_forever()
