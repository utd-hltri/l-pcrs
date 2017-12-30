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
import tensorflow as tf

FLAGS = tf.app.flags.FLAGS

tf.app.flags.DEFINE_string('output_dir', '/home/rmm120030/working/eeg/relnet/data/feature', 'Dataset destination.')
tf.app.flags.DEFINE_string('run_name', 'relnet', 'Run name')
tf.app.flags.DEFINE_string('input_file', '/home/rmm120030/working/eeg/relnet/data/seed.json', 'Input json datafile.')
tf.app.flags.DEFINE_boolean('only_relevant_sections', False,
                            'Only use the two sections the subj/obj entities are from instead of the full record?')
tf.app.flags.DEFINE_integer('max_record_length', 20, 'maximum number of sentences in a record')
tf.app.flags.DEFINE_integer('max_sentence_length', 200, 'maximum number of sentences in a record')
tf.app.flags.DEFINE_integer('max_entities', 50, 'maximum number of entities in a record')

PAD_TOKEN = '_PAD'
PAD_ID = 0
rel2id = {u'NONE': 0,
          u'EVOKES': 1,
          u'EVIDENCES': 2,
          u'TREATMENT_FOR': 3,
          u'OCCURS_WITH': 4,
          u'OCCURS_WITH_P': 4,
          u'OCCURS_WITH_A': 4,
          u'OCCURS_WITH_Te': 4,
          u'OCCURS_WITH_Tr': 4}


def parse_records(json_datafile, only_relevant_sections=False):
    """
    Parse the json data file

    records is a list of (story, entity_indexes, entities, label) tuples where:
      - story is a list of lists of token strings
      - entity_indexes is list of ints
      - entities is a list of entity id strings
      - label is a string
    """
    records = []
    records_json = json.load(json_datafile)['records']
    for record in records_json:
        section_dict = {}
        for section_json in record['sections']:
            sentences = [[s.strip() for s in sentence.split(' ')] for sentence in section_json['sentences']]
            entities = section_json['concepts']
            section_dict[section_json['name']] = (sentences, entities)
        if not only_relevant_sections:
            full_story = []
            all_entities = set()
            for (sentences, entities) in section_dict.values():
                full_story.extend(sentences)
                all_entities.update(entities)
            all_entities = list(all_entities)
        for label_json in record['labels']:
            if only_relevant_sections:
                entities = set()
                (subj_sec, subj_ents) = section_dict[label_json['section1']]
                (obj_sec, obj_ents) = section_dict[label_json['section2']]
                entities.update(subj_ents)
                story = subj_sec
                if not label_json['section1'] == label_json['section2']:
                    story.extend(obj_sec)
                    entities.update(obj_ents)
                entities = list(entities)
            else:
                entities = all_entities
                story = full_story
            entity_indexes = [0 for _ in entities]
            entity_indexes[entities.index(label_json['subject'])] = 1
            entity_indexes[entities.index(label_json['object'])] = 1
            assert (sum(entity_indexes) == 2)
            records.append((story, entity_indexes, entities, label_json['label']))

    return records


def save_dataset(records, path):
    """
    Save the stories into TFRecords.

    NOTE: Since each sentence is a consistent length from padding, we use
    `tf.train.Example`, rather than a `tf.train.SequenceExample`, which is
    _slightly_ faster.
    """
    writer = tf.python_io.TFRecordWriter(path)
    for story, entity_indexes, entities, label in tqdm.tqdm(records, "writing"):
        story_flat = [token_id for sentence in story for token_id in sentence]

        story_feature = tf.train.Feature(int64_list=tf.train.Int64List(value=story_flat))
        entity_indexes_feature = tf.train.Feature(int64_list=tf.train.Int64List(value=entity_indexes))
        keys_feature = tf.train.Feature(int64_list=tf.train.Int64List(value=entities))
        label_feature = tf.train.Feature(int64_list=tf.train.Int64List(value=[label]))

        features = tf.train.Features(feature={
            'story': story_feature,
            'entity_indexes': entity_indexes_feature,
            'keys': keys_feature,
            'label': label_feature
        })

        example = tf.train.Example(features=features)
        writer.write(example.SerializeToString())
    writer.close()


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
    for story, entity_indexes, entities, label in records:
        story = [[token_to_id[token] for token in sentence] for sentence in story]
        entities = [token_to_id[ent] for ent in entities]
        label = rel2id[label]
        story_ids.append((story, entity_indexes, entities, label))
    return story_ids


