# Copyright 2016 Mandiant, A FireEye Company
# Authors: Brian Jones
# License: Apache 2.0

''' Example run script for "Relational Learning with TensorFlow" tutorial '''

import math
import os
import time
from collections import defaultdict
from pprint import pprint

import numpy as np
import pandas as pd
import tensorflow as tf
from sklearn import metrics

import models
import util

flags = tf.app.flags
flags.DEFINE_string("data_dir", "/home/rmm120030/working/eeg/knowledge_graph/trimmed2", "data directory")
flags.DEFINE_string("triple_filename", "all_relations_typed.tsv", "name of the file with the triples")
flags.DEFINE_string("ckpt_dir", "ckpt", "checkpoint directory")
flags.DEFINE_string("model_dir", None, "checkpoint directory")
flags.DEFINE_string("model", "transe", "Model type [transe, bilinear, cp]")
flags.DEFINE_string("loss", "ranking_margin", "Model loss type [least_squares, logistic, ranking_margin]")
flags.DEFINE_string("optimizer", "default", "optimizer [default, gd, adam, adagrad]")
flags.DEFINE_string("dist", "euclidean", "distance function for TransX methods [euclidean, manhattan, sqeuclidean]")
flags.DEFINE_string("out_dir", None, "output directory. Uses data_dir if None")
flags.DEFINE_string("seed_emb_file", "/home/rmm120030/working/eeg/knowledge_graph/conc2vec/embeddings.tsv", "output directory. Uses data_dir if None")
flags.DEFINE_integer("max_iter", 30000, "number of mini-batch iteration to use during training [30000]")
flags.DEFINE_integer("emb_size", 100, "Embedding size [20, 50, 100]")
flags.DEFINE_integer("rel_emb_size", None, "Embedding size [20, 50]")
flags.DEFINE_integer("batch_pos_cnt", 100, "Number of positive examples to use in each mini-batch")
flags.DEFINE_integer("num_val", 5000, "number of validation examples to use during training [5000]")
flags.DEFINE_integer("num_test", 5000, "number of test examples to use during testing [5000]")
flags.DEFINE_integer("validate_every_x", 5000, "Validate every x iterations [5000]")
flags.DEFINE_float("max_ent_norm", 1.0, "clip gradients to this norm [50]")
flags.DEFINE_float("margin", 1.0, "Margin parameter for parwise ranking hinge loss")
flags.DEFINE_float("smoothing_weight", 0.0005, "Weight of the smoothing penalty in the loss function")
flags.DEFINE_boolean("personal_space", True, "Get out of my personal space.")
flags.DEFINE_boolean("load_model", False, "If true, load a model from the checkpoint dir else train a new one.")
flags.DEFINE_boolean("add_bias", False,
                     "If True, a bias Variable will be added to the output for least_squares and logistic models.")
flags.DEFINE_boolean("similarity", False, "Use similarity (instead of distance) for evalutaion.")
flags.DEFINE_boolean("manh", False, "Use manhattan distance for evaluation.")
flags.DEFINE_boolean("skip_val", False, "Skip validation.")
flags.DEFINE_boolean("use_seed_emb", False, "Use seed embeddings?")
flags.DEFINE_boolean("smooth", False, "Use smoothing?")


FLAGS = flags.FLAGS


#######################################
# Data preprocessing

def read_triples(fpath, def_df):
    df = pd.read_table(fpath, names=['head', 'rel', 'tail'])
    df['head'] = def_df.loc[df['head']]['word'].values
    df['tail'] = def_df.loc[df['tail']]['word'].values
    return df


