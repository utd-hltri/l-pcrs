import tensorflow as tf

import data


# taken from https://github.com/fomorians/highway-fcn/blob/master/main.py
def weight_bias(W_shape, b_shape, bias_init=0.1):
  W = tf.Variable(tf.truncated_normal(W_shape, stddev=0.1), name='weight')
  b = tf.Variable(tf.constant(bias_init, shape=b_shape), name='bias')
  return W, b


# taken from https://github.com/fomorians/highway-fcn/blob/master/main.py
def highway_layer(inputs, size, activation, carry_bias=-10.0):
  W, b = weight_bias([size, size], [size])
  
  with tf.name_scope('transform_gate'):
    W_T, b_T = weight_bias([size, size], [size], bias_init=carry_bias)
  
  H = activation(tf.matmul(inputs, W) + b, name='activation')
  T = tf.sigmoid(tf.matmul(inputs, W_T) + b_T, name='transform_gate')
  C = tf.sub(1.0, T, name="carry_gate")   # C = (1 - T)
  
  y = tf.add(tf.mul(H, T), tf.mul(inputs, C), name='y')   # y = (H * T) + (x * C)
  return y


def fully_connected_relu_layer(insize, outsize, name, inputs):
  w = tf.get_variable("%s_w" % name, [insize, outsize])
  b = tf.get_variable("%s_b" % name, [outsize])
  return tf.nn.relu_layer(inputs, w, b)

def fully_connected_relu6_layer(insize, outsize, name, inputs):
  w = tf.get_variable("%s_w" % name, [insize, outsize])
  b = tf.get_variable("%s_b" % name, [outsize])
  return tf.nn.relu6(tf.matmul(inputs, w) + b)

def sigmoid_layer(insize, outsize, name, inputs):
  w = tf.get_variable("%s_w" % name, [insize, outsize])
  b = tf.get_variable("%s_b" % name, [outsize])
  return tf.sigmoid(tf.add(tf.matmul(inputs, w), b))


class SingleAttributeSvmLossModel(object):
  
  def __init__(self, config, is_training, attr, feature_size):
    batch_size = config.batch_size
    embedding_size = config.embedding_size
    
    self._input_data = inputs = tf.placeholder(tf.float32, [batch_size, feature_size], name='inputs')
    labels = tf.placeholder(tf.int32, [batch_size], name=('label-%s' % attr))
    labels = tf.cast(labels, tf.float32)

    # fully connected relu layers
    prev_size = feature_size
    relu_size = config.relu_size
    for i in xrange(config.depth):
      inputs = fully_connected_relu_layer(prev_size, relu_size, "relu%d" % i, inputs)
      prev_size = relu_size

    # multi-purpose embedding
    self._embedding = embedding = fully_connected_relu_layer(prev_size, embedding_size, "embedding", inputs)

    # svm loss
    W, b = weight_bias([embedding_size, 1], [1])
    y_raw = tf.matmul(embedding, W) + b
    predictions = tf.sign(y_raw, name=('predictions-%s' % attr))

    reg_loss = 0.5 * tf.reduce_sum(tf.square(W))
    hinge_loss = tf.reduce_sum(tf.square(tf.maximum(tf.zeros([batch_size,1]), 1 - labels * y_raw)))
    svm_loss = reg_loss + config.svmC*hinge_loss
    tf.identity(svm_loss, name=('smce_loss_%s' % attr))

    losses = svm_loss
    self._cost = cost = tf.reduce_mean(losses, name='loss')
    # back propagation
    if not is_training:
      return
    
    self._lr = tf.Variable(0.0, trainable=False, name="lr")
    self._tvars = tvars = tf.trainable_variables()
    grads, global_norm = tf.clip_by_global_norm(tf.gradients(cost, tvars), config.max_grad_norm, name='clipped_gradients')
    self._grads = grads
    self._global_norm = global_norm
    optimizer = tf.train.GradientDescentOptimizer(self._lr)
    self._train_op = optimizer.apply_gradients(zip(grads, tvars), name='ApplyGradients')
  
  def assign_lr(self, session, lr_value):
    session.run(tf.assign(self._lr, lr_value))
  
  @property
  def loss(self):
    return self._losses
  
  @property
  def cost(self):
    return self._cost
  
  @property
  def train_op(self):
    return self._train_op
  
  @property
  def inputs(self):
    return self._input_data
  
  @property
  def tvars(self):
    return self._tvars[0]
  
  @property
  def grads(self):
    return self._grads[0]

  @property
  def norm(self):
    return self._global_norm
  
  @property
  def embedding(self):
    return self._embedding


