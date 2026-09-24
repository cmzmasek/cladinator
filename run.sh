#!/bin/bash
# Usage: ./run.sh <tree> [extra cladinator options...]
# Runs dist/cladinator.jar (build with "ant" first); writes out/<name>.tsv and out/<name>.log
cd "$(dirname "$0")"
mkdir -p out
t=$1; shift
n=$(basename "$t" .nwk)
rm -f out/$n.tsv
java -jar dist/cladinator.jar "$@" "$t" out/$n.tsv > out/$n.log 2>&1
echo "=== $n (exit $?) $*"
