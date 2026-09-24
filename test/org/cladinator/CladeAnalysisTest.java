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

import org.forester.io.parsers.PhylogenyParser;
import org.forester.io.parsers.nhx.NHXParser;
import org.forester.io.parsers.util.ParserUtils;
import org.forester.phylogeny.Phylogeny;
import org.forester.phylogeny.factories.ParserBasedPhylogenyFactory;
import org.forester.phylogeny.factories.PhylogenyFactory;
import org.forester.util.ForesterUtil;
import org.forester.util.UserException;

import java.io.File;
import java.util.List;
import java.util.regex.Pattern;

public class CladeAnalysisTest {

    private final static String PATH_TO_TEST_DATA = System.getProperty("user.dir") + ForesterUtil.getFileSeparator()
            + "test/data" + ForesterUtil.getFileSeparator();

    public static void main(final String[] args) {
        boolean failed = false;
        if (!testCladeAnalysis3()) {
            System.out.println("Clade analysis 3 failed");
            failed = true;
        }
        if (!testCladeAnalysis4()) {
            System.out.println("Clade analysis 4 failed");
            failed = true;
        }
        if (!testCladeAnalysis5()) {
            System.out.println("Clade analysis 5 failed");
            failed = true;
        }
        if (!testCladeAnalysis6()) {
            System.out.println("Clade analysis 6 failed");
            failed = true;
        }
        if (!testCladeAnalysisSeparator()) {
            System.out.println("Clade analysis separator failed");
            failed = true;
        }
        if (!testCladeAnalysisManyPrefixes()) {
            System.out.println("Clade analysis many prefixes failed");
            failed = true;
        }
        if (!testCladeAnalysisRootPlacement()) {
            System.out.println("Clade analysis root placement failed");
            failed = true;
        }
        if (!testLikelyProblematicQuery()) {
            System.out.println("Likely problematic query failed");
            failed = true;
        }
        if (!testConfidenceRenormalization()) {
            System.out.println("Confidence renormalization failed");
            failed = true;
        }
        if (!testClassification()) {
            System.out.println("Classification failed");
            failed = true;
        }
        if (!failed) {
            System.out.println("OK");
        } else {
            System.out.println("NOT OK");
            System.exit(1);
        }
    }

    public static boolean test() {
        if (!testCladeAnalysis3()) {
            return false;
        }
        if (!testCladeAnalysis4()) {
            return false;
        }
        if (!testCladeAnalysisSeparator()) {
            return false;
        }
        if (!testCladeAnalysisManyPrefixes()) {
            return false;
        }
        if (!testCladeAnalysisRootPlacement()) {
            return false;
        }
        if (!testLikelyProblematicQuery()) {
            return false;
        }
        if (!testConfidenceRenormalization()) {
            return false;
        }
        if (!testClassification()) {
            return false;
        }
        return true;
    }

