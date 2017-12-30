#!/bin/env bash

if [[ $# != 4 ]]; then
  echo "Usage: $0 CORPUS_COUNTS_PATH KEYWORDS_PATH COOCCURRENCE_MEASURE_CLASS OUTPUT_PATH"
  exit $E_BADARGS
fi

MAVEN_OPTS=-Xmx4g \
  mvn clean install \
  exec:java \
  -Dexec.mainClass="edu.utdallas.hlt.trecmed.offline.CooccurrenceMeasurer" \
  -Dexec.args="$1 $2 $3 $4"
