#!/bin/env sh

METAMAP_HOME=/shared/aifiles/disk1/travis/data/metamap_2012/public_mm

killall mmserver12
killall mmserver12.BINARY.Linux
until $(sleep 1 && $METAMAP_HOME/bin/wsdserverctl restart && sleep 1 && $METAMAP_HOME/bin/skrmedpostctl restart && sleep 1 && $METAMAP_HOME/bin/mmserver12); do
  echo "MetaMap crashed with exit code $?." >&2
  echo "Restarting MetaMap..." >&2
  sleep 1
  killall mmserver12
  killall mmserver12.BINARY.Linux
  sleep 1
done
