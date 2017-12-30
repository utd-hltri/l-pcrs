import os
import random

rel2idx = {"doc_relations": 0,
           "HISTORY_MEDICATIONS": 1,
           "HISTORY_DESCRIPTION": 2,
           "HISTORY_IMPRESSION": 3,
           "HISTORY_CORRELATION": 4,
           "MEDICATIONS_DESCRIPTION": 5,
           "MEDICATIONS_IMPRESSION": 6,
           "MEDICATIONS_CORRELATION": 7,
           "DESCRIPTION_IMPRESSION": 8,
           "DESCRIPTION_CORRELATION": 9,
           "IMPRESSION_CORRELATION": 10}


def join_rel_files(dir, joint_filename):
  triples = []
  for name in rel2idx:
    triples.extend([(line.split('\t')[0], name, line.split('\t')[1]) for line in open(os.path.join(dir, name + '.tsv'), 'r')])
  random.shuffle(triples)
  with open(os.path.join(dir, joint_filename), 'w+') as outfile:
    for triple in triples:
      outfile.write('%s\t%s\t%s' % triple)


def load_all_data(config):
  triples = []
  ent2idx = {}
  for rel in rel2idx.keys():
    triples.extend(read_triples(os.path.join(config.data_dir, rel +".tsv"), ent2idx, rel2idx[rel]))
  print("read %s triples from %s" % (len(triples), config.data_dir))
  return triples, ent2idx


def read_triples(triple_file, entity2idx, rel_idx):
  with open(triple_file, 'r') as tfile:
    lines = tfile.readlines()
  print("read in %s triples from %s" % (len(lines), triple_file))

  return [(get_or_add(entity2idx, line.split("\t")[0].strip()),
           rel_idx,
           get_or_add(entity2idx, line.split("\t")[1].strip()))
          for line in lines]


def get_or_add(map, entity):
  if entity not in map:
    map[entity] = len(map)
  return map[entity]

