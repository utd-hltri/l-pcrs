#!/bin/env bash

if [[ $# != 3 ]]; then
  echo "Usage: $0 CORPUS_PATH STOPWORD_COUNT OUTPUT_PATH"
  exit $E_BADARGS
fi

MAVEN_OPTS=-Xmx10g \
  mvn clean install \
  exec:java \
  -Dexec.mainClass="edu.utdallas.hlt.trecmed.offline.CooccurrenceCounter" \
  -Dexec.args="stopwords $1 $2 $3"
