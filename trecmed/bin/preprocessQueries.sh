#!/bin/env bash

if [[ $# != 2 ]]; then
  echo "Usage: $0 QUERIES OUTPUT"
  exit $E_BADARGS
fi

JAVA_OPTS="-ea" target/start edu.utdallas.hlt.trecmed.offline.QueryPreprocessor $*
