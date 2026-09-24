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

/**
 * One placement of the query in the tree.
 *
 * @param weight             the placement confidence, rescaled so that the placements of a query add up to 1
 * @param clade              the label prefix shared by all reference leaves of the clade the placement edge lies
 *                           in (the query is within this clade); {@link AnalysisMulti#UNKNOWN} if there is none
 * @param down               the label prefix of the sister clade below the placement edge (the query is sister to
 *                           it); {@link AnalysisMulti#UNKNOWN} if there is none
 * @param up                 the label prefix of the rest of {@code clade} above the placement edge;
 *                           {@link AnalysisMulti#UNKNOWN} if there is none
 * @param sisterIsSingleLeaf whether the sister clade is a single reference leaf
 * @param pendantLength      the branch length of the query node; negative if the tree has none
 * @param nearestLeaf        the label of the reference leaf closest to the query by path length (the first of
 *                           equals); null if there is no reference leaf
 * @param nearestDistance    the path length to that leaf; negative if the tree has no branch lengths
 */
public record Placement(double weight,
                        String clade,
                        String down,
                        String up,
                        boolean sisterIsSingleLeaf,
                        double pendantLength,
                        String nearestLeaf,
                        double nearestDistance) {

    public boolean hasPendantLength() {
        return pendantLength >= 0.0;
    }

    public boolean hasNearestDistance() {
        return (nearestLeaf != null) && (nearestDistance >= 0.0);
    }
}
