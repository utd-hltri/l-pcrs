"""
Loads and pre-processes a bAbI dataset into TFRecords.
"""
from __future__ import absolute_import
from __future__ import print_function
from __future__ import division

import os
import tqdm
import json
import random
from multiprocessing.pool import ThreadPool
import numpy as np
import tensorflow as tf
from itertools import repeat

PAD_TOKEN = '_PAD_'
PAD_ID = 0
rel2id = {u'NONE': 0,
          u'EVOKES': 1,
          u'EVIDENCES': 2,
          u'TREATMENT_FOR': 3,
          # u'OCCURS_WITH': 4,
          # u'OCCURS_WITH_P': 4,
          # u'OCCURS_WITH_A': 4,
          # u'OCCURS_WITH_Te': 4,
          # u'OCCURS_WITH_Tr': 4
          }

FLAGS = tf.app.flags.FLAGS
tf.app.flags.DEFINE_string('output_dir', '/home/rmm120030/working/eeg/relnet/data/attr-feature', 'Dataset destination.')
tf.app.flags.DEFINE_string('run_name', 'relnet', 'Run name')
tf.app.flags.DEFINE_string('input_file', '/home/rmm120030/working/eeg/relnet/data/seed_attr2.json', 'Input json datafile.')
tf.app.flags.DEFINE_boolean('only_relevant_sections', False,
                            'Only use the two sections the subj/obj entities are from instead of the full record?')
tf.app.flags.DEFINE_integer('max_record_length', 20, 'maximum number of sentences in a record')
tf.app.flags.DEFINE_integer('max_sentence_length', 200, 'maximum number of tokens in a sentence')
tf.app.flags.DEFINE_integer('max_entities', 50, 'maximum number of entities in a record')
tf.app.flags.DEFINE_string('metadata', None, 'if not None, use this metadata instead of creating new metadata')

ATTR_DELIMITER = ":::"
attr2index = {"POL":  1,
              "MOD":  2,
              "FB":   3,
              "DISP": 4,
              "HEMI": 5,
              "REC":  6,
              "MAG":  7,
              "BK":   8}


def blank_token_vector(num_attrs):
    return list(repeat(PAD_ID, num_attrs + 1))


def token_2_attribute_vector(token, num_attrs):
    vector = list(repeat(PAD_TOKEN, num_attrs + 1))
    attrs = token.split(ATTR_DELIMITER)
    vector[0] = attrs[0]
    if len(attrs) > 1:
        for attr in attrs[1:9]:
            vector[attr2index[attr.split('=')[0]]] = attr
        for i, attr in enumerate(attrs[9:num_attrs+2]):
            vector[i+9] = attr

    return vector


def parse_records(json_datafile,
                  only_relevant_sections=False,
                  thread_pool=None  # type: ThreadPool
                 ):
    """
    Parse the json data file

    records is a list of (story, entity_indexes, entities, label) tuples where:
      - story is a list of lists of token strings
      - entity_indexes is list of ints
      - entities is a list of entity id strings
      - label is a string
    """
    if only_relevant_sections:
        print('Only saving sentences from relevant section for each example...')
    records = []
    records_json = json.load(json_datafile)['records']
    max_attrs = 0
    for record in tqdm.tqdm(records_json, 'reading'):
        result = thread_pool.apply_async(parse_record, (record, max_attrs, only_relevant_sections))
        (examples, max_attrs) = result.get()
        records.extend(examples)
    return records, max_attrs


