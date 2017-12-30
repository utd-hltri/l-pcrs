#!/bin/env bash

QUERIES=/home/travis/documents/trec2012/data/2011_topics101-135_v4.xml.gz

if [[ $# != 2 ]]; then
  echo "Usage: $0 RELEVANCE RUNTAG"
  exit $E_BADARGS
fi

MAVEN_OPTS="-ea -Xmx10g" \
  mvn install \
  exec:java \
  -Dexec.mainClass="edu.utdallas.hlt.trecmed.offline.GappyLMTrainer2" \
  -Dexec.args="$QUERIES $*"
