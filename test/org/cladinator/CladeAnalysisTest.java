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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.util.List;
import java.util.regex.Pattern;

import org.forester.io.parsers.PhylogenyParser;
import org.forester.io.parsers.nhx.NHXParser;
import org.forester.io.parsers.util.ParserUtils;
import org.forester.phylogeny.Phylogeny;
import org.forester.phylogeny.factories.ParserBasedPhylogenyFactory;
import org.forester.phylogeny.factories.PhylogenyFactory;
import org.forester.util.UserException;
import org.junit.jupiter.api.Test;

import org.cladinator.Classification.Conclusion;

class CladeAnalysisTest {

    private static final PhylogenyFactory FACTORY = ParserBasedPhylogenyFactory.getInstance();
    private static final String TEST_DATA = "test" + File.separator + "data" + File.separator;
    private static final double DELTA = 1E-9;
    private static final Pattern QUERY = AnalysisMulti.DEFAULT_QUERY_PATTERN_FOR_PPLACER_TYPE;

    private static Phylogeny tree(final String newick) throws Exception {
        return FACTORY.create(newick, new NHXParser())[0];
    }

    private static ResultMulti analyze(final String newick) throws Exception {
        return AnalysisMulti.execute(tree(newick), ".");
    }

    private static Classification classify(final String newick, final double cutoff) throws Exception {
        return Classification.of(analyze(newick), cutoff);
    }

    /** Asserts a prefix list: prefix, confidence, prefix, confidence, ... */
    private static void assertPrefixes(final List<Prefix> actual, final Object... prefix_and_confidence) {
        assertEquals(prefix_and_confidence.length / 2, actual.size(), "number of prefixes in " + actual);
        for (int i = 0; i < actual.size(); ++i) {
            assertEquals(prefix_and_confidence[2 * i], actual.get(i).getPrefix(), "prefix " + i + " of " + actual);
            assertEquals((Double) prefix_and_confidence[2 * i + 1], actual.get(i).getConfidence(), 1E-4,
                    "confidence of " + actual.get(i));
        }
    }

    private static ResultMulti withPrefixes(final String separator, final Object... prefix_and_confidence) {
        final ResultMulti res = new ResultMulti(separator);
        for (int i = 0; i < prefix_and_confidence.length; i += 2) {
            res.addGreatestCommonPrefix((String) prefix_and_confidence[i], (Double) prefix_and_confidence[i + 1]);
        }
        res.analyze();
        return res;
    }

    // ---- ResultMulti: prefixes are summed over their levels, sorted, and collapsed to one per top-level clade

    @Test
    void collapsedPrefixes() {
        assertPrefixes(withPrefixes(".", "A.1.1", 0.3, "A.1.2", 0.3, "A.1.3", 0.3, "B.1", 0.1).getCollapsedMultiHitPrefixes(),
                "A.1", 0.9, "B.1", 0.1);
        assertPrefixes(withPrefixes(".", "A.1.1.1", 0.1, "A.1", 0.7, "A.1.2", 0.1, "B.1", 0.1).getCollapsedMultiHitPrefixes(),
                "A.1", 0.9, "B.1", 0.1);
        assertPrefixes(withPrefixes(".", "A.1.1.1", 0.1, "A.1.1.1.1", 0.6, "A.1", 0.1, "A.1.2", 0.1, "B.1", 0.1).getCollapsedMultiHitPrefixes(),
                "A.1", 0.9, "B.1", 0.1);
        assertPrefixes(withPrefixes(".", "A.1.1.1", 0.1, "A.1.1.1.1", 0.3, "A.1", 0.1, "A.1.2", 0.1, "B.1", 0.1, "B.1.1.1", 0.3).getCollapsedMultiHitPrefixes(),
                "A.1", 0.6, "B.1", 0.4);
        assertPrefixes(withPrefixes(".", "A.1.1.1.1", 0.35, "A.1.1.1.2", 0.35, "A.1", 0.1, "A.1.2", 0.1, "B.1", 0.1).getCollapsedMultiHitPrefixes(),
                "A.1", 0.9, "B.1", 0.1);
        assertPrefixes(withPrefixes(".", "A.1.1.1.1", 0.2, "C.2.3", 0.2, "A.1.5", 0.1, "A.3.1.4", 0.2, "B.1.1", 0.2, "B.1.2", 0.09, "D.1.1.1.1", 0.01).getCollapsedMultiHitPrefixes(),
                "A", 0.5, "B.1", 0.29, "C.2.3", 0.2, "D.1.1.1.1", 0.01);
        assertPrefixes(withPrefixes(".", "A.1.1.1", 0.05, "A.1.1.1.1", 0.65, "A.1", 0.1, "A.1.2", 0.1, "B.1", 0.1).getCollapsedMultiHitPrefixes(),
                "A.1", 0.9, "B.1", 0.1);
        assertPrefixes(withPrefixes(".", "A.1.1.1", 0.07, "A.1.1.1.1", 0.9, "A.1", 0.01, "A.1.2", 0.01, "B.1", 0.01).getCollapsedMultiHitPrefixes(),
                "A.1", 0.99, "B.1", 0.01);
    }

