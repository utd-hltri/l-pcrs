from sklearn import svm, datasets, metrics
from sklearn import cross_validation as cv
import numpy as np
import sys

X, Y = datasets.load_svmlight_file(sys.argv[1])
model = svm.SVC()

f1s = []
for run, (train, test) in enumerate(cv.StratifiedKFold(Y,5)):
  model.fit(X[train], Y[train])
  predictions = model.predict(X[test])
  f1s.append(metrics.f1_score(Y[test], predictions))
  print 'Run %d' % (run + 1)
  print metrics.classification_report(Y[test], predictions)
print 'Average f1: %f' % np.average(np.asarray(f1s))