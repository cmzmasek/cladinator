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

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.forester.util.ForesterUtil;

/**
 * The conclusion drawn from the placements of a query, at a given confidence cutoff.
 *
 * <p>The assignment is the most specific clade whose summed placement weight reaches the cutoff: starting from
 * the top-level clade with the highest weight, the walk goes down to the best-supported sub-clade that reaches
 * the cutoff, and stops when two sub-clades tie (or when none reaches it).
 *
 * <p>The conclusion at the assigned clade X comes from the placements within X: a placement with a sister clade
 * and an "uncle" clade of different labels lies between labeled sub-clades of X ("potential for novel
 * sub-species within X"); one with equal labels, or one that lies in a sub-clade of X, is a member of X. The
 * conclusion is the one with more weight, which is reported as its support.
 *
 * <p>If no clade reaches the cutoff but the placements outside all labeled clades do, the query is "outside all
 * clades", and "sister to" the sister clade if one reaches the cutoff. Otherwise there is no confident assignment,
 * and the best-matching top-level clades are reported.
 */
public final class Classification {

    public enum Conclusion {
        MEMBER, NOVEL_WITHIN, OUTSIDE_SISTER_TO, OUTSIDE, NO_CONFIDENT_ASSIGNMENT
    }

    /** Weights this close to each other are ties; weights this close below the cutoff still reach it. */
    public final static double TOLERANCE = 1E-9;

    private final String _assignment;
    private final double _confidence;
    private final Conclusion _conclusion;
    private final String _conclusion_clade;
    private final double _support;
    private final String _bracket_down;
    private final String _bracket_up;
    private final List<Prefix> _best_matches;
    private final List<Prefix> _competing_subclades;
    private final Map<String, Double> _single_leaf_sisters;
    private final Double _pendant_length;

    private Classification(final String assignment,
                           final double confidence,
                           final Conclusion conclusion,
                           final String conclusion_clade,
                           final double support,
                           final String bracket_down,
                           final String bracket_up,
                           final List<Prefix> best_matches,
                           final List<Prefix> competing_subclades,
                           final Map<String, Double> single_leaf_sisters,
                           final Double pendant_length) {
        _assignment = assignment;
        _confidence = confidence;
        _conclusion = conclusion;
        _conclusion_clade = conclusion_clade;
        _support = support;
        _bracket_down = bracket_down;
        _bracket_up = bracket_up;
        _best_matches = Collections.unmodifiableList(best_matches);
        _competing_subclades = Collections.unmodifiableList(competing_subclades);
        _single_leaf_sisters = Collections.unmodifiableMap(single_leaf_sisters);
        _pendant_length = pendant_length;
    }

