import sys, os, random


def sample(tuples, k):
  likelihoods = [t[3] for t in tuples]
  pmass = sum(likelihoods)
  min_= min(likelihoods)
  ret = []
  for i in xrange(k):
    p = random.uniform(min_, pmass)
    sum_ = min_
    for t in tuples:
      sum_ += t[3]
      if sum_ >= p:
        ret.append(t)
        break
  return ret


data_dir = sys.argv[1]
num_samples = int(sys.argv[2])
true_energies = [(line.split('\t')[0], line.split('\t')[1], line.split('\t')[2], -float(line.split('\t')[3]))
                 for line in open(os.path.join(data_dir, 'true_energies.tsv'), 'r')]
true_energies = [t for t in true_energies if t[0] != t[2] and len(t) == 4]
false_energies = [(line.split('\t')[0], line.split('\t')[1], line.split('\t')[2], -float(line.split('\t')[3]))
                 for line in open(os.path.join(data_dir, 'false_energies.tsv'), 'r')]
false_energies = [t for t in false_energies if t[0] != t[2] and len(t) == 4]

triples = sample(true_energies, num_samples)
triples.extend(sample(false_energies, num_samples))

random.shuffle(triples)

with open(os.path.join(data_dir, 'triples.tsv'), 'w+') as f:
  for triple in triples:
    f.write('%s\t%s\t%s\n' % (triple[0], triple[1], triple[2]))