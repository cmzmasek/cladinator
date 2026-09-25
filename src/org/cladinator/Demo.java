// SPDX-License-Identifier: GPL-3.0-or-later
//
// cladinator -- clades within clades of annotated labels
// Copyright (C) 2026 Christian M. Zmasek
// All rights reserved
//
// This program is free software: you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation, either version 3 of the License, or
// (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program. If not, see <https://www.gnu.org/licenses/>.
//
// Contact: czmasek at jcvi dot org

package org.cladinator;

import java.util.List;

import org.cladinator.Classification.Conclusion;

/**
 * The demo (-demo): synthetic trees with hierarchical clade annotations and a query placed in them, from the
 * plain case to the edge cases, each with the conclusion it is meant to show. cladinator draws and analyzes
 * them; the tests check that they still conclude as meant.
 *
 * <p>Every tree has the same reference (see {@link #REFERENCE}); only the query differs.
 */
public final class Demo {

    /** The reference tree the cases share, described. */
    public static final String REFERENCE = "Every tree has the same reference: clade A with the sub-clades A.1 (leaves"
            + " labeled A.1.1 and A.1.2) and A.2 (A.2.1 and A.2.2), clade B (B.1 and B.2) and clade C, with all"
            + " branches 0.1 long unless a case says otherwise. Most labels have two leaves, standing for the"
            + " sequences of a sub-species. The query Q is placed as pplacer's guppy sing writes it: one leaf per"
            + " placement, named Q_#<placement>_M=<confidence>.";

    /** One case: a tree with the options it needs and the conclusion it is meant to show. */
    public record Case(String title,
                       /** What the case shows, a few sentences. */
                       String shows,
                       String newick,
                       /** The confidence cutoff (-c). */
                       double cutoff,
                       /** The distance threshold (-d), or null. */
                       Double distanceThreshold,
                       /** Whether the labels need the extra processing (-x), with its default separator "|". */
                       boolean extraProcessing,
                       Conclusion expected,
                       /** The clade the expected conclusion names, or null. */
                       String expectedClade) {
    }

    // The reference in pieces, each a balanced subtree with its branch length; a clade is two pieces joined.
    private static final String A_1_1 = "(A.1.1:0.1,A.1.1:0.1):0.1";
    private static final String A_1_2 = "(A.1.2:0.1,A.1.2:0.1):0.1";
    private static final String A_2 = "((A.2.1:0.1,A.2.1:0.1):0.1,A.2.2:0.1):0.1";
    private static final String A_1 = clade(A_1_1, A_1_2);
    private static final String B_C = "(((B.1:0.1,B.1:0.1):0.1,B.2:0.1):0.1,(C:0.1,C:0.1):0.1):0.1";
    private static final double CUTOFF = 0.7;

    private static String clade(final String left, final String right) {
        return "(" + left + "," + right + "):0.1";
    }

    /** The tree with the given A clade next to B and C. */
    private static String tree(final String a) {
        return "(" + a + "," + B_C + ");";
    }

    /** The A.1.1 leaves with a query among them: sister to one A.1.1 leaf, with the other next to them. */
    private static String a11With(final String query, final String pendant) {
        return "(A.1.1:0.1,(A.1.1:0.05," + query + ":" + pendant + "):0.05):0.1";
    }

    /** The A.1.1 leaves with a query next to each. */
    private static String a11WithTwo(final String query1, final String query2) {
        return "((A.1.1:0.05," + query1 + ":0.05):0.05,(A.1.1:0.05," + query2 + ":0.05):0.05):0.1";
    }

    /** The tree of case 1 with a sequence identifier in front of every annotation, as sequences come named. */
    private static final String WITH_IDENTIFIERS = "((((s01|A.1.1:0.1,(s02|A.1.1:0.05,Q_#1_M=1.0:0.05):0.05):0.1,"
            + "(s03|A.1.2:0.1,s04|A.1.2:0.1):0.1):0.1,((s05|A.2.1:0.1,s06|A.2.1:0.1):0.1,s07|A.2.2:0.1):0.1):0.1,"
            + "(((s08|B.1:0.1,s09|B.1:0.1):0.1,s10|B.2:0.1):0.1,(s11|C:0.1,s12|C:0.1):0.1):0.1);";

