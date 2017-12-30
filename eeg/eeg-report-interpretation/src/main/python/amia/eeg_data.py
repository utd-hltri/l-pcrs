import bz2file
import os
import numpy as np

from itertools import ifilter, izip_longest
from eegtools.io import edfplus

def read_bz2_edf(fn, signal_dim):
    # print "Reading EDF %s" % (fn)
    with bz2file.open(fn, 'rb') as b:
        reader = edfplus.BaseEDFReader(b)
        _ = reader.read_header()
        time, signals, events = reader.read_record()
        length = max([len(s) for s in signals])
        if length > 0:
            data = np.zeros([signal_dim, length], dtype=np.float32)
            # print("Length: ", len(signals))
            for i in xrange(signal_dim):
                data[i] = signals[i]
            # print("Data: ", data)
            return data

def edf_path_iterator(path, signal_dim):
    # print "Iterating path"
    for subdir, dirs, files in os.walk(path):
        for filename in ifilter(lambda fn: fn.endswith(".edf.bz2"), files):
            try:
                yield read_bz2_edf(os.path.join(subdir, filename), signal_dim)
            except Exception as e:
                pass

def round_to(n, num):
    resto = n % num
    if (resto <= (num / 2)):
        return n - resto
    else:
        return n + num - resto


def edf_iterator(path, batch_size, num_steps, signal_dim):
    it = edf_path_iterator(path, signal_dim)
    n = 0
    while True:
        n = n + 1
        batch = []
        for _ in xrange(batch_size):
            batch.append(next(it))
        times = max([d.shape[1] for d in batch])
        batch_data = np.zeros([batch_size, times, signal_dim])
        # print(batch_data.shape)
        for i in xrange(batch_size):
            batch_data[i, 0:batch[i].shape[1], :] = np.transpose(batch[i])
        # print(batch_data.shape)
        for i in xrange(0, times - num_steps, num_steps):
            x = batch_data[:, i : i + num_steps, :]
            y = batch_data[:, i + 1 : i + num_steps + 1, :]
            # print "Yielding from %d to %d [max = %d], (x,y) = %s %s" % (i * num_steps, (i + 1) * num_steps, times, x.shape, y.shape)
            yield (x, y, n)