    @Test
    void collapsedPrefixesWithMultiCharacterSeparator() {
        assertPrefixes(withPrefixes("_/_", "AA_/_abc_/_def", 0.07, "AA_/_abc_/_sfc", 0.9, "AA_/_abc_/_xcd", 0.01,
                "AA_/_abc_/_memr", 0.01, "AA_/_abc_/_fkem_/_odem", 0.01).getCollapsedMultiHitPrefixes(),
                "AA_/_abc", 1.0);
        assertPrefixes(withPrefixes("_/_", "AA_/_abc_/_def", 0.07, "AA_/_abc_/_sfc", 0.6, "AA_/_abc_/_xcd", 0.01,
                "AA_/_abc_/_memr", 0.01, "AA_/_abc_/_fkem_/_odem", 0.01, "BB_/_fke_/_dme_/_nx2", 0.3).getCollapsedMultiHitPrefixes(),
                "AA_/_abc", 0.7, "BB_/_fke_/_dme_/_nx2", 0.3);
    }

    // ---- AnalysisMulti on trees

    @Test
    void realPplacerOutput() throws Exception {
        final File file = new File(TEST_DATA + "pplacer_2.tre");
        final PhylogenyParser parser = ParserUtils.createParserDependingOnFileType(file, true);
        final ResultMulti res = AnalysisMulti.execute(FACTORY.create(file, parser)[0]);
        assertEquals("CED9_CAEBR", res.getQueryNamePrefix());
        assertEquals(7, res.getNumberOfMatches());
        assertEquals(14, res.getReferenceTreeNumberOfExternalNodes());
        assertPrefixes(res.getCollapsedMultiHitPrefixes(), "A", 0.7707, "?", 0.2293);
        assertPrefixes(res.getCollapsedMultiHitPrefixesDown(), "A", 1.0);
        assertPrefixes(res.getCollapsedMultiHitPrefixesUp(), "A", 0.7707, "C.5", 0.2293);
    }

    @Test
    void singlePlacementWithinSubclade() throws Exception {
        final ResultMulti res = analyze("(((((A.1.1,Q_#1_M=1),A.1.2),(A.2.1,A.2.2)),((A.3.1,A.3.2),(A.4.1,A.4.2))),(((B.1,B.2),B.3),(C.1,C.2)))");
        assertEquals("Q", res.getQueryNamePrefix());
        assertEquals(1, res.getNumberOfMatches());
        assertEquals(13, res.getReferenceTreeNumberOfExternalNodes());
        assertPrefixes(res.getCollapsedMultiHitPrefixes(), "A.1", 1.0);
        assertPrefixes(res.getCollapsedMultiHitPrefixesDown(), "A.1.1", 1.0);
        assertPrefixes(res.getCollapsedMultiHitPrefixesUp(), "A.1.2", 1.0);
    }

    @Test
    void twoPlacementsInSisterSubclades() throws Exception {
        final ResultMulti res = analyze("(((((A.1.1,A.1.2),Q_#0_M=0.5),((A.2.1,A.2.2),Q_#1_M=0.5)),((A.3.1,A.3.2),(A.4.1,A.4.2))),(((B.1,B.2),B.3),(C.1,C.2)))");
        assertEquals(2, res.getNumberOfMatches());
        assertPrefixes(res.getCollapsedMultiHitPrefixes(), "A", 1.0);
        assertPrefixes(res.getAllMultiHitPrefixesDown(), "A", 1.0, "A.1", 0.5, "A.2", 0.5);
        assertPrefixes(res.getAllMultiHitPrefixesUp(), "A", 1.0, "A.1", 0.5, "A.2", 0.5);
    }