    private static boolean testCladeAnalysis3() {
        try {
            final ResultMulti res1 = new ResultMulti();
            res1.addGreatestCommonPrefix("A.1.1", 0.3);
            res1.addGreatestCommonPrefix("A.1.2", 0.3);
            res1.addGreatestCommonPrefix("A.1.3", 0.3);
            res1.addGreatestCommonPrefix("B.1", 0.1);
            res1.analyze();
            System.out.print(res1.toString());
            System.out.println("------------------------- ");
            System.out.println();
            final ResultMulti res2 = new ResultMulti(".");
            res2.addGreatestCommonPrefix("A.1.1.1", 0.1);
            res2.addGreatestCommonPrefix("A.1", 0.7);
            res2.addGreatestCommonPrefix("A.1.2", 0.1);
            res2.addGreatestCommonPrefix("B.1", 0.1);
            res2.analyze();
            System.out.print(res2.toString());
            System.out.println("------------------------- ");
            System.out.println();
            final ResultMulti res3 = new ResultMulti(".");
            res3.addGreatestCommonPrefix("A.1.1.1", 0.1);
            res3.addGreatestCommonPrefix("A.1.1.1.1", 0.6);
            res3.addGreatestCommonPrefix("A.1", 0.1);
            res3.addGreatestCommonPrefix("A.1.2", 0.1);
            res3.addGreatestCommonPrefix("B.1", 0.1);
            res3.analyze();
            System.out.print(res3.toString());
            System.out.println("------------------------- ");
            System.out.println();
            final ResultMulti res33 = new ResultMulti(".");
            res33.addGreatestCommonPrefix("A.1.1.1", 0.1);
            res33.addGreatestCommonPrefix("A.1.1.1.1", 0.3);
            res33.addGreatestCommonPrefix("A.1", 0.1);
            res33.addGreatestCommonPrefix("A.1.2", 0.1);
            res33.addGreatestCommonPrefix("B.1", 0.1);
            res33.addGreatestCommonPrefix("B.1.1.1", 0.3);
            res33.analyze();
            System.out.print(res33.toString());
            System.out.println("------------------------- ");
            System.out.println();
            final ResultMulti res4 = new ResultMulti();
            res4.addGreatestCommonPrefix("A.1.1.1.1", 0.35);
            res4.addGreatestCommonPrefix("A.1.1.1.2", 0.35);
            res4.addGreatestCommonPrefix("A.1", 0.1);
            res4.addGreatestCommonPrefix("A.1.2", 0.1);
            res4.addGreatestCommonPrefix("B.1", 0.1);
            res4.analyze();
            System.out.print(res4.toString());
            System.out.println("------------------------- ");
            System.out.println();
            final ResultMulti res5 = new ResultMulti();
            res5.addGreatestCommonPrefix("A.1.1.1.1", 0.2);
            res5.addGreatestCommonPrefix("C.2.3", 0.2);
            res5.addGreatestCommonPrefix("A.1.5", 0.1);
            res5.addGreatestCommonPrefix("A.3.1.4", 0.2);
            res5.addGreatestCommonPrefix("B.1.1", 0.2);
            res5.addGreatestCommonPrefix("B.1.2", 0.09);
            res5.addGreatestCommonPrefix("D.1.1.1.1", 0.01);
            res5.analyze();
            System.out.print(res5.toString());
            System.out.println("------------------------- ");
            System.out.println();
            final ResultMulti res6 = new ResultMulti();
            res6.addGreatestCommonPrefix("A.1.1.1", 0.05);
            res6.addGreatestCommonPrefix("A.1.1.1.1", 0.65);
            res6.addGreatestCommonPrefix("A.1", 0.1);
            res6.addGreatestCommonPrefix("A.1.2", 0.1);
            res6.addGreatestCommonPrefix("B.1", 0.1);
            res6.analyze();
            System.out.print(res6.toString());
            System.out.println("------------------------- ");
            System.out.println();
            final ResultMulti res7 = new ResultMulti();
            res7.addGreatestCommonPrefix("A.1.1.1", 0.07);
            res7.addGreatestCommonPrefix("A.1.1.1.1", 0.9);
            res7.addGreatestCommonPrefix("A.1", 0.01);
            res7.addGreatestCommonPrefix("A.1.2", 0.01);
            res7.addGreatestCommonPrefix("B.1", 0.01);
            res7.analyze();
            System.out.print(res7.toString());
            System.out.println("------------------------- ");
            System.out.println();
            final ResultMulti res8 = new ResultMulti("_/_");
            res8.addGreatestCommonPrefix("AA_/_abc_/_def", 0.07);
            res8.addGreatestCommonPrefix("AA_/_abc_/_sfc", 0.9);
            res8.addGreatestCommonPrefix("AA_/_abc_/_xcd", 0.01);
            res8.addGreatestCommonPrefix("AA_/_abc_/_memr", 0.01);
            res8.addGreatestCommonPrefix("AA_/_abc_/_fkem_/_odem", 0.01);
            res8.analyze();
            System.out.print(res8.toString());
            System.out.println("------------------------- ");
            System.out.println();
            final ResultMulti res9 = new ResultMulti("_/_");
            res9.addGreatestCommonPrefix("AA_/_abc_/_def", 0.07);
            res9.addGreatestCommonPrefix("AA_/_abc_/_sfc", 0.6);
            res9.addGreatestCommonPrefix("AA_/_abc_/_xcd", 0.01);
            res9.addGreatestCommonPrefix("AA_/_abc_/_memr", 0.01);
            res9.addGreatestCommonPrefix("AA_/_abc_/_fkem_/_odem", 0.01);
            res9.addGreatestCommonPrefix("BB_/_fke_/_dme_/_nx2", 0.3);
            res9.analyze();
            System.out.print(res9.toString());
            System.out.println("------------------------- ");
            System.out.println();
        } catch (final Exception e) {
            e.printStackTrace(System.out);
            return false;
        }
        return true;
    }

