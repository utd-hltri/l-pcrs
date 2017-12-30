from __future__ import print_function
from __future__ import absolute_import

import csv
import re
import sys

import numpy as np

from tensorflow.python.platform import gfile


csv.field_size_limit(sys.maxsize)

# Special vocabulary symbols - we always put them at the start.
_PAD = "_PAD"
_GO = "_GO"
_EOS = "_EOS"
_UNK = "_UNK"
_START_VOCAB = [_PAD, _GO, _EOS, _UNK]

PAD_ID = 0
GO_ID = 1
EOS_ID = 2
UNK_ID = 3


def _pad(ids, pad_id, length):
    """Pad or trim list to len length.

    Args:
    ids: list of ints to pad
    pad_id: what to pad with
    length: length to pad or trim to

    Returns:
    ids trimmed or padded with pad_id
    """
    assert pad_id is not None
    assert length is not None

    if len(ids) < length:
        a = [pad_id] * (length - len(ids))
        return ids + a
    else:
        return ids[:length]


class Batcher(object):

    def __init__(self, raw_input_texts, raw_output_texts, raw_output_classes,
                 vocab, max_input_len, max_output_len, batch_size, min_input_len=2, min_output_len=2):
        batched_input_lens = []
        batched_input_texts = []
        batched_output_lens = []
        batched_output_texts = []
        batched_output_classes = []
        for i in xrange(0, len(raw_input_texts) - batch_size, batch_size):
            input_lens = []
            input_texts = []
            output_lens = []
            output_texts = []
            for j in xrange(batch_size):
                input_text = seq_to_token_ids(raw_input_texts[i + j], vocab)
                input_len = len(input_text)
                output_text = seq_to_token_ids(raw_output_texts[i + j], vocab)
                output_len = len(output_text)
                # TODO: fix this -- requires adjusting remaining elements to ensure all batches are the same size
                # if input_len > min_input_len and output_len > min_output_len:
                input_lens.append(input_len)
                input_texts.append(_pad(input_text, PAD_ID, max_input_len))
                output_lens.append(output_len)
                output_texts.append(_pad(output_text, PAD_ID, max_output_len))
            batched_input_lens.append(input_lens)
            batched_input_texts.append(input_texts)
            batched_output_lens.append(output_lens)
            batched_output_texts.append(output_texts)
            batched_output_classes.append(raw_output_classes[i:i + batch_size])

        self._input_texts = np.asarray(batched_input_texts)
        self._input_lens = np.asarray(batched_input_lens)
        self._output_texts = np.asarray(batched_output_texts)
        self._output_lens = np.asarray(batched_output_lens)
        self._output_classes = np.asarray(batched_output_classes)
        self._queue_position = self._num_batches = len(self._input_texts)


    def get_batch(self):
        if self._queue_position == self._num_batches:
            self._batch_queue = np.random.permutation(range(0, self._num_batches))
            self._queue_position = 0
        i = self._batch_queue[self._queue_position]
        self._queue_position += 1

        print('Batch %d:' % i)
        print('  Input Texts: ', self._input_texts[i])
        print('  Input Lens: ', self._input_lens[i])
        print('  Output Texts: ', self._output_texts[i])
        print('  Output Lens: ', self._output_lens[i])
        print('  Output Classes: ', self._output_classes[i])

        return self._input_texts[i], self._input_lens[i], \
               self._output_texts[i], self._output_lens[i], self._output_classes[i]


# Regular expressions used to tokenize.
# _WORD_SPLIT = re.compile("([.,!?\"':;)(])")
_DIGIT_RE = re.compile(r"\d")


