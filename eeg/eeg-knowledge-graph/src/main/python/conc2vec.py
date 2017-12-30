import gensim
import sys
import time
import os

sentences = os.path.join(sys.argv[1], "conc2vec.txt")
concepts = os.path.join(sys.argv[1], "concepts.txt")
embeddings = os.path.join(sys.argv[1], "embeddings2.tsv")

data = [[t for t in s.split()] for s in open(sentences, 'r')]

print("read %s sentences from %s. Begin training..." % (len(data), sys.argv[1]))
start = time.time()
model = gensim.models.Word2Vec(data, min_count=2, iter=100)
print("Training time: %s seconds" % (time.time() - start))

with open(embeddings, "w+") as f:
  for conc in set(open(concepts, "r")):
    conc = conc.strip()
    if conc in model.wv.vocab:
      s = conc
      for d in model.wv[conc]:
        s = s + "\t" + str(d)
      f.write(s)
      f.write("\n")
    else:
      print('no vector for %s' % conc)