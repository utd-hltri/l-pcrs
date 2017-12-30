import re, os
import numpy as np
import random
from collections import Counter

label_sizes = {'BACKGROUND':2,
               # 'CENTRAL':2,
               'DISPERSAL':3,
               'FREQUENCY_BAND':7,
               # 'FRONTAL':2,
               # 'FRONTOCENTRAL':2,
               # 'FRONTOTEMPORAL':2,
               'HEMISPHERE':4,
               'MAGNITUDE':3,
               'MODALITY':4,
               'MORPHOLOGY':27,
               # 'OCCIPITAL':2,
               # 'PARIETAL':2,
               'POLARITY':2,
               'RECURRENCE':3,
               # 'TEMPORAL':2,
               'TYPE':4,
               'LOCATION':8}
# activity_attribtes = ['BACKGROUND', 'CENTRAL', 'DISPERSAL', 'FREQUENCY_BAND', 'FRONTAL', 'FRONTOCENTRAL', 'FRONTOTEMPORAL',
#                       'HEMISPHERE', 'MAGNITUDE', 'MODALITY', 'MORPHOLOGY', 'OCCIPITAL', 'PARIETAL', 'POLARITY', 'RECURRENCE',
#                       'TEMPORAL']

# activity_attribtes = ['BACKGROUND', 'DISPERSAL', 'FREQUENCY_BAND', 'HEMISPHERE', 'MAGNITUDE', 'MODALITY', 'MORPHOLOGY',
#                       'POLARITY', 'RECURRENCE', 'LOCATION']
activity_attributes = ['FREQUENCY_BAND']

event_attributes = ['MODALITY', 'POLARITY', 'TYPE']


def raw_data(data_dir, config, trim_uncommon_features=True):
  # dict of id -> sparse vector
  vectors = {}
  # dict of id -> dict(attr -> label)
  labels = {}
  feature_counter = Counter()
  attr_type = config.type
  with open(os.path.join(data_dir, attr_type, "%s_attr.svml" % attr_type)) as data_file:
    print 'reading vectors from %s' % data_file
    for line in data_file:
        line = line.strip().split()
        if len(line) > 0:
          vectors[line[0]] = line[1:]
          for f in line[1:]:
            feature_counter[f] += 1
  if attr_type == "activity":
    attributes = activity_attributes
  elif attr_type == "event":
    attributes = event_attributes
  else:
    raise ValueError("valid types: {activity, event}. given: ", attr_type)
  print "found %d vectors" % len(vectors)
  for attr in attributes:
    with open(os.path.join(data_dir, attr_type, "%s.lbl" % attr)) as data_file:
      print 'reading labels from %s' % data_file
      for line in data_file:
        line = line.strip().split()
        if len(line) > 0:
          d = labels[line[0]] if line[0] in labels.keys() else {a: [0] for a in attributes}
          # d = map(attr -> value)
          # if attr=LOCATION, d = map(attr -> [values]) since LOCATION is multivalued
          d[attr] = [float(i) for i in line[1:]] if attr == 'LOCATION' else [float(line[1])]
          labels[line[0]] = d
  print "found %d labels" % len(labels)
  if trim_uncommon_features:
    featureset = [f for f in feature_counter.keys() if feature_counter[f] > config.uncommon_feature_threshold]
    print 'got rid of %d uncommon features, leaving %d usable features' % (len(feature_counter.keys()) - len(featureset), len(featureset))
  else:
    featureset = feature_counter.keys()
  vecs = {key: [featureset.index(val) for val in vec if val in featureset] for key, vec in vectors.items()}
  print "using %d vectors" % len(vecs)
  return vecs, labels, len(featureset)


def raw_data_boundary(data_file, max_seq_length):
  """Load labeled sparse vectors from data directory "data_path".
    Args:
      data_file: sparse vector file
      max_seq_length: maximum sequence length
    Return:
      data: 2d array of strings where each string represents a labeled sparse vector as represented in the passed file
  """
  # first, save all the vectors as strings in a list of lists
  data = []
  sequence = []
  with open(data_file, "r") as dataFile:
    for line in dataFile:
      if (len(line.strip()) > 0) and (len(sequence) <= max_seq_length):
        sequence.append(line)
      else:
        data.append(sequence)
        sequence = []
  print('using 2d data from {}'.format(data_file))
  return data


def attr_batch_generator(vector_map, label_map, config, feature_size):
  batch_size = config.batch_size
  num_batches = int(len(vector_map) / config.batch_size) + (0 if len(vector_map) % batch_size == 0 else 1)
  print "generating %d documents in %d batches" % (len(vector_map), num_batches)
  attributes = activity_attributes if config.type == "activity" else event_attributes
  for batch in xrange(num_batches):
    vectors = np.zeros((batch_size, feature_size), dtype=np.bool)
    label_vectors = {lbl:np.zeros((batch_size, label_sizes[lbl]), dtype=np.bool) for lbl in attributes}
    aids = []
    for b, key in enumerate(vector_map.keys()[batch * batch_size : min((batch+1) * batch_size, len(vector_map.keys()))]):
      sparse_vector = vector_map[key]
      # print 'making vector for %s:%s' % (key, sparse_vector)
      sparse_to_dense(sparse_vector, vectors, b)
      labels = label_map[key]
      for attr in attributes:
        # print 'call to s2d for attr %s: %d' % (attr, labels[attr])
        sparse_to_dense(labels[attr], label_vectors[attr], b)
      # if batch % 10 == 0:
      #   print labels
      aids.append(key)
    yield vectors, label_vectors, aids


def boolean_attr_batch_generator(vector_map, label_map, config, feature_size, attributes):
  batch_size = config.batch_size
  num_batches = int(len(vector_map) / config.batch_size) + (0 if len(vector_map) % batch_size == 0 else 1)
  print "generating %d documents in %d batches" % (len(vector_map), num_batches)
  for batch in xrange(num_batches):
    vectors = np.zeros((batch_size, feature_size), dtype=np.bool)
    label_vectors = {lbl:(-1)*np.ones((batch_size), dtype=np.float32) for lbl in attributes}
    aids = []
    for b, key in enumerate(vector_map.keys()[batch * batch_size : min((batch+1) * batch_size, len(vector_map.keys()))]):
      sparse_vector = vector_map[key]
      # print 'making vector for %s:%s' % (key, sparse_vector)
      sparse_to_dense(sparse_vector, vectors, b)
      labels = label_map[key]
      for attr in attributes:
        # print 'call to s2d for attr %s: %d' % (attr, labels[attr])
        if (int(labels[attr][0]) == int(config.label_number)):
          label_vectors[attr][b] = 1.0
        else:
          label_vectors[attr][b] = -1.0
      # if batch % 10 == 0:
      #   print labels
      aids.append(key)
    yield vectors, label_vectors, aids


def sparse_to_dense(sparse_boolean_vector, dense_matrix, x1, x2=None):
  if x2 is None:
    for idx in sparse_boolean_vector:
      dense_matrix[x1, int(idx)] = 1.0
  else:
    for idx in sparse_boolean_vector:
      dense_matrix[x1, x2, int(idx)] = 1.0


def split(line):
  if '\t' in line:
    return re.split('\t', line.strip())
  elif ' ' in line:
    return re.split(' ', line.strip())
  else:
    raise ValueError("No recognized delimiter in training vector file line [%s]. Valid delimiters are spaces and tabs (don't mix them)."
                     % line)