#!/bin/env bash

QUERIES=/home/travis/documents/trec2012/data/2012_topics136-185_v1.xml.gz
NGD=/home/travis/documents/trec2012/data/2012_2grams_ngd_v1.ser
QRELS=/home/travis/documents/trec2012/official/2012_treceval_qrels.txt
if [[ $# < 1 ]]; then
  echo "Usage: $0 RUNTAG"
  exit $E_BADARGS
fi

JAVA_OPTS="-ea -Xmx10g -Druntag=$1" \
  sbt "run-main edu.utdallas.hlt.trecmed.framework.TREC $QUERIES $* --edu.utdallas.hlt.trecmed.expansion.NGDExpander.PATH=$NGD --edu.utdallas.hlt.trecmed.evaluation.Evaluator.QRELS_PATH=$QRELS"
