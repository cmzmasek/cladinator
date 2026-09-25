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
import java.util.List;
import java.util.Map;

/**
 * What the reference leaf labels of a tree look like after the label processing: whether they are clade
 * annotations that leaves share, or labels of their own (e.g. sequence identifiers, because a mapping file,
 * the separator or the extra processing is missing or wrong).
 *
 * @param topLevelCounts the reference leaves per top-level label (the part before the first separator), most
 *                       first
 * @param minLevels      the smallest number of levels of a label (0 without leaves)
 * @param maxLevels      the largest number of levels of a label
 */
public record LabelStatistics(Map<String, Integer> topLevelCounts, int minLevels, int maxLevels) {

    /** What to check when the labels are not clade annotations. */
    public final static String HINT = "check the annotation separator (-s), the mapping file (-m) and the extra processing (-x)";

    /** The number of reference leaves. */
    public int leaves() {
        int n = 0;
        for (final int c : topLevelCounts.values()) {
            n += c;
        }
        return n;
    }

    /** The number of distinct top-level labels. */
    public int distinctTopLevel() {
        return topLevelCounts.size();
    }

    /** The number of leaves whose top-level label no other leaf has. */
    public int leavesAlone() {
        int n = 0;
        for (final int c : topLevelCounts.values()) {
            if (c == 1) {
                ++n;
            }
        }
        return n;
    }

    /** No two leaves share a top-level label: nothing can be classified beyond "sister to" a leaf. */
    public boolean noneShared() {
        final int leaves = leaves();
        return (leaves > 0) && (distinctTopLevel() == leaves);
    }

    /** More than half of the leaves have a top-level label of their own. */
    public boolean mostAlone() {
        return (leaves() > 0) && (2 * leavesAlone() > leaves());
    }

    /**
     * The warning for labels that do not look like clade annotations: one for {@link #noneShared()}, a milder
     * one for {@link #mostAlone()}; null if the labels look fine.
     */
    public String problem() {
        final int leaves = leaves();
        if (noneShared()) {
            final List<String> examples = new ArrayList<>();
            for (final String label : topLevelCounts.keySet()) {
                if (examples.size() == 2) {
                    break;
                }
                examples.add("\"" + label + "\"");
            }
            return "every reference leaf has a label of its own (" + leaves + (leaves == 1 ? " leaf, " : " leaves, ")
                    + distinctTopLevel() + " distinct top-level " + (leaves == 1 ? "label" : "labels") + ", e.g. "
                    + String.join(", ", examples) + "): the clade annotations are not being read; " + HINT;
        }
        if (mostAlone()) {
            return "most reference leaves have a label of their own (" + leavesAlone() + " of " + leaves + "): " + HINT;
        }
        return null;
    }

    /** E.g. "21 reference leaves, 3 top-level clades (A: 12, B: 8, C: 1), 1-4 label levels". */
    public String summary() {
        final int leaves = leaves();
        final int distinct = distinctTopLevel();
        final List<String> clades = new ArrayList<>();
        int i = 0;
        for (final Map.Entry<String, Integer> e : topLevelCounts.entrySet()) {
            if (i++ == 8) {
                clades.add("...");
                break;
            }
            clades.add(e.getKey() + ": " + e.getValue());
        }
        return leaves + " reference " + (leaves == 1 ? "leaf" : "leaves") + ", " + distinct + " top-level "
                + (distinct == 1 ? "clade" : "clades") + " (" + String.join(", ", clades) + "), "
                + (minLevels == maxLevels ? String.valueOf(minLevels) : minLevels + "-" + maxLevels) + " label "
                + (maxLevels == 1 ? "level" : "levels");
    }
}
