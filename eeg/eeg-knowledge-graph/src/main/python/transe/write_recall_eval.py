import os
import sys

import pandas as pd

import eval
import util

triples_file = sys.argv[1]
embeddings_dir = sys.argv[2]
outfile = sys.argv[3]
triples_df = pd.read_table(triples_file, names=['head', 'rel', 'tail'])
idx2name, ent_embs = util.load_embeddings(os.path.join(embeddings_dir, 'entity_embeddings.tsv'))
r_idx2name, rel_embs = util.load_embeddings(os.path.join(embeddings_dir, 'relation_embeddings.tsv'))

r_name2idx = {v: k for (k, v) in r_idx2name.iteritems()}

util.write_all_recalls(triples_df, ent_embs, rel_embs, eval.angular_distance, outfile, idx2name, r_name2idx)
