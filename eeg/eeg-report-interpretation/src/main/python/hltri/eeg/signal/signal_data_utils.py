import bz2file
import os
import numpy as np
import math

from itertools import ifilter, izip_longest
from eegtools.io import edfplus

def read_bz2_edf(fn, signal_dim):
    # print "Reading EDF %s" % (fn)
    with bz2file.open(fn, 'rb') as b:
        reader = edfplus.BaseEDFReader(b)
        _ = reader.read_header()
        time, signals, events = reader.read_record()
        # print('%d signals' % len(signals))
        length = max([len(s) for s in signals])
        if length > 0:
            data = np.zeros([signal_dim, length], dtype=np.float32)
            # print("Length: ", len(signals))
            for i in xrange(min(len(signals), signal_dim)):
                data[i] = signals[i]
            # print("Data: ", data)
            return data

def edf_path_iterator(path, signal_dim):
    # print "Iterating path"
    for subdir, dirs, files in os.walk(path):
        files.sort()
        signal = None
        for filename in ifilter(lambda fn: fn.endswith(".edf.bz2"), files):
            # print('-handling segment %s/%s' % (subdir, filename))
            try:
                segment = read_bz2_edf(os.path.join(subdir, filename), signal_dim)
            except Exception:
                pass
            signal = segment if signal is None else np.concatenate((signal, segment), axis=1)
        if signal is not None:
            s = os.path.split(subdir)
            sid = s[1].split('_')[0]
            pid = os.path.split(s[0])[1]
            name = '%s_%s' % (pid, sid)
            # print('processing signal ' + name)
            yield signal, name


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
            signal, _ = next(it)
            batch.append(signal)
        # times: maximum signal length among signals in the batch
        signal_lengths = [d.shape[1] for d in batch]
        times = max(signal_lengths)
        batch_data = np.zeros([batch_size, times, signal_dim])
        # print(batch_data.shape)
        for i in xrange(batch_size):
            batch_data[i, 0:batch[i].shape[1], :] = np.transpose(batch[i])
        # print(batch_data.shape)
        for i in xrange(0, times - num_steps, num_steps):
            l = np.zeros([batch_size], dtype=np.int32)
            for b in xrange(batch_size):
                l[b] = max(0, min(batch_size, signal_lengths[b] - (i + num_steps)))
            x = batch_data[:, i : i + num_steps, :]
            y = batch_data[:, i + 1 : i + num_steps + 1, :]
            # print "Yielding from %d to %d [max = %d], (x,y) = %s %s" % (i * num_steps, (i + 1) * num_steps, times, x.shape, y.shape)
            yield (x, y, n, l)


def edf_iterator_with_names(path, batch_size, num_steps, signal_dim):
    assert batch_size == 1, 'Since each batch is of a single signal, batch_size must be 1. This ensures the memory ' \
                            'is properly propagated through the signal, without skipping signal segments. ' \
                            'For efficiency, increase num_steps instead of batch_size.'
    it = edf_path_iterator(path, signal_dim)
    n = 0
    total_batch_size = num_steps
    while True:
        n += 1
        full_signal, name = next(it)
        full_signal = np.transpose(full_signal)
        signal_length = full_signal.shape[0]
        if signal_length > 0:
            num_batches_for_this_signal = int(math.ceil(float(signal_length) / (num_steps)))
            # print('signal length: %s, num batches: %s' % (signal_length, num_batches_for_this_signal))
            batches = []
            lengths = []
            for b in xrange(num_batches_for_this_signal):
                start = b * num_steps
                b_length = min(signal_length - start, total_batch_size)
                lengths.append(b_length)
                # print("start: %s\nss: %s" % (start, b_length))
                x = np.zeros([total_batch_size, signal_dim])
                y = full_signal[start:start + b_length, :]
                x[:b_length,:] = y
                batches.append(np.reshape(x, [batch_size, num_steps, signal_dim]))
            yield(batches, name, n, lengths)
