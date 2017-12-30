#!/bin/env bash

QUERIES=/home/travis/documents/trec2012/data/2011_topics101-135_v4.xml.gz

if [[ $# < 1 ]]; then
  echo "Usage: $0 OUTPUT"
  exit $E_BADARGS
fi

MAVEN_OPTS="-ea -Xmx10g" \
  mvn clean install \
  exec:java \
  -Dexec.mainClass="edu.utdallas.hlt.trecmed.offline.AssertionTrainer" \
  -Dexec.args="$*"