def create_vocabulary(vocabulary_path, words, max_vocabulary_size, normalize_digits=True):
    """Create vocabulary file (if it does not exist yet) from data file.
    Data file is assumed to contain one sentence per line. Each sentence is
    tokenized and digits are normalized (if normalize_digits is set).
    Vocabulary contains the most-frequent tokens up to max_vocabulary_size.
    We write it to vocabulary_path in a one-token-per-line format, so that later
    token in the first line gets id=0, second line gets id=1, and so on.
    Args:
      vocabulary_path: path where the vocabulary will be created.
      data_path: data file that will be used to create vocabulary.
      max_vocabulary_size: limit on the size of the created vocabulary.
      tokenizer: a function to use to tokenize each data sentence;
        if None, basic_tokenizer will be used.
      normalize_digits: Boolean; if true, all digits are replaced by 0s.
    """
    if not gfile.Exists(vocabulary_path):
        print("Creating vocabulary %s with max size %d" % (vocabulary_path, max_vocabulary_size))
        vocab = {}
        counter = 0
        for w in words:
            counter += 1
            if counter % 10000 == 0:
                print("  processing word %d = %s" % (counter, w))
            word = re.sub(_DIGIT_RE, "0", w) if normalize_digits else w
            if word in vocab:
                vocab[word] += 1
            else:
                vocab[word] = 1
        vocab_list = _START_VOCAB + sorted(vocab, key=vocab.get, reverse=True)
        if len(vocab_list) > max_vocabulary_size:
            vocab_list = vocab_list[:max_vocabulary_size]
        with gfile.GFile(vocabulary_path, mode="w") as vocab_file:
            for w in vocab_list:
                vocab_file.write(w + "\n")


def initialize_vocabulary(vocabulary_path):
    """Initialize vocabulary from file.
    We assume the vocabulary is stored one-item-per-line, so a file:
      dog
      cat
    will result in a vocabulary {"dog": 0, "cat": 1}, and this function will
    also return the reversed-vocabulary ["dog", "cat"].
    Args:
      vocabulary_path: path to the file containing the vocabulary.
    Returns:
      a pair: the vocabulary (a dictionary mapping string to integers), and
      the reversed vocabulary (a list, which reverses the vocabulary mapping).
    Raises:
      ValueError: if the provided vocabulary_path does not exist.
    """
    if gfile.Exists(vocabulary_path):
        rev_vocab = []
        with gfile.GFile(vocabulary_path, mode="r") as f:
            rev_vocab.extend(f.readlines())
        rev_vocab = [line.strip() for line in rev_vocab]
        vocab = dict([(x, y) for (y, x) in enumerate(rev_vocab)])
        return vocab, rev_vocab
    else:
        raise ValueError("Vocabulary file %s not found.", vocabulary_path)


def seq_to_token_ids(words, vocabulary, normalize_digits=True):
    if not normalize_digits:
        return [vocabulary.get(w, UNK_ID) for w in words]
        # Normalize digits by 0 before looking words up in the vocabulary.
    return [vocabulary.get(re.sub(_DIGIT_RE, "0", w), UNK_ID) for w in words]


def read_eeg_data(path):
    data = {}

    print("Reading '" + path + ".impr.tsv'...")
    with open(path + ".impr.tsv", 'rb') as itsv:
        tsvin = csv.reader(itsv, delimiter='\t', quoting=csv.QUOTE_NONE, lineterminator='\n')
        for row in tsvin:
            docid = row[0]
            clazz = row[1]
            impr = row[2].split(" ")
            if impr:
                data[docid] = {'label': clazz, 'desc': [], 'impr': impr}

    print("Reading '" + path + ".desc.tsv'...")
    with open(path + ".desc.tsv",'rb') as dtsv:
        tsvin = csv.reader(dtsv, delimiter='\t',  quoting=csv.QUOTE_NONE, lineterminator='\n')
        for row in tsvin:
            docid = row[0]
            if data[docid]:
                word = row[1]
                data[docid]['desc'].append(word)


    input_texts = []
    output_texts = []
    output_classes = []
    document_ids = []
    for key, value in data.iteritems():
        document_ids.append(key)
        input_texts.append(value['desc'])
        output_texts.append(value['impr'])
        if (value['label'] == 'ABNORMAL'):
            output_classes.append(1)
        else:
            output_classes.append(0)

    return input_texts, output_texts, output_classes, document_ids