    private static boolean testCladeAnalysis4() {
        try {
            final File intreefile1 = new File(PATH_TO_TEST_DATA + "pplacer_2.tre");
            final PhylogenyFactory factory = ParserBasedPhylogenyFactory.getInstance();
            final PhylogenyParser pp = ParserUtils.createParserDependingOnFileType(intreefile1, true);
            final Phylogeny p1 = factory.create(intreefile1, pp)[0];
            final ResultMulti res2 = AnalysisMulti.execute(p1);
            res2.analyze();
            System.out.print(res2.toString());
            System.out.println("------------------------- ");
            System.out.println();
        } catch (final Exception e) {
            e.printStackTrace(System.out);
            return false;
        }
        return true;
    }

    private static boolean testCladeAnalysis5() {
        try {
            final PhylogenyFactory factory = ParserBasedPhylogenyFactory.getInstance();
            final String t1s = "(((((A.1.1,Q_#1_M=1),A.1.2),(A.2.1,A.2.2)),((A.3.1,A.3.2),(A.4.1,A.4.2))),(((B.1,B.2),B.3),(C.1,C.2)))";
            final Phylogeny t1 = factory.create(t1s, new NHXParser())[0];
            final ResultMulti res1 = AnalysisMulti.execute(t1, ".");
            res1.analyze();
            System.out.print(res1.toString());
            System.out.println("------------------------- ");
            System.out.println();


        } catch (final Exception e) {
            e.printStackTrace(System.out);
            return false;
        }
        return true;
    }

    private static boolean testCladeAnalysis6() {
        try {
            final PhylogenyFactory factory = ParserBasedPhylogenyFactory.getInstance();
            final String t1s = "(((((A.1.1,A.1.2),Q_#0_M=0.5),((A.2.1,A.2.2),Q_#1_M=0.5)),((A.3.1,A.3.2),(A.4.1,A.4.2))),(((B.1,B.2),B.3),(C.1,C.2)))";
            final Phylogeny t1 = factory.create(t1s, new NHXParser())[0];
            final ResultMulti res1 = AnalysisMulti.execute(t1, ".");
            res1.analyze();
            System.out.print(res1.toString());
            System.out.println("------------------------- ");
            System.out.println();


        } catch (final Exception e) {
            e.printStackTrace(System.out);
            return false;
        }
        return true;
    }

