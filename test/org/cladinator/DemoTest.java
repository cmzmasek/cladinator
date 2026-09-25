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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

import org.forester.io.parsers.nhx.NHXParser;
import org.forester.phylogeny.Phylogeny;
import org.forester.phylogeny.factories.ParserBasedPhylogenyFactory;
import org.junit.jupiter.api.Test;

import org.cladinator.Classification.Conclusion;

/** The demo cases conclude as they are meant to, so that -demo stays a valid demo and a valid check. */
class DemoTest {

    private static final Pattern QUERY = AnalysisMulti.DEFAULT_QUERY_PATTERN_FOR_PPLACER_TYPE;
    private static final String SEP = ".";

    /** What a case gives when run as cladinator runs it. */
    private record Run(LabelStatistics stats, ResultMulti result, Classification classification) {
    }

    private static Run run(final Demo.Case c) throws Exception {
        final Phylogeny phy = ParserBasedPhylogenyFactory.getInstance().create(c.newick(), new NHXParser())[0];
        if (c.extraProcessing()) {
            AnalysisMulti.performExtraProcessing1(QUERY, phy, "|", false, SEP, false);
        }
        final LabelStatistics stats = AnalysisMulti.prepare(phy, QUERY, SEP);
        final ResultMulti res = AnalysisMulti.execute(phy, QUERY, SEP, stats);
        return new Run(stats, res, Classification.of(res, c.cutoff(), c.distanceThreshold()));
    }

    @Test
    void everyCaseConcludesAsMeant() throws Exception {
        for (final Demo.Case c : Demo.CASES) {
            final Classification cl = run(c).classification();
            assertEquals(c.expected(), cl.getConclusion(), c.title());
            assertEquals(c.expectedClade(), cl.getConclusionClade(), c.title());
        }
    }

    @Test
    void casesAreDistinctAndCoverEveryConclusion() {
        final Set<Conclusion> seen = EnumSet.noneOf(Conclusion.class);
        final Set<String> titles = new HashSet<>();
        for (final Demo.Case c : Demo.CASES) {
            seen.add(c.expected());
            assertTrue(titles.add(c.title()), "title used twice: " + c.title());
            assertFalse(c.shows().isBlank(), c.title());
        }
        assertEquals(EnumSet.allOf(Conclusion.class), seen);
    }

    /** The edge cases show the feature their options or labels are there for. */
    @Test
    void edgeCasesShowTheirFeature() throws Exception {
        int by_distance = 0;
        int ties = 0;
        int identifiers_unread = 0;
        int single_leaf = 0;
        int unrooted = 0;
        int rescaled = 0;
        for (final Demo.Case c : Demo.CASES) {
            final Run r = run(c);
            if (c.distanceThreshold() != null) {
                assertTrue(r.classification().isByDistance(), c.title());
                ++by_distance;
            }
            if (c.cutoff() < 0.7) {
                assertFalse(r.classification().getCompetingSubclades().isEmpty(), c.title());
                ++ties;
            }
            if (c.newick().contains("|") && !c.extraProcessing()) {
                assertTrue(r.stats().noneShared(), c.title());
                ++identifiers_unread;
            } else {
                assertFalse(r.stats().noneShared(), c.title());
                assertFalse(r.stats().mostAlone(), c.title());
            }
            if (!r.classification().getSingleLeafSisters().isEmpty()) {
                ++single_leaf;
            }
            for (final String w : r.result().getWarnings()) {
                if (w.contains("unrooted")) {
                    ++unrooted;
                }
                if (w.contains("rescaled")) {
                    ++rescaled;
                }
            }
        }
        assertEquals(1, by_distance);
        assertEquals(1, ties);
        assertEquals(1, identifiers_unread);
        assertEquals(1, single_leaf);
        assertEquals(1, unrooted);
        assertEquals(1, rescaled);
    }
}