    @Test
    void separatorIsUsedThroughout() throws Exception {
        final ResultMulti dot = analyze("((((A.1.1,A.1.2),Q_#1_M=0.6),((A.2.1,A.2.2),Q_#2_M=0.4)),((B.1.1,B.1.2),B.2.1))");
        final ResultMulti us = AnalysisMulti.execute(tree("((((A_1_1,A_1_2),Q_#1_M=0.6),((A_2_1,A_2_2),Q_#2_M=0.4)),((B_1_1,B_1_2),B_2_1))"), "_");
        assertPrefixes(dot.getAllMultiHitPrefixesDown(), "A", 1.0, "A.1", 0.6, "A.2", 0.4);
        assertPrefixes(us.getAllMultiHitPrefixesDown(), "A", 1.0, "A_1", 0.6, "A_2", 0.4);
        assertPrefixes(us.getAllMultiHitPrefixesUp(), "A", 1.0, "A_2", 0.6, "A_1", 0.4);
        assertPrefixes(us.getCollapsedMultiHitPrefixes(), "A", 1.0);
    }

    @Test
    void manyPrefixesAreSortedByConfidence() throws Exception {
        // 40 placements next to 40 clades: the sort misbehaved from 32 elements on
        final int n = 40;
        final double total = n * (n + 1) / 2.0;
        final StringBuilder sb = new StringBuilder("(");
        for (int k = 1; k <= n; ++k) {
            sb.append(k > 1 ? "," : "").append("(((X").append(k).append(".1.1,X").append(k).append(".1.2),Q_#")
                    .append(k).append("_M=").append(k / total).append("),(X").append(k).append(".2.1,X").append(k).append(".2.2))");
        }
        final ResultMulti res = analyze(sb.append(")").toString());
        assertEquals(n, res.getAllMultiHitPrefixes().size());
        assertEquals("X" + n, res.getAllMultiHitPrefixes().get(0).getPrefix());
        assertEquals("X" + n + ".1", res.getAllMultiHitPrefixesDown().get(0).getPrefix());
        assertEquals("X" + n + ".2", res.getAllMultiHitPrefixesUp().get(0).getPrefix());
        for (final List<Prefix> l : List.of(res.getAllMultiHitPrefixes(), res.getCollapsedMultiHitPrefixes(),
                res.getAllMultiHitPrefixesDown(), res.getCollapsedMultiHitPrefixesDown(),
                res.getAllMultiHitPrefixesUp(), res.getCollapsedMultiHitPrefixesUp())) {
            for (int i = 1; i < l.size(); ++i) {
                assertTrue(l.get(i).getConfidence() <= l.get(i - 1).getConfidence(), "not sorted: " + l);
            }
        }
    }

    @Test
    void placementOnTheRootCountsAsUnknownInAllLists() throws Exception {
        final ResultMulti res = analyze("(Q_#1_M=0.3,(((A.1.1,Q_#2_M=0.7),A.1.2),(A.2.1,A.2.2)),((B.1.1,B.1.2),B.2.1))");
        assertPrefixes(res.getAllMultiHitPrefixes(), "A.1", 0.7, "?", 0.3);
        assertPrefixes(res.getAllMultiHitPrefixesDown(), "A.1.1", 0.7, "?", 0.3);
        assertPrefixes(res.getAllMultiHitPrefixesUp(), "A.1.2", 0.7, "?", 0.3);
        final ResultMulti all_root = analyze("(Q_#1_M=1.0,((A.1.1,A.1.2),A.2.1),((B.1.1,B.1.2),B.2.1))");
        assertPrefixes(all_root.getAllMultiHitPrefixes(), "?", 1.0);
        assertPrefixes(all_root.getAllMultiHitPrefixesDown(), "?", 1.0);
        assertPrefixes(all_root.getAllMultiHitPrefixesUp(), "?", 1.0);
    }

