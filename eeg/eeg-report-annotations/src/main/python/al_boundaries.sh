#!/bin/bash
for i in `seq 0 9`; do
    echo run $i...
    pythontf9 concept/train.py --data_dir=/home/rmm120030/working/eeg/vec/boundary/al/run$i --mode=boundary --max_max_epoch=13 "$@"
    echo done
done
