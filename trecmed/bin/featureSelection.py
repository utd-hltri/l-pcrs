#!/usr/bin/python2

import logging
import warnings
import numpy as np
from sklearn.datasets import load_svmlight_file
from sklearn import svm, ensemble, metrics
from time import time

# Configure logging
logging.basicConfig(level=logging.INFO,
                    format='%(asctime)s %(levelname)s %(message)s')

###############################################################################
# Load SVM-Light features
###############################################################################
print("Loading features...")
t0 = time()
with warnings.catch_warnings():
  warnings.simplefilter("ignore")
  X, y, qid = load_svmlight_file("/users/travis/code/hltri-shared/trecmed/output/JAMIA_02/features.svml", query_id=True)
duration = time() - t0
print("Loaded %d samples with %d eatures in %0.3f seconds." % (X.shape[0], X.shape[1], duration))

###############################################################################
# Feature selection
###############################################################################
print("\nStarting feature selection...")
t0 = time()
clf = ensemble.ExtraTreesClassifier()
X_new = clf.fit(X.toarray(), y).transform(X)
print(clf.feature_importances_)
print(np.argsort(clf.feature_importances_)[::-1])
duration = time() - t0
print("Reduced %d features to %d in %0.3f seconds." % (X.shape[1], X_new.shape[1], duration))

###############################################################################
# Cross-validation
###############################################################################
#lolo = LeaveOneLabelOut(qid)

###############################################################################
# Train/test splitting
###############################################################################
t0 = time()
split_qid = 131
split_index = np.where(qid == split_qid)[0][0]
print("\nSplitting training and testing set at QID #%d..." % split_qid)
X_train, X_test = X_new[:split_index], X_new[split_index:]
y_train, y_test = y[:split_index], y[split_index:]
duration = time() - t0
print("Created %d training and %d testing samples in %0.3f seconds." % (X_train.shape[0], X_test.shape[0], duration))


###############################################################################
# Train model
###############################################################################
print("\nTraining model...")
t0 = time()
clf = svm.LinearSVC()
clf.fit(X_train, y_train)
duration = time() - t0
print("Trained SVC classifier in %0.3f seconds." % duration)

###############################################################################
# Test & evaluate model
###############################################################################
print("\nEvaluating...")
t0 = time()
pred = clf.predict(X_test)
duration = time() - t0
print("%d predictions made in %0.3f seconds." % (y_test.shape[0], duration))

print("\nF1-Score:\t%0.3f" % metrics.f1_score(y_test, pred))
print("\nMAP-Score:\t%0.3f" % metrics.average_precision_score(y_test, pred))
print("\nClassification Report:")
print(metrics.classification_report(y_test, pred))
print("\nConfusion Matrix:")
print(metrics.confusion_matrix(y_test, pred))



X.shape, y.shape
