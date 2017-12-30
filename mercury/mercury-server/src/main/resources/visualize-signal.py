#!/bin/env python2
import mne
import bz2file
import tempfile
import sys
import mpld3

src_file = sys.argv[1]
out_file = sys.argv[2]

with bz2file.open(src_file, 'rb') as b, tempfile.NamedTemporaryFile() as t:
  t.write(b.read())
  edf = mne.io.read_raw_edf(t.name)
  fig = edf.plot()
  fig.savefig(out_file)