    // The same tree labeled with "." and with "_" as annotation separator must give the same result.
    private static boolean testCladeAnalysisSeparator() {
        try {
            final PhylogenyFactory factory = ParserBasedPhylogenyFactory.getInstance();
            final String dot = "((((A.1.1,A.1.2),Q_#1_M=0.6),((A.2.1,A.2.2),Q_#2_M=0.4)),((B.1.1,B.1.2),B.2.1))";
            final String us = "((((A_1_1,A_1_2),Q_#1_M=0.6),((A_2_1,A_2_2),Q_#2_M=0.4)),((B_1_1,B_1_2),B_2_1))";
            final ResultMulti res_dot = AnalysisMulti.execute(factory.create(dot, new NHXParser())[0], ".");
            final ResultMulti res_us = AnalysisMulti.execute(factory.create(us, new NHXParser())[0], "_");
            if (!res_dot.getAllMultiHitPrefixesDown().get(0).getPrefix().equals("A")) {
                return false;
            }
            if (!res_dot.getAllMultiHitPrefixesUp().get(0).getPrefix().equals("A")) {
                return false;
            }
            if (!samePrefixes(res_dot.getAllMultiHitPrefixes(), res_us.getAllMultiHitPrefixes())) {
                return false;
            }
            if (!samePrefixes(res_dot.getCollapsedMultiHitPrefixes(), res_us.getCollapsedMultiHitPrefixes())) {
                return false;
            }
            if (!samePrefixes(res_dot.getAllMultiHitPrefixesDown(), res_us.getAllMultiHitPrefixesDown())) {
                return false;
            }
            if (!samePrefixes(res_dot.getCollapsedMultiHitPrefixesDown(), res_us.getCollapsedMultiHitPrefixesDown())) {
                return false;
            }
            if (!samePrefixes(res_dot.getAllMultiHitPrefixesUp(), res_us.getAllMultiHitPrefixesUp())) {
                return false;
            }
            if (!samePrefixes(res_dot.getCollapsedMultiHitPrefixesUp(), res_us.getCollapsedMultiHitPrefixesUp())) {
                return false;
            }
        } catch (final Exception e) {
            e.printStackTrace(System.out);
            return false;
        }
        return true;
    }

    // Placements next to 40 clades give 40+ prefixes; lists must come out sorted by descending confidence
    // (the sort misbehaved from 32 elements on).
    private static boolean testCladeAnalysisManyPrefixes() {
        try {
            final int n = 40;
            final double total = n * (n + 1) / 2.0;
            final StringBuilder sb = new StringBuilder("(");
            for (int k = 1; k <= n; ++k) {
                if (k > 1) {
                    sb.append(",");
                }
                sb.append("(((X").append(k).append(".1.1,X").append(k).append(".1.2),Q_#").append(k)
                        .append("_M=").append(k / total).append("),(X").append(k).append(".2.1,X").append(k)
                        .append(".2.2))");
            }
            sb.append(")");
            final PhylogenyFactory factory = ParserBasedPhylogenyFactory.getInstance();
            final ResultMulti res = AnalysisMulti.execute(factory.create(sb.toString(), new NHXParser())[0], ".");
            if (res.getAllMultiHitPrefixes().size() != n) {
                return false;
            }
            if (!res.getAllMultiHitPrefixes().get(0).getPrefix().equals("X" + n)) {
                return false;
            }
            if (!res.getAllMultiHitPrefixesDown().get(0).getPrefix().equals("X" + n + ".1")) {
                return false;
            }
            if (!res.getAllMultiHitPrefixesUp().get(0).getPrefix().equals("X" + n + ".2")) {
                return false;
            }
            for (final List<Prefix> l : List.of(res.getAllMultiHitPrefixes(),
                    res.getCollapsedMultiHitPrefixes(),
                    res.getAllMultiHitPrefixesDown(),
                    res.getCollapsedMultiHitPrefixesDown(),
                    res.getAllMultiHitPrefixesUp(),
                    res.getCollapsedMultiHitPrefixesUp())) {
                for (int i = 1; i < l.size(); ++i) {
                    if (l.get(i).getConfidence() > l.get(i - 1).getConfidence()) {
                        return false;
                    }
                }
            }
        } catch (final Exception e) {
            e.printStackTrace(System.out);
            return false;
        }
        return true;
    }