def parse_record(record, max_attrs, only_relevant_sections):
    section_dict = {}
    examples = []
    for section_json in record['sections']:
        sentences = [[s.strip() for s in sentence.split(' ')] for sentence in section_json['sentences']]
        entities_ = section_json['concepts']
        section_dict[section_json['name']] = (sentences, entities_)
    if not only_relevant_sections:
        full_story = []
        all_entities = set()
        for (sentences, entities) in section_dict.values():
            full_story.extend(sentences)
            all_entities.update(entities)
        all_entities = list(all_entities)
    for label_json in record['labels']:
        if label_json['label'] in rel2id:
            if only_relevant_sections:
                (subj_sec, subj_ents) = section_dict[label_json['section1']]
                entities = set(subj_ents)
                story = subj_sec
                if not label_json['section1'] == label_json['section2']:
                    (obj_sec, obj_ents) = section_dict[label_json['section2']]
                    story.extend(obj_sec)
                    entities.update(obj_ents)
                entities = list(entities)
            else:
                entities = all_entities
                story = full_story
            entity_indexes = [entities.index(label_json['subject']), entities.index(label_json['object'])]
            for entity_ in entities:
                max_attrs = max(max_attrs, len(entity_.split(ATTR_DELIMITER)) - 1)
            examples.append((story, entity_indexes, entities, label_json['label']))
    return examples, max_attrs


def save_dataset(records,
                 path,
                 metadata,
                 thread_pool  # type: ThreadPool
                 ):
    """
    Save the stories into TFRecords.

    NOTE: Since each sentence is a consistent length from padding, we use
    `tf.train.Example`, rather than a `tf.train.SequenceExample`, which is
    _slightly_ faster.
    """
    print('writing dataset to %s...' % path)
    writer = tf.python_io.TFRecordWriter(path)
    for story, entity_indexes, entities, label in tqdm.tqdm(records, "writing"):
        thread_pool.apply(save_example, (story, entity_indexes, entities, label, writer, metadata))
    writer.close()


def save_example(story, entity_indexes, entities, label, writer, metadata):
    story, entity_indexes, entities = pad_record(story, entity_indexes, entities, metadata)
    story_flat = [attr for sentence in story for token in sentence for attr in token]
    entities_flat = [attr for entity in entities for attr in entity]
    ei_flat = [idx for ei in entity_indexes for idx in ei]

    story_feature = tf.train.Feature(int64_list=tf.train.Int64List(value=story_flat))
    entity_indexes_feature = tf.train.Feature(int64_list=tf.train.Int64List(value=ei_flat))
    keys_feature = tf.train.Feature(int64_list=tf.train.Int64List(value=entities_flat))
    label_feature = tf.train.Feature(int64_list=tf.train.Int64List(value=[label]))

    features = tf.train.Features(feature={
        'story': story_feature,
        'entity_indexes': entity_indexes_feature,
        'keys': keys_feature,
        'label': label_feature
    })

    example = tf.train.Example(features=features)
    writer.write(example.SerializeToString())


def tokenize_records(records, token_to_id):
    """
    Convert all tokens into their unique ids.
    :param records: list of (story, entity_indexes, entities, label) tuples where
        story is a list of lists of tokens
        ...
    :param token_to_id: dict
    :return: list of (story, query, answer) with tokens converted to unique integer ids
    """
    story_ids = []
    def getid(t):
        if t in token_to_id:
            return token_to_id[t]
        else:
            print('%s not in vocab!' % t)
            return PAD_ID
    for story, entity_indexes, entities, label in records:
        story = [[[getid(attr) for attr in token] for token in sentence] for sentence in story]
        entities = [[getid(attr) for attr in ent] for ent in entities]
        label = rel2id[label]
        story_ids.append((story, entity_indexes, entities, label))
    return story_ids


def add_attrs(records, num_attrs, vocab):
    records_with_attrs = []
    for story, entity_indexes, entities, label in tqdm.tqdm(records, 'creating tokenizer'):
        story = [[token_2_attribute_vector(tok, num_attrs) for tok in sentence] for sentence in story]
        entities = [token_2_attribute_vector(entity, num_attrs) for entity in entities]
        if entities[entity_indexes[0]][0] != entities[entity_indexes[1]][0]:
            records_with_attrs.append((story, entity_indexes, entities, label))
    token_to_id = {token: i for i, token in enumerate(vocab)}
    token_to_id[PAD_TOKEN] = PAD_ID
    token_to_id['POL=POS'] = token_to_id['POL=POSITIVE']
    token_to_id['POL=NEG'] = token_to_id['POL=NEGATIVE']
    return records_with_attrs, vocab, token_to_id