class SingleAttributeModel(object):

  def __init__(self, config, is_training, attr, feature_size):
    batch_size = config.batch_size
    embedding_size = data.label_sizes[attr]

    self._input_data = inputs = tf.placeholder(tf.float32, [batch_size, feature_size], name='inputs')
    labels = tf.placeholder(tf.int32, [batch_size, data.label_sizes[attr]], name=('label-%s' % attr))

    # fully connected relu layers
    prev_size = feature_size
    relu_size = config.relu_size
    for i in xrange(config.depth):
      inputs = fully_connected_relu_layer(prev_size, relu_size, "relu%d" % i, inputs)
      prev_size = relu_size

    # multi-purpose embedding
    self._embedding = embedding = fully_connected_relu_layer(prev_size, embedding_size, "embedding", inputs)
    tf.identity(embedding, name=('logits-%s' % attr))

    losses = tf.nn.softmax_cross_entropy_with_logits(embedding, tf.cast(labels, tf.float32), name=('smce_loss_%s' % attr))
    self._cost = cost = tf.reduce_mean(losses, name='loss')
    # back propagation
    if not is_training:
      return

    self._lr = tf.Variable(0.0, trainable=False, name="lr")
    self._tvars = tvars = tf.trainable_variables()
    grads, global_norm = tf.clip_by_global_norm(tf.gradients(cost, tvars), config.max_grad_norm, name='clipped_gradients')
    self._grads = grads
    self._global_norm = global_norm
    optimizer = tf.train.GradientDescentOptimizer(self._lr)
    self._train_op = optimizer.apply_gradients(zip(grads, tvars), name='ApplyGradients')

  def assign_lr(self, session, lr_value):
    session.run(tf.assign(self._lr, lr_value))

  @property
  def loss(self):
    return self._losses

  @property
  def cost(self):
    return self._cost

  @property
  def train_op(self):
    return self._train_op

  @property
  def inputs(self):
    return self._input_data

  @property
  def tvars(self):
    return self._tvars[0]

  @property
  def grads(self):
    return self._grads[0]

  @property
  def norm(self):
    return self._global_norm

  @property
  def embedding(self):
    return self._embedding


class AttributeModel(object):
  
  def __init__(self, config, is_training, attrs, feature_size):
    batch_size = config.batch_size
    embedding_size = config.embedding_size
    
    self._input_data = inputs = tf.placeholder(tf.float32, [batch_size, feature_size], name='inputs')
    labels = {}
    for attr in attrs:
      labels[attr] = tf.placeholder(tf.int32, [batch_size, data.label_sizes[attr]], name=('label-%s' % attr))
    
    # relu layers
    relu_size = config.relu_size
    prev_size = feature_size
    for i in xrange(config.depth):
      inputs = fully_connected_relu_layer(prev_size, relu_size, "relu%d" % i, inputs)
      prev_size = relu_size
    
    # multi-purpose embedding
    self._embedding = embedding = fully_connected_relu_layer(prev_size, embedding_size, "embedding", inputs)
    
    # sigmoid-softmax on embedding for all attributes
    logits_map = {}
    # predictions_list = []
    for attr in attrs:
      logits = sigmoid_layer(embedding_size, data.label_sizes[attr], "sigmoid_%s" % attr, embedding)
      logits_map[attr] = logits
      tf.identity(logits, name=('logits-%s' % attr))
    
    losses = []
    for attr in attrs:
      if attr is 'LOCATION':
        # each location gets its own loss
        losses.extend([tf.reshape(t, [config.batch_size]) for t in
                       tf.split(1, data.label_sizes['LOCATION'],
                                tf.nn.sigmoid_cross_entropy_with_logits(logits_map[attr],
                                                                        tf.cast(labels[attr], tf.float32),
                                                                        name=('smce_loss_%s' % attr)))])
        # average the location losses
        # losses.append(tf.reduce_mean(tf.nn.sigmoid_cross_entropy_with_logits(logits_map[attr],
        #                                                                      tf.cast(labels[attr], tf.float32),
        #                                                                      name=('smce_loss_%s' % attr))))
      else:
        losses.append(tf.nn.softmax_cross_entropy_with_logits(logits_map[attr], tf.cast(labels[attr], tf.float32),
                                                              name=('smce_loss_%s' % attr)))
    self._cost = cost = tf.reduce_mean(losses)
    
    # back propagation
    if not is_training:
      return
    
    self._lr = tf.Variable(0.0, trainable=False, name="lr")
    self._tvars = tvars = tf.trainable_variables()
    grads, global_norm = tf.clip_by_global_norm(tf.gradients(cost, tvars), config.max_grad_norm)
    self._grads = grads
    self._global_norm = global_norm
    optimizer = tf.train.GradientDescentOptimizer(self._lr)
    self._train_op = optimizer.apply_gradients(zip(grads, tvars), name='ApplyGradients')
  
  def assign_lr(self, session, lr_value):
    session.run(tf.assign(self._lr, lr_value))
  
  @property
  def loss(self):
    return self._losses
  
  @property
  def cost(self):
    return self._cost
  
  @property
  def train_op(self):
    return self._train_op
  
  @property
  def inputs(self):
    return self._input_data
  
  @property
  def tvars(self):
    return self._tvars[0]
  
  @property
  def grads(self):
    return self._grads[0]
  
  @property
  def norm(self):
    return self._global_norm
  
  @property
  def embedding(self):
    return self._embedding


