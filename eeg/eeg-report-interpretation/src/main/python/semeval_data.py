import re, os
import numpy as np
import sys


def raw_data(data_path):
  return sparse_data(os.path.join(data_path, "train.svml")), sparse_data(os.path.join(data_path, "devel.svml"))


def sparse_data(labeled_data):#, max_seq_length):
  """Load labeled sparse vectors from data directory "data_path".
    Args:
      labeled_data: sparse vector file
      max_seq_length: maximum sequence length (timestep dimension)
    Return:
      data: matrix of strings where each string represents a labeled sparse vector as represented in the passed file
      continuation: boolean vector indicating whether or not the corresponding datum sequence is a continuation of the
      previous one
  """
  # first, save all the vectors as strings in a list of lists
  data = []
  sequence = []
  # continuation = [0]
  with open(labeled_data, "r") as dataFile:
    for line in dataFile:
      if (len(line.strip()) > 0):
        sequence.append(line)
        # if (len(sequence) >= max_seq_length):
        #   data.append(sequence)
        #   sequence = []
        #   continuation.append(1)
      else:
        data.append(sequence)
        sequence = []
        # continuation.append(0)
  print('using data from {}'.format(labeled_data))
  return data#, continuation[0:len(continuation) - 1]

label_idxs = {}
def batch_iterator(data, timestep, dimension, batch_size):
  """
  Produces a generator of pairs of tensors representing a batch of data with corresponding labels
  Args:
    labeled_data: string path to the sparse vector file
    timestep: int, size of the largest sequence
    dimension: int, vector dimension
    batch_size: int, number of examples to be processed at a time

  Yields:
    Pairs of the batches of data and its labels
    The first element of the tuple is the data, which is a tensor of shape [batch_size, timestep, dimension].
    The second element of the tuple is the labels, which is a tensor of shape [batch_size, timestep].
  """
  num_batches = len(data) // batch_size
  print('Starting %d batches...' % num_batches)
  for batch in xrange(num_batches):
    # Data and Labels are [b, t, d] tensors where:
    # b is batch size
    # t is a timestep (max sequence length)
    # d is vector dimension
    Data = np.zeros((batch_size, timestep, dimension), dtype=np.float32)
    Labels = np.zeros((batch_size, timestep), dtype=np.int32)
    # n: example number
    for n, sequence in enumerate(data[batch * batch_size : min(len(data), (batch+1) * batch_size)]):
      # t: timestep
      for t, vec_string in enumerate(sequence):
        if '\t' in vec_string:
          entries = re.split('\t', vec_string.strip())
        elif ' ' in vec_string:
          entries = re.split(' ', vec_string.strip())
        else:
          sys.exit("No recognized delimiter in training vector file. Valid delimiters are spaces and tabs (don't mix them).")
        # data
        for idx in entries[1:]:
          Data[n, t, int(idx[:idx.index(":")])] = float(idx[idx.index(":") + 1:])
        # labels
        if not label_idxs.__contains__(entries[0]):
          label_idxs[entries[0]] = len(label_idxs)
          print("making new index entry for class {} -> {}".format(entries[0], label_idxs[entries[0]]))
        Labels[n, t] = label_idxs[entries[0]]
    yield (Data, Labels)