    // A placement attached to the root has no bracketing clades: it counts as "?" in the up/down lists
    // too, so that they still add up to 1.0 next to other placements.
    private static boolean testCladeAnalysisRootPlacement() {
        try {
            final PhylogenyFactory factory = ParserBasedPhylogenyFactory.getInstance();
            final String t = "(Q_#1_M=0.3,(((A.1.1,Q_#2_M=0.7),A.1.2),(A.2.1,A.2.2)),((B.1.1,B.1.2),B.2.1))";
            final ResultMulti res = AnalysisMulti.execute(factory.create(t, new NHXParser())[0], ".");
            if (!isPrefixes(res.getAllMultiHitPrefixes(), "A.1", 0.7, "?", 0.3)) {
                return false;
            }
            if (!isPrefixes(res.getAllMultiHitPrefixesDown(), "A.1.1", 0.7, "?", 0.3)) {
                return false;
            }
            if (!isPrefixes(res.getAllMultiHitPrefixesUp(), "A.1.2", 0.7, "?", 0.3)) {
                return false;
            }
            final String all_root = "(Q_#1_M=1.0,((A.1.1,A.1.2),A.2.1),((B.1.1,B.1.2),B.2.1))";
            final ResultMulti res2 = AnalysisMulti.execute(factory.create(all_root, new NHXParser())[0], ".");
            if (!isPrefixes(res2.getAllMultiHitPrefixes(), "?", 1.0)) {
                return false;
            }
            if (!isPrefixes(res2.getAllMultiHitPrefixesDown(), "?", 1.0)) {
                return false;
            }
            if (!isPrefixes(res2.getAllMultiHitPrefixesUp(), "?", 1.0)) {
                return false;
            }
        } catch (final Exception e) {
            e.printStackTrace(System.out);
            return false;
        }
        return true;
    }

    // The non-homologous-query check needs branch lengths; without them (all distances 0) it must not flag.
    private static boolean testLikelyProblematicQuery() {
        try {
            final PhylogenyFactory factory = ParserBasedPhylogenyFactory.getInstance();
            final Pattern q = AnalysisMulti.DEFAULT_QUERY_PATTERN_FOR_PPLACER_TYPE;
            final String normal = "((((A.1.1:0.1,A.1.2:0.1):0.1,Q_#1_M=1.0:0.1):0.1,(A.2.1:0.1,A.2.2:0.1):0.1):0.1,B.1:0.3)";
            final String long_branch = "((((A.1.1:0.1,A.1.2:0.1):0.1,Q_#1_M=1.0:5.0):0.1,(A.2.1:0.1,A.2.2:0.1):0.1):0.1,B.1:0.3)";
            final String no_lengths = "((((A.1.1,A.1.2),Q_#1_M=1.0),(A.2.1,A.2.2)),B.1)";
            final String only_queries = "(Q_#1_M=0.5:0.1,Q_#2_M=0.5:0.1)";
            if (AnalysisMulti.likelyProblematicQuery(factory.create(normal, new NHXParser())[0], q, 2)) {
                return false;
            }
            if (!AnalysisMulti.likelyProblematicQuery(factory.create(long_branch, new NHXParser())[0], q, 2)) {
                return false;
            }
            if (AnalysisMulti.likelyProblematicQuery(factory.create(no_lengths, new NHXParser())[0], q, 2)) {
                return false;
            }
            if (AnalysisMulti.likelyProblematicQuery(factory.create(only_queries, new NHXParser())[0], q, 2)) {
                return false;
            }
        } catch (final Exception e) {
            e.printStackTrace(System.out);
            return false;
        }
        return true;
    }