def preprocess(train, val, test):
    '''
    Removes triples with a (head,tail) pair found in a val/test example.
    Removes triples from val/test that have unseen heads or tails in the training set.
    :param train: PD DataFrame training set
    :param val: PD DataFrame validation set
    :param test: PD DataFrame test set
    :return:
    '''
    # mask will be a boolean mask where
    #  1 = bad training example (i.e. same (head,tail) found in a val/test example)
    #  0 = good training example
    mask = np.zeros(len(train)).astype(bool)
    # lookup will be dict with
    #  key: (head, tail) pair
    #  value: a list of indices in the training data with that (head, tail) pair
    lookup = defaultdict(list)
    for idx,h,_,t in train.itertuples():
        lookup[(h,t)].append(idx)
    for h,_,t in pd.concat((val,test)).itertuples(index=False):
        if lookup[(h,t)] < len(train):
          mask[lookup[(h,t)]] = True
        if lookup[(t,h)] < len(train):
          mask[lookup[(t,h)]] = True
    train = train.loc[~mask]
    heads, tails = set(train['head']), set(train['tail'])
    val = val.loc[val['head'].isin(heads) & val['tail'].isin(tails)]
    test = test.loc[test['head'].isin(heads) & test['tail'].isin(tails)]
    return train, val, test


#######################################
# Models used in tutorial

def cp():
    return models.Contrastive_CP(embedding_size=FLAGS.emb_size,  #20
                                 maxnorm=FLAGS.max_norm,  #1.5
                                 batch_pos_cnt=FLAGS.batch_pos_cnt,  #100,
                                 max_iter=FLAGS.max_iter,  #30000,
                                 model_type=get_model_type(FLAGS.loss, 'least_squares'),
                                 add_bias=FLAGS.add_bias,  #False
                                 opt=get_optimizer(FLAGS.optimizer, tf.train.AdagradOptimizer(1.0)))


def bilinear():
    return models.Bilinear(embedding_size=FLAGS.emb_size,  #20,
                           maxnorm=FLAGS.max_norm,  #1.0,
                           rel_maxnorm_mult=6.0,
                           batch_pos_cnt=FLAGS.batch_pos_cnt,  #100,
                           max_iter=FLAGS.max_iter,  #30000,
                           model_type=get_model_type(FLAGS.loss, 'logistic'),
                           add_bias=FLAGS.add_bias,  #True
                           opt=get_optimizer(FLAGS.optimizer, tf.train.AdagradOptimizer(1.0)))


def transe(entity_coocurrence_matrix=None):
    return models.TransE(embedding_size=FLAGS.emb_size,  #20
                         batch_pos_cnt=FLAGS.batch_pos_cnt,  #100,
                         max_iter=FLAGS.max_iter,  #30000,
                         dist=FLAGS.dist,  #'euclidean'
                         margin=FLAGS.margin,  #1.0,
                         opt=get_optimizer(FLAGS.optimizer, tf.train.AdagradOptimizer(1.0)),
                         entity_coocurrence_matrix=entity_coocurrence_matrix,
                         smoothing_weight=FLAGS.smoothing_weight)


def transr(rel_emb_size, entity_coocurrence_matrix=None):
  if rel_emb_size is None:
      rel_emb_size = FLAGS.emb_size
  return models.TransR(embedding_size=FLAGS.emb_size,  # 20
                       batch_pos_cnt=FLAGS.batch_pos_cnt,  # 100,
                       max_iter=FLAGS.max_iter,  # 30000,
                       dist=FLAGS.dist,  # 'euclidean'
                       margin=FLAGS.margin,  # 1.0,
                       opt=get_optimizer(FLAGS.optimizer, tf.train.AdagradOptimizer(1.0)),
                       rel_embedding_size=rel_emb_size,
                       entity_coocurrence_matrix=entity_coocurrence_matrix,
                       smoothing_weight=FLAGS.smoothing_weight)


#######################################
# Helper functions for model initialization

def get_model_type(type, default=None):
  if type == 'default':
    return default
  return type


def get_optimizer(name, default=None):
  if name == 'adam':
    return tf.train.AdamOptimizer()
  elif name == 'gd':
    return tf.train.GradientDescentOptimizer(FLAGS.lr)
  elif name == 'adagrad':
    return tf.train.AdagradOptimizer(1.0)
  elif name == 'default':
    return default
  else:
    raise ValueError('Invalid optimizer: %s not in %s' % (name, '[adam, gd, adagrad, default]'))


def get_dist_fun(model):
  if model.dist == 'manhattan':
    return lambda x: sum(np.absolute(x))
  elif model.dist == 'euclidean':
    # +eps because gradients can misbehave for small values in sqrt
    return lambda x: math.sqrt(sum(np.square(x)) + model.EPS)
  elif model.dist == 'sqeuclidean':
    return lambda x: sum(np.square(x))
  else:
    raise Exception('Unknown distance type: %s' % model.dist)


def angular_distance(a, b):
    # if len(a.shape) != 2:
    #     raise ValueError('invalid a shape: %s' % a.shape)
    # if len(b.shape) != 2:
    #     raise ValueError('invalid b shape: %s' % b.shape)
    return np.arccos(metrics.pairwise.cosine_similarity(a, b)) / np.pi


def angular_similarity(a, b):
    return 1.0 - angular_distance(a, b)


def manhattan_distance(a, b):
    if type(a) is list:
      a = np.asarray(a)
    if type(b) is list:
      b = np.asarray(b)
    diff = a - b
    return -np.sum(np.maximum(diff, 0, diff), axis=1).reshape(len(a), 1)


# def norm_likelihood(x):
#     return 1.0 - (x / np.linalg.norm(x))


def create_cooc_matrix(triple_idx_array, num_ents):
  triple_set = set([tuple(triple) for triple in triple_idx_array])
  subj_dict = {}
  obj_dict = {}
  for triple in triple_set:
      add_to_set_dict(subj_dict, triple[0], (triple[1], triple[2]))
      add_to_set_dict(obj_dict, triple[2], (triple[1], triple[0]))

  coocs = np.ndarray([num_ents, num_ents])
  for i in xrange(num_ents):
      subj_set_i = subj_dict[i] if i in subj_dict else set()
      obj_set_i = obj_dict[i] if i in obj_dict else set()
      for j in xrange(num_ents):
          subj_set_j = subj_dict[j] if j in subj_dict else set()
          obj_set_j = obj_dict[j] if j in obj_dict else set()
          coocs[i, j] = len(subj_set_i.intersection(subj_set_j)) + len(obj_set_i.intersection(obj_set_j))
      coocs[i, :] = coocs[i, :] / sum(coocs[i, :])

  return coocs


def add_to_set_dict(set_dict, key, value):
    s = set_dict[key] if key in set_dict else set()
    s.add(value)
    set_dict[key] = s


def make_model_dir(config):
    mdir = config.model_dir
    if mdir is None:
        mdir = os.path.join(config.data_dir, config.ckpt_dir)
    if not os.path.exists(mdir):
        os.mkdir(mdir)
    mdir = os.path.join(mdir, '%s_%s_%s_%s' % (FLAGS.model, FLAGS.loss, FLAGS.dist, 'smooth' if config.smooth else ''))
    if not os.path.exists(mdir):
        os.mkdir(mdir)
    return mdir


if __name__ == '__main__':
    ###################################
    # MODEL
    
    rng = np.random.RandomState(1337)
    energy_fun = angular_similarity if FLAGS.similarity else angular_distance
    energy_fun = manhattan_distance if FLAGS.manh else energy_fun
    model_dir = make_model_dir(FLAGS)

    ###################################
    # DATA
    
    data_dir = FLAGS.data_dir
    all_data = pd.read_table(os.path.join(data_dir, FLAGS.triple_filename), names=['head', 'rel', 'tail'])
    print('loaded %s triples from %s' % (len(all_data), data_dir))
    num_test, num_val = FLAGS.num_test, FLAGS.num_val
    num_train = len(all_data) - (num_test + num_val)
    train = all_data[:num_train]
    val = all_data[num_train:(num_train+num_val)]
    test = all_data[-num_test:]
    # definitions = pd.read_table(os.path.join(data_dir, 'wordnet-mlj12-definitions.txt'),
    #                             index_col=0, names=['word', 'definition'])
    # train = read_wordnet(os.path.join(data_dir, 'wordnet-mlj12-train.txt'), definitions)
    # val = read_wordnet(os.path.join(data_dir, 'wordnet-mlj12-valid.txt'), definitions)
    # test = read_wordnet(os.path.join(data_dir, 'wordnet-mlj12-test.txt'), definitions)
    combined_df = pd.concat((train, val, test))
    all_train_entities = set(train['head']).union(train['tail'])
    all_train_relationships = set(train['rel'])
    
    print()
    print('Train shape:', train.shape)
    print('Validation shape:', val.shape)
    print('Test shape:', test.shape)
    print('Training entity count: {}'.format(len(all_train_entities)))
    print('Training relationship type count: {}'.format(len(all_train_relationships)))
    
    print()
    print('Preprocessing to remove instances from train that have a similar counterpart in val/test...')
    train, val, test = preprocess(train, val, test)
    all_train_entities = set(train['head']).union(train['tail'])
    all_train_relationships = set(train['rel'])
    
    print('Adding negative examples to val and test...')
    combined_df = pd.concat((train, val, test))
    true_val, true_test = val, test
    val = util.create_tf_pairs(val, combined_df, rng)
    test = util.create_tf_pairs(test, combined_df, rng)
    print('Train shape:', train.shape)
    print('Validation shape:', val.shape)
    print('Test shape:', test.shape)
    print()
    print('Skip validation? %s' % FLAGS.skip_val)

    if FLAGS.model == 'cp':
        print('Using separate encoding for head and tail entities')
        field_categories = (set(train['head']), 
                            all_train_relationships,
                            set(train['tail']))
    else:
        print('Using the same encoding for head and tail entities')
        field_categories = (all_train_entities, 
                            all_train_relationships,
                            all_train_entities)

    train, train_idx_array = util.make_categorical(train, field_categories)
    val, val_idx_array = util.make_categorical(val, field_categories)
    test, test_idx_array = util.make_categorical(test, field_categories)
    true_test, true_test_array = util.make_categorical(true_test, field_categories)
    true_val, true_val_array = util.make_categorical(true_val, field_categories)
    combined_df, all_facts_array = util.make_categorical(combined_df, field_categories)
    print('Train check:', train.shape, not train.isnull().values.any())
    print('Val check:', val.shape, not val.isnull().values.any())
    print('Test check:', test.shape, not test.isnull().values.any())
    print('Test idx check:', test_idx_array.shape)
    print('True Test check:', true_test.columns)

    out_dir = FLAGS.data_dir if FLAGS.out_dir is None else os.path.join(FLAGS.data_dir, FLAGS.out_dir)
    if not os.path.exists(out_dir):
        os.mkdir(out_dir)
    with open(os.path.join(out_dir, 'flags.txt'), "w+") as f:
      f.write(str(FLAGS))

    ###################################
    # TRAIN
    
    # Monitor progress on current training batch and validation set
    co_matrix = create_cooc_matrix(train_idx_array, len(all_train_entities)) if FLAGS.smooth else None

    if FLAGS.model == 'transe':
      model = transe(co_matrix)
    elif FLAGS.model == 'bilinear':
      model = bilinear()
    elif FLAGS.model == 'cp':
      model = cp()
    elif FLAGS.model == 'transr':
      model = transr(FLAGS.rel_emb_size, co_matrix)
    else:
      raise ValueError('Invalid model: %s not in %s' % (FLAGS.model, '[transe, bilinear, cp]'))

    print(model.__class__)
    pprint(model.__dict__)

    start = time.time()
    val_labels = np.array(val['truth_flag'], dtype=np.float)
    val_feed_dict = model.create_feed_dict(val_idx_array, val_labels)

    model_file = os.path.join(model_dir, "model")
    known_fact_set = set(tuple(t) for t in all_facts_array)

    skip = FLAGS.skip_val
    print('Skip validation? %s' % skip)

    train_start = time.time()
    def train_step_callback(itr, batch_feed_dict):
        if (itr < 20) or (itr % 500 == 0):
            peritr = (time.time() - train_start) / (1+itr)
            print('done itr %s. %s sec/10k itrs. ETA: %s sec' % (itr, peritr * 10000, peritr * (model.max_iter - itr)))
        if (itr > 0 and (itr % FLAGS.validate_every_x) == 0) or (itr == (model.max_iter-1)):
            print('Saving model to %s\n' % model_file)
            model.saver.save(model.sess, model_file, global_step=itr)
            if not skip:
                elapsed = int(time.time() - start)
                avg_batch_loss = model.sess.run(model.loss, batch_feed_dict) / len(batch_feed_dict[model.target])
                avg_val_loss = model.sess.run(model.loss, val_feed_dict) / len(val_labels)
                val_acc = util.model_pair_ranking_accuracy(model, val_idx_array)
                # Check embedding norms
                names,model_vars = zip(*model.embeddings())
                var_vals = model.sess.run(model_vars)
                for name,var in zip(names, var_vals):
                    norms = np.linalg.norm(var, axis=1)
                    print('Itr {}. {} min/max norm: {:.2} {:.2}'.format(itr, name, np.min(norms), np.max(norms)))
                    if name is 'entity':
                        ent_emb = var
                    elif name is 'rel':
                        rel_emb = var
                avg_rank, hat, hah, pat, pah, acc = util.ranking_evaluation(ent_emb, rel_emb, true_val_array,
                                                                            energy_fun, known_fact_set)
                msg = 'Itr {}, train loss: {:.3}, val loss: {:.3}, val pairwise acc: {:.2}\n' \
                      'val avg_rank: {:.2f}, val hits@10: {:.2%}, val hits@100: {:.2%}\n' \
                      'val p@10: {:.2%}, val p@100: {:.2%}, val rank_acc: {:.2%}, elapsed: {}'
                print(msg.format(itr, avg_batch_loss, avg_val_loss, val_acc, avg_rank, hat, hah, pat, pah, acc, elapsed))
        return True

    if FLAGS.load_model:
        idx2name, entity_embeddings = util.load_embeddings(os.path.join(out_dir, 'entity_embeddings.tsv'))
        rel_idx2name, relation_embeddings = util.load_embeddings(os.path.join(out_dir, 'relation_embeddings.tsv'))
    else:
        print('Training...')
        idx2name = {code: name for (name, code) in zip(train['head'], train['head'].cat.codes)}
        # for (code, name) in zip(val['head'], val['head'].cat.codes):
        #   if code not in idx2name:
        #     idx2name[code] = name
        # for (code, name) in zip(test['head'], test['head'].cat.codes):
        #   if code not in idx2name:
        #     idx2name[code] = name

        # idx2name = dict(enumerate(combined_df['head'].cat.categories))
        name2idx = {v: k for (k,v) in idx2name.iteritems()}
        embeddings_init = util.load_seed_embeddings(FLAGS.seed_emb_file, name2idx, model.embedding_size)\
            if FLAGS.use_seed_emb else None
        model.fit(train_idx_array, train_step_callback, embeddings_init)
        entity_embeddings, relation_embeddings = model.sess.run([model.entity_embedding_vars, model.rel_embedding_vars])
        util.write_embeddings(entity_embeddings, idx2name, os.path.join(out_dir, 'entity_embeddings.tsv'))
        rel_idx2name = dict(enumerate(combined_df['rel'].cat.categories))
        util.write_embeddings(relation_embeddings, rel_idx2name, os.path.join(out_dir, 'relation_embeddings.tsv'))
        print('Done training.')

    ###################################
    # TEST

    # Write Predictions
    print('evaluating on test set...')
    util.meam_recip_rank({v:k for (k, v) in rel_idx2name.iteritems()}, test, known_fact_set, relation_embeddings,
                         entity_embeddings, {v:k for (k,v) in idx2name.iteritems()}, energy_fun)

    triple_set_df = combined_df.drop_duplicates(subset=['head', 'rel', 'tail'])
    triple_set_df_tf = util.create_tf_pairs(triple_set_df, triple_set_df, rng)
    triple_set_df_tf, _ = util.make_categorical(triple_set_df_tf, field_categories)
    util.write_predicted_energies(triple_set_df_tf, os.path.join(out_dir, "true_energies.tsv"),
                                  os.path.join(out_dir, "false_energies.tsv"), entity_embeddings,
                                  relation_embeddings, energy_fun)
    triple_set_df = pd.DataFrame(triple_set_df_tf[triple_set_df_tf['truth_flag'] == True])

    util.write_all_recalls(triple_set_df, entity_embeddings, relation_embeddings, energy_fun,
                           os.path.join(out_dir, 'prec.tsv'), idx2name, {str(v):k for (k,v) in rel_idx2name.iteritems()})
    # Test evaluation
    test_labels = np.array(test['truth_flag'], dtype=np.float)
    test_feed_dict = model.create_feed_dict(test_idx_array, test_labels, training=False)
    acc, pred, scores, thresh_map = util.model_threshold_and_eval(model, test, val)

    avg_rank, hat, hah, pat, pah, acc = util.ranking_evaluation(entity_embeddings, relation_embeddings, true_test_array,
                                                                energy_fun, known_fact_set, true_test, True)
    results_df = test.copy()
    print('Test set accuracy: {:.2%}'.format(float(acc)))
    print('Test average rank: {:.2f}'.format(avg_rank))
    print('Test hits at 10: {:.2%}'.format(hat))
    print('Test hits at 100: {:.2%}'.format(hah))
    print('Test precision at 10: {:.2%}'.format(pat))
    print('Test precision at 100: {:.2%}'.format(pah))
    print('Test ranking accuracy: {:.2%}'.format(acc))
    aps = list(true_test['ap_subj'])
    aps.extend(list(true_test['ap_obj']))
    print('Mean Average Precision: {:.2%}').format(np.average(aps))
    print('Relationship breakdown:')
    results_df['score'] = scores
    results_df['prediction'] = pred
    results_df['is_correct'] = pred == test['truth_flag']
    for rel in set(results_df['rel']):
        rows = results_df[results_df['rel'] == rel]
        n = len(rows)
        correct = rows['is_correct'].sum()
        wrong = n - correct

        rows = true_test[true_test['rel'] == rel]
        rankings = list(rows['held_out_subj'])
        rankings.extend(list(rows['held_out_obj']))
        avg_rank = float(sum(rankings)) / len(rankings)
        hat = float(len([x for x in rankings if x <= 10])) / len(rankings)
        hah = float(len([x for x in rankings if x <= 100])) / len(rankings)

        pats = list(rows['pa10_subj'])
        pats.extend(list(rows['pa10_obj']))
        pat = np.average(pats)

        pahs = list(rows['pa100_subj'])
        pahs.extend(list(rows['pa100_obj']))
        pah = np.average(pahs)

        accs = list(rows['acc_subj'])
        accs.extend(list(rows['acc_obj']))
        acc = np.average(accs)

        aps = list(rows['ap_subj'])
        aps.extend(list(rows['ap_obj']))
        map = np.average(aps)
        print('acc:{:.2} rel:{}, {} / {}, avg_rank:{:.2f}, hat: {:.3%}, hah: {:.3%}, pat: {:.3%}, pah: {:.3%}, '
              'acc: {:.3%}, map: {:.3%}'
              .format(float(correct)/n, rel, correct, n, avg_rank, hat, hah, pat, pah, acc, map))
