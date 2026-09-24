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
import java.util.regex.Pattern;

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
 *
 * <p>With a distance threshold, the distance to the nearest reference leaf decides between member and novel
 * instead of the topology: a placement closer than the threshold to a reference leaf supports membership in that
 * leaf's clade, one farther than the threshold from every reference leaf supports a novel lineage within the
 * assigned clade. If membership in a leaf's clade reaches the cutoff, that clade is the assignment.
 */
public final class Classification {

    public enum Conclusion {
        MEMBER, NOVEL_WITHIN, OUTSIDE_SISTER_TO, OUTSIDE, NO_CONFIDENT_ASSIGNMENT
    }

    /** Weights this close to each other are ties; weights this close below the cutoff still reach it. */
    public final static double TOLERANCE = 1E-9;

    private String _assignment = null;
    private double _confidence = 0.0;
    private Conclusion _conclusion = Conclusion.NO_CONFIDENT_ASSIGNMENT;
    private String _conclusion_clade = null;
    private double _support = 0.0;
    private String _bracket_down = null;
    private String _bracket_up = null;
    private List<Prefix> _best_matches = new ArrayList<>();
    private List<Prefix> _competing_subclades = new ArrayList<>();
    private Map<String, Double> _single_leaf_sisters = new LinkedHashMap<>();
    private Double _pendant_length = null;
    private String _nearest_leaf = null;
    private Double _nearest_distance = null;
    private boolean _by_distance = false;
    private boolean _distance_not_applied = false;
    private String _nearest_leaf_outside_clade = null;

    private Classification() {
    }

    public static Classification of(final ResultMulti res, final double cutoff) {
        return of(res, cutoff, null);
    }

    /**
     * @param distance_threshold the distance to the nearest reference leaf below which the query is a member of
     *                           that leaf's clade, and at or above which it is a novel lineage; null to decide
     *                           by topology only
     */
    public static Classification of(final ResultMulti res, final double cutoff, final Double distance_threshold) {
        if ((cutoff <= 0.0) || (cutoff > 1.0) || Double.isNaN(cutoff)) {
            throw new IllegalArgumentException("cutoff must be greater than 0 and at most 1");
        }
        if ((distance_threshold != null) && (!(distance_threshold >= 0.0) || Double.isInfinite(distance_threshold))) {
            throw new IllegalArgumentException("distance threshold must be a non-negative number");
        }
        final List<Placement> placements = res.getPlacements();
        if (placements.isEmpty()) {
            throw new IllegalArgumentException("no placements");
        }
        final Classification c = new Classification();
        c.topological(res, cutoff);
        c.lengths(placements);
        if (distance_threshold != null) {
            c.byDistance(res, cutoff, distance_threshold);
        }
        c._best_matches = Collections.unmodifiableList(c._best_matches);
        c._competing_subclades = Collections.unmodifiableList(c._competing_subclades);
        c._single_leaf_sisters = Collections.unmodifiableMap(c._single_leaf_sisters);
        return c;
    }

    private void topological(final ResultMulti res, final double cutoff) {
        final List<Placement> placements = res.getPlacements();
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
        if ((placements.size() == 1) && !placements.get(0).down().equals(AnalysisMulti.UNKNOWN)
                && !placements.get(0).up().equals(AnalysisMulti.UNKNOWN)) {
            _bracket_down = placements.get(0).down();
            _bracket_up = placements.get(0).up();
        }
        final List<Prefix> labeled_top = new ArrayList<>();
        for (final Prefix p : top) {
            if (!p.getPrefix().equals(AnalysisMulti.UNKNOWN) && reaches(p.getConfidence(), cutoff)) {
                labeled_top.add(p);
            }
        }
        if (!labeled_top.isEmpty() && !isTie(labeled_top)) {
            // walk down to the most specific clade that reaches the cutoff
            String x = labeled_top.get(0).getPrefix();
            while (true) {
                final List<Prefix> children = new ArrayList<>();
                final int child_level = levels(x, sep) + 1;
                for (final Map.Entry<String, Double> e : weights.entrySet()) {
                    if (ForesterUtil.isContainsPrefix(e.getKey(), x, sep) && (levels(e.getKey(), sep) == child_level)
                            && reaches(e.getValue(), cutoff)) {
                        children.add(new Prefix(e.getKey(), e.getValue(), sep));
                    }
                }
                if (children.isEmpty()) {
                    break;
                }
                sortByWeight(children);
                if (isTie(children)) {
                    _competing_subclades = tied(children);
                    break;
                }
                x = children.get(0).getPrefix();
            }
            double member = 0.0;
            double novel = 0.0;
            for (final Placement p : placements) {
                if (p.clade().equals(AnalysisMulti.UNKNOWN) || !ForesterUtil.isContainsPrefix(p.clade(), x, sep)) {
                    continue;
                }
                if (!p.clade().equals(x) || p.down().equals(p.up())) {
                    member += p.weight();
                } else {
                    novel += p.weight();
                    if (p.sisterIsSingleLeaf()) {
                        _single_leaf_sisters.merge(p.down(), p.weight(), Double::sum);
                    }
                }
            }
            final boolean is_novel = novel > member + TOLERANCE;
            _assignment = x;
            _confidence = weights.get(x);
            _conclusion = is_novel ? Conclusion.NOVEL_WITHIN : Conclusion.MEMBER;
            _conclusion_clade = x;
            _support = is_novel ? novel : member;
            if (!is_novel) {
                _single_leaf_sisters.clear();
            }
            return;
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
            _confidence = outside;
            if (!sister_list.isEmpty() && !isTie(sister_list)) {
                _conclusion = Conclusion.OUTSIDE_SISTER_TO;
                _conclusion_clade = sister_list.get(0).getPrefix();
                _support = sister_list.get(0).getConfidence();
            } else {
                _conclusion = Conclusion.OUTSIDE;
                _support = outside;
            }
            return;
        }
        _conclusion = Conclusion.NO_CONFIDENT_ASSIGNMENT;
        _best_matches = tied(top);
        _confidence = top.get(0).getConfidence();
        _support = top.get(0).getConfidence();
    }

