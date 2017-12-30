import tensorflow as tf

# common parameters
tf.app.flags.DEFINE_float("learning_rate", 0.9, "Learning rate.")
tf.app.flags.DEFINE_float("learning_rate_decay_factor", 0.95, "Learning rate decays by this much.")
tf.app.flags.DEFINE_float("max_grad_norm", 5.0, "Clip gradients to this norm.")
tf.app.flags.DEFINE_float("init_scale", 0.4,
                          "Initial values for all params will be draw randomly from (-init_scale, init_scale).")
tf.app.flags.DEFINE_float("val_proportion", 0.2, "Proportion of data to hold out for validation.")
tf.app.flags.DEFINE_integer("batch_size", 10, "Batch size to use during training.")
tf.app.flags.DEFINE_integer("steps_per_checkpoint", 2000, "How many training steps to do per checkpoint.")
tf.app.flags.DEFINE_integer("uncommon_feature_threshold", 1, "Ignore features that occur this many times or less.")
tf.app.flags.DEFINE_integer("max_epoch", 5, "???")
tf.app.flags.DEFINE_integer("max_max_epoch", 13, "Number of epochs")
tf.app.flags.DEFINE_integer("feature_size", 2004, "Number of features in feature vectors")

# control options
tf.app.flags.DEFINE_string("model", "deep-relu", "Model type: [highway, single, deep-relu]")
tf.app.flags.DEFINE_string("type", "activity", "Concept Type: {activity, event}")
tf.app.flags.DEFINE_string("data_dir", "/home/rmm120030/working/eeg/vec/tf3", "Data directory")
tf.app.flags.DEFINE_string("data_filename", None, "Data filename (if nonstandard)")
tf.app.flags.DEFINE_string("model_dir_name", "model", "Model directory name")
tf.app.flags.DEFINE_string("model_dir", None, "Full path to model directory")
tf.app.flags.DEFINE_string("reportfile", "/home/rmm120030/working/eeg/vec/tf3/report.csv", "Data directory")
tf.app.flags.DEFINE_string("mode", "train", "Mode: [train, eval, boundary]")
tf.app.flags.DEFINE_string("val_data_dir", None, "Directory to write (1) validation data and (2) labels to.")
tf.app.flags.DEFINE_boolean("verbose", False, "Show detailed results every epoch?")
tf.app.flags.DEFINE_string("checkpoint", "checkpoint", "the checkpoint you want to load for prediction")
tf.app.flags.DEFINE_integer("num_splits", 5, "Number of cv splits")
tf.app.flags.DEFINE_string("pred_file", "predictions.txt", "the file to write predictions to")

# deep relu
tf.app.flags.DEFINE_integer("depth", 5, "Depth of the deep relu attribute network.")
tf.app.flags.DEFINE_integer("relu_size", 1000, "Size of the hidden relus.")
tf.app.flags.DEFINE_integer("embedding_size", 1000, "Size of the multi-purpose embedding.")

# highway
tf.app.flags.DEFINE_float("carry_bias", -1.0,
                          "Carry Bias for Highway layers. Should be in (-inf, 1.0). Lower means more emphasis on carrying.")

# single
tf.app.flags.DEFINE_string("attr", "None", "Attribute type for model=single")

# SVM config
tf.app.flags.DEFINE_integer("label_number", None, "Integer number of the label (for boolean classification).")
tf.app.flags.DEFINE_float("svmC", 1.0, "C parameter for SVM")

# LSTM config
tf.app.flags.DEFINE_integer("num_steps", 40, "Timestep dimension (max sentence length).")
tf.app.flags.DEFINE_integer("hidden_size", 200, "RNN hidden dimension size.")
tf.app.flags.DEFINE_integer("vocab_size", 9009, "Boundary feature size.")
tf.app.flags.DEFINE_integer("labels_size", 2, "Number of labels.")
tf.app.flags.DEFINE_float("keep_prob", 0.9, "Keep probability.")
tf.app.flags.DEFINE_float("val_prob", 0.2, "Proportion of sentences to use for validation")
tf.app.flags.DEFINE_integer("num_layers", 2, "Number of RNN stacks.")

flags = tf.app.flags.FLAGS