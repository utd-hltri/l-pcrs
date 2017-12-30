import re, os
import numpy as np
import sys
import random
import math


reverse_labels = {"BEFORE": "AFTER",
                  "AFTER": "BEFORE",
                  "BEGINS_ON": "ENDS_ON",
                  "ENDS_ON": "BEGINS_ON",
                  "CONTAINS": "CONTAINED",
                  "CONTAINED": "CONTAINS",
                  "OVERLAP": "OVERLAP",
                  "NONE": "NONE"}


def raw_data_boundary(config, name="boundary.svml"):
  data_file = os.path.join(config.data_dir, name) if config.type is None else os.path.join(config.data_dir, config.type, name)
  with open(data_file, "r") as dataFile:
    train_data, val_data = read_raw_data(config, dataFile)
  print('using 2d data from {}. {} training sentences, {} validation sentences'.format(data_file, len(train_data), len(val_data)))
  return train_data, val_data


def read_raw_data(config, lines):
  train_data = []
  val_data = []
  sequence = []
  for line in lines:
    # print('line: %s' % line)
    if (len(sequence) >= config.num_steps) or (len(line.strip()) == 0):
      if len(sequence) > 0:
        if random.random() > config.val_prob:
          # print('adding seq %s to data' % sequence)
          train_data.append(sequence)
        else:
          val_data.append(sequence)
      sequence = []
    if len(line.strip()) > 0:
      # print('adding %s to current sequence' % line)
      sequence.append(line)

  if len(sequence) > 0:
    train_data.append(sequence)
  return train_data, val_data


def raw_data_relation(data_path, type):
  return sparse_data_2d(os.path.join(data_path, "token.%s.svml" % type)), \
         sparse_data_1d(os.path.join(data_path, "event.%s.svml" % type), map=True), \
         sparse_data_1d(os.path.join(data_path, "pair.%s.svml" % type))


