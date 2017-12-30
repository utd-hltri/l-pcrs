#!/bin/env bash

QUERIES=/home/travis/documents/trec2012/data/2011_topics101-135_v4.xml.gz
NGD=/home/travis/documents/trec2012/data/2011_2grams_ngd_v2.ser
QRELS=/home/travis/documents/trec2012/official/2011_qrels.txt

ARGS="$*"
OPTS=$(getopt -q --long runtag: -- "$@")

eval set -- "$OPTS"
while true; do
  case "$1" in
    --runtag) runtag="$2"; shift 2;;
  *) break ;;
  esac
done

target/start edu.utdallas.hlt.trecmed.framework.TREC \
  $ARGS \
  --edu.utdallas.hlt.trecmed.expansion.NGDExpander.PATH=$NGD \
  --edu.utdallas.hlt.trecmed.evaluation.Evaluator.QRELS_PATH=$QRELS
  -Druntag=$runtag
