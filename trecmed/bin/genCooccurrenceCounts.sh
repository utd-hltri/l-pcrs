#!/bin/env bash

if [[ $# != 5 ]]; then
  echo "Usage: $0 CORPUS_PATH CORPUS_STOPWORD_PATH KEYWORDS_PATH GRAM_SIZE OUTPUT_PATH"
  exit $E_BADARGS
fi

MAVEN_OPTS=-Xmx10g \
  mvn clean install \
  exec:java \
  -Dexec.mainClass="edu.utdallas.hlt.trecmed.offline.CooccurrenceCounter" \
  -Dexec.args="generate $1 $2 $3 $4 $5"