    @Test
    void likelyProblematicQueryNeedsBranchLengths() throws Exception {
        assertFalse(AnalysisMulti.likelyProblematicQuery(tree("((((A.1.1:0.1,A.1.2:0.1):0.1,Q_#1_M=1.0:0.1):0.1,(A.2.1:0.1,A.2.2:0.1):0.1):0.1,B.1:0.3)"), QUERY, 2));
        assertTrue(AnalysisMulti.likelyProblematicQuery(tree("((((A.1.1:0.1,A.1.2:0.1):0.1,Q_#1_M=1.0:5.0):0.1,(A.2.1:0.1,A.2.2:0.1):0.1):0.1,B.1:0.3)"), QUERY, 2));
        assertFalse(AnalysisMulti.likelyProblematicQuery(tree("((((A.1.1,A.1.2),Q_#1_M=1.0),(A.2.1,A.2.2)),B.1)"), QUERY, 2), "no branch lengths");
        assertFalse(AnalysisMulti.likelyProblematicQuery(tree("(Q_#1_M=0.5:0.1,Q_#2_M=0.5:0.1)"), QUERY, 2), "no reference leaves");
    }

    @Test
    void confidencesAreRescaledToOne() throws Exception {
        final ResultMulti res = analyze("((((A.1.1,A.1.2),Q_#1_M=0.95),(A.2.1,A.2.2)),((B.1.1,Q_#2_M=0.04),B.2.1))");
        assertPrefixes(res.getAllMultiHitPrefixes(), "A", 0.95 / 0.99, "B", 0.04 / 0.99);
        assertEquals(1, res.getWarnings().size());
        assertTrue(res.getWarnings().get(0).contains("0.99"), res.getWarnings().get(0));
        assertTrue(analyze("((((A.1.1,A.1.2),Q_#1_M=0.96),(A.2.1,A.2.2)),((B.1.1,Q_#2_M=0.04),B.2.1))").getWarnings().isEmpty());
        // within tolerance (real pplacer output is printed with ~6 digits): no warning
        assertTrue(analyze("((((A.1.1,A.1.2),Q_#1_M=0.959999),(A.2.1,A.2.2)),((B.1.1,Q_#2_M=0.04),B.2.1))").getWarnings().isEmpty());
    }

    @Test
    void invalidConfidencesAreInputErrors() {
        for (final String bad : new String[]{"0", "NaN", "Infinity", "-0.5", "abc"}) {
            final String t = "((((A.1.1,A.1.2),Q_#1_M=" + bad + "),(A.2.1,A.2.2)),((B.1.1,Q_#2_M=0),B.2.1))";
            final UserException e = assertThrows(UserException.class, () -> analyze(t), bad);
            assertFalse(e.getMessage().startsWith("ERROR"), e.getMessage());
        }
    }

    @Test
    void unrootedTreeIsNoted() throws Exception {
        assertTrue(analyze("(Q_#1_M=1.0,((A.1.1,A.1.2),A.2.1),((B.1.1,B.1.2),B.2.1))").getWarnings().get(0).contains("3 children"));
        assertTrue(analyze("((((A.1.1,A.1.2),Q_#1_M=1.0),(A.2.1,A.2.2)),((B.1.1,B.1.2),B.2.1))").getWarnings().isEmpty());
    }

    @Test
    void leafWithUniqueTopLevelLabelIsNoted() throws Exception {
        // OUTGROUP inside the clade of A.2.1: every clade containing it has no common label
        final ResultMulti res = analyze("((((A.1.1,A.1.2),Q_#1_M=1.0),(A.2.1,OUTGROUP)),((B.1.1,B.1.2),B.2.1))");
        assertEquals(1, res.getWarnings().size());
        assertTrue(res.getWarnings().get(0).contains("\"OUTGROUP\""), res.getWarnings().get(0));
        // an outgroup attached to the root is fine
        assertTrue(analyze("((((A.1.1,A.1.2),(A.2.1,A.2.2)),Q_#1_M=1.0),OUT.1)").getWarnings().isEmpty());
        // a one-level label shared by two leaves is fine
        assertTrue(analyze("((((A.1.1,A.1.2),Q_#1_M=1.0),(C,C)),((B.1.1,B.1.2),B.2.1))").getWarnings().isEmpty());
    }

    // ---- Classification by topology