    public static Classification of(final ResultMulti res, final double cutoff) {
        if ((cutoff <= 0.0) || (cutoff > 1.0) || Double.isNaN(cutoff)) {
            throw new IllegalArgumentException("cutoff must be greater than 0 and at most 1");
        }
        final List<Placement> placements = res.getPlacements();
        if (placements.isEmpty()) {
            throw new IllegalArgumentException("no placements");
        }
        final String sep = res.getSeparator();
        // summed weight of every prefix of the clades the placements lie in
        final Map<String, Double> weights = new TreeMap<>();
        double outside = 0.0;
        for (final Placement p : placements) {
            if (p.clade().equals(AnalysisMulti.UNKNOWN)) {
                outside += p.weight();
            } else {
                for (final String prefix : ForesterUtil.spliIntoPrefixes(p.clade(), sep)) {
                    weights.merge(prefix, p.weight(), Double::sum);
                }
            }
        }
        final List<Prefix> top = new ArrayList<>();
        for (final Map.Entry<String, Double> e : weights.entrySet()) {
            if (!e.getKey().contains(sep)) {
                top.add(new Prefix(e.getKey(), e.getValue(), sep));
            }
        }
        if (outside > 0.0) {
            top.add(new Prefix(AnalysisMulti.UNKNOWN, outside, sep));
        }
        sortByWeight(top);
        final String bracket_down;
        final String bracket_up;
        if ((placements.size() == 1) && !placements.get(0).down().equals(AnalysisMulti.UNKNOWN)
                && !placements.get(0).up().equals(AnalysisMulti.UNKNOWN)) {
            bracket_down = placements.get(0).down();
            bracket_up = placements.get(0).up();
        } else {
            bracket_down = null;
            bracket_up = null;
        }
        final Double pendant = pendantLength(placements);

        final List<Prefix> labeled_top = new ArrayList<>();
        for (final Prefix p : top) {
            if (!p.getPrefix().equals(AnalysisMulti.UNKNOWN) && reaches(p.getConfidence(), cutoff)) {
                labeled_top.add(p);
            }
        }
        if (!labeled_top.isEmpty() && !isTie(labeled_top)) {
            // walk down to the most specific clade that reaches the cutoff
            String x = labeled_top.get(0).getPrefix();
            List<Prefix> competing = new ArrayList<>();
            while (true) {
                final List<Prefix> children = new ArrayList<>();
                final int child_level = x.split(java.util.regex.Pattern.quote(sep)).length + 1;
                for (final Map.Entry<String, Double> e : weights.entrySet()) {
                    if (ForesterUtil.isContainsPrefix(e.getKey(), x, sep)
                            && (e.getKey().split(java.util.regex.Pattern.quote(sep)).length == child_level)
                            && reaches(e.getValue(), cutoff)) {
                        children.add(new Prefix(e.getKey(), e.getValue(), sep));
                    }
                }
                if (children.isEmpty()) {
                    break;
                }
                sortByWeight(children);
                if (isTie(children)) {
                    competing = tied(children);
                    break;
                }
                x = children.get(0).getPrefix();
            }
            double member = 0.0;
            double novel = 0.0;
            final Map<String, Double> single_leaf_sisters = new LinkedHashMap<>();
            for (final Placement p : placements) {
                if (p.clade().equals(AnalysisMulti.UNKNOWN) || !ForesterUtil.isContainsPrefix(p.clade(), x, sep)) {
                    continue;
                }
                if (!p.clade().equals(x) || p.down().equals(p.up())) {
                    member += p.weight();
                } else {
                    novel += p.weight();
                    if (p.sisterIsSingleLeaf()) {
                        single_leaf_sisters.merge(p.down(), p.weight(), Double::sum);
                    }
                }
            }
            final boolean is_novel = novel > member + TOLERANCE;
            return new Classification(x, weights.get(x), is_novel ? Conclusion.NOVEL_WITHIN : Conclusion.MEMBER, x,
                    is_novel ? novel : member, bracket_down, bracket_up, new ArrayList<>(), competing,
                    is_novel ? single_leaf_sisters : new LinkedHashMap<>(), pendant);
        }
        if (labeled_top.isEmpty() && reaches(outside, cutoff)) {
            // outside all labeled clades; sister to a clade if one reaches the cutoff
            final Map<String, Double> sisters = new TreeMap<>();
            for (final Placement p : placements) {
                if (p.clade().equals(AnalysisMulti.UNKNOWN) && !p.down().equals(AnalysisMulti.UNKNOWN)) {
                    sisters.merge(p.down(), p.weight(), Double::sum);
                }
            }
            final List<Prefix> sister_list = new ArrayList<>();
            for (final Map.Entry<String, Double> e : sisters.entrySet()) {
                if (reaches(e.getValue(), cutoff)) {
                    sister_list.add(new Prefix(e.getKey(), e.getValue(), sep));
                }
            }
            sortByWeight(sister_list);
            if (!sister_list.isEmpty() && !isTie(sister_list)) {
                final Prefix sister = sister_list.get(0);
                return new Classification(null, outside, Conclusion.OUTSIDE_SISTER_TO, sister.getPrefix(),
                        sister.getConfidence(), bracket_down, bracket_up, new ArrayList<>(), new ArrayList<>(),
                        new LinkedHashMap<>(), pendant);
            }
            return new Classification(null, outside, Conclusion.OUTSIDE, null, outside, bracket_down, bracket_up,
                    new ArrayList<>(), new ArrayList<>(), new LinkedHashMap<>(), pendant);
        }
        final List<Prefix> best = tied(top);
        return new Classification(null, top.get(0).getConfidence(), Conclusion.NO_CONFIDENT_ASSIGNMENT, null,
                top.get(0).getConfidence(), bracket_down, bracket_up, best, new ArrayList<>(),
                new LinkedHashMap<>(), pendant);
    }

