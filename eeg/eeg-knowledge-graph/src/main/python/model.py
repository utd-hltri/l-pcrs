import tensorflow as tf
import numpy as np
import os
import random
import time


class TransEModel(object):
  def __init__(self, config, is_training):
    self.checkpoint_dir = config.checkpoint_dir
    self.show = config.show
    self.batch_size = config.batch_size
    self.val_batch_size = config.val_batch_size
    self.num_batches = config.num_batches
    self.nepoch = config.nepoch
    self.init = config.init
    self.lr = config.lr
    self.emb_size = config.emb_size
    self.gamma = config.gamma
    self.num_entities = config.num_entities
    self.num_relations = config.num_relations
    self.clip_grads = config.clip_grads
    self.max_grad_norm = config.max_grad_norm
    self.max_entity_norm = config.max_ent_norm
    if config.optimizer == 'adam':
      self.opt = lambda x: tf.train.AdamOptimizer()
    elif config.optimizer == 'gd':
      self.opt = lambda x: tf.train.GradientDescentOptimizer(x)
    else:
      raise ValueError("optimizer must be in [adam, gd]")
    if config.energy == 'l2':
      self.energy_fun = lambda x: tf.sqrt(tf.reduce_sum(tf.square(x[0] + x[1] - x[2]) + 1e-3, reduction_indices=1))
    elif config.optimizer == 'l1':
      self.energy_fun = lambda x: tf.reduce_sum(tf.abs(x[0] + x[1] - x[2]), reduction_indices=1)
    else:
      raise ValueError("energy function must be in [l2, l1]")

    self.rel_emb_matrix = tf.Variable(tf.random_uniform([self.num_relations, self.emb_size],
                                                        minval=-self.init, maxval=self.init), name='R')
    self.ent_emb_matrix = tf.Variable(tf.random_uniform([self.num_entities, self.emb_size],
                                                        minval=-self.init, maxval=self.init), name='E')
    # self.ent_emb_matrix = tf.placeholder(tf.float32, [self.num_entities, self.emb_size])

    if is_training:
      self.true_subj = tf.placeholder(tf.int32, [self.batch_size])
      self.true_obj = tf.placeholder(tf.int32, [self.batch_size])
      self.r = tf.placeholder(tf.int32, [self.batch_size])
      self.false_subj = tf.placeholder(tf.int32, [self.batch_size])
      self.false_obj = tf.placeholder(tf.int32, [self.batch_size])
      self.global_step = tf.Variable(0, name="global_step")

      with tf.device("/cpu:0"):
        true_s = tf.nn.embedding_lookup(self.ent_emb_matrix, self.true_subj)
        true_o = tf.nn.embedding_lookup(self.ent_emb_matrix, self.true_obj)
        r = tf.nn.embedding_lookup(self.rel_emb_matrix, self.r)
        false_s = tf.nn.embedding_lookup(self.ent_emb_matrix, self.false_subj)
        false_o = tf.nn.embedding_lookup(self.ent_emb_matrix, self.false_obj)
      true_energy = self.energy_fun([true_s, r, true_o])
      false_energy = self.energy_fun([false_s, r, false_o])

      self.loss = tf.reduce_sum(tf.maximum(0.0, self.gamma + true_energy - false_energy), name="loss")

      # apply gradients
      params = [self.rel_emb_matrix, self.ent_emb_matrix]
      optimizer = self.opt(self.lr)
      grads_and_vars = optimizer.compute_gradients(self.loss, params)
      if self.clip_grads:
        grads_and_vars = [(tf.clip_by_norm(gv[0], self.max_grad_norm), gv[1]) for gv in grads_and_vars]

      inc = self.global_step.assign_add(1)
      with tf.control_dependencies([inc]):
        self.optim = optimizer.apply_gradients(grads_and_vars)

      # normalize the entity embeddings at each batch (we don't want to backprop through this operation)
      # we need to make sure this happens after the gradients are applied to the entity embeddings
      with tf.control_dependencies([self.optim]):
        indices = tf.unique(tf.concat(0, [self.true_subj, self.true_obj, self.false_subj, self.false_obj]))[0]
        selected_rows = tf.nn.embedding_lookup(self.ent_emb_matrix, indices)
        row_norms = tf.nn.l2_normalize(selected_rows, dim=1)
        scaling = self.max_entity_norm / tf.maximum(row_norms, self.max_entity_norm)
        scaled = selected_rows * scaling
        self.assign = tf.scatter_update(self.ent_emb_matrix, indices, scaled)

          # self.ent_emb_matrix = tf.stop_gradient(tf.nn.l2_normalize(self.ent_emb_matrix, dim=1))
          # self.ent_emb_matrix.assign(tf.nn.l2_normalize(self.ent_emb_matrix, dim=1))
    else:
      self.test_subjects = tf.placeholder(tf.int32, [self.val_batch_size])
      self.test_objects = tf.placeholder(tf.int32, [self.val_batch_size])
      self.test_relations = tf.placeholder(tf.int32, [self.val_batch_size])
      with tf.device("/cpu:0"):
        subject_embeddings = tf.nn.embedding_lookup(self.ent_emb_matrix, self.test_subjects)
        object_embeddings = tf.nn.embedding_lookup(self.ent_emb_matrix, self.test_objects)
        relation_embeddings = tf.nn.embedding_lookup(self.rel_emb_matrix, self.test_relations)

      energies = [self.calc_energies_with_held_out_subjects(r, o) for (o, r) in
                  zip(tf.unpack(object_embeddings), tf.unpack(relation_embeddings))]
      energies.extend([self.calc_energies_with_held_out_objects(r, s) for (s, r) in
                       zip(tf.unpack(subject_embeddings), tf.unpack(relation_embeddings))])
      self.energies = tf.pack(energies)

  def calc_energies_with_held_out_subjects(self, relation, obj):
    return tf.pack(
      [self.energy_fun([subj, relation, obj]) for subj in tf.split(0, self.num_entities, self.ent_emb_matrix)])

  def calc_energies_with_held_out_objects(self, relation, subj):
    return tf.pack(
      [self.energy_fun([subj, relation, obj]) for obj in tf.split(0, self.num_entities, self.ent_emb_matrix)])

  def train_batch(self, data, session):
    true_subj = np.ndarray([self.batch_size], dtype=np.int32)
    true_obj = np.ndarray([self.batch_size], dtype=np.int32)
    rel = np.ndarray([self.batch_size], dtype=np.int32)
    false_subj = np.ndarray([self.batch_size], dtype=np.int32)
    false_obj = np.ndarray([self.batch_size], dtype=np.int32)
    for b in xrange(self.batch_size):
      triple = random.choice(data)
      true_subj[b] = triple[0]
      rel[b] = triple[1]
      true_obj[b] = triple[2]
      if random.random > 0.5:
        false_subj[b] = random.randint(0, self.num_entities)
        false_obj[b] = triple[2]
      else:
        false_subj[b] = triple[0]
        false_obj[b] = random.randint(0, self.num_entities)

    loss, _, _ = session.run([self.loss, self.assign, self.optim], feed_dict={self.true_subj: true_subj,
                                                              self.true_obj: true_obj,
                                                              self.r: rel,
                                                              self.false_subj: false_subj,
                                                              self.false_obj: false_obj})
    return np.sum(loss) / self.batch_size

  def val_batch(self, data, session):
    # for triple in data:
    #   assert triple[0] < self.num_entities, 'triple: %s' % triple
    #   assert triple[2] < self.num_entities, 'triple: %s' % triple
    rankings = []
    subjects = np.zeros([self.val_batch_size], dtype=np.int32)
    relations = np.zeros([self.val_batch_size], dtype=np.int32)
    objects = np.zeros([self.val_batch_size], dtype=np.int32)
    for b, triple in enumerate(data):
      subjects[b] = triple[0]
      relations[b] = triple[1]
      objects[b] = triple[2]
    energy_lists = session.run([self.energies], feed_dict={self.test_subjects: subjects,
                                                           self.test_objects: objects,
                                                           self.test_relations: relations})
    entities = [d[0] for d in data]
    entities.extend([d[2] for d in data])
    rankings.extend([self.get_ranking(e, i) for (e, i) in zip(energy_lists[0], entities)])
    return rankings

  def get_ranking(self, energies, idx):
    if (len(energies.shape) > 2):
      energies = np.squeeze(energies)
    energies_with_idx = list(enumerate(energies))
    sorted_energies = sorted(energies_with_idx, key=lambda tup: tup[1])
    return [x[0] for x in sorted_energies].index(idx)
