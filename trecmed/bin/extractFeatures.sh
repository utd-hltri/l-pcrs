#!/bin/env bash

ARGS="$*"
OPTS=$(getopt -q --long runtag: -- "$@")

eval set -- "$OPTS"
while true; do
  case "$1" in
    --runtag) runtag="$2"; shift 2;;
  *) break ;;
  esac
done

MAVEN_OPTS="-ea -server -XX:+UseConcMarkSweepGC -XX:+AggressiveOpts -XX:+UseFastAccessorMethods -XX:+OptimizeStringConcat -Xmx4g" \
  mvn \
  compile \
  exec:java \
  -Druntag="$runtag" \
  -Dexec.mainClass="edu.utdallas.hlt.trecmed.offline.FeatureVectorizer" \
  -Dexec.args="$ARGS"
