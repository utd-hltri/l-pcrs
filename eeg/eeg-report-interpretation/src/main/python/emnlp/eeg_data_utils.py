"""Utilities for importing TUH EEG data, tokenizing, vocabularies."""
from __future__ import absolute_import
from __future__ import division
from __future__ import print_function

import sys
import os
import re
import csv
csv.field_size_limit(sys.maxsize)

from tensorflow.python.platform import gfile

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

# Regular expressions used to tokenize.
_WORD_SPLIT = re.compile("([.,!?\"':;)(])")
_DIGIT_RE = re.compile(r"\d")


def read_raw_data(path):
    data = {}
    print("Reading " + path + ".impr.tsv")
    with open(path + ".impr.tsv", 'rb') as itsv:
        tsvin = csv.reader(itsv, delimiter='\t', quoting=csv.QUOTE_NONE, lineterminator='\n')
        for row in tsvin:
            docid = row[0]
            clazz = row[1]
            impr = row[2].split(" ")
            if impr:
                data[docid] = {'label': clazz, 'desc': [], 'impr': impr}

    print("Reading " + path + ".desc.tsv")
    with open(path + ".desc.tsv",'rb') as dtsv:
        tsvin = csv.reader(dtsv, delimiter='\t',  quoting=csv.QUOTE_NONE, lineterminator='\n')
        for row in tsvin:
            docid = row[0]
            if data[docid]:
                vector = row[1:]
                if vector:
                    data[docid]['desc'].append(vector)
                else:
                    del data[docid]


    return data


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


def desc_words(raw_data):
    for _, doc in raw_data.iteritems():
        for word in doc['desc']:
            yield word[0]


def impr_words(raw_data):
    for _, doc in raw_data.iteritems():
        for word in doc['impr']:
            yield word


def build_data(data_dir,
               desc_vocab_path,
               impr_vocab_path,
               desc_vocab_size,
               impr_vocab_size,
               buckets,
               max_size=None,):
    print("Creating vocabulary")
    raw_data = read_raw_data(data_dir + "/train")
    create_vocabulary(desc_vocab_path, desc_words(raw_data), desc_vocab_size)
    create_vocabulary(impr_vocab_path, impr_words(raw_data), impr_vocab_size)

    print("Loading vocabulary")
    desc_w2i, _ = initialize_vocabulary(desc_vocab_path)
    impr_w2i, _ = initialize_vocabulary(impr_vocab_path)

    print("Generating data")
    all_data = []
    for x in ["train", "dev", "test"]:
        print("Generating " + x + " data")
        data = [[] for _ in buckets]
        counter = 0
        for id, value in read_raw_data(data_dir + "/" + x).iteritems():
            if not max_size or counter < max_size:
                counter += 1
                label = value['label']
                desc_ids = seq_to_token_ids((word[0] for word in value['desc']), desc_w2i)
                impr_ids = seq_to_token_ids(value['impr'], impr_w2i)
                impr_ids.append(EOS_ID)
                if desc_ids and impr_ids and label:
                    for bucket_id, (desc_size, impr_size) in enumerate(buckets):
                        if len(desc_ids) < desc_size and len(impr_ids) < impr_size:
                            data[bucket_id].append([desc_ids, label, impr_ids, id])
        all_data.append(data)

    return all_data