    // Placement confidences that do not add up to 1 (placement programs drop low-weight placements) are
    // rescaled, with a warning; a sum of 0 is an error.
    private static boolean testConfidenceRenormalization() {
        try {
            final PhylogenyFactory factory = ParserBasedPhylogenyFactory.getInstance();
            final String t = "((((A.1.1,A.1.2),Q_#1_M=0.95),(A.2.1,A.2.2)),((B.1.1,Q_#2_M=0.04),B.2.1))";
            final ResultMulti res = AnalysisMulti.execute(factory.create(t, new NHXParser())[0], ".");
            if (!isPrefixes(res.getAllMultiHitPrefixes(), "A", 0.95 / 0.99, "B", 0.04 / 0.99)) {
                return false;
            }
            if (res.getWarnings().size() != 1 || !res.getWarnings().get(0).contains("0.99")) {
                return false;
            }
            final String exact = "((((A.1.1,A.1.2),Q_#1_M=0.96),(A.2.1,A.2.2)),((B.1.1,Q_#2_M=0.04),B.2.1))";
            final ResultMulti res2 = AnalysisMulti.execute(factory.create(exact, new NHXParser())[0], ".");
            if (!res2.getWarnings().isEmpty()) {
                return false;
            }
            // within tolerance (real pplacer output is printed with ~6 digits): no warning
            final String near = "((((A.1.1,A.1.2),Q_#1_M=0.959999),(A.2.1,A.2.2)),((B.1.1,Q_#2_M=0.04),B.2.1))";
            final ResultMulti res3 = AnalysisMulti.execute(factory.create(near, new NHXParser())[0], ".");
            if (!res3.getWarnings().isEmpty()) {
                return false;
            }
            for (final String bad : new String[]{
                    "((((A.1.1,A.1.2),Q_#1_M=0),(A.2.1,A.2.2)),((B.1.1,Q_#2_M=0.0),B.2.1))",
                    "((((A.1.1,A.1.2),Q_#1_M=NaN),(A.2.1,A.2.2)),((B.1.1,Q_#2_M=0.5),B.2.1))",
                    "((((A.1.1,A.1.2),Q_#1_M=Infinity),(A.2.1,A.2.2)),((B.1.1,Q_#2_M=0.5),B.2.1))",
                    "((((A.1.1,A.1.2),Q_#1_M=-0.5),(A.2.1,A.2.2)),((B.1.1,Q_#2_M=1.5),B.2.1))",
                    "((((A.1.1,A.1.2),Q_#1_M=abc),(A.2.1,A.2.2)),((B.1.1,Q_#2_M=0.5),B.2.1))"}) {
                try {
                    AnalysisMulti.execute(factory.create(bad, new NHXParser())[0], ".");
                    return false;
                } catch (final UserException expected) {
                    if (expected.getMessage().startsWith("ERROR")) {
                        return false;
                    }
                }
            }
        } catch (final Exception e) {
            e.printStackTrace(System.out);
            return false;
        }
        return true;
    }

