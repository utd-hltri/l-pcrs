#!/bin/env bash
PROJECT_DIR=$(cd "${BASH_SOURCE[0]%/*}" && pwd -P)/..
MAINCLASS="edu.utdallas.hltri.scribe.brat.BratReader"

if [[ $# != 2 ]]; then
  echo "Usage: $0 BRAT_SOURCE_DIR GATE_INDEX_PATH"
  exit $E_BADARGS
fi

sbt "run-main $MAINCLASS $1 $2"