def get_tokenizer_and_add_attrs(records, num_attrs):
    "Recover unique tokens as a vocab and map the tokens to ids."
    tokens_all = []
    records_with_attrs = []
    for story, entity_indexes, entities, label in tqdm.tqdm(records, 'creating tokenizer'):
        story = [[token_2_attribute_vector(tok, num_attrs) for tok in sentence] for sentence in story]
        entities = [token_2_attribute_vector(entity, num_attrs) for entity in entities]
        tokens_all.extend([token[0] for sentence in story for token in sentence]
                          + [a for entity in entities for a in entity])
        if entities[entity_indexes[0]][0] != entities[entity_indexes[1]][0]:
            records_with_attrs.append((story, entity_indexes, entities, label))
    vocab = [PAD_TOKEN] + sorted(set(tokens_all))
    token_to_id = {token: i for i, token in enumerate(vocab)}
    token_to_id[PAD_TOKEN] = PAD_ID
    print('Vocab size: %d' % len(vocab))
    return records_with_attrs, vocab, token_to_id


def pad_record(story, entity_indexes, entities, metadata):
    for sentence in story:
        for _ in range(metadata['max_sentence_length'] - len(sentence)):
            sentence.append(blank_token_vector(metadata['num_attrs']))
        assert len(sentence) == metadata['max_sentence_length']

    ei_mat = np.zeros([metadata['max_entities_length'], metadata['max_entities_length']], dtype=np.int64)
    ei_mat[entity_indexes[0], entity_indexes[1]] = 1

    for _ in range(metadata['max_story_length'] - len(story)):
        story.append([blank_token_vector(metadata['num_attrs']) for _ in range(metadata['max_sentence_length'])])
    assert len(story) == metadata['max_story_length']

    for _ in range(metadata['max_entities_length'] - len(entities)):
        entities.append(blank_token_vector(metadata['num_attrs']))
    assert len(entities) == metadata['max_entities_length']

    return story, ei_mat, entities


def truncate_records(stories, max_length, max_entities):
    "Truncate a story to the specified maximum length."
    stories_truncated = []
    for story, entity_indexes, entities, label in stories:
        story_truncated = story[-max_length:]
        if entity_indexes[0] < max_entities and entity_indexes[1] < max_entities:
            entities = entities[-max_entities:]
            stories_truncated.append((story_truncated, entity_indexes, entities, label))
        else:
            print('removing example with %s entities!' % len(entities))
    return stories_truncated


def create_dataset_with_preexisting_metadata(config, pool, metadata):
    "Main entrypoint."

    json_path = config.input_file
    print('Reading json data from %s...' % json_path)

    # read stories
    with open(json_path, 'r') as jsonfile:
        records, _ = parse_records(jsonfile, config.only_relevant_sections, pool)
    # task_size = len(records)
    # print('Maximum number of attributes: %s' % max_attrs)

    # truncate stories to max story length
    records = truncate_records(records, metadata['max_story_length'], metadata['max_entities_length'])

    records, vocab, token_to_id = add_attrs(records, metadata['num_attrs'], metadata['vocab'])

    # convert stories from strings to indexes into the vocab set
    records = tokenize_records(records, token_to_id)

    return records