    private static boolean reaches(final double weight, final double cutoff) {
        return weight >= cutoff - TOLERANCE;
    }

    /** Whether the first two of a list sorted by weight have the same weight. */
    private static boolean isTie(final List<Prefix> sorted) {
        return (sorted.size() > 1)
                && (Math.abs(sorted.get(0).getConfidence() - sorted.get(1).getConfidence()) < TOLERANCE);
    }

    /** The entries of a list sorted by weight that have the same weight as the first. */
    private static List<Prefix> tied(final List<Prefix> sorted) {
        final List<Prefix> l = new ArrayList<>();
        for (final Prefix p : sorted) {
            if (Math.abs(p.getConfidence() - sorted.get(0).getConfidence()) < TOLERANCE) {
                l.add(p);
            }
        }
        return l;
    }

    /** Descending weight; ties keep their (alphabetical) input order. */
    private static void sortByWeight(final List<Prefix> l) {
        l.sort((a, b) -> Double.compare(b.getConfidence(), a.getConfidence()));
    }

    /** The weighted mean of the pendant branch lengths, or null if the tree has none. */
    private static Double pendantLength(final List<Placement> placements) {
        double sum = 0.0;
        double weight = 0.0;
        for (final Placement p : placements) {
            if (p.hasPendantLength()) {
                sum += p.weight() * p.pendantLength();
                weight += p.weight();
            }
        }
        return (weight > 0.0) ? sum / weight : null;
    }

    /** The assigned clade, or null if no clade reaches the cutoff. */
    public String getAssignment() {
        return _assignment;
    }

    /** The summed weight of the assignment; without one, of what the conclusion is about. */
    public double getConfidence() {
        return _confidence;
    }

    public Conclusion getConclusion() {
        return _conclusion;
    }

    /** The clade the conclusion names: the assignment, or the sister clade for OUTSIDE_SISTER_TO; else null. */
    public String getConclusionClade() {
        return _conclusion_clade;
    }

    /** The summed weight of the placements that support the conclusion. */
    public double getSupport() {
        return _support;
    }

    /** The sister clade of a single placement, or null (several placements, or no labeled sister clade). */
    public String getBracketDown() {
        return _bracket_down;
    }

    /** The "uncle" clade of a single placement, or null (several placements, or no labeled uncle clade). */
    public String getBracketUp() {
        return _bracket_up;
    }

    /** For NO_CONFIDENT_ASSIGNMENT: the top-level clades with the highest weight (more than one if they tie). */
    public List<Prefix> getBestMatches() {
        return _best_matches;
    }

    /** Sub-clades of the assignment that reach the cutoff with the same weight, so that the walk stopped. */
    public List<Prefix> getCompetingSubclades() {
        return _competing_subclades;
    }

    /**
     * For NOVEL_WITHIN: the labels of single reference leaves the query is sister to, with the weight of those
     * placements. Membership in such a leaf's taxon cannot be excluded from the topology.
     */
    public Map<String, Double> getSingleLeafSisters() {
        return _single_leaf_sisters;
    }

    /** The weighted mean pendant branch length of the query, or null if the tree has no branch lengths. */
    public Double getPendantLength() {
        return _pendant_length;
    }
}
