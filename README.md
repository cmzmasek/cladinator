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

The reference tree must be rooted, with an outgroup: which clade lies
"above" a placement depends on the root. Every reference leaf needs a
hierarchical label (a leaf without one, e.g. an outgroup named `OUT`, turns
every clade that contains it into "no common label"). A conclusion of
"member of clade X" needs at least two reference leaves labeled `X` around
the query, so clades represented by a single leaf can only ever give
"potential for novel sub-species" (see the note on single leaves below).

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
| `-sq` | split query names at `_` and print one row per part (e.g. `S1_S2` gives rows for `S1` and `S2`) |
| `-c=<cutoff>` | minimum summed placement confidence for assigning a clade (default: 0.7) |
| `-nh=<factor>` | a query is reported as likely non-homologous when all its placements are at least `<factor>` times as far from the root as the farthest reference leaf (default: 2, `0` turns the check off) |
| `-q=<pattern>` | expert option: regular expression for query names (default: `_#\d+_M=(.+)`) |

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
- An existing output file is not overwritten; the program stops with
  `[...] already exists`.

## Output

The results are printed and, if an output file is given, written to it as a
tab-separated table. It starts with `#` lines recording the program version
and the settings of the run (input file, separator, query pattern, cutoff,
non-homologous factor, and any mapping or processing options), then the
column names, then one row per query and tree:

| Column | Content |
|---|---|
| `Tree #` | number of the tree in the input file |
| `Query` | query name, i.e. the node name before the query pattern (with `-sq`, a name such as `S1_S2` gives one row per part) |
| `Assignment` | the most specific clade whose summed placement confidence reaches the cutoff; empty when there is none |
| `Confidence` | summed placement confidence of the assignment; without one, of what the conclusion is about |
| `Brackets` | the down-tree (sister) and up-tree clades, for a single placement |
| `Conclusion` | see below |
| `Support` | summed placement confidence of the placements that support the conclusion |
| `Placement count` | number of placements of the query in the tree |
| `Pendant length` | the query's branch length, averaged over the placements by confidence (empty without branch lengths) |
| `Reference depth` | distance from the root to the farthest reference leaf (empty without branch lengths) |
| `Clade confidences` | every clade prefix with its summed confidence, e.g. `A:1.0;A.1:0.9;A.2:0.1`; the basis of the assignment |
| `Down-tree confidences` | the same for the down-tree bracketing clades (the query's sister clades) |
| `Up-tree confidences` | the same for the up-tree bracketing clades |
| `Warnings` | problems with the input, and notes on the conclusion (see below) |

Placement confidences (pplacer's likelihood weight ratios, `M=`) are expected
to add up to 1 for a query, and are always rescaled to exactly 1 before the
analysis. Placement programs can drop low-weight placements without rescaling
the rest; if the sum differs from 1 by more than 0.0001, the row says so in
`Warnings`.

### How the conclusion is drawn

Each placement of the query lies in a clade (the label shared by all
reference leaves around the placement edge), sister to the clade below the
edge, with the rest of the clade above it. The summed confidence of a clade
is the confidence of all placements within it.

The **assignment** is the most specific clade whose summed confidence
reaches the cutoff (`-c`, default 0.7): starting from the top-level clade
with the highest confidence, the walk goes down to the best-supported
sub-clade that reaches the cutoff, and stops when two sub-clades tie (noted
in `Warnings`) or none reaches it.

The **conclusion** at the assigned clade X comes from the placements within
X. A placement between labeled sub-clades of X (sister clade and up-tree
clade with different labels, e.g. `A.1` and `A.2`) points to a novel
sub-species within X; a placement among leaves of one label, or within a
sub-clade of X, is that of a member of X. The conclusion is the one with
more confidence, reported in `Support`:

- `member of clade X`
- `potential for novel sub-species within clade X`

If no clade reaches the cutoff but the placements outside all labeled clades
do (e.g. the query attaches next to the root, or sister to a whole clade):

- `outside all clades, sister to clade X`: the sister clade X reaches the cutoff
- `outside all clades`

Otherwise:

- `no confident assignment (best match: clade X 0.6)`, or with a tie
  `no confident assignment (tie: clade A 0.5, clade B 0.5)`

Notes in `Warnings`:

- `sister to a single reference leaf (A.1.1: 0.9): membership in A.1.1 cannot
  be excluded`: a query that belongs to the taxon of a single reference leaf
  is placed sister to that leaf, exactly like a novel lineage would be; the
  topology cannot tell the two apart. `Pendant length` compared with
  `Reference depth` can: a member has a short pendant branch.
- `sub-clades of A tie at the cutoff: A.1 0.5, A.2 0.5`
- `the root has 3 children (unrooted tree?): the up-tree brackets depend on
  the root`

Problems with a tree are reported in its row, and the other trees are
still analyzed:

- `Input error: no query found (query pattern: ...)`: no node matches the
  query pattern
- `Input error: ...`: other problems with the tree, e.g. query nodes with
  different names in one tree, a leaf missing from the mapping file, a
  malformed annotation, or placement confidences that add up to 0
- `Input sequence error: Likely non-homologous query sequence`: every
  placement of the query is at least `-nh` times (default 2) as far from the
  root as the farthest reference leaf (only checked when the tree has branch
  lengths)

After the table, a summary line counts the trees with a result, with a likely
non-homologous query, and with input errors. The exit status is 0 unless the
program could not run at all (bad options, unreadable input, existing output
file) or no tree at all could be analyzed.

## Other tools

`cladinator_tree_prepare` is a helper that turns a rooted phyloXML reference
tree, whose leaves carry a `subspecies:clade` property, into a Newick tree
labeled by those annotations:

```
java -cp dist/cladinator.jar org.cladinator.cladinator_tree_prepare <in-tree> <out-tree>
```

## Changes

**Unreleased**

- Placement confidences that do not add up to 1 are rescaled, with a warning
  in the new `Warnings` column, instead of stopping the run.
- Any problem with one tree (e.g. two different queries in it, a leaf missing
  from the mapping file) is reported in that tree's row; the run continues.
- Query names are no longer split at `_` by default (a pplacer query
  `CED9_CAEBR` now gives one row); `-sq` restores the old behavior, for
  result rows and error rows alike.
- New options `-c` (confidence cutoff, was fixed at 0.7) and `-nh`
  (non-homologous query factor, was fixed at 2; `0` turns the check off).
- The assignment is now the most specific clade that reaches the cutoff
  (0.9 in `A.1` and 0.1 elsewhere in `A` gave `A 1.0`, now `A.1 0.9`).
- A query sister to a whole clade `A` was reported as "within clade A"; it
  is now `outside all clades, sister to clade A`. Several placements between
  the sub-clades of `A` gave "member of clade A" while one gave "potential
  for novel sub-species within clade A"; both now give the latter.
- The `X-like` assignment and the conclusions "similar to clade X" and
  "potential for novel sub-species" (without a clade) are gone; those cases
  are `outside all clades` or `no confident assignment`.
- Ties are named (`no confident assignment (tie: clade A 0.5, clade B 0.5)`)
  instead of resolved alphabetically.
- New columns `Support`, `Pendant length` and `Reference depth`; notes on
  single-leaf sister clades, sub-clade ties, and unrooted trees in
  `Warnings`.
- The clade analysis for a query given by name (`AnalysisSingle`) is gone.
- The table starts with `#` lines recording the version and settings of the
  run, and has three new columns with the full clade, down-tree and up-tree
  confidence distributions.

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
