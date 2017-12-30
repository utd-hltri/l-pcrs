import os
import sys

import numpy as np
import pandas as pd

import eval
import random_sample
import util

triples_file = sys.argv[1]
embeddings_dir = "/home/rmm120030/working/eeg/knowledge_graph/trimmed2/sim_out"
observed_triples_file = "/home/rmm120030/working/eeg/knowledge_graph/trimmed2/all_relations_typed.tsv"
rng = np.random.RandomState(2)

if sys.argv[2] == 'new':
  random_sample.sample(observed_triples_file, triples_file)

triples_df = pd.read_table(triples_file, names=['head', 'rel', 'tail'])
observed_triples_df = pd.read_table(observed_triples_file, names=['head', 'rel', 'tail'])
idx2name, ent_embs = util.load_embeddings(os.path.join(embeddings_dir, 'entity_embeddings.tsv'))
r_idx2name, rel_embs = util.load_embeddings(os.path.join(embeddings_dir, 'relation_embeddings.tsv'))

name2idx = {v.replace('_', ' '): k for (k, v) in idx2name.iteritems()}
r_name2idx = {v: k for (k, v) in r_idx2name.iteritems()}

all_entities = set(observed_triples_df['head']).union(observed_triples_df['tail'])
all_rels = set(observed_triples_df['rel'])
_, observed_array = util.make_categorical(observed_triples_df, (all_entities, all_rels, all_entities))

observed_set = set([(int(nda[0]), int(nda[1]), int(nda[2])) for nda in observed_array])

keep = []
for _, row in triples_df.iterrows():
  keep.append(row['head'].replace('_', ' ') in name2idx.keys() and row['tail'].replace('_', ' ') in name2idx.keys())
triples_df['keep'] = keep
triples_df = triples_df[triples_df['keep'] == True]
print('num triples: %s' % len(triples_df))

reciprocals = {r: 0 for r in r_name2idx.keys()}
ranks = {r: 0 for r in r_name2idx.keys()}
lengths = {r: 0 for r in r_name2idx.keys()}
print('calculating ranks...')
print('%s total triples, %s unique triples' % (len(observed_triples_df), len(observed_set)))
for i, row in triples_df.iterrows():
  rel = '%s_%s' % (row['rel'], row['head'].split('_')[0]) if row['rel'] == 'OCCURS_WITH' else row['rel']
  r = r_name2idx[rel]
  head = row['head'].replace('_', ' ')
  tail = row['tail'].replace('_', ' ')
  s_rankings = util.rank_all_entities(r, rel_embs[r], ent_embs, eval.manhattan_distance, o=name2idx[tail])
  rank = len(ent_embs)
  for j, s in enumerate(s_rankings):
    if (s[0], r, name2idx[tail]) in observed_set:
      rank = j+1
      break
  if rank > 1:
    o_rankings = util.rank_all_entities(r, rel_embs[r], ent_embs, eval.manhattan_distance, s=name2idx[head])
    for j, o in enumerate(o_rankings):
      if j+1 >= rank:
        break
      if (name2idx[head], r, o[0]) in observed_set:
        rank = j+1
        break
  if rank < 100:
    reciprocals[rel] += 1.0 / rank
    ranks[rel] += rank
    lengths[rel] += 1
    # print ('rank for %s: %s' % ((head, rel, tail), rank))

mrrs = {r: reciprocals[r] / max(1, lengths[r]) for r in reciprocals.keys()}
mrs = {r: float(ranks[r]) / max(1, lengths[r]) for r in ranks.keys()}
# print('ranks: %s' % ranks)
# print('mrs: %s' % mrs)
print('sizes: %s' % lengths)
print('Mean reciprocal ranks: %s' % mrrs)
print('total MRR: {:.2%}'.format(sum(reciprocals.values()) / sum(lengths.values())))