def create_dataset(config, pool):
    "Main entrypoint."

    json_path = config.input_file
    print('Reading json data from %s...' % json_path)

    # read stories
    with open(json_path, 'r') as jsonfile:
        records, max_attrs = parse_records(jsonfile, config.only_relevant_sections, pool)
    task_size = len(records)
    print('Maximum number of attributes: %s' % max_attrs)

    # truncate stories to max story length
    records = truncate_records(records, config.max_record_length, 100)

    records, vocab, token_to_id = get_tokenizer_and_add_attrs(records, max_attrs)
    vocab_size = len(vocab)

    # convert stories from strings to indexes into the vocab set
    records = tokenize_records(records, token_to_id)

    story_lengths = [len(sentence) for story, _, _, _ in records for sentence in story]
    max_sentence_length = max(story_lengths)
    max_story_length = max([len(story) for story, _, _, _ in records])
    max_entities_length = max([len(entities) for _, _, entities, _ in records])

    print('real max_sentence_length: %s tokens' % max_sentence_length)
    max_sentence_length = min(max_sentence_length, config.max_sentence_length)
    print('real max_story_length: %s sentences' % max_story_length)
    max_story_length = min(max_story_length, config.max_record_length)
    print('real max_entities_length: %s entities' % max_entities_length)
    max_entities_length = min(max_entities_length, config.max_entities)
    metadata = {
        'run_name': config.run_name,
        'task_size': task_size,
        'max_entities_length': max_entities_length,
        'max_story_length': max_story_length,
        'max_sentence_length': max_sentence_length,
        'vocab': vocab,
        'vocab_size': vocab_size,
        'num_attrs': max_attrs
    }

    return records, metadata


def main():
    pool = ThreadPool(processes=1)
    if not os.path.exists(FLAGS.output_dir):
        print('config output dir: %s' % FLAGS.output_dir)
        os.makedirs(FLAGS.output_dir)

    if FLAGS.metadata is not None:
        with tf.gfile.Open(FLAGS.metadata) as metadata_file:
            metadata = json.load(metadata_file)
        data = create_dataset_with_preexisting_metadata(FLAGS, pool, metadata)
        dataset_path = os.path.join(FLAGS.output_dir, '%s.tfrecords' % FLAGS.run_name)
        save_dataset(data, dataset_path, metadata, pool)
    else:
        data, metadata = create_dataset(FLAGS, pool)
        metadata_path = os.path.join(FLAGS.output_dir, '%s.json' % FLAGS.run_name)
        dataset_path_train = os.path.join(FLAGS.output_dir, '%s_train.tfrecords' % FLAGS.run_name)
        dataset_path_test = os.path.join(FLAGS.output_dir, '%s_test.tfrecords' % FLAGS.run_name)
        metadata['filename'] = {
            'train': os.path.basename(dataset_path_train),
            'test': os.path.basename(dataset_path_test)
        }
        with open(metadata_path, 'w') as f:
            json.dump(metadata, f)

        # num_train = int(0.8 * len(data))
        # random.seed(1337)
        # random.shuffle(data)
        # train = data[:num_train]
        # test = data[num_train:]

        pair2list = {}
        for story, entity_indexes, entities, label in data:
            pair = (entities[entity_indexes[0]][0], entities[entity_indexes[1]][0])
            list_ = pair2list[pair] if pair in pair2list else []
            list_.append((story, entity_indexes, entities, label))
            pair2list[pair] = list_
        print('%d different relations in dataset: %s...' % (len(pair2list.keys()), pair2list.keys()[:5]))

        pair_keys = list(pair2list.keys())
        random.seed(1337)
        random.shuffle(pair_keys)
        num_train = int(0.75 * len(pair_keys))
        train = [v for k in pair_keys[:num_train] for v in pair2list[k]]
        random.seed(1337)
        random.shuffle(train)
        test = [v for k in pair_keys[num_train:] for v in pair2list[k]]
        # num_additional = int(0.1 * len(train))
        # test.extend(train[:num_additional])
        # train = train[num_additional:]
        random.seed(1337)
        random.shuffle(test)

        # save_dataset(train, dataset_path_train, metadata, pool)
        save_dataset(test, dataset_path_test, metadata, pool)

if __name__ == '__main__':
    main()
