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
| `-rs` | remove the annotation separator from clade names in the output (e.g. `A.1.2` becomes `A12`) |
| `--q=<pattern>` | expert option: regular expression for query names (default: `_#\d+_M=(.+)`) |

Examples:

```
java -jar dist/cladinator.jar pp_out_tree.sing.tre result.tsv
java -jar dist/cladinator.jar -s=_ -m=map.tsv pp_out_trees.sing.tre result.tsv
java -jar dist/cladinator.jar -x -xk -m=map.tsv pp_out_trees.sing.tre result.tsv
```

Notes on options:

- The separator must not be a character with a meaning in Newick (`:`,
  `,`, `;`, `(`, `)`, `[`, `]`), because the tree cannot be read then.
- `-rs` only changes how names are printed, not the analysis. It can make
  different clades look the same: `A.1.1` and `A.11` both print as `A11`.

## Output

The results are printed and, if an output file is given, written to it as a
tab-separated table with one row per query and tree:

| Column | Content |
|---|---|
| `Tree #` | number of the tree in the input file |
| `Query` | query name (a name containing `_`, such as `QX_QY`, gives one row per part) |
| `Assignment` | the clade assigned; `X-like` when only one of the bracketing clades is known; empty when there is no assignment |
| `Confidence` | summed placement confidence of the assignment (or of the best match) |
| `Brackets` | the down- and up-tree bracketing clades, for a single placement |
| `Conclusion` | see below |
| `Placement count` | number of placements of the query in the tree |

A clade is assigned when it reaches a summed confidence of at least 0.7.
Possible conclusions:

- `member of clade X`
- `potential for novel sub-species within clade X`
- `potential for novel sub-species similar to clade X`
- `potential for novel sub-species different from all current sub-species`
- `potential for novel sub-species` (e.g. all placements on the root)
- `no confident assignment (best match: clade X)`: no clade reaches 0.7

Problems with a tree are reported in its row, and the other trees are
still analyzed:

- `Input error: no query found (query pattern: ...)`: no node matches the
  query pattern
- `Input sequence error: Likely non-homologous query sequence`: every
  placement of the query is at least twice as far from the root as the
  farthest reference leaf (only checked when the tree has branch lengths)

## Other tools

`cladinator_tree_prepare` is a helper that turns a rooted phyloXML reference
tree, whose leaves carry a `subspecies:clade` property, into a Newick tree
labeled by those annotations:

```
java -cp dist/cladinator.jar org.cladinator.cladinator_tree_prepare <in-tree> <out-tree>
```

## Changes

**3.1.0** (2026-09-24)

- A tree without a query no longer stops the run; it gets an
  `Input error: no query found` row.
- A placement on the root next to other placements no longer stops the run
  with "confidences add up to ... instead of 1.0".
- When no clade reaches the 0.7 cutoff, the conclusion is now
  `no confident assignment (best match: clade X)` instead of
  `member of clade X`.
- `-s` with a separator other than `.` is now used throughout the analysis
  (it gave wrong conclusions before), and `-s=|` works without `-x`.
- Trees with 32 or more distinct clade prefixes are now ranked correctly.
- `-rs` now works.
- Trees without branch lengths are no longer reported as non-homologous.
- No blank line after each tree's rows in the output table.

**3.0.2** (2026-04-01): last version as part of
[forester](https://github.com/cmzmasek/forester).

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
