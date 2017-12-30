#!/bin/env bash

MAVEN_OPTS="-ea -XX:+UseConcMarkSweepGC -XX:+AggressiveOpts -XX:+UseFastAccessorMethods -Xmx6g" \
  mvn \
  compile \
  exec:java \
  -Druntag="train" \
  -Dexec.mainClass="edu.utdallas.hlt.trecmed.feature.MatlabDBN" \
  -Dexec.args="$*"
