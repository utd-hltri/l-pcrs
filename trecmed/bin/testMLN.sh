#!/bin/bash

train=$1
tests=$2

echo "============ TESTING ==========="                                                                                                                                                                                                   
echo "  Input: $train"
echo " Output: $tests"
echo "================================"    i

mkdir -p -v /users/travis/work/sigir14/experiments/$tests

$HOME/downloads/alchemy-2/bin/infer -m \
  -i /users/travis/work/sigir14/experiments/$train/2011_trained.mln \
  -e /users/travis/code/hltri-shared/trecmed/output/$tests/evidence.db \
  -r /users/travis/work/sigir14/experiments/$tests/probabilities.txt \
  -q IsRelevant

echo "============ FINISHED =========="                                                                                                                                                                                                   
echo "  Input: $train"
echo " Output: $tests"
echo "================================"    i

