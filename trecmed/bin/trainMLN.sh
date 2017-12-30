#!/bin/bash
name=$1

echo "============ TRAINING =========="
echo "  Experiment: $name"
echo "================================"

mkdir -p -v $HOME/work/sigir14/experiments/$name

$HOME/downloads/alchemy-2/bin/learnwts \
  -i $HOME/work/sigir14/quantization_v02/trecv2.mln \
  -o $HOME/work/sigir14/experiments/$name/2011_trained.mln \
  -t $HOME/code/hltri-shared/trecmed/output/$name/evidence.db \
  -ne IsRelevant | tee $HOME/work/sigir14/experiments/$name/training.log

echo "============ FINISHED =========="
echo " Saved: $HOME/work/sigir14/experiments/$name"
echo "================================"
