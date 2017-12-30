import os
import sys

import numpy as np
import pandas as pd

import util

triples_file = sys.argv[1]
embeddings_dir = sys.argv[2]
observed_triples_file = sys.argv[3]
rng = np.random.RandomState(int(sys.argv[4]))
triples_df = pd.read_table(triples_file, names=['head', 'rel', 'tail'])
observed_triples_df = pd.read_table(observed_triples_file, names=['head', 'rel', 'tail'])
idx2name, ent_embs = util.load_embeddings(os.path.join(embeddings_dir, 'entity_embeddings.tsv'))
r_idx2name, rel_embs = util.load_embeddings(os.path.join(embeddings_dir, 'relation_embeddings.tsv'))

name2idx = {v.replace('_', ' '): k for (k, v) in idx2name.iteritems()}
r_name2idx = {v: k for (k, v) in r_idx2name.iteritems()}

keep = []
for _, row in triples_df.iterrows():
  keep.append(row['head'].replace('_', ' ') in name2idx.keys() and row['tail'].replace('_', ' ') in name2idx.keys())
triples_df['keep'] = keep
triples_df = triples_df[triples_df['keep'] == True]
print('Judging with %s gold' % len(triples_df))

tf_df = util.create_tf_pairs(triples_df, observed_triples_df, rng, ['head', 'rel', 'tail', 'keep'])
print('created tf pairs')
# tf_df = triples_df

energies = []
for _, row in tf_df.iterrows():
  rel = '%s_%s' % (row['rel'], row['head'].split('_')[0]) if row['rel'] == 'OCCURS_WITH' else row['rel']
  diff = ent_embs[name2idx[row['head'].replace('_', ' ')]] + \
         rel_embs[r_name2idx[rel]] - \
         ent_embs[name2idx[row['tail'].replace('_', ' ')]]
  energies.append(np.sum(np.maximum(diff, 0)))
print('calculated energies')

tf_df['energies'] = energies
tf_df = tf_df.sort_values('energies', ascending=False)
print(tf_df)

# count = 0
# with open(sys.argv[5], 'w+') as out:
#   for _, row in tf_df.iterrows():
#     if count < 25:
#       out.write('%s\t%s\t%s\n' % (row['head'], row['rel'], row['tail']))
#       count += 1

print('calculating accuracies...')
accs = []
size = float(len(tf_df))
for i in xrange(len(tf_df)):
  pos_correct = len(tf_df[:i][tf_df['truth_flag'] == True])
  neg_correct = len(tf_df[i:][tf_df['truth_flag'] == False])
  accs.append((pos_correct + neg_correct) / size)

print('Triple Classification accuracy: %s' % max(accs))