import tqdm
import random
from sklearn.dummy import DummyClassifier
from sklearn import metrics
from collections import Counter
import pprint as pp
import numpy as np
from multiprocessing.pool import ThreadPool

def numpify(_data):
  X = []
  Y = []
  for story, entity_indexes, entities, label in tqdm.tqdm(_data, "numpifying"):
    Y.append(label)
    X.append(np.random.rand(100))
  return X, Y


def get_data():
  from . import prep_data
  data, _ = prep_data.create_dataset(prep_data.FLAGS)
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
  return numpify(train), numpify(test), prep_data.rel2id


def get_data_attr():
  from . import prep_data_attr
  pool = ThreadPool(1)
  data, _ = prep_data_attr.create_dataset(prep_data_attr.FLAGS, pool)
  num_train = int(0.8 * len(data))
  random.seed(1337)
  random.shuffle(data)
  train = data[:num_train]
  test = data[num_train:]
  return numpify(train), numpify(test), prep_data_attr.rel2id


(train_x, train_y), (test_x, test_y), rel2id = get_data_attr()
dummy = DummyClassifier()
dummy.fit(train_x, train_y)
predictions = dummy.predict(test_x)

id2name = {v: k for (k, v) in rel2id.iteritems()}
id2name[4] = 'OCCURS_WITH'
print(metrics.classification_report(test_y, predictions))
print('Accuracy: %s' % metrics.accuracy_score(test_y, predictions))

class_dist = Counter(train_y)
class_dist.update(test_y)
print('class distribution:')
pp.pprint({id2name[i]: class_dist[i] for i in range(5)})
