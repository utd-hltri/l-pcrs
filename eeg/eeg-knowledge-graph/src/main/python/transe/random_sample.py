import sys, random

def sample(triple_pool, out=None):
  all_triples = [(line.split('\t')[0], line.split('\t')[1], line.split('\t')[2]) for line in open(triple_pool, 'r')
                 if len(line) > 2]

  rels_done = 0
  rels = {}

  triples = []

  while rels_done < 7:
    triple = random.sample(all_triples, 1)[0]
    if triple[1] not in rels:
      rels[triple[1]] = 0
    if rels[triple[1]] < 100:
      triples.append(triple)
      rels[triple[1]] += 1
      if rels[triple[1]] == 100:
        rels_done += 1

  # for triple in all_triples:
  #   if triple[1] not in rels:
  #     rels[triple[1]] = 0
  #   if rels[triple[1]] < 100:
  #     triples.append(triple)
  #     rels[triple[1]] += 1

  if out is not None:
    with open(out, 'w+') as f:
      for t in triples:
        f.write('%s\t%s\t%s\n' % (t[0], t[1], t[2]))


sample(sys.argv[1], sys.argv[2])