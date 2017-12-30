# Copyright 2016 Mandiant, A FireEye Company
# Authors: Brian Jones
# License: Apache 2.0

''' Utility functions for "Relational Learning with TensorFlow" tutorial '''

import numpy as np
import pandas as pd
import time
from scipy import spatial
from itertools import izip
from collections import Counter


def make_id_map(column):
    return {k:v for (k,v) in izip(column.cat.codes, column)}


def df_to_idx_array(df):
    '''Converts a Pandas DataFrame containing relationship triples
       into a numpy index array.

    Args:
        df: Pandas DataFrame with columns 'head', 'rel', and 'tail'. These
            columns must be Categorical. See make_categorical().

    Returns:
        A (N x 3) numpy integer index array built from the column Categorical 
            codes.
    '''
    idx_array = np.zeros((len(df),3), dtype=np.int)
    idx_array[:,0] = df['head'].cat.codes
    idx_array[:,1] = df['rel'].cat.codes
    idx_array[:,2] = df['tail'].cat.codes
    return idx_array


def make_categorical(df, field_sets):
    '''Make DataFrame columns Categorical so that they can be converted to
       index arrays for feeding into TensorFlow models.
       
    Args:
        df: Pandas DataFrame with columns 'head', 'rel', and 'tail'
        field_sets: A tuples containing the item category sets: (head_set,
            rel_set, tail_set). Note that head_set and tail_set can
            be the same if the model embeds all entities into a common
            space.
        
    Returns:
        A new Pandas DataFrame where the 'head', 'rel', and 'tail' columns have 
        been made Caetgorical using the supplied field_sets.
    '''
    head_set, rel_set, tail_set = field_sets
    result = pd.DataFrame()
    result['head'] = pd.Categorical(df['head'].values, categories=head_set)
    result['rel'] = pd.Categorical(df['rel'].values, categories=rel_set)
    result['tail'] = pd.Categorical(df['tail'].values, categories=tail_set)
    if 'truth_flag' in df:
        result['truth_flag'] = df['truth_flag']
    return result, df_to_idx_array(result)


def corrupt(triple, field_replacements, forbidden_set, 
            rng, fields=[0,2], max_tries=1000):
    ''' Produces a corrupted negative triple for the supplied positive triple
    using rejection sampling. Only a single field (from one in the fields 
    argument) is changed.
    
    Args:
        triple: A tuple or list with 3 entries: (head, rel, tail)
            
        field_replacements: A tuple of array-like: (head entities, relationships, 
            tail entities), each containing the (unique) items to use as 
            replacements for the corruption
        
        forbidden_set: A set of triples (typically all known true triples)
            that we should not accidentally create when generating corrupted
            negatives.
        
        rng: Numpy RandomState object
        
        fields: The fields that can be replaced in the triple. Default is 
            [0,2] which corresponds to the head and tail entries. [0,1,2]
            would randomly replace any of the three entries.
        
        max_tries: The maximum number of random corruption attempts before
            giving up and throwing an exception. A corruption attempt can fail
            if the sampled negative is a triple found in forbidden_set.
            
    Returns:
        A corrupted tuple (head, rel, tail) where one entry is different
        than the triple passed in.
    '''
    collision = False
    for _ in range(max_tries):
        field = rng.choice(fields)
        replacements = field_replacements[field]
        corrupted = list(triple)
        corrupted[field] = replacements[rng.randint(len(replacements))]
        collision = (tuple(corrupted) in forbidden_set)
        if not collision:
            break
    if collision:
        raise Exception('Failed to sample a corruption for {} after {} tries'.format(triple, max_tries))
    return corrupted


def create_tf_pairs(true_df, all_true_df, rng, columns=None):
    '''Creates a DataFrame with constrastive positive/negative pairs given
       true triples to constrast and set of "all known" true triples in order
       to avoid accidentally sampling a negative from this set. 
       
    Args:
        true_df: Pandas DataFrame containing true triples to contrast.
            It must contain columns 'head', 'rel', and 'tail'. One 
            random negative will be created for each.
        all_true_df: Pandas DataFrame containing "all known" true triples.
            This will be used to to avoid randomly generating negatives
            that happen to be true but were not in true_df.   
        rng: A Numpy RandomState object
        
    Returns:
        A new Pandas DataFrame with alternating pos/neg pairs. If true_df
        contains rows [p1, p2, ..., pN], then this will contain 2N rows in the
        form [p1, n1, p2, n2, ..., pN, nN].  
    '''
    if columns is None:
        columns = ['head', 'rel', 'tail']
    all_true_tuples = set(all_true_df.itertuples(index=False))
    replacements = (list(set(true_df['head'])), [], list(set(true_df['tail'])))
    result = []
    for triple in true_df.itertuples(index=False):
        corruption = corrupt(triple, replacements, all_true_tuples, rng)
        result.append(triple)
        result.append(corruption)
    result = pd.DataFrame(result, columns=columns)
    result['truth_flag'] = np.tile([True, False], len(true_df))
    return result
    

def threshold_and_eval(test_df, test_scores, val_df, val_scores):
    ''' Test set evaluation protocol from:
        Socher, Richard, et al. "Reasoning with neural tensor networks for 
        knowledge base completion." Advances in Neural Information Processing 
        Systems. 2013.
    
    Finds model output thresholds using val_df to create a binary
    classifier, and then measures classification accuracy on the test
    set scores using these thresholds. A different threshold is found
    for each relationship type. All Dataframes must have a 'rel' column.
       
    Args:
        test_df: Pandas DataFrame containing the test triples
        test_scores: A numpy array of test set scores, one for each triple
            in test_df
        val_df: A Pandas DataFrame containing the validation triples       
        val_scores: A numpy array of validation set scores, one for each triple
            in val_df 
        
    Returns:
        A tuple containing (accuracy, test_predictions, test_scores, threshold_map)
            accuracy: the overall classification accuracy on the test set
            test_predictions: True/False output for test set
            test_scores: Test set scores
            threshold_map: A dict containing the per-relationship thresholds
                found on the validation set, e.g. {'_has_part': 0.562}
    '''
    def find_thresh(df, scores):
        ''' find threshold that maximizes accuracy on validation set '''
        #print(df.shape, scores.shape)
        sorted_scores = sorted(scores)
        best_score, best_thresh = -np.inf, -np.inf
        for i in range(len(sorted_scores)-1):
            thresh = (sorted_scores[i] + sorted_scores[i+1]) / 2.0
            predictions = (scores > thresh)
            correct = np.sum(predictions == df['truth_flag'])
            if correct >= best_score:
                best_score, best_thresh = correct, thresh
        return best_thresh
    threshold_map = {}
    for relationship in set(val_df['rel']):
        mask = np.array(val_df['rel'] == relationship)
        threshold_map[relationship] = find_thresh(val_df.loc[mask], val_scores[mask])
    test_entry_thresholds = np.array([threshold_map[r] for r in test_df['rel']])
    test_predictions = (test_scores > test_entry_thresholds)
    accuracy = np.sum(test_predictions == test_df['truth_flag']) / len(test_predictions) 
    return accuracy, test_predictions, test_scores, threshold_map


def model_threshold_and_eval(model, test_df, val_df):
    ''' See threshold_and_eval(). This is the same except that the supplied 
    model will be used to generate the test_scores and val_scores.
    
    Args:
        model: A trained relational learning model whose predict() will be
            called on index arrays generated from test_df and val_df
        test_df: Pandas DataFrame containing the test triples
        val_df: A Pandas DataFrame containing the validation triples

    Returns:
        A tuple containing (accuracy, test_predictions, test_scores, threshold_map)
            accuracy: the overall classification accuracy on the test set
            test_predictions: True/False output for test set
            test_scores: Test set scores
            threshold_map: A dict containing the per-relationship thresholds
                found on the validation set, e.g. {'_has_part': 0.562}
    '''
    val_scores = model.predict(df_to_idx_array(val_df))
    test_scores = model.predict(df_to_idx_array(test_df))
    return threshold_and_eval(test_df, test_scores, val_df, val_scores)


def pair_ranking_accuracy(model_output):
    ''' Pair ranking accuracy. This only works when model_output comes from
    alternating positive/negative pairs: [pos,neg,pos,neg,...,pos,neg]
    
    Returns:
        The fraction of pairs for which the positive example is scored higher
        than the negative example
    '''
    output_pairs = np.reshape(model_output, [-1,2])
    correct = np.sum(output_pairs[:,0] > output_pairs[:,1])
    return float(correct) / len(output_pairs)


def model_pair_ranking_accuracy(model, data):
    ''' See pair_ranking_accuracy(), this simply calls model.predict(data) to
    generate model_output
    
    Returns:
        The fraction of pairs for which the positive example is scored higher
        than the negative example
    '''
    return pair_ranking_accuracy(model.predict(data))


def ranking_evaluation(entity_embeddings, relation_embeddings, test_idx_array, dist_fun, facts=None,
                       test_df=None, calc_map=False):
    ''' Calculates five ranking scores on the set of passed triples with held out subjects/objects:
        average rank: the average rank of the true held out entity among all entities
        hits at 10: the percentage of true entities that were in the top 10 of the predicted entity ranks
        hits at 100: the percentage of true entities that were in the top 100 of the predicted entity ranks
        precision at 10: average of the percent of facts ranked in the top 10 of the predicted entity ranks
        precision at 10:0 average of the percent of facts ranked in the top 100 of the predicted entity ranks
        ranking accuracy: average of the percent of probability mass assigned to facts
    Args:
        :param entity_embeddings: the entity embedding matrix
        :param relation_embeddings: the relation embedding matrix
        :param test_df: Pandas DataFrame with the triples to evaluate
        :param test_idx_array: index array of the triples in test_df
        :param facts: all known true triples
        :param dist_fun: distance function
        :param calc_map: if True, will calculate the mean average precision and put it in the test_df
    Returns:
        tuple (average_rank, hits@10, hits@100, p@10, p@100, acc))
    '''
    held_out_subj = []
    held_out_obj = []
    pa10_subj = []
    pa10_obj = []
    pa100_subj = []
    pa100_obj = []
    acc_subj = []
    acc_obj = []
    ap_subj = []
    ap_obj = []
    if test_df is not None:
        _subj_emb = []
        _obj_emb = []
    print('being evaluation of %s test triples...' % len(test_idx_array))
    _start = time.time()
    if facts is None:
        facts = set()

    for i, (s, r, o) in enumerate(test_idx_array):
        if i % 100 == 0:
            spt = (time.time() - _start) / (i+1)
            print('done {} test_triples. {:.2} sec/triple. eta: {:.2f} sec'
                  .format(i, spt, spt * (len(test_idx_array) - i)))
        # print(s_idx, r_idx, o_idx)

        r_emb = relation_embeddings[r]
        hos_rankings = rank_all_entities(r, r_emb, entity_embeddings, dist_fun, o=o, forbidden_triples=set())
        held_out_subj.append([t[0] for t in hos_rankings if t[0] == s or (t[0], r, o) not in facts].index(s))
        pa10_subj.append(precisionAtK(10, hos_rankings, facts, r, o=o))
        pa100_subj.append(precisionAtK(100, hos_rankings, facts, r, o=o))
        acc_subj.append(ranking_accuracy(hos_rankings, facts, r, o=o))

        hoo_rankings = rank_all_entities(r, r_emb, entity_embeddings, dist_fun, s=s, forbidden_triples=set())
        held_out_obj.append([t[0] for t in hoo_rankings if t[0] == o or (s, r, t[0]) not in facts].index(o))
        pa10_obj.append(precisionAtK(10, hoo_rankings, facts, r, s=s))
        pa100_obj.append(precisionAtK(100, hoo_rankings, facts, r, s=s))
        acc_obj.append(ranking_accuracy(hoo_rankings, facts, r, s=s))

        if test_df is not None:
            _subj_emb.append(entity_embeddings[s] + r_emb)
            _obj_emb.append(entity_embeddings[o])
        if calc_map:
            ap_subj.append(average_precision(hos_rankings, facts, r, o=o))
            ap_obj.append(average_precision(hoo_rankings, facts, r, s=s))
    if test_df is not None:
        test_df['held_out_subj'] = held_out_subj
        test_df['held_out_obj'] = held_out_obj
        test_df['pa10_subj'] = pa10_subj
        test_df['pa100_subj'] = pa100_subj
        test_df['pa10_obj'] = pa10_obj
        test_df['pa100_obj'] = pa100_obj
        test_df['acc_subj'] = acc_subj
        test_df['acc_obj'] = acc_obj
        test_df['energies'] = dist_fun(np.asarray(_subj_emb), np.asarray(_obj_emb)).tolist()
        if calc_map:
            test_df['ap_subj'] = ap_subj
            test_df['ap_obj'] = ap_obj
    rankings = held_out_obj
    rankings.extend(held_out_subj)
    pa10 = pa10_subj
    pa10.extend(pa10_obj)
    pa100 = pa100_subj
    pa100.extend(pa100_obj)
    acc = acc_obj
    acc.extend(acc_subj)
    return float(sum(rankings)) / len(rankings),\
           float(len([x for x in rankings if x <= 10])) / len(rankings),\
           float(len([x for x in rankings if x <= 100])) / len(rankings),\
           np.average(pa10),\
           np.average(pa100),\
           np.average(acc)


def rank_all_entities(r, r_emb, embeddings, dist_fun, s=None, o=None, forbidden_triples=None):
    '''
    for a (s, r, o) triple, return a ranked list of energies(distances) for that triple with either s or o removed and
    replaced by every entity
    :param r: relation index
    :param r_emb: relation embedding
    :param embeddings: all entity embeddings
    :param dist_fun: distance function
    :param s: subject index. If None, hold out subject
    :param o: object index. If None, hold out object
    :param forbidden_triples: set of triples to exclude from ranking
    :return: a list of tuples (e, d) where e is an entity index and d is the distance value
             of the triple with e instead of the held out entity
    '''
    if s is None:
        o_emb = embeddings[o]
        if forbidden_triples is None:
            energies = list(enumerate(dist_fun(embeddings + r_emb, np.repeat(o_emb.reshape(1, -1), len(embeddings), axis=0))))
        else:
            indexed_embs = {i: e for (i, e) in enumerate(embeddings) if (i, r, o) not in forbidden_triples}
            energies = zip(indexed_embs.keys(), dist_fun(np.asarray(indexed_embs.values()) + r_emb,
                                                       np.repeat(o_emb.reshape(1, -1), len(indexed_embs), axis=0)))
        energies.sort(key=lambda t: t[1][0], reverse=True)
        return energies
    elif o is None:
        s_emb = embeddings[s] + r_emb
        if forbidden_triples is None:
            energies = list(enumerate(dist_fun(np.repeat(s_emb.reshape(1, -1), len(embeddings), axis=0), embeddings)))
        else:
            indexed_embs = {i: e for (i, e) in enumerate(embeddings) if (s, r, i) not in forbidden_triples}
            energies = zip(indexed_embs.keys(), dist_fun(np.repeat(s_emb.reshape(1, -1), len(indexed_embs), axis=0),
                                                         np.asarray(indexed_embs.values())))
        # energies = [(e, dist_fun(s_emb + r_emb, e_emb)) for e, e_emb in enumerate(embeddings)
        #             if (s, r, e) not in forbidden_triples]
        energies.sort(key=lambda t: t[1][0], reverse=True)
        return energies


def precisionAtK(k, rankings, facts, r, s=None, o=None):
    if s is None:
        return float(len([t for t in rankings[:k] if (t[0], r, o) in facts])) / k
    elif o is None:
        return float(len([t for t in rankings[:k] if (s, r, t[0]) in facts])) / k
    else:
        raise ValueError('either s or o should be none to determine which should be held out')


def ranking_accuracy(rankings, facts, r, s=None, o=None):
    if s is None:
        return sum([t[1] for t in rankings if (t[0], r, o) in facts]) / sum([t[1] for t in rankings])
    elif o is None:
        return sum([t[1] for t in rankings if (s, r, t[0]) in facts]) / sum([t[1] for t in rankings])
    else:
        raise ValueError('either s or o should be none to determine which should be held out')


def average_precision(rankings, facts, r, s=None, o=None):
    p = 0.0
    num_rel = 0.0
    if s is None:
        for k in xrange(len(rankings)):
            if (k, r, o) in facts:
                p += precisionAtK(k+1, rankings, facts, r, s, o)
                num_rel += 1
    elif o is None:
        for k in xrange(len(rankings)):
            if (s, r, k) in facts:
                p += precisionAtK(k+1, rankings, facts, r, s, o)
                num_rel += 1
    else:
        raise ValueError('either s or o should be none to determine which should be held out')
    return p / max(1.0, num_rel)


def write_predicted_energies(df, truefile, falsefile, ent_embs, rel_embs, energy_fun=None):
    true_energies = []
    false_energies = []
    done_triples = set()
    idx_array = df_to_idx_array(df)
    df['head_idx'] = idx_array[:,0]
    df['rel_idx'] = idx_array[:,1]
    df['tail_idx'] = idx_array[:,2]
    for s, r, o, s_idx, r_idx, o_idx, tf in zip(df['head'], df['rel'], df['tail'], df['head_idx'], df['rel_idx'],
                                                df['tail_idx'], df['truth_flag']):
        if (s, r, o) not in done_triples:
            energy = energy_fun(np.asarray([ent_embs[s_idx] + rel_embs[r_idx]]), np.asarray([ent_embs[o_idx]]))[0,0]
            if tf:
                true_energies.append((s, r, o, energy))
            else:
                false_energies.append((s, r, o, energy))
            done_triples.add((s, r, o))
    true_energies = sorted(true_energies, key=lambda x: x[3], reverse=True)
    false_energies = sorted(false_energies, key=lambda x: x[3], reverse=True)
    with open(truefile, 'w+') as f:
        for tup in true_energies:
            f.write('%s\t%s\t%s\t%f\n' % tup)
    with open(falsefile, 'w+') as f:
        for tup in false_energies:
            f.write('%s\t%s\t%s\t%f\n' % tup)
    print('wrote predicted energies')


def write_all_recalls(big_df, ent_embs, rel_embs, energy_fun, outfile, idx2name, r_name2idx):
    name2idx = {v: k for (k, v) in idx2name.iteritems()}
    energies = []
    for _, row in big_df.iterrows():
        if (r_name2idx is None):
            energies.append(energy_fun([ent_embs[row['head_idx']] + rel_embs[row['rel_idx']]],
                                   [ent_embs[row['tail_idx']]]))
        else:
            energies.append(energy_fun([ent_embs[name2idx[row['head']]] + rel_embs[r_name2idx[row['rel']]]],
                                       [ent_embs[name2idx[row['tail']]]]))
    big_df['energies'] = energies
    big_df = big_df.sort_values(by='energies', ascending=False)
    cnt = Counter()
    rows = []
    for _, row in big_df.iterrows():
    # for _, row in big_df[big_df['truth_flag'] == True].iterrows():
        rel_type = row['rel']
        if 'OCCURS_WITH' in str(rel_type):
            rel_type = 'OCCURS_WITH'
        if cnt[rel_type] < 10:
            rows.append(row)
            cnt[rel_type] += 1
    df = pd.DataFrame(rows, columns=list(big_df.columns.values))
    print('we got %s things. Let\'s get em writing' % len(df))
    with open(outfile, "w+") as f:
        for s, r, o in zip(df['head'], df['rel'], df['tail']):
            r = str(r)
            r_idx = r_name2idx[r]
            rankings = rank_all_entities(r, rel_embs[r_idx], ent_embs, energy_fun, s=name2idx[s],
                                         forbidden_triples=set())
            c = 0
            s_type = s.split('_')[0]
            for (o_idx, _) in rankings:
                o_type = idx2name[o_idx].split('_')[0]
                if ("OCCURS" in r and o_type == s_type) or \
                   ("EVOKES" in r and o_type == "A") or \
                   ("EVIDENCES" in r and o_type == "P") or \
                   ("TREATMENT" in r and o_type == "P"):
                    f.write('%s\t%s\t%s\n' % (s, r, idx2name[o_idx]))
                    c += 1
                if c == 10:
                    break
            rankings = rank_all_entities(r, rel_embs[r_idx], ent_embs, energy_fun, o=name2idx[o],
                                         forbidden_triples=set())
            c = 0
            o_type = o.split('_')[0]
            s_type_ = s_type
            for (s_idx, _) in rankings:
                s_type = idx2name[s_idx].split('_')[0]
                if ("OCCURS" in r and o_type == s_type) or ("TREATMENT" in r and s_type == "Tr") or \
                    (("EVOKES" in r or "EVIDENCES" in r) and (s_type_ == s_type)):
                    f.write('%s\t%s\t%s\n' % (idx2name[s_idx], r, o))
                    c += 1
                if c == 10:
                    break
            f.write('\n')
    print('been did')


def write_embeddings(embeddings, idx2name, outfile):
    print('writing %s embeddings to %s' % (len(embeddings), outfile))
    with open(outfile, 'w+') as f:
        for idx, emb in enumerate(embeddings):
            f.write('%s\t' % idx2name[idx])
            for x in emb:
                f.write('\t%s' % x)
            f.write('\n')


def load_embeddings(infile):
    emb_list = []
    names_list = []
    with open(infile, 'r') as f:
        for line in f:
            if len(line) > 0:
                arr = line.split('\t')
                names_list.append(arr[0])
                emb_list.append(np.asarray([float(x.strip()) for x in arr[1:] if len(x.strip()) > 0]))
    print('read %s embeddings from %s' % (len(emb_list), infile))
    return dict(enumerate(names_list)), np.asarray(emb_list)


def load_seed_embeddings(infile, name2idx, emb_size):
    idx2name_seed, embeddings_init = load_embeddings(infile)
    assert embeddings_init.shape[1] == emb_size,\
        "initial embeddings of size %s should be %s" % (embeddings_init.shape[1], emb_size)

    init_sd = 1.0 / np.sqrt(emb_size)
    embeddings = init_sd * np.random.randn(len(name2idx), emb_size).astype(np.float32)
    bad_seeds = 0
    for (i, name) in idx2name_seed.iteritems():
        if name in name2idx:
            embeddings[name2idx[name]] = embeddings_init[i]
        else:
            bad_seeds += 1
    print('loaded initial value for %s of %s concepts' % (len(embeddings_init) - bad_seeds, len(name2idx)))
    return embeddings


def meam_recip_rank(r_name2idx, triples_df, observed_set, rel_embs, ent_embs, name2idx, efun):
    reciprocals = {r: 0 for r in r_name2idx.keys()}
    ranks = {r: 0 for r in r_name2idx.keys()}
    lengths = {r: 0 for r in r_name2idx.keys()}
    print('calculating ranks...')
    for i, row in triples_df.iterrows():
        rel = row['rel']
        r = r_name2idx[rel]
        head = row['head']
        tail = row['tail']
        s_rankings = rank_all_entities(r, rel_embs[r], ent_embs, efun, o=name2idx[tail])
        rank = len(ent_embs)
        for j, s in enumerate(s_rankings):
            # print(type(s[0]))
            # print(type(r))
            # print(type(name2idx[tail]))
            # print((s[0], r, name2idx[tail]))
            if (s[0], r, name2idx[tail]) in observed_set:
                rank = j + 1
                break
        if rank > 1:
            o_rankings = rank_all_entities(r, rel_embs[r], ent_embs, efun, s=name2idx[head])
            for j, o in enumerate(o_rankings):
                if j + 1 >= rank:
                    break
                # print(type(o[0]))
                # print(type(r))
                # print(type(name2idx[head]))
                if (name2idx[head], r, o[0]) in observed_set:
                    rank = j + 1
                    break
        if rank < 100:
            reciprocals[rel] += 1.0 / rank
            ranks[rel] += rank
            lengths[rel] += 1
            # print('rank for %s: %s' % ((head, rel, tail), rank))

    mrrs = {r: reciprocals[r] / max(1, lengths[r]) for r in reciprocals.keys()}
    mrs = {r: float(ranks[r]) / max(1, lengths[r]) for r in ranks.keys()}
    print('ranks: %s' % ranks)
    print('mrs: %s' % mrs)
    print('sizes: %s' % lengths)
    print('Mean reciprocal ranks: %s' % mrrs)
    print('total MRR: {:.2%}'.format(sum(reciprocals.values()) / sum(lengths.values())))


def get_triples(ranked_triples_file, outfile):
    triples_by_relation = {}
    with open(ranked_triples_file, 'r') as infile:
        for line in infile:
            if len(line) > 2:
                arr = line.split('\t')
                rel = arr[1]
                if rel not in triples_by_relation.keys():
                    triples_by_relation[rel] = []



class ContrastiveTrainingProvider(object):
    ''' Provides mini-batches for stochastic gradient descent by augmenting 
    a set of positive training triples with random contrastive negative samples. 
    
    Args:
        train: A 2D numpy array with positive training triples in its rows
        batch_pos_cnt: Number of positive examples to use in each mini-batch
        separate_head_tail: If True, head and tail corruptions are sampled
            from entity sets limited to those found in the respective location.
            If False, head and tail replacements are sampled from the set of
            all entities, regardless of location.
        rng: (optional) A NumPy RandomState object

    TODO: Allow a variable number of negative examples per positive. Right
    now this class always provides a single negative per positive, generating
    pairs: [pos, neg, pos, neg, ...]
    '''
    
    def __init__(self, train, batch_pos_cnt=50, 
                 separate_head_tail=False, rng=None):
        self.train = train
        self.batch_pos_cnt = batch_pos_cnt 
        self.separate_head_tail = separate_head_tail
        if rng is None:
            rng = np.random.RandomState()
        self.rng = rng
        self.num_examples = len(train)
        self.epochs_completed = 0
        self.index_in_epoch = 0
        # store set of training tuples for quickly checking negatives
        self.triples_set = set(tuple(t) for t in train)
        # replacement entities
        if separate_head_tail:
            head_replacements = list(set(train[:,0]))
            tail_replacements = list(set(train[:,2]))
        else:
            all_entities = set(train[:,0]).union(train[:,2])
            head_replacements = tail_replacements = list(all_entities)
        self.field_replacements = [head_replacements,
                                   list(set(train[:,1])),
                                   tail_replacements]
        self._shuffle_data()

    def _shuffle_data(self):
        self.rng.shuffle(self.train)

    def next_batch(self):
        ''' 
        Returns:
            A tuple (batch_triples, batch_labels):
            batch_triples: Bx3 numpy array of triples, where B=2*batch_pos_cnt
            batch_labels: numpy array with 0/1 labels for each row in 
                batch_triples
            Each positive is followed by a constrasting negative, so batch_labels
            will alternate: [1, 0, 1, 0, ..., 1, 0]
        '''
        start = self.index_in_epoch
        self.index_in_epoch += self.batch_pos_cnt
        if self.index_in_epoch > self.num_examples:
            # Finished epoch, shuffle data
            self.epochs_completed += 1
            self.index_in_epoch = self.batch_pos_cnt
            start = 0
            self._shuffle_data()
        end = self.index_in_epoch
        batch_triples = []
        batch_labels = []
        for positive in self.train[start:end]:
            batch_triples.append(positive)
            batch_labels.append(1.0)
            negative = corrupt(positive, self.field_replacements, self.triples_set, self.rng)
            batch_triples.append(negative)
            batch_labels.append(0.0)
        batch_triples = np.vstack(batch_triples)
        batch_labels = np.array(batch_labels)
        return batch_triples, batch_labels