def get_tokenizer(records):
    "Recover unique tokens as a vocab and map the tokens to ids."
    tokens_all = []
    for story, entity_indexes, entities, label in records:
        tokens_all.extend([token for sentence in story for token in sentence] + entities)
    vocab = [PAD_TOKEN] + sorted(set(tokens_all))
    token_to_id = {token: i for i, token in enumerate(vocab)}
    print('Vocab size: %d' % len(vocab))
    return vocab, token_to_id


def pad_records(records, max_sentence_length, max_story_length, max_entities_length):
    "Pad sentences, stories, and queries to a consistent length."
    for story, entity_indexes, entities, label in records:
        for sentence in story:
            for _ in range(max_sentence_length - len(sentence)):
                sentence.append(PAD_ID)
            assert len(sentence) == max_sentence_length

        for _ in range(max_entities_length - len(entity_indexes)):
            entity_indexes.append(0)
        assert len(entity_indexes) == max_entities_length

        for _ in range(max_story_length - len(story)):
            story.append([PAD_ID for _ in range(max_sentence_length)])
        assert len(story) == max_story_length

        for _ in range(max_entities_length - len(entities)):
            entities.append(PAD_ID)
        assert len(entities) == max_entities_length

    return records


def truncate_records(stories, max_length):
    "Truncate a story to the specified maximum length."
    stories_truncated = []
    for story, a, b, c in stories:
        story_truncated = story[-max_length:]
        stories_truncated.append((story_truncated, a, b, c))
    return stories_truncated


def create_dataset(config):
    "Main entrypoint."

    json_path = config.input_file
    print('Reading json data from %s...' % json_path)

    # read stories
    with open(json_path, 'r') as jsonfile:
        records = parse_records(jsonfile, config.only_relevant_sections)
    task_size = len(records)

    # truncate stories to max story length
    records = truncate_records(records, config.max_record_length)

    vocab, token_to_id = get_tokenizer(records)
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
        'vocab_size': vocab_size
    }

    # pad each story sentence
    records_pad = pad_records(records, max_sentence_length, max_story_length, max_entities_length)

    return records_pad, metadata


def main():
    if not os.path.exists(FLAGS.output_dir):
        print('config output dir: %s' % FLAGS.output_dir)
        os.makedirs(FLAGS.output_dir)
    metadata_path = os.path.join(FLAGS.output_dir, '%s.json' % FLAGS.run_name)
    dataset_path_train = os.path.join(FLAGS.output_dir, '%s_train.tfrecords' % FLAGS.run_name)
    dataset_path_test = os.path.join(FLAGS.output_dir, '%s_test.tfrecords' % FLAGS.run_name)
    data, metadata = create_dataset(FLAGS)
    metadata['filename'] = {
        'train': os.path.basename(dataset_path_train),
        'test': os.path.basename(dataset_path_test)
    }
    with open(metadata_path, 'w') as f:
        json.dump(metadata, f)

    pair2list = {}
    for story, entity_indexes, entities, label in data:
        pair = tuple([entities[i] for i, e in enumerate(entity_indexes) if e == 1])
        list_ = pair2list[pair] if pair in pair2list else []
        list_.append((story, entity_indexes, entities, label))
        pair2list[pair] = list_
    print('%d different relations in dataset: %s...' % (len(pair2list.keys()), pair2list.keys()[:5]))

    num_train = int(0.75 * len(pair2list.keys()))
    train = [v for k in pair2list.keys()[:num_train] for v in pair2list[k]]
    random.seed(1337)
    random.shuffle(train)
    test = [v for k in pair2list.keys()[num_train:] for v in pair2list[k]]
    random.seed(1337)
    random.shuffle(test)

    save_dataset(train, dataset_path_train)
    save_dataset(test, dataset_path_test)

if __name__ == '__main__':
    main()