def sparse_data_2d(data_file, max_seq_length):
  """Load labeled sparse vectors from data directory "data_path".
    Args:
      data_file: sparse vector file
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


def sparse_data_1d(data_file, map=False):
  """Load labeled sparse vectors from data directory "data_path".
    Args:
      data_file: sparse vector file
      map: if set to True, return a map using the first element of each line as keys and an array comprising
        the rest of the line as values
    Return:
      data: list of strings where each string represents a labeled sparse vector as represented in the passed file
  """
  # first, save all the vectors as strings in a list
  if map:
    data = {}
  else :
    data = []
  with open(data_file, "r") as dataFile:
    for line in dataFile:
      if map:
        arr = split(line)
        data[arr[0]] = arr[1:]
      else:
        data.append(line)
  print('using 1d data from {}'.format(data_file))
  return data


seq_label_idxs = {'O': 0,
                  'I': 1}
def seq_batch_iterator(data, timestep, dimension, batch_size, use_names=False):
  """
  Produces a generator of pairs of tensors representing a batch of data with corresponding labels
  Args:
    labeled_data: string path to the sparse vector file
    timestep: int, size of the largest sequence
    dimension: int, vector dimension
    batch_size: int, number of examples to be processed loadJson a time

  Yields:
    Pairs of the batches of data and its labels
    The first element of the tuple is the data, which is a tensor of shape [batch_size, timestep, dimension].
    The second element of the tuple is the labels, which is a tensor of shape [batch_size, timestep].
  """
  num_batches = int(max(1, math.ceil(float(len(data)) / batch_size)))
  print('Starting %d batches...' % num_batches)
  for batch in xrange(num_batches):
    # Data and Labels are [b, t, d] tensors where:
    # b is batch size
    # t is a timestep (max sequence length)
    # d is vector dimension
    Data = np.zeros((batch_size, timestep, dimension), dtype=np.float32)
    Labels = np.zeros((batch_size, timestep), dtype=np.int32)
    Lengths = np.zeros((batch_size), dtype=np.int32)
    Names = []
    # n: example number
    for n, sequence in enumerate(data[batch * batch_size : min(len(data), (batch+1) * batch_size)]):
      # t: timestep
      for t, vec_string in enumerate(sequence):
        entries = split(vec_string)
        # data
        if use_names:
          Names.append(entries[-1])
          sparse_to_dense(entries[1:-1], Data, n, t)
        else:
          sparse_to_dense(entries[1:], Data, n, t)
        # labels
        if not seq_label_idxs.__contains__(entries[0]):
          seq_label_idxs[entries[0]] = len(seq_label_idxs)
          print("making new index entry for class {} -> {}".format(entries[0], seq_label_idxs[entries[0]]))
        Labels[n, t] = seq_label_idxs[entries[0]]
      Lengths[n] = len(sequence)
    yield (Data, Labels, Lengths) if not use_names else (Data, Labels, Lengths, Names)


rel_label_idxs = {}
def rel_batch_iterator(tokens, events, pairs, tfeat_dimension, efeat_dimension, pfeat_dimension, max_timestep,
                       batch_size, num_labels, binary_label=None):
  """

  :param tokens: [seq, tok] matrix of strings where each string is a sparse feature vector for a token
  :param events: 2*(seq length) list of strings where each string is a sparse feature vector for an event head
  :param pairs: seq length list of strings where each string is a sparse feature vector where vec[0] is the label
  :param tfeat_dimension: token feature vector size
  :param efeat_dimension: event feature vector size
  :param pfeat_dimension: pairwise feature vector size
  :param max_timestep: longest sequence length
  :param batch_size: batch size
  :param num_labels: number of unique labels (classes)
  :return:
  """
  num_batches = len(pairs) // batch_size
  print 'Starting %d batches...' % num_batches
  if binary_label is not None:
    print 'Doing binary classification for class [%s]' % str(binary_label)
  for batch in xrange(num_batches / 2):
    tok_feats = np.zeros((batch_size, max_timestep, tfeat_dimension), dtype=np.float32)
    ev_feats = np.zeros((batch_size, 2, efeat_dimension), dtype=np.float32)
    pair_feats = np.zeros((batch_size, pfeat_dimension), dtype=np.float32)
    labels = np.zeros((batch_size, num_labels), dtype=np.int32)
    seqlengths = np.zeros((batch_size), dtype=np.int64)
    s = np.zeros((max_timestep, batch_size), dtype=np.bool)
    rev_tok_feats = np.zeros((batch_size, max_timestep, tfeat_dimension), dtype=np.float32)
    rev_ev_feats = np.zeros((batch_size, 2, efeat_dimension), dtype=np.float32)
    rev_pair_feats = np.zeros((batch_size, pfeat_dimension), dtype=np.float32)
    rev_labels = np.zeros((batch_size, num_labels), dtype=np.int32)
    rev_seqlengths = np.zeros((batch_size), dtype=np.int64)
    rev_s = np.zeros((max_timestep, batch_size), dtype=np.bool)
    rev_flag = False
    for b, pair in enumerate(pairs[batch * batch_size * 2 : min(len(pairs), (batch + 1) * batch_size * 2)]):
      pair_list = split(pair)
      if rev_flag:
        make_feature_vectors(pair_list, num_labels, b // 2, rev_s, rev_labels, binary_label, rev_pair_feats, rev_ev_feats, rev_tok_feats,
                             rev_seqlengths, tokens, events)
        rev_flag = False
      else :
        make_feature_vectors(pair_list, num_labels, b // 2, s, labels, binary_label, pair_feats, ev_feats, tok_feats,
                           seqlengths, tokens, events)
        rev_flag = True
    yield (tok_feats, ev_feats, pair_feats, labels, seqlengths, s, False)
    yield (rev_tok_feats, rev_ev_feats, rev_pair_feats, rev_labels, rev_seqlengths, rev_s, True)


def sampling_rel_batch_iterator(tokens, events, pairs, tfeat_dimension, efeat_dimension, pfeat_dimension, max_timestep,
                                batch_size, num_labels, num_batches, binary_label=None):
  """
  :param tokens: [seq, tok] matrix of strings where each string is a sparse feature vector for a token
  :param events: [seq, tok] matrix of strings where each string is a sparse feature vector for an event head
  :param pairs: seq length list of strings where each string is a sparse feature vector where vec[0] is the label
  :param tfeat_dimension: token feature vector size
  :param efeat_dimension: event feature vector size
  :param pfeat_dimension: pairwise feature vector size
  :param max_timestep: longest sequence length
  :param batch_size: batch size
  :param num_labels: number of unique labels (classes)
  :return:
  """
  pair_map = {}
  rev_pair_map = {}
  for pair in pairs:
    pair = split(pair)
    lbl = pair[5]
    if int(pair[1]) < int(pair[2]):
      add_to_map(pair_map, lbl, pair)
    else:
      add_to_map(rev_pair_map, lbl, pair)

  print 'Starting %d batches...' % num_batches
  if binary_label is not None:
    print 'Doing binary classification for class [%s]' % str(binary_label)
  rev_flag = True
  for batch in xrange(num_batches):
    # switch reversal every batch
    if rev_flag:
      rev_flag = False
    else:
      rev_flag = True
    tok_feats = np.zeros((batch_size, max_timestep, tfeat_dimension), dtype=np.float32)
    ev_feats = np.zeros((batch_size, 2, efeat_dimension), dtype=np.float32)
    pair_feats = np.zeros((batch_size, pfeat_dimension), dtype=np.float32)
    labels = np.zeros((batch_size, num_labels), dtype=np.int32)
    seqlengths = np.zeros((batch_size), dtype=np.int64)
    s = np.zeros((max_timestep, batch_size), dtype=np.bool)
    for b in xrange(batch_size):
      # sample random class
      if rev_flag:
        pairs_by_label = sample_class_uniform(rev_pair_map)
      else:
        pairs_by_label = sample_class_uniform(pair_map)
      # sample random example from class
      pair_list = pairs_by_label[random.randint(0, len(pairs_by_label) - 1)]
      make_feature_vectors(pair_list, num_labels, b, s, labels, binary_label, pair_feats, ev_feats, tok_feats,
                           seqlengths, tokens, events)
    yield (tok_feats, ev_feats, pair_feats, labels, seqlengths, s, rev_flag)


def make_feature_vectors(pair_list, num_labels, b, s, labels, binary_label, pair_feats, ev_feats, tok_feats, seqlengths,
                         tokens, events):
  # pair_list = <sequence number> <idx of event head of event 1> <idx of event head of event 2>
  #             <event 1 number> <event 2 number> <label> [<sparse features>]
  seqnum = int(pair_list[0])
  # s denotes the token position for the two heads of the events being related
  s[int(pair_list[1]), b] = True
  s[int(pair_list[2]), b] = True
  if pair_list[1] == pair_list[2]:
    print 'pair with same event head'
    exit()
  label = pair_list[5]

  if num_labels == 2:
    # binary classification on label=binary_label
    if str(label) == str(binary_label):
      # [0, 1] if label = binary label
      labels[b, 1] = 1.0
    else:
      # [1, 0] otherwise
      labels[b, 0] = 1.0
  elif binary_label is None:
    # test all labels loadJson once
    if not rel_label_idxs.__contains__(label):
      rel_label_idxs[label] = len(rel_label_idxs)
      print("making new index entry for class {} -> {}".format(label, rel_label_idxs[label]))
    labels[b, rel_label_idxs[label]] = 1.0
  else:
    print "ERROR: Using binary label but num_labels is %d" % num_labels
    exit()

  sparse_to_dense(pair_list[6:], pair_feats, b)

  # event features
  sparse_to_dense(events[pair_list[3]], ev_feats, b, x2=0)
  sparse_to_dense(events[pair_list[4]], ev_feats, b, x2=1)

  sentence = tokens[seqnum]
  seqlengths[b] = len(sentence)
  for t, tok_vec_string in enumerate(sentence):
    sparse_to_dense(split(tok_vec_string), tok_feats, b, x2=t)


def add_to_map(map, label, pair):
  if map.__contains__(label):
    list = map.get(label)
  else:
    list = []
  list.append(pair)
  map[label] = list


def sample_class_ternary(pair_map, label):
  if label is None:
    return pair_map.get(pair_map.keys()[random.randint(0, len(pair_map.keys()) - 1)])
  else:
    label = str(label)
    i = random.randint(0, 2)
    # pick a random class that is neither label or reverse[label]
    if i == 0:
      while True:
        clazz = pair_map.keys()[random.randint(0, len(pair_map.keys()) - 1)]
        if clazz is not label and clazz is not reverse_labels[label]:
          return pair_map[clazz]
    # use label
    elif i == 1:
      return pair_map[label]
    # use reverse[label]
    else:
      return pair_map[reverse_labels[label]]


def sample_class(pair_map, label):
  if label is None:
    return pair_map.get(pair_map.keys()[random.randint(0, len(pair_map.keys()) - 1)])
  else:
    label = str(label)
    i = random.randint(0, 1)
    # pick a random class that is not label
    if i == 0:
      while True:
        clazz = pair_map.keys()[random.randint(0, len(pair_map.keys()) - 1)]
        if clazz is not label:
          return pair_map[clazz]
    # use label
    else:
      return pair_map[label]


def sample_class_uniform(pair_map):
  return pair_map.get(pair_map.keys()[random.randint(0, len(pair_map.keys()) - 1)])


def sparse_to_dense(sparse_vector, dense_matrix, x1, x2=None):
  if x2 is None:
    for idx in sparse_vector:
      dense_matrix[x1, int(idx[:idx.index(':')])] = float(idx[idx.index(':') + 1:])
  else:
    for idx in sparse_vector:
      dense_matrix[x1, x2, int(idx[:idx.index(':')])] = float(idx[idx.index(':') + 1:])


def split(line):
  if '\t' in line:
    return re.split('\t', line.strip())
  elif ' ' in line:
    return re.split(' ', line.strip())
  else:
    sys.exit("No recognized delimiter in training vector file line [%s]. Valid delimiters are spaces and tabs (don't mix them)."
             % line)