    private static boolean testClassification() {
        try {
            final PhylogenyFactory factory = ParserBasedPhylogenyFactory.getInstance();
            // the most specific clade reaching the cutoff: 0.9 within A.1 (sister to the single leaf A.1.1), 0.1 in A
            final String p13 = "(((((A.1.1,Q_#1_M=0.9),A.1.2),(A.1.3,A.1.4)),((A.2.1,A.2.2),Q_#2_M=0.1)),((B.1.1,B.1.2),B.2.1))";
            Classification c = classify(factory, p13, 0.7);
            if (!"A.1".equals(c.getAssignment()) || !ForesterUtil.isEqual(c.getConfidence(), 0.9)
                    || (c.getConclusion() != Classification.Conclusion.NOVEL_WITHIN)
                    || !ForesterUtil.isEqual(c.getSupport(), 0.9) || (c.getBracketDown() != null)
                    || (c.getSingleLeafSisters().size() != 1) || !ForesterUtil.isEqual(c.getSingleLeafSisters().get("A.1.1"), 0.9)) {
                System.out.println("p13: " + describe(c));
                return false;
            }
            // sister to the whole clade A: outside all clades
            final String p12 = "((((A.1.1,A.1.2),(A.2.1,A.2.2)),Q_#1_M=1.0),OUT.1)";
            c = classify(factory, p12, 0.7);
            if ((c.getAssignment() != null) || (c.getConclusion() != Classification.Conclusion.OUTSIDE_SISTER_TO)
                    || !"A".equals(c.getConclusionClade()) || !ForesterUtil.isEqual(c.getSupport(), 1.0)
                    || !"A".equals(c.getBracketDown()) || !"OUT.1".equals(c.getBracketUp())) {
                System.out.println("p12: " + describe(c));
                return false;
            }
            // between the sub-clades A.1 and A.2, sister clade of two leaves: novel within A, no single-leaf note
            final String t01 = "((((A.1.1,A.1.2),Q_#1_M=1.0),(A.2.1,A.2.2)),((B.1.1,B.1.2),B.2.1))";
            c = classify(factory, t01, 0.7);
            if (!"A".equals(c.getAssignment()) || (c.getConclusion() != Classification.Conclusion.NOVEL_WITHIN)
                    || !"A.1".equals(c.getBracketDown()) || !"A.2".equals(c.getBracketUp())
                    || !c.getSingleLeafSisters().isEmpty()) {
                System.out.println("t01: " + describe(c));
                return false;
            }
            // two placements, 0.6 sister to A.1 and 0.4 sister to A.2: the same conclusion as t01, support 1.0
            final String t05 = "((((A.1.1,A.1.2),Q_#1_M=0.6),((A.2.1,A.2.2),Q_#2_M=0.4)),((B.1.1,B.1.2),B.2.1))";
            c = classify(factory, t05, 0.7);
            if (!"A".equals(c.getAssignment()) || (c.getConclusion() != Classification.Conclusion.NOVEL_WITHIN)
                    || !ForesterUtil.isEqual(c.getSupport(), 1.0) || (c.getBracketDown() != null)) {
                System.out.println("t05: " + describe(c));
                return false;
            }
            // among leaves of one label: member
            final String member = "((((A.1.1,Q_#1_M=1.0),A.1.1),(A.2.1,A.2.2)),B.1)";
            c = classify(factory, member, 0.7);
            if (!"A.1.1".equals(c.getAssignment()) || (c.getConclusion() != Classification.Conclusion.MEMBER)
                    || !ForesterUtil.isEqual(c.getSupport(), 1.0)) {
                System.out.println("member: " + describe(c));
                return false;
            }
            // 0.5 / 0.5 between A and B: no confident assignment, both named
            final String tie = "((((A.1.1,A.1.2),Q_#1_M=0.5),(A.2.1,A.2.2)),(((B.1.1,B.1.2),Q_#2_M=0.5),(B.2.1,B.2.2)))";
            c = classify(factory, tie, 0.7);
            if ((c.getAssignment() != null) || (c.getConclusion() != Classification.Conclusion.NO_CONFIDENT_ASSIGNMENT)
                    || (c.getBestMatches().size() != 2) || !"A".equals(c.getBestMatches().get(0).getPrefix())
                    || !"B".equals(c.getBestMatches().get(1).getPrefix())) {
                System.out.println("tie: " + describe(c));
                return false;
            }
            // sub-clades A.1 and A.2 both reach a cutoff of 0.5: the walk stops at A, member (both are within sub-clades)
            final String subtie = "(((((A.1.1,Q_#1_M=0.5),A.1.2),((A.2.1,Q_#2_M=0.5),A.2.2)),(A.3.1,A.3.2)),((B.1.1,B.1.2),B.2.1))";
            c = classify(factory, subtie, 0.5);
            if (!"A".equals(c.getAssignment()) || (c.getConclusion() != Classification.Conclusion.MEMBER)
                    || !ForesterUtil.isEqual(c.getSupport(), 1.0) || (c.getCompetingSubclades().size() != 2)
                    || !"A.1".equals(c.getCompetingSubclades().get(0).getPrefix())) {
                System.out.println("subtie: " + describe(c));
                return false;
            }
            // the same tree at 0.7: A, member, no competing sub-clades
            c = classify(factory, subtie, 0.7);
            if (!"A".equals(c.getAssignment()) || !c.getCompetingSubclades().isEmpty()) {
                System.out.println("subtie 0.7: " + describe(c));
                return false;
            }
            // everything on the root: outside all clades
            final String root = "(Q_#1_M=1.0,((A.1.1,A.1.2),A.2.1),((B.1.1,B.1.2),B.2.1))";
            c = classify(factory, root, 0.7);
            if ((c.getAssignment() != null) || (c.getConclusion() != Classification.Conclusion.OUTSIDE)
                    || !ForesterUtil.isEqual(c.getConfidence(), 1.0)) {
                System.out.println("root: " + describe(c));
                return false;
            }
            // pendant length and reference depth from the branch lengths; none without them
            final String lengths = "((((A.1.1:0.1,A.1.2:0.1):0.1,Q_#1_M=1.0:0.25):0.1,(A.2.1:0.1,A.2.2:0.1):0.1):0.1,((B.1.1:0.1,B.1.2:0.1):0.1,B.2.1:0.1):0.1)";
            final ResultMulti res = AnalysisMulti.execute(factory.create(lengths, new NHXParser())[0], ".");
            c = Classification.of(res, 0.7);
            if ((c.getPendantLength() == null) || !ForesterUtil.isEqual(c.getPendantLength(), 0.25)
                    || (res.getReferenceDepth() == null) || !ForesterUtil.isEqual(res.getReferenceDepth(), 0.4)) {
                System.out.println("lengths: " + c.getPendantLength() + " " + res.getReferenceDepth());
                return false;
            }
            final ResultMulti res2 = AnalysisMulti.execute(factory.create(t01, new NHXParser())[0], ".");
            if ((Classification.of(res2, 0.7).getPendantLength() != null) || (res2.getReferenceDepth() != null)) {
                return false;
            }
            try {
                Classification.of(res2, 0.0);
                return false;
            } catch (final IllegalArgumentException expected) {
                // ok
            }
        } catch (final Exception e) {
            e.printStackTrace(System.out);
            return false;
        }
        return true;
    }

