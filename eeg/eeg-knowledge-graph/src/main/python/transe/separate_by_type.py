import sys, os

dir_ = sys.argv[1]
triples = [(line.split('\t')[0], line.split('\t')[1], line.split('\t')[2])
           for line in open(os.path.join(dir_, "all_relations.tsv"), 'r')]
with open(os.path.join(dir_, 'all_relations_typed.tsv'), 'w+') as f:
  for triple in triples:
    rel = triple[1]
    if rel == 'OCCURS_WITH':
      rel = 'OCCURS_WITH_%s' % triple[0].split('_')[0]
    f.write('%s\t%s\t%s' % (triple[0], rel, triple[2]))
