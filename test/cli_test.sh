#!/bin/bash
# Command-line regression tests: runs dist/cladinator.jar on each case in test/cli/cases.tsv
# and compares the output table with test/cli/expected/<name>.tsv.
# A case line is: name, tree, options, and optionally the expected exit code (default 0); a -demo case has no tree.
# Usage: test/cli_test.sh            run all cases
#        test/cli_test.sh --update   rewrite the expected tables from the current jar
cd "$(dirname "$0")/.."
JAR=dist/cladinator.jar
TMP=$(mktemp -d)
trap 'rm -rf "$TMP"' EXIT
update=0
[ "$1" = "--update" ] && update=1
failed=0
while IFS= read -r line; do
    case "$line" in ''|'#'*) continue ;; esac
    # cut, not read: read with a tab IFS collapses an empty options field
    name=$(printf '%s\n' "$line" | cut -f1)
    tree=$(printf '%s\n' "$line" | cut -f2)
    opts=$(printf '%s\n' "$line" | cut -f3)
    expected_rc=$(printf '%s\n' "$line" | cut -f4)
    out="$TMP/$name.tsv"
    case " $opts " in
    *" -demo "*)
        # a -demo case takes no tree and compares the demo on stdout (from "Demo:" on), expected in <name>.txt
        # shellcheck disable=SC2086
        java -jar "$JAR" $opts > "$TMP/$name.log" 2>&1
        rc=$?
        sed -n '/^Demo:/,$p' "$TMP/$name.log" > "$out.cmp"
        expected="test/cli/expected/$name.txt" ;;
    *" -dry-run "*)
        # a -dry-run case compares the report on stdout (from "Dry run:" on), expected in <name>.txt
        # shellcheck disable=SC2086
        java -jar "$JAR" $opts "test/data/$tree" > "$TMP/$name.log" 2>&1
        rc=$?
        sed -n '/^Dry run:/,$p' "$TMP/$name.log" > "$out.cmp"
        expected="test/cli/expected/$name.txt" ;;
    *)
        # shellcheck disable=SC2086
        java -jar "$JAR" $opts "test/data/$tree" "$out" > "$TMP/$name.log" 2>&1
        rc=$?
        # the "# cladinator <version> (<date>)" line is left out so that a version bump does not change every table
        grep -v '^# cladinator ' "$out" > "$out.cmp" 2>/dev/null
        expected="test/cli/expected/$name.tsv" ;;
    esac
    if [ "$rc" != "${expected_rc:-0}" ]; then
        echo "FAIL $name: exit code $rc, expected ${expected_rc:-0}"; tail -3 "$TMP/$name.log"; failed=1; continue
    fi
    if [ $update = 1 ]; then
        cp "$out.cmp" "$expected"; echo "updated $name"
    elif ! diff -u "$expected" "$out.cmp"; then
        echo "FAIL $name: output differs"; failed=1
    fi
done < test/cli/cases.tsv
[ $failed = 0 ] && echo "CLI tests OK" || echo "CLI tests NOT OK"
exit $failed