    @Test
    void assignmentIsTheMostSpecificCladeReachingTheCutoff() throws Exception {
        // 0.9 within A.1 (sister to the single leaf A.1.1), 0.1 elsewhere in A
        final Classification c = classify("(((((A.1.1,Q_#1_M=0.9),A.1.2),(A.1.3,A.1.4)),((A.2.1,A.2.2),Q_#2_M=0.1)),((B.1.1,B.1.2),B.2.1))", 0.7);
        assertEquals("A.1", c.getAssignment());
        assertEquals(0.9, c.getConfidence(), DELTA);
        assertEquals(Conclusion.NOVEL_WITHIN, c.getConclusion());
        assertEquals(0.9, c.getSupport(), DELTA);
        assertNull(c.getBracketDown());
        assertEquals(1, c.getSingleLeafSisters().size());
        assertEquals(0.9, c.getSingleLeafSisters().get("A.1.1"), DELTA);
    }

    @Test
    void sisterToAWholeCladeIsOutsideAllClades() throws Exception {
        final Classification c = classify("((((A.1.1,A.1.2),(A.2.1,A.2.2)),Q_#1_M=1.0),OUT.1)", 0.7);
        assertNull(c.getAssignment());
        assertEquals(Conclusion.OUTSIDE_SISTER_TO, c.getConclusion());
        assertEquals("A", c.getConclusionClade());
        assertEquals(1.0, c.getSupport(), DELTA);
        assertEquals("A", c.getBracketDown());
        assertEquals("OUT.1", c.getBracketUp());
    }

    @Test
    void betweenSubcladesIsNovelWithin() throws Exception {
        final Classification single = classify("((((A.1.1,A.1.2),Q_#1_M=1.0),(A.2.1,A.2.2)),((B.1.1,B.1.2),B.2.1))", 0.7);
        assertEquals("A", single.getAssignment());
        assertEquals(Conclusion.NOVEL_WITHIN, single.getConclusion());
        assertEquals("A.1", single.getBracketDown());
        assertEquals("A.2", single.getBracketUp());
        assertTrue(single.getSingleLeafSisters().isEmpty(), "the sister clade has two leaves");
        // 0.6 sister to A.1 and 0.4 sister to A.2: the same conclusion, support 1.0
        final Classification two = classify("((((A.1.1,A.1.2),Q_#1_M=0.6),((A.2.1,A.2.2),Q_#2_M=0.4)),((B.1.1,B.1.2),B.2.1))", 0.7);
        assertEquals("A", two.getAssignment());
        assertEquals(Conclusion.NOVEL_WITHIN, two.getConclusion());
        assertEquals(1.0, two.getSupport(), DELTA);
        assertNull(two.getBracketDown());
    }

    @Test
    void amongLeavesOfOneLabelIsMember() throws Exception {
        final Classification c = classify("((((A.1.1,Q_#1_M=1.0),A.1.1),(A.2.1,A.2.2)),B.1)", 0.7);
        assertEquals("A.1.1", c.getAssignment());
        assertEquals(Conclusion.MEMBER, c.getConclusion());
        assertEquals(1.0, c.getSupport(), DELTA);
    }

    @Test
    void topLevelTieIsNoConfidentAssignment() throws Exception {
        final Classification c = classify("((((A.1.1,A.1.2),Q_#1_M=0.5),(A.2.1,A.2.2)),(((B.1.1,B.1.2),Q_#2_M=0.5),(B.2.1,B.2.2)))", 0.7);
        assertNull(c.getAssignment());
        assertEquals(Conclusion.NO_CONFIDENT_ASSIGNMENT, c.getConclusion());
        assertEquals(2, c.getBestMatches().size());
        assertEquals("A", c.getBestMatches().get(0).getPrefix());
        assertEquals("B", c.getBestMatches().get(1).getPrefix());
    }