    /** The weighted mean pendant length and nearest distance, and the nearest leaf of the heaviest placement. */
    private void lengths(final List<Placement> placements) {
        double pendant_sum = 0.0;
        double pendant_weight = 0.0;
        double nearest_sum = 0.0;
        double nearest_weight = 0.0;
        Placement heaviest = null;
        for (final Placement p : placements) {
            if (p.hasPendantLength()) {
                pendant_sum += p.weight() * p.pendantLength();
                pendant_weight += p.weight();
            }
            if (p.hasNearestDistance()) {
                nearest_sum += p.weight() * p.nearestDistance();
                nearest_weight += p.weight();
            }
            if ((p.nearestLeaf() != null) && ((heaviest == null) || (p.weight() > heaviest.weight()))) {
                heaviest = p;
            }
        }
        _pendant_length = (pendant_weight > 0.0) ? pendant_sum / pendant_weight : null;
        _nearest_distance = (nearest_weight > 0.0) ? nearest_sum / nearest_weight : null;
        _nearest_leaf = (heaviest != null) ? heaviest.nearestLeaf() : null;
    }

    private void byDistance(final ResultMulti res, final double cutoff, final double threshold) {
        final List<Placement> placements = res.getPlacements();
        final String sep = res.getSeparator();
        for (final Placement p : placements) {
            if (!p.hasNearestDistance()) {
                _distance_not_applied = true;
                return;
            }
        }
        // placements closer than the threshold to a reference leaf, by that leaf's label; the others are far
        final Map<String, Double> close = new TreeMap<>();
        double far = 0.0;
        for (final Placement p : placements) {
            if (p.nearestDistance() < threshold) {
                close.merge(p.nearestLeaf(), p.weight(), Double::sum);
            } else {
                far += p.weight();
            }
        }
        final List<Prefix> close_list = new ArrayList<>();
        for (final Map.Entry<String, Double> e : close.entrySet()) {
            close_list.add(new Prefix(e.getKey(), e.getValue(), sep));
        }
        sortByWeight(close_list);
        // the best-supported leaf's clade; leaves that tie count for their common clade, if they have one
        Prefix best_close = null;
        if (!close_list.isEmpty()) {
            if (!isTie(close_list)) {
                best_close = close_list.get(0);
            } else {
                final List<String> labels = new ArrayList<>();
                double weight = 0.0;
                for (final Prefix p : tied(close_list)) {
                    labels.add(p.getPrefix());
                    weight += p.getConfidence();
                }
                final String common = ForesterUtil.greatestCommonPrefix(labels, sep);
                if (!ForesterUtil.isEmpty(common)) {
                    best_close = new Prefix(common, weight, sep);
                }
            }
        }
        final double best_close_weight = (best_close != null) ? best_close.getConfidence() : 0.0;
        _by_distance = true;
        _single_leaf_sisters = new LinkedHashMap<>();
        if ((best_close != null) && (best_close_weight >= far - TOLERANCE)) {
            // a member of the nearest leaf's clade
            _conclusion = Conclusion.MEMBER;
            _conclusion_clade = best_close.getPrefix();
            _support = best_close_weight;
            if (reaches(best_close_weight, cutoff)) {
                _assignment = best_close.getPrefix();
                _confidence = best_close_weight;
                _competing_subclades = new ArrayList<>();
            } else if ((_assignment != null) && !ForesterUtil.isContainsPrefix(best_close.getPrefix(), _assignment, sep)) {
                _nearest_leaf_outside_clade = _assignment;
            }
        } else if (_assignment != null) {
            // farther than the threshold from every reference leaf: novel within the clade of the placements
            _conclusion = Conclusion.NOVEL_WITHIN;
            _conclusion_clade = _assignment;
            _support = far;
        }
        // with no assignment and no membership, the placements are outside all clades or unresolved: unchanged
    }

    private static int levels(final String prefix, final String sep) {
        return prefix.split(Pattern.quote(sep)).length;
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
     * For NOVEL_WITHIN by topology: the labels of single reference leaves the query is sister to, with the weight
     * of those placements. Membership in such a leaf's taxon cannot be excluded from the topology.
     */
    public Map<String, Double> getSingleLeafSisters() {
        return _single_leaf_sisters;
    }

    /** The weighted mean pendant branch length of the query, or null if the tree has no branch lengths. */
    public Double getPendantLength() {
        return _pendant_length;
    }

    /** The reference leaf nearest to the heaviest placement, or null if there is none. */
    public String getNearestLeaf() {
        return _nearest_leaf;
    }

    /** The weighted mean distance from the query to its nearest reference leaf, or null without branch lengths. */
    public Double getNearestDistance() {
        return _nearest_distance;
    }

    /** Whether the conclusion (member or novel) was decided by the distance threshold. */
    public boolean isByDistance() {
        return _by_distance;
    }

    /** Whether a distance threshold was given but could not be applied, because the tree has no branch lengths. */
    public boolean isDistanceNotApplied() {
        return _distance_not_applied;
    }

    /**
     * When the distance threshold makes the query a member of a leaf's clade that is not within the clade of its
     * placements: that clade; else null.
     */
    public String getNearestLeafOutsideClade() {
        return _nearest_leaf_outside_clade;
    }
}