    /** The cases, from the plain one to the edge cases. */
    public static final List<Case> CASES = List.of(
            new Case("one placement deep inside a sub-clade",
                    "The query is sister to an A.1.1 leaf, with the other A.1.1 leaf next to them, so it is a member"
                            + " of A.1.1. The assignment is the most specific clade whose placements reach the cutoff"
                            + " (0.7): A.1.1, not A.1 or A. The brackets are the sister clade of the placement and"
                            + " the clade on the other side of it.",
                    tree(clade(clade(a11With("Q_#1_M=1.0", "0.05"), A_1_2), A_2)),
                    CUTOFF, null, false, Conclusion.MEMBER, "A.1.1"),
            new Case("one placement sister to a whole sub-clade",
                    "The query branches off between the sub-clades A.1 and A.2: it is within A, but within neither"
                            + " sub-clade, so there is potential for a novel sub-species within A. The brackets"
                            + " [A.1, A.2] say between which clades it lies.",
                    tree(clade("(" + A_1 + ",Q_#1_M=1.0:0.05):0.05", A_2)),
                    CUTOFF, null, false, Conclusion.NOVEL_WITHIN, "A"),
            new Case("one placement sister to a single reference leaf",
                    "The sister of the query is the single A.2.2 leaf, with A.2.1 leaves on the other side."
                            + " Topologically this is a novel sub-species within A.2, but one leaf cannot tell a novel"
                            + " sub-species from a second sequence of A.2.2, so the Warnings column says that"
                            + " membership in A.2.2 cannot be excluded. A distance threshold (-d, case 8) can decide.",
                    tree(clade(A_1, "((A.2.1:0.1,A.2.1:0.1):0.1,(A.2.2:0.05,Q_#1_M=1.0:0.05):0.05):0.1")),
                    CUTOFF, null, false, Conclusion.NOVEL_WITHIN, "A.2"),
            new Case("one placement at the root",
                    "The query branches off at the root, outside every labeled clade: there is no assignment, and"
                            + " the conclusion is outside all clades. (A query outside all clades but next to one"
                            + " clade is reported as sister to that clade.)",
                    "((" + clade(A_1, A_2) + "," + B_C + "):0.1,Q_#1_M=1.0:0.1);",
                    CUTOFF, null, false, Conclusion.OUTSIDE, null),
            new Case("two placements in the same sub-clade",
                    "The two placements lie among the A.1.1 leaves, and their confidences add up (0.6 + 0.4):"
                            + " A.1.1 is assigned with confidence 1.0. Brackets are given for a single placement only.",
                    tree(clade(clade(a11WithTwo("Q_#1_M=0.6", "Q_#2_M=0.4"), A_1_2), A_2)),
                    CUTOFF, null, false, Conclusion.MEMBER, "A.1.1"),
            new Case("placements split between two sub-clades, with -c=0.5",
                    "Half of the confidence lies in A.1.1 and half in A.2.1. With -c=0.5 both A.1 and A.2 reach the"
                            + " cutoff and tie, so the walk down from A stops there: member of A, with the tie noted"
                            + " in the Warnings column. With the default cutoff neither sub-clade would reach it, and"
                            + " the assignment would be A as well.",
                    tree(clade(clade(a11With("Q_#1_M=0.5", "0.05"), A_1_2),
                            "((A.2.1:0.1,(A.2.1:0.05,Q_#2_M=0.5:0.05):0.05):0.1,A.2.2:0.1):0.1")),
                    0.5, null, false, Conclusion.MEMBER, "A"),
            new Case("placements split between two top-level clades",
                    "0.6 of the confidence lies in A and 0.4 in B: no clade reaches the cutoff of 0.7, so there is"
                            + " no assignment, and the best match is reported. The Clade confidences column lists"
                            + " every clade the placements lie in, with the summed confidence.",
                    "(" + clade(clade(a11With("Q_#1_M=0.6", "0.05"), A_1_2), A_2)
                            + ",(((B.1:0.1,(B.1:0.05,Q_#2_M=0.4:0.05):0.05):0.1,B.2:0.1):0.1,(C:0.1,C:0.1):0.1):0.1);",
                    CUTOFF, null, false, Conclusion.NO_CONFIDENT_ASSIGNMENT, null),
            new Case("a long pendant branch, with -d=0.2",
                    "The placement of case 1, but on a pendant branch of 0.3 and with -d=0.2. Topologically the"
                            + " query is a member of A.1.1. By distance, the nearest reference leaf is 0.35 away,"
                            + " farther than the threshold, so the query is a potential novel sub-species within"
                            + " A.1.1, marked (by distance). The Pendant length and Nearest distance columns carry"
                            + " the lengths, which the drawing does not show.",
                    tree(clade(clade(a11With("Q_#1_M=1.0", "0.3"), A_1_2), A_2)),
                    CUTOFF, 0.2, false, Conclusion.NOVEL_WITHIN, "A.1.1"),
            new Case("sequence identifiers in the labels, without -x",
                    "The leaves are named like s01|A.1.1: an identifier, then the annotation. Without -x the whole"
                            + " name is the label, every leaf has a label of its own, and the result is about single"
                            + " leaves. The loud warning in the Warnings column (and at the end of a run, on stderr)"
                            + " says so; -dry-run shows it before a run.",
                    WITH_IDENTIFIERS,
                    CUTOFF, null, false, Conclusion.OUTSIDE_SISTER_TO, "s02|A.1.1"),
            new Case("the same labels, with -x",
                    "-x takes the annotation after the | (the -xs separator) as the label: the same tree now gives"
                            + " a member of A.1.1, as in case 1.",
                    WITH_IDENTIFIERS,
                    CUTOFF, null, true, Conclusion.MEMBER, "A.1.1"),
            new Case("an unrooted tree",
                    "The root has three children, as in a tree built from an unrooted reference. The analysis"
                            + " works, but which clade lies on the far side of a placement depends on where the root"
                            + " is, so the Warnings column notes the rooting.",
                    "(" + clade(a11With("Q_#1_M=1.0", "0.05"), A_1_2) + "," + A_2 + "," + B_C + ");",
                    CUTOFF, null, false, Conclusion.MEMBER, "A.1.1"),
            new Case("confidences that do not add up to 1",
                    "The placement confidences are 0.5 and 0.3. cladinator rescales them to add up to 1 (0.625 and"
                            + " 0.375), says so in the Warnings column, and draws the conclusion from the rescaled"
                            + " values.",
                    tree(clade(clade(a11WithTwo("Q_#1_M=0.5", "Q_#2_M=0.3"), A_1_2), A_2)),
                    CUTOFF, null, false, Conclusion.MEMBER, "A.1.1"));

    /** A conclusion as cladinator words it, e.g. "member of clade A.1.1". */
    public static String describe(final Conclusion conclusion, final String clade) {
        switch (conclusion) {
            case MEMBER:
                return "member of clade " + clade;
            case NOVEL_WITHIN:
                return "potential for novel sub-species within clade " + clade;
            case OUTSIDE_SISTER_TO:
                return "outside all clades, sister to clade " + clade;
            case OUTSIDE:
                return "outside all clades";
            case NO_CONFIDENT_ASSIGNMENT:
            default:
                return "no confident assignment";
        }
    }

    private Demo() {
    }
}
