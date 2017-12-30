from sklearn.metrics import f1_score, precision_score, recall_score, accuracy_score
import skflow
import numpy as np
import tensorflow as tf
import csv

# Load data
X_train = []
y_train = []
with open('/home/travis/work/eegs/emnlp/data_v3_tsv/train.tsv', 'rb') as tsv:
    for line in csv.reader(tsv, delimiter='\t'):
        X_train.append(line[0])
        y_train.append(line[1])

X_test = []
y_test = []
with open('/home/travis/work/eegs/emnlp/data_v3_tsv/test.tsv', 'rb') as tsv:
    for line in csv.reader(tsv, delimiter='\t'):
        X_test.append(line[0])
        y_test.append(line[1])


X_train = np.asarray(X_train)
y_train = np.asarray(y_train)

# Define evaluation function
def evaluate(y_test, h_test):
    a = accuracy_score(y_test, h_test)
    p = precision_score(y_test, h_test)
    r = precision_score(y_test, h_test)
    f1 = f1_score(y_test, h_test)
    print("Accuracy: %f", a)
    print("Precision: %f", p)
    print("Recall: %f", r)
    print("F1-Measure: %f", f1)


# Baseline model parameters
EMBEDDING_SIZE = 50
N_WORDS = 9553
MAX_DOCUMENT_LENGTH=400


def rnn_model(X, y):
    word_vectors = skflow.ops.categorical_variable(X, 9553, EMBEDDING_SIZE, 'words')
    word_list = skflow.ops.split_squeeze(0, MAX_DOCUMENT_LENGTH, word_vectors)
    cell = tf.nn.rnn_cell.BasicRNNCell(EMBEDDING_SIZE)
    _, encoding = tf.nn.rnn(cell, word_list, dtype=tf.float32)
    return skflow.models.logistic_regression(encoding, y)


# 1-layer basic RNN
#classifier = skflow.TensorFlowRNNClassifier(rnn_size=EMBEDDING_SIZE,
#                                            n_classes=2,
#                                            cell_type='basic',
#                                            input_op_fn=input_op_fn,
#                                            num_layers=1,
#                                            bidirectional=False,
#                                            sequence_length=None,
#                                            steps=1000,
#                                            optimizer='Adam',
#                                            learning_rate=0.01,
#                                            continue_training=True)

classifier = skflow.TensorFlowEstimator(model_fn=rnn_model, n_classes=2,
                                        steps=1000, optimizer='Adam',
                                        learning_rate=0.01,
                                        continue_training=True)

# Training loop
while True:
    classifier.fit(X_train, y_train, logdir='/tmp/tf_examples/word_rnn')
    evaluate(y_test, classifier.predict(X_test.toarray()))