    @Test
    void subcladeTieStopsTheWalk() throws Exception {
        final String t = "(((((A.1.1,Q_#1_M=0.5),A.1.2),((A.2.1,Q_#2_M=0.5),A.2.2)),(A.3.1,A.3.2)),((B.1.1,B.1.2),B.2.1))";
        final Classification c = classify(t, 0.5);
        assertEquals("A", c.getAssignment());
        assertEquals(Conclusion.MEMBER, c.getConclusion(), "both placements lie within sub-clades of A");
        assertEquals(1.0, c.getSupport(), DELTA);
        assertEquals(2, c.getCompetingSubclades().size());
        assertEquals("A.1", c.getCompetingSubclades().get(0).getPrefix());
        final Classification at_07 = classify(t, 0.7);
        assertEquals("A", at_07.getAssignment());
        assertTrue(at_07.getCompetingSubclades().isEmpty());
    }

    @Test
    void everythingOnTheRootIsOutsideAllClades() throws Exception {
        final Classification c = classify("(Q_#1_M=1.0,((A.1.1,A.1.2),A.2.1),((B.1.1,B.1.2),B.2.1))", 0.7);
        assertNull(c.getAssignment());
        assertEquals(Conclusion.OUTSIDE, c.getConclusion());
        assertEquals(1.0, c.getConfidence(), DELTA);
    }

    @Test
    void branchLengthsGivePendantLengthAndReferenceDepth() throws Exception {
        final ResultMulti res = analyze("((((A.1.1:0.1,A.1.2:0.1):0.1,Q_#1_M=1.0:0.25):0.1,(A.2.1:0.1,A.2.2:0.1):0.1):0.1,((B.1.1:0.1,B.1.2:0.1):0.1,B.2.1:0.1):0.1)");
        assertEquals(0.25, Classification.of(res, 0.7).getPendantLength(), DELTA);
        assertEquals(0.4, res.getReferenceDepth(), DELTA);
        final ResultMulti none = analyze("((((A.1.1,A.1.2),Q_#1_M=1.0),(A.2.1,A.2.2)),((B.1.1,B.1.2),B.2.1))");
        assertNull(Classification.of(none, 0.7).getPendantLength());
        assertNull(none.getReferenceDepth());
    }

    @Test
    void cutoffMustBeInRange() throws Exception {
        final ResultMulti res = analyze("((((A.1.1,A.1.2),Q_#1_M=1.0),(A.2.1,A.2.2)),((B.1.1,B.1.2),B.2.1))");
        assertThrows(IllegalArgumentException.class, () -> Classification.of(res, 0.0));
        assertThrows(IllegalArgumentException.class, () -> Classification.of(res, 1.5));
        assertThrows(IllegalArgumentException.class, () -> Classification.of(res, Double.NaN));
    }

    // ---- Classification by distance

    /** Sister to the single leaf A.1.1 at 0.001: the distance to A.1.1 is 0.101. */
    private static final String SISTER_TO_SINGLE_LEAF = "((((A.1.1:0.1,Q_#1_M=1.0:0.001):0.1,A.1.2:0.1):0.1,(A.2.1:0.1,A.2.2:0.1):0.1):0.1,((B.1.1:0.1,B.1.2:0.1):0.1,B.2.1:0.1):0.1)";

    @Test
    void nearestReferenceLeaf() throws Exception {
        final Placement p = analyze(SISTER_TO_SINGLE_LEAF).getPlacements().get(0);
        assertEquals("A.1.1", p.nearestLeaf());
        assertEquals(0.101, p.nearestDistance(), DELTA);
        // the nearest leaf can be in the uncle clade: sister at 0.9, uncle leaf at 0.3
        final Placement u = analyze("((((A.1.1:0.9,Q_#1_M=1.0:0.1):0.1,A.1.2:0.1):0.1,(A.2.1:0.1,A.2.2:0.1):0.1):0.1,B.1:0.3)").getPlacements().get(0);
        assertEquals("A.1.2", u.nearestLeaf());
        assertEquals(0.3, u.nearestDistance(), DELTA);
    }

    @Test
    void closerThanTheThresholdIsMemberOfTheLeafsClade() throws Exception {
        final Classification c = Classification.of(analyze(SISTER_TO_SINGLE_LEAF), 0.7, 0.2);
        assertEquals("A.1.1", c.getAssignment());
        assertEquals(Conclusion.MEMBER, c.getConclusion());
        assertEquals("A.1.1", c.getConclusionClade());
        assertEquals(1.0, c.getSupport(), DELTA);
        assertTrue(c.isByDistance());
        assertTrue(c.getSingleLeafSisters().isEmpty());
        assertEquals("A.1.1", c.getNearestLeaf());
        assertEquals(0.101, c.getNearestDistance(), DELTA);
    }

