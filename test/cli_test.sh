#!/bin/bash
# Command-line regression tests: runs dist/cladinator.jar on each case in test/cli/cases.tsv
# and compares the output table with test/cli/expected/<name>.tsv.
# Usage: test/cli_test.sh            run all cases
#        test/cli_test.sh --update   rewrite the expected tables from the current jar
cd "$(dirname "$0")/.."
JAR=dist/cladinator.jar
TMP=$(mktemp -d)
trap 'rm -rf "$TMP"' EXIT
update=0
[ "$1" = "--update" ] && update=1
failed=0
while IFS=$'\t' read -r name tree opts; do
    case "$name" in ''|'#'*) continue ;; esac
    out="$TMP/$name.tsv"
    # shellcheck disable=SC2086
    java -jar "$JAR" $opts "test/data/$tree" "$out" > "$TMP/$name.log" 2>&1
    rc=$?
    if [ $rc != 0 ]; then
        echo "FAIL $name: exit code $rc"; tail -3 "$TMP/$name.log"; failed=1; continue
    fi
    # the "# cladinator <version> (<date>)" line is left out so that a version bump does not change every table
    grep -v '^# cladinator ' "$out" > "$out.cmp"
    if [ $update = 1 ]; then
        cp "$out.cmp" "test/cli/expected/$name.tsv"; echo "updated $name"
    elif ! diff -u "test/cli/expected/$name.tsv" "$out.cmp"; then
        echo "FAIL $name: output differs"; failed=1
    fi
done < test/cli/cases.tsv
[ $failed = 0 ] && echo "CLI tests OK" || echo "CLI tests NOT OK"
exit $failed