    private static Classification classify(final PhylogenyFactory factory, final String tree, final double cutoff)
            throws Exception {
        return Classification.of(AnalysisMulti.execute(factory.create(tree, new NHXParser())[0], "."), cutoff);
    }

    private static String describe(final Classification c) {
        return c.getAssignment() + " " + c.getConfidence() + " " + c.getConclusion() + " " + c.getConclusionClade() + " "
                + c.getSupport() + " [" + c.getBracketDown() + ", " + c.getBracketUp() + "] best=" + c.getBestMatches()
                + " competing=" + c.getCompetingSubclades() + " single=" + c.getSingleLeafSisters();
    }

    // prefix_and_confidence: prefix, confidence, prefix, confidence, ...
    private static boolean isPrefixes(final List<Prefix> l, final Object... prefix_and_confidence) {
        if (l.size() * 2 != prefix_and_confidence.length) {
            return false;
        }
        for (int i = 0; i < l.size(); ++i) {
            if (!l.get(i).getPrefix().equals(prefix_and_confidence[2 * i])) {
                return false;
            }
            if (!ForesterUtil.isEqual(l.get(i).getConfidence(), (Double) prefix_and_confidence[2 * i + 1])) {
                return false;
            }
        }
        return true;
    }

    private static boolean samePrefixes(final List<Prefix> dot, final List<Prefix> us) {
        if (dot.size() != us.size()) {
            return false;
        }
        for (int i = 0; i < dot.size(); ++i) {
            if (!dot.get(i).getPrefix().equals(us.get(i).getPrefix().replace('_', '.'))) {
                return false;
            }
            if (!ForesterUtil.isEqual(dot.get(i).getConfidence(), us.get(i).getConfidence())) {
                return false;
            }
        }
        return true;
    }
}