    @Test
    void fartherThanTheThresholdIsNovelWithinTheClade() throws Exception {
        final Classification c = Classification.of(analyze(SISTER_TO_SINGLE_LEAF), 0.7, 0.05);
        assertEquals("A.1", c.getAssignment());
        assertEquals(Conclusion.NOVEL_WITHIN, c.getConclusion());
        assertEquals(1.0, c.getSupport(), DELTA);
        assertTrue(c.isByDistance());
    }

    @Test
    void withoutThresholdTheTopologyDecides() throws Exception {
        final Classification c = Classification.of(analyze(SISTER_TO_SINGLE_LEAF), 0.7);
        assertFalse(c.isByDistance());
        assertEquals(Conclusion.NOVEL_WITHIN, c.getConclusion());
        assertFalse(c.getSingleLeafSisters().isEmpty());
    }

    @Test
    void nestedButOnALongBranchIsNovelByDistance() throws Exception {
        final ResultMulti res = analyze("((((A.1.1:0.1,Q_#1_M=1.0:2.0):0.1,A.1.1:0.1):0.1,(A.2.1:0.1,A.2.2:0.1):0.1):0.1,B.1:0.3)");
        assertEquals(Conclusion.MEMBER, Classification.of(res, 0.7).getConclusion());
        final Classification c = Classification.of(res, 0.7, 0.5);
        assertEquals("A.1.1", c.getAssignment());
        assertEquals(Conclusion.NOVEL_WITHIN, c.getConclusion());
        assertEquals(1.0, c.getSupport(), DELTA);
    }

    @Test
    void mixedPlacementsKeepTheTopologicalAssignment() throws Exception {
        // 0.6 close to A.1.1, 0.4 far: member of A.1.1 with support 0.6, the assignment stays A.1
        final Classification c = Classification.of(analyze("(((((A.1.1:0.1,Q_#1_M=0.6:0.001):0.1,A.1.2:0.1):0.1,((A.1.3:0.1,Q_#2_M=0.4:1.0):0.1,A.1.4:0.1):0.1):0.1,(A.2.1:0.1,A.2.2:0.1):0.1):0.1,B.1:0.3)"), 0.7, 0.2);
        assertEquals("A.1", c.getAssignment());
        assertEquals(Conclusion.MEMBER, c.getConclusion());
        assertEquals("A.1.1", c.getConclusionClade());
        assertEquals(0.6, c.getSupport(), DELTA);
    }

    @Test
    void tiedNearestLeavesCountForTheirCommonClade() throws Exception {
        final Classification c = Classification.of(analyze("(((((A.1.1:0.1,Q_#1_M=0.5:0.001):0.1,(A.1.2:0.1,Q_#2_M=0.5:0.001):0.1):0.1,(A.1.3:0.1,A.1.4:0.1):0.1):0.1,(A.2.1:0.1,A.2.2:0.1):0.1):0.1,B.1:0.3)"), 0.7, 0.2);
        assertEquals("A.1", c.getAssignment());
        assertEquals(Conclusion.MEMBER, c.getConclusion());
        assertEquals("A.1", c.getConclusionClade());
        assertEquals(1.0, c.getSupport(), DELTA);
        assertTrue(c.isByDistance());
    }

    @Test
    void thresholdIsNotAppliedWithoutBranchLengths() throws Exception {
        final Classification c = Classification.of(analyze("((((A.1.1,Q_#1_M=1.0),A.1.2),(A.2.1,A.2.2)),B.1)"), 0.7, 0.2);
        assertFalse(c.isByDistance());
        assertTrue(c.isDistanceNotApplied());
        assertEquals(Conclusion.NOVEL_WITHIN, c.getConclusion());
    }

    @Test
    void thresholdMustBeNonNegative() throws Exception {
        final ResultMulti res = analyze(SISTER_TO_SINGLE_LEAF);
        assertThrows(IllegalArgumentException.class, () -> Classification.of(res, 0.7, -1.0));
        assertThrows(IllegalArgumentException.class, () -> Classification.of(res, 0.7, Double.NaN));
        assertNotNull(Classification.of(res, 0.7, 0.0));
    }
}