class HighwayAttributeModel(object):
  
  def __init__(self, config, is_training, attrs, feature_size):
    batch_size = config.batch_size
    embedding_size = config.embedding_size
    bi = tf.random_normal_initializer(stddev=config.init_scale)
    
    self._input_data = inputs = tf.placeholder(tf.float32, [batch_size, feature_size], name='inputs')
    labels = {}
    for attr in attrs:
      labels[attr] = tf.placeholder(tf.int32, [batch_size, data.label_sizes[attr]], name=('label-%s' % attr))
    
    relu_size = config.relu_size
    # prev_size = math.floor((feature_size + config.relu_size)/2)
    inputs = tf.contrib.layers.fully_connected(inputs, relu_size, biases_initializer=bi, scope='fc1')
    
    # highway layers
    with tf.name_scope('highway'):
      for i in xrange(config.depth):
        inputs = highway_layer(inputs, relu_size, tf.nn.relu, carry_bias=config.carry_bias)
    
    # multi-purpose embedding
    self._embedding = embedding = tf.contrib.layers.fully_connected(inputs, embedding_size, biases_initializer=bi, scope='embedding')
    
    # sigmoid-softmax on embedding for all attributes
    logits_map = {}
    # predictions_list = []
    for attr in attrs:
      logits = tf.contrib.layers.fully_connected(embedding, data.label_sizes[attr], activation_fn=tf.sigmoid,
                                                 biases_initializer=bi, scope=('sigmoid-%s' % attr))
      logits_map[attr] = logits
      tf.identity(logits, name=('logits-%s' % attr))
    
    losses = []
    for attr in attrs:
      if attr is 'LOCATION':
        losses.extend([tf.reshape(t, [config.batch_size]) for t in
                       tf.split(1, data.label_sizes['LOCATION'],
                                tf.nn.sigmoid_cross_entropy_with_logits(logits_map[attr],
                                                                        tf.cast(labels[attr], tf.float32),
                                                                        name=('smce_loss_%s' % attr)))])
      else:
        losses.append(tf.nn.softmax_cross_entropy_with_logits(logits_map[attr], tf.cast(labels[attr], tf.float32),
                                                              name=('smce_loss_%s' % attr)))
    self._cost = cost = tf.reduce_mean(losses)
    
    # back propagation
    if not is_training:
      return
    
    self._lr = tf.Variable(0.0, trainable=False, name="lr")
    self._tvars = tvars = tf.trainable_variables()
    grads, global_norm = tf.clip_by_global_norm(tf.gradients(cost, tvars), config.max_grad_norm)
    self._grads = grads
    self._global_norm = global_norm
    optimizer = tf.train.AdamOptimizer()
    self._train_op = optimizer.apply_gradients(zip(grads, tvars), name='ApplyGradients')
  
  def assign_lr(self, session, lr_value):
    session.run(tf.assign(self._lr, lr_value))
  
  @property
  def loss(self):
    return self._losses
  
  @property
  def cost(self):
    return self._cost
  
  @property
  def train_op(self):
    return self._train_op
  
  @property
  def inputs(self):
    return self._input_data
  
  @property
  def tvars(self):
    return self._tvars[0]
  
  @property
  def grads(self):
    return self._grads[0]
  
  @property
  def norm(self):
    return self._global_norm
  
  @property
  def embedding(self):
    return self._embedding


