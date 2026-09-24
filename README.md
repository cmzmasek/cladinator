# cladinator

Clades within clades of annotated labels: classifies query sequences placed
on an annotated reference tree (for example pplacer output) by the
hierarchical labels of the clades they land in.

Reference leaves carry hierarchical annotations such as `A.1.2` (clade `A`,
sub-clade `A.1`, sub-sub-clade `A.1.2`). Query placements are recognized by
a name pattern, by default the pplacer form `<query>_#<n>_M=<confidence>`.
For each input tree, cladinator reports the most specific clade the query
belongs to, the confidence of that assignment, the bracketing clades, and a
conclusion such as "member of clade A.1" or "potential for novel sub-species
within clade A".

cladinator was previously part of
[forester](https://github.com/cmzmasek/forester) and uses `forester.jar` for
reading and writing trees.

## Requirements

- Java 21 or later
- [Apache Ant](https://ant.apache.org/) to build

## Build

```
ant          # builds dist/cladinator.jar and copies forester.jar next to it
ant test     # runs the test suite
ant clean
```

`dist/cladinator.jar` finds `forester.jar` in the same directory, so keep
the two files together.

## Usage

```
java -jar dist/cladinator.jar [options] <input tree(s) file> [output table file]
```

Options:

| Option | Meaning |
|---|---|
| `-s=<separator>` | annotation separator (default: `.`) |
| `-m=<mapping table>` | map node names to annotations (tab-separated, two columns) |
| `-x` | extra processing of annotations (e.g. `Q16611\|A.1.1` becomes `A.1.1`) |
| `-xs=<separator>` | separator for extra annotations (default: `\|`) |
| `-xk` | keep extra annotations (e.g. `Q16611\|A.1.1` becomes `A.1.1.Q16611`) |
| `-S=<pattern>` | special processing with a pattern (e.g. `(\d+)([a-z]+)_.+` changes `6q_EF42` to `6.q`) |
| `-rs` | remove the annotation separator in the output |
| `--q=<pattern>` | expert option: regular expression for query names (default: `_#\d+_M=(.+)`) |

Examples:

```
java -jar dist/cladinator.jar pp_out_tree.sing.tre result.tsv
java -jar dist/cladinator.jar -s=_ -m=map.tsv pp_out_trees.sing.tre result.tsv
java -jar dist/cladinator.jar -x -xk -m=map.tsv pp_out_trees.sing.tre result.tsv
```

The output table has one row per query and tree, with the columns
`Tree #`, `Query`, `Assignment`, `Confidence`, `Brackets`, `Conclusion` and
`Placement count`.

`cladinator_tree_prepare` is a helper that turns a rooted phyloXML reference
tree, whose leaves carry a `subspecies:clade` property, into a Newick tree
labeled by those annotations:

```
java -cp dist/cladinator.jar org.cladinator.cladinator_tree_prepare <in-tree> <out-tree>
```

## License

Copyright (C) 2026 Christian M. Zmasek

cladinator is free software: you can redistribute it and/or modify it under
the terms of the GNU General Public License as published by the Free
Software Foundation, either version 3 of the License, or (at your option)
any later version (SPDX: `GPL-3.0-or-later`). See [LICENSE](LICENSE).

`lib/forester.jar` is [forester](https://github.com/cmzmasek/forester),
also GPL-3.0-or-later.

## Contact

Christian M. Zmasek, czmasek at jcvi dot org