class HighwayAttributeModelSvmLoss(object):
  def __init__(self, config, is_training, attrs, feature_size):
    batch_size = config.batch_size
    embedding_size = config.embedding_size
    bi = tf.random_normal_initializer(stddev=config.init_scale)

    self._input_data = inputs = tf.placeholder(tf.float32, [batch_size, feature_size], name='inputs')
    labels = {}
    for attr in attrs:
      labels[attr] = tf.placeholder(tf.int32, [batch_size, data.label_sizes[attr]], name=('label-%s' % attr))

    relu_size = config.relu_size
    # prev_size = math.floor((feature_size + config.relu_size)/2)
    inputs = tf.contrib.layers.fully_connected(inputs, relu_size, biases_initializer=bi, scope='fc1')

    # highway layers
    with tf.name_scope('highway'):
      for i in xrange(config.depth):
        inputs = highway_layer(inputs, relu_size, tf.nn.relu, carry_bias=config.carry_bias)

    # multi-purpose embedding
    self._embedding = embedding = tf.contrib.layers.fully_connected(inputs, embedding_size, biases_initializer=bi,
                                                                    scope='embedding')

    # sigmoid-softmax on embedding for all attributes
    logits_map = {}
    # predictions_list = []
    for attr in attrs:
      logits = tf.contrib.layers.fully_connected(embedding, data.label_sizes[attr], activation_fn=tf.sigmoid,
                                                 biases_initializer=bi, scope=('sigmoid-%s' % attr))
      logits_map[attr] = logits
      tf.identity(logits, name=('logits-%s' % attr))

    losses = []
    for attr in attrs:
      losses.append(multi_class_hinge_loss(logits_map[attr], tf.argmax(labels[attr], 1), batch_size,
                                           data.label_sizes[attr], ('smce_loss_%s' % attr)))
    self._cost = cost = tf.reduce_mean(losses)

    # back propagation
    if not is_training:
      return

    self._lr = tf.Variable(0.0, trainable=False, name="lr")
    self._tvars = tvars = tf.trainable_variables()
    grads, global_norm = tf.clip_by_global_norm(tf.gradients(cost, tvars), config.max_grad_norm)
    self._grads = grads
    self._global_norm = global_norm
    optimizer = tf.train.AdamOptimizer()
    self._train_op = optimizer.apply_gradients(zip(grads, tvars), name='ApplyGradients')

  def assign_lr(self, session, lr_value):
    session.run(tf.assign(self._lr, lr_value))

  @property
  def loss(self):
    return self._losses

  @property
  def cost(self):
    return self._cost

  @property
  def train_op(self):
    return self._train_op

  @property
  def inputs(self):
    return self._input_data

  @property
  def tvars(self):
    return self._tvars[0]

  @property
  def grads(self):
    return self._grads[0]

  @property
  def norm(self):
    return self._global_norm

  @property
  def embedding(self):
    return self._embedding


def multi_class_hinge_loss(logits, labels, batch_size, n_classes, name):
  """logits: unscaled scores, tensor, shape=(batch_size, n_classes)
    label: tensor, shape=(batch_size, )
    batch_size, n_classes: int
    from http://stackoverflow.com/questions/36904298/how-to-implement-multi-class-hinge-loss-in-tensorflow
    """
  # get the correct logit
  flat_logits = tf.reshape(logits, (-1,))
  correct_id = tf.cast(tf.range(0, batch_size), tf.int64) * n_classes + labels
  # this is a [batch size] vector of the values of the logit corresponding to the correct classes
  correct_logit = tf.gather(flat_logits, correct_id)

  # get the wrong maximum logit
  max_label = tf.argmax(logits, 1)
  top2, _ = tf.nn.top_k(logits, k=2, sorted=True)
  top2 = tf.split(1, 2, top2)
  for i in xrange(2):
      top2[i] = tf.reshape(top2[i], (batch_size, ))
  wrong_max_logit = tf.select(tf.equal(max_label, labels), top2[1], top2[0])

  # calculate multi-class hinge loss
  return tf.reduce_mean(tf.maximum(0., 1. + wrong_max_logit - correct_logit), name=name)