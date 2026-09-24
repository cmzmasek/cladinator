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

import java.io.*;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Locale;
import java.util.Map;
import java.util.List;
import java.util.SortedMap;
import java.util.function.UnaryOperator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.forester.io.parsers.PhylogenyParser;
import org.forester.io.parsers.util.ParserUtils;
import org.forester.phylogeny.Phylogeny;
import org.forester.phylogeny.PhylogenyNode;
import org.forester.phylogeny.factories.ParserBasedPhylogenyFactory;
import org.forester.phylogeny.factories.PhylogenyFactory;
import org.forester.util.BasicTable;
import org.forester.util.BasicTableParser;
import org.forester.util.CommandLineArguments;
import org.forester.util.EasyWriter;
import org.forester.util.ForesterUtil;
import org.forester.util.UserException;

public final class cladinator {

    final static private String PRG_NAME = "cladinator";
    final static private String PRG_VERSION = "3.3.0";
    final static private String PRG_DATE = "2026-09-24";
    final static private String PRG_DESC = "clades within clades of annotated labels -- analysis of pplacer-type outputs";
    final static private String E_MAIL = "czmasek at jcvi dot org";
    final static private String WWW = "https://github.com/cmzmasek/cladinator";
    final static private String HELP_OPTION_1 = "help";
    final static private String HELP_OPTION_2 = "h";
    final static private String SEP_OPTION = "s";
    final static private String QUERY_PATTERN_OPTION = "q";
    final static private String MAPPING_FILE_OPTION = "m";
    final static private String EXTRA_PROCESSING_OPTION1 = "x";
    final static private String EXTRA_PROCESSING1_SEP_OPTION = "xs";
    final static private String EXTRA_PROCESSING1_KEEP_EXTRA_OPTION = "xk";
    final static private String SPECIAL_PROCESSING_OPTION = "S";
    final static private String REMOVE_ANNOT_SEP_OPTION = "rs";
    final static private String SPLIT_QUERY_OPTION = "sq";
    final static private String CUTOFF_OPTION = "c";
    final static private String NON_HOMOLOGOUS_FACTOR_OPTION = "nh";
    final static private String DISTANCE_THRESHOLD_OPTION = "d";
    final static private double CUTOFF_DEFAULT = 0.7;
    final static private double NON_HOMOLOGOUS_FACTOR_DEFAULT = 2.0;
    final static private String QUERY_NAME_SPLIT_SEP = "_";
    final static private String SEP_DEFAULT = ".";
    final static private Pattern QUERY_PATTERN_DEFAULT = AnalysisMulti.DEFAULT_QUERY_PATTERN_FOR_PPLACER_TYPE;
    final static private String EXTRA_PROCESSING1_SEP_DEFAULT = "|";
    final static private boolean EXTRA_PROCESSING1_KEEP_EXTRA_DEFAULT = false;
    /** Confidences at or above cutoff - this tolerance pass, so that -c=1 is reachable despite rounding. */
    final static private double CUTOFF_TOLERANCE = 1E-9;
    /** The columns of the output table, in order. */
    final static private String[] COLUMNS = {"Tree #", "Query", "Assignment", "Confidence", "Brackets", "Conclusion",
            "Support", "Placement count", "Pendant length", "Reference depth", "Nearest leaf", "Nearest distance",
            "Clade confidences", "Down-tree confidences", "Up-tree confidences", "Warnings"};
    private final static DecimalFormat LENGTH_FORMAT = new DecimalFormat("0.0#####", DecimalFormatSymbols.getInstance(Locale.ROOT));
    final static private String NON_HOMOLOGOUS_QUERY_MESSAGE = "Input sequence error: Likely non-homologous query sequence";


    public static void main(final String args[]) {
        try {
            ForesterUtil.printProgramInformation(PRG_NAME, PRG_DESC, PRG_VERSION, PRG_DATE, E_MAIL, WWW, ForesterUtil.getForesterLibraryInformation());
            CommandLineArguments cla = null;
            try {
                cla = new CommandLineArguments(args);
            } catch (final Exception e) {
                ForesterUtil.fatalError(PRG_NAME, e.getMessage());
            }
            if (cla.isOptionSet(HELP_OPTION_1) || cla.isOptionSet(HELP_OPTION_2)) {
                System.out.println();
                print_help();
                System.exit(0);
            }
            if ((cla.getNumberOfNames() != 1) && (cla.getNumberOfNames() != 2)) {
                print_help();
                System.exit(-1);
            }
            final List<String> allowed_options = new ArrayList<>();
            allowed_options.add(SEP_OPTION);
            allowed_options.add(QUERY_PATTERN_OPTION);
            allowed_options.add(MAPPING_FILE_OPTION);
            allowed_options.add(EXTRA_PROCESSING_OPTION1);
            allowed_options.add(EXTRA_PROCESSING1_SEP_OPTION);
            allowed_options.add(EXTRA_PROCESSING1_KEEP_EXTRA_OPTION);
            allowed_options.add(SPECIAL_PROCESSING_OPTION);
            allowed_options.add(REMOVE_ANNOT_SEP_OPTION);
            allowed_options.add(SPLIT_QUERY_OPTION);
            allowed_options.add(CUTOFF_OPTION);
            allowed_options.add(NON_HOMOLOGOUS_FACTOR_OPTION);
            allowed_options.add(DISTANCE_THRESHOLD_OPTION);
            final String dissallowed_options = cla.validateAllowedOptionsAsString(allowed_options);
            if (dissallowed_options.length() > 0) {
                ForesterUtil.fatalError(PRG_NAME, "unknown option(s): " + dissallowed_options);
            }

            String separator = SEP_DEFAULT;
            if (cla.isOptionSet(SEP_OPTION)) {
                if (cla.isOptionValueSet(SEP_OPTION)) {
                    separator = cla.getOptionValue(SEP_OPTION);
                } else {
                    ForesterUtil.fatalError(PRG_NAME, "no value for separator option");
                }
            }
            Pattern compiled_query = null;
            if (cla.isOptionSet(QUERY_PATTERN_OPTION)) {
                if (cla.isOptionValueSet(QUERY_PATTERN_OPTION)) {
                    final String query_str = cla.getOptionValue(QUERY_PATTERN_OPTION);
                    try {
                        compiled_query = Pattern.compile(query_str);
                    } catch (final PatternSyntaxException e) {
                        ForesterUtil.fatalError(PRG_NAME, "error in regular expression: " + query_str + ": " + e.getMessage());
                    }
                } else {
                    ForesterUtil.fatalError(PRG_NAME, "no value for query pattern option");
                }
            }
            File mapping_file = null;
            if (cla.isOptionSet(MAPPING_FILE_OPTION)) {
                if (cla.isOptionValueSet(MAPPING_FILE_OPTION)) {
                    final String mapping_file_str = cla.getOptionValue(MAPPING_FILE_OPTION);
                    final String error = ForesterUtil.isReadableFile(mapping_file_str);
                    if (!ForesterUtil.isEmpty(error)) {
                        ForesterUtil.fatalError(PRG_NAME, error);
                    }
                    mapping_file = new File(mapping_file_str);
                } else {
                    ForesterUtil.fatalError(PRG_NAME, "no value for mapping file");
                }
            }
            final Pattern pattern = (compiled_query != null) ? compiled_query : QUERY_PATTERN_DEFAULT;
            final File intreefile = cla.getFile(0);
            final String error_intreefile = ForesterUtil.isReadableFile(intreefile);
            if (!ForesterUtil.isEmpty(error_intreefile)) {
                ForesterUtil.fatalError(PRG_NAME, error_intreefile);
            }
            final File outtablefile;
            if (cla.getNumberOfNames() > 1) {
                outtablefile = cla.getFile(1);
                final String error_outtablefile = ForesterUtil.isWritableFile(outtablefile);
                if (!ForesterUtil.isEmpty(error_outtablefile)) {
                    ForesterUtil.fatalError(PRG_NAME, error_outtablefile);
                }
            } else {
                outtablefile = null;
            }


            final BasicTable<String> t;
            final SortedMap<String, String> map;
            if (mapping_file != null) {
                t = BasicTableParser.parse(mapping_file, '\t');
                if (t.getNumberOfColumns() != 2) {
                    ForesterUtil.fatalError(PRG_NAME, "mapping file needs to have 2 tab-separated columns, not " + t.getNumberOfColumns());
                }
                map = t.getColumnsAsMap(0, 1);
            } else {
                t = null;
                map = null;
            }
            final boolean extra_processing1;
            if (cla.isOptionSet(EXTRA_PROCESSING_OPTION1)) {
                extra_processing1 = true;
            } else {
                extra_processing1 = false;
            }
            String extra_processing1_sep = EXTRA_PROCESSING1_SEP_DEFAULT;
            if (cla.isOptionSet(EXTRA_PROCESSING1_SEP_OPTION)) {
                if (!extra_processing1) {
                    ForesterUtil.fatalError(PRG_NAME, "extra processing is not enabled, cannot set -" + EXTRA_PROCESSING1_SEP_OPTION + " option");
                }
                if (cla.isOptionValueSet(EXTRA_PROCESSING1_SEP_OPTION)) {
                    extra_processing1_sep = cla.getOptionValue(EXTRA_PROCESSING1_SEP_OPTION);
                } else {
                    ForesterUtil.fatalError(PRG_NAME, "no value for extra processing separator");
                }
            }
            if (extra_processing1 && extra_processing1_sep.equals(separator)) {
                ForesterUtil.fatalError(PRG_NAME, "extra processing separator must not be the same as the annotation-separator");
            }
            boolean extra_processing1_keep = EXTRA_PROCESSING1_KEEP_EXTRA_DEFAULT;
            if (cla.isOptionSet(EXTRA_PROCESSING1_KEEP_EXTRA_OPTION)) {
                if (!extra_processing1) {
                    ForesterUtil.fatalError(PRG_NAME, "extra processing is not enabled, cannot set -" + EXTRA_PROCESSING1_KEEP_EXTRA_OPTION + " option");
                }
                extra_processing1_keep = true;
            }
            Pattern special_pattern = null;
            boolean special_processing = false;
            if (cla.isOptionSet(SPECIAL_PROCESSING_OPTION)) {
                if (extra_processing1) {
                    ForesterUtil.fatalError(PRG_NAME, "extra processing cannot be used together with special processing pattern");
                }
                if (cla.isOptionValueSet(SPECIAL_PROCESSING_OPTION)) {
                    final String str = cla.getOptionValue(SPECIAL_PROCESSING_OPTION);
                    try {
                        special_pattern = Pattern.compile(str);
                    } catch (final PatternSyntaxException e) {
                        ForesterUtil.fatalError(PRG_NAME, "error in special processing pattern: " + str + ": " + e.getMessage());
                    }
                    special_processing = true;
                } else {
                    ForesterUtil.fatalError(PRG_NAME, "no value for special processing pattern");
                }
            }
            final boolean remove_annotation_sep = cla.isOptionSet(REMOVE_ANNOT_SEP_OPTION);
            final boolean split_query = cla.isOptionSet(SPLIT_QUERY_OPTION);
            double cutoff = CUTOFF_DEFAULT;
            if (cla.isOptionSet(CUTOFF_OPTION)) {
                if (!cla.isOptionValueSet(CUTOFF_OPTION)) {
                    ForesterUtil.fatalError(PRG_NAME, "no value for confidence cutoff");
                }
                cutoff = cla.getOptionValueAsDouble(CUTOFF_OPTION);
                if (Double.isNaN(cutoff) || (cutoff <= 0) || (cutoff > 1)) {
                    ForesterUtil.fatalError(PRG_NAME, "confidence cutoff must be greater than 0 and at most 1");
                }
            }
            double nh_factor = NON_HOMOLOGOUS_FACTOR_DEFAULT;
            if (cla.isOptionSet(NON_HOMOLOGOUS_FACTOR_OPTION)) {
                if (!cla.isOptionValueSet(NON_HOMOLOGOUS_FACTOR_OPTION)) {
                    ForesterUtil.fatalError(PRG_NAME, "no value for non-homologous query factor");
                }
                nh_factor = cla.getOptionValueAsDouble(NON_HOMOLOGOUS_FACTOR_OPTION);
                if (Double.isNaN(nh_factor) || Double.isInfinite(nh_factor) || (nh_factor < 0)) {
                    ForesterUtil.fatalError(PRG_NAME, "non-homologous query factor must be a non-negative number (0 turns the check off)");
                }
            }

            Double distance_threshold = null;
            if (cla.isOptionSet(DISTANCE_THRESHOLD_OPTION)) {
                if (!cla.isOptionValueSet(DISTANCE_THRESHOLD_OPTION)) {
                    ForesterUtil.fatalError(PRG_NAME, "no value for distance threshold");
                }
                distance_threshold = cla.getOptionValueAsDouble(DISTANCE_THRESHOLD_OPTION);
                if (!(distance_threshold >= 0) || Double.isInfinite(distance_threshold)) {
                    ForesterUtil.fatalError(PRG_NAME, "distance threshold must be a non-negative number");
                }
            }

            final String sep = separator;
            final UnaryOperator<String> label = remove_annotation_sep ? (name -> name.replace(sep, "")) : (name -> name);
            final Settings settings = new Settings(pattern, separator, map, extra_processing1, extra_processing1_sep,
                    extra_processing1_keep, special_processing, special_pattern, label, split_query, cutoff, nh_factor,
                    distance_threshold);

            System.out.println("Input tree                 : " + intreefile);
            if (mapping_file != null) {
                System.out.println("Mapping file               : " + mapping_file + " (" + t.getNumberOfRows() + " rows)");
            }
            System.out.println("Annotation-separator       : " + separator);
            if (remove_annotation_sep) {
                System.out.println("Remove anno.-sep. in output: " + remove_annotation_sep);
            }
            if (split_query) {
                System.out.println("Split query names at \"" + QUERY_NAME_SPLIT_SEP + "\": " + split_query);
            }
            System.out.println("Query pattern              : " + pattern);
            if (extra_processing1) {
                System.out.println("Extra processing           : " + extra_processing1);
                System.out.println("Extra processing separator : " + extra_processing1_sep);
                System.out.println("Keep extra annotations     : " + extra_processing1_keep);
            }
            if (special_processing) {
                System.out.println("Special processing         : " + special_processing);
                System.out.println("Special processing pattern : " + special_pattern);
            }
            System.out.println("Confidence cutoff          : " + cutoff);
            if (distance_threshold != null) {
                System.out.println("Distance threshold         : " + distance_threshold);
            }
            if (nh_factor > 0) {
                System.out.println("Non-homologous query factor: " + nh_factor);
            } else {
                System.out.println("Non-homologous query check : off");
            }
            if (outtablefile != null) {
                System.out.println("Output table               : " + outtablefile);
            }

            Phylogeny phys[] = null;
            try {
                final PhylogenyFactory factory = ParserBasedPhylogenyFactory.getInstance();
                final PhylogenyParser pp = ParserUtils.createParserDependingOnFileType(intreefile, true);
                phys = factory.create(intreefile, pp);
            } catch (final IOException e) {
                ForesterUtil.fatalError(PRG_NAME, "Could not read \"" + intreefile + "\" [" + e.getMessage() + "]");
            }
            if (phys.length == 0) {
                ForesterUtil.fatalError(PRG_NAME, "\"" + intreefile + "\" does not contain any trees");
            }
            System.out.println("Number of input trees      : " + phys.length);
            if (phys.length == 1) {
                System.out.println("Ext. nodes in input tree   : " + phys[0].getNumberOfExternalNodes());
            }
            final List<BufferedWriter> writers = new ArrayList<>();
            final EasyWriter outtable_writer = (outtablefile != null) ? ForesterUtil.createEasyWriter(outtablefile) : null;
            if (outtable_writer != null) {
                writers.add(outtable_writer);
            }
            // not closed at the end: closing it would close System.out
            writers.add(new BufferedWriter(new PrintWriter(System.out)));
            System.out.println();
            System.out.println("Results:");
            System.out.println();
            final StringBuilder header = new StringBuilder();
            header.append("# " + PRG_NAME + " " + PRG_VERSION + " (" + PRG_DATE + ")\n");
            header.append("# input trees: " + intreefile + "\n");
            if (mapping_file != null) {
                header.append("# mapping file: " + mapping_file + "\n");
            }
            header.append("# annotation separator: " + separator + "\n");
            header.append("# query pattern: " + pattern + "\n");
            header.append("# confidence cutoff: " + cutoff + "\n");
            if (distance_threshold != null) {
                header.append("# distance threshold: " + distance_threshold + "\n");
            }
            header.append("# non-homologous query factor: " + (nh_factor > 0 ? String.valueOf(nh_factor) : "off") + "\n");
            if (extra_processing1) {
                header.append("# extra processing: separator \"" + extra_processing1_sep + "\", keep extra: " + extra_processing1_keep + "\n");
            }
            if (special_processing) {
                header.append("# special processing pattern: " + special_pattern + "\n");
            }
            if (remove_annotation_sep) {
                header.append("# annotation separator removed from clade names in this table\n");
            }
            if (split_query) {
                header.append("# query names split at \"" + QUERY_NAME_SPLIT_SEP + "\"\n");
            }
            header.append("#" + String.join("\t", COLUMNS) + "\n");
            emit(writers, header.toString());
            int counter = 0;
            int input_errors = 0;
            int non_homologous = 0;
            for (final Phylogeny phy : phys) {
                ++counter;
                String rows;
                try {
                    rows = analyzeTree(phy, counter, settings);
                } catch (final TreeProblem e) {
                    // A problem with this tree only: report it in its row(s) and go on with the next tree.
                    if (e.input_error) {
                        ++input_errors;
                    } else {
                        ++non_homologous;
                    }
                    rows = "";
                    for (final String query : queryNames(e.query_name, split_query)) {
                        rows += errorRow(counter, query, e.getMessage(), e.placements);
                    }
                }
                emit(writers, rows);
            }
            for (final BufferedWriter w : writers) {
                w.flush();
            }
            if (outtable_writer != null) {
                outtable_writer.close();
            }
            System.out.println();
            System.out.println("Trees: " + counter + ", with result: " + (counter - input_errors - non_homologous)
                    + ", likely non-homologous query: " + non_homologous + ", input errors: " + input_errors);
            if ((counter > 0) && (input_errors == counter)) {
                ForesterUtil.fatalError(PRG_NAME, "no tree could be analyzed (" + input_errors + " input error(s), see the table)");
            }
        } catch (final IOException e) {
            ForesterUtil.fatalError(PRG_NAME, e.getMessage());
        } catch (final Exception e) {
            e.printStackTrace();
            ForesterUtil.fatalError(PRG_NAME, "Unexpected error!");
        }
    }

    /** The settings of a run that the per-tree analysis and the output need. */
    private record Settings(Pattern pattern,
                            String separator,
                            SortedMap<String, String> map,
                            boolean extra_processing1,
                            String extra_processing1_sep,
                            boolean extra_processing1_keep,
                            boolean special_processing,
                            Pattern special_pattern,
                            /** How clade names are printed (identity, or with -rs without the separator). */
                            UnaryOperator<String> label,
                            boolean split_query,
                            /** Minimum summed placement confidence for a clade to be assigned. */
                            double cutoff,
                            /** Factor for the non-homologous query check; 0 turns the check off. */
                            double nh_factor,
                            /** Distance to the nearest reference leaf that separates member from novel; null: by topology. */
                            Double distance_threshold) {
    }

    /** A tree that gets an error row instead of a result: an input error, or a likely non-homologous query. */
    private static final class TreeProblem extends Exception {
        final String query_name;
        final int placements;
        final boolean input_error;

        TreeProblem(final String message, final String query_name, final int placements, final boolean input_error) {
            super(message);
            this.query_name = query_name;
            this.placements = placements;
            this.input_error = input_error;
        }
    }

    private static void emit(final List<BufferedWriter> writers, final String text) throws IOException {
        for (final BufferedWriter w : writers) {
            w.write(text);
            w.flush(); // keeps the console in order with what the analysis prints directly
        }
    }

    /** Analyzes one tree and returns its row(s) of the table. */
    private static String analyzeTree(final Phylogeny phy, final int counter, final Settings st) throws TreeProblem {
        final Pattern pattern = st.pattern();
        final List<PhylogenyNode> query_nodes = phy.getNodes(pattern); // null for an empty tree
        if ((query_nodes == null) || query_nodes.isEmpty()) {
            throw new TreeProblem("Input error: no query found (query pattern: " + pattern + ")", "", 0, true);
        }
        final String query_name = queryNamePrefix(query_nodes.get(0), pattern);
        try {
            if (st.map() != null) {
                AnalysisMulti.performMapping(pattern, st.map(), phy, true);
            }
            if (st.extra_processing1()) {
                AnalysisMulti.performExtraProcessing1(pattern, phy, st.extra_processing1_sep(), st.extra_processing1_keep(), st.separator(), true);
            } else if (st.special_processing()) {
                AnalysisMulti.performSpecialProcessing1(pattern, phy, st.separator(), st.special_pattern(), true);
            }
            if ((st.nh_factor() > 0) && AnalysisMulti.likelyProblematicQuery(phy, pattern, st.nh_factor())) {
                throw new TreeProblem(NON_HOMOLOGOUS_QUERY_MESSAGE, query_name, query_nodes.size(), false);
            }
            final ResultMulti res = AnalysisMulti.execute(phy, pattern, st.separator());
            return resultRows(res, counter, st);
        } catch (final UserException e) {
            throw new TreeProblem("Input error: " + e.getMessage(), query_name, query_nodes.size(), true);
        }
    }

    /** The query name: the part of a query node's name before the query pattern. */
    private static String queryNamePrefix(final PhylogenyNode query_node, final Pattern pattern) {
        final String name = query_node.getName();
        final Matcher m = pattern.matcher(name);
        return m.find() ? name.substring(0, m.start()) : name;
    }

    /** The names to print for a query: the name itself, or with -sq its "_"-separated parts, one row each. */
    private static String[] queryNames(final String query_name, final boolean split_query) {
        return split_query ? query_name.split(QUERY_NAME_SPLIT_SEP) : new String[]{query_name};
    }

    private static int column(final String name) {
        return Arrays.asList(COLUMNS).indexOf(name);
    }

    /** The row(s) for a result: the classification of the query at the cutoff, one row per query name. */
    private static String resultRows(final ResultMulti res, final int counter, final Settings st) {
        final Classification c = Classification.of(res, st.cutoff(), st.distance_threshold());
        final StringBuilder rows = new StringBuilder();
        for (final String query : queryNames(res.getQueryNamePrefix(), st.split_query())) {
            rows.append(row(counter, query, c, res, st.label()));
        }
        return rows.toString();
    }

    private static String errorRow(final int counter, final String query, final String message, final int placements) {
        final String[] cells = new String[COLUMNS.length];
        Arrays.fill(cells, "");
        cells[column("Tree #")] = String.valueOf(counter);
        cells[column("Query")] = query;
        cells[column("Conclusion")] = message;
        cells[column("Placement count")] = String.valueOf(placements);
        return String.join("\t", cells) + "\n";
    }

    /** All prefixes of a list with their confidences, e.g. "A:1.0;A.1:0.9;A.2:0.1". */
    private static String confidences(final List<Prefix> prefixes, final UnaryOperator<String> label) {
        final StringBuilder sb = new StringBuilder();
        for (final Prefix p : prefixes) {
            if (sb.length() > 0) {
                sb.append(";");
            }
            sb.append(label.apply(p.getPrefix())).append(":").append(Prefix.CONFIDENCE_FORMAT.format(p.getConfidence()));
        }
        return sb.toString();
    }

    private static String row(final int counter, final String query, final Classification c, final ResultMulti res, final UnaryOperator<String> label) {
        final String[] cells = new String[COLUMNS.length];
        Arrays.fill(cells, "");
        cells[column("Tree #")] = String.valueOf(counter);
        cells[column("Query")] = query;
        cells[column("Assignment")] = (c.getAssignment() != null) ? label.apply(c.getAssignment()) : "";
        cells[column("Confidence")] = Prefix.CONFIDENCE_FORMAT.format(c.getConfidence());
        cells[column("Brackets")] = (c.getBracketDown() != null)
                ? "[" + label.apply(c.getBracketDown()) + ", " + label.apply(c.getBracketUp()) + "]" : "n/a";
        cells[column("Conclusion")] = conclusionText(c, label);
        cells[column("Support")] = Prefix.CONFIDENCE_FORMAT.format(c.getSupport());
        cells[column("Placement count")] = String.valueOf(res.getNumberOfMatches());
        cells[column("Pendant length")] = (c.getPendantLength() != null) ? LENGTH_FORMAT.format(c.getPendantLength()) : "";
        cells[column("Reference depth")] = (res.getReferenceDepth() != null) ? LENGTH_FORMAT.format(res.getReferenceDepth()) : "";
        cells[column("Nearest leaf")] = (c.getNearestLeaf() != null) ? label.apply(c.getNearestLeaf()) : "";
        cells[column("Nearest distance")] = (c.getNearestDistance() != null) ? LENGTH_FORMAT.format(c.getNearestDistance()) : "";
        cells[column("Clade confidences")] = confidences(res.getAllMultiHitPrefixes(), label);
        cells[column("Down-tree confidences")] = confidences(res.getAllMultiHitPrefixesDown(), label);
        cells[column("Up-tree confidences")] = confidences(res.getAllMultiHitPrefixesUp(), label);
        final List<String> warnings = new ArrayList<>(res.getWarnings());
        warnings.addAll(notes(c, label));
        cells[column("Warnings")] = String.join("; ", warnings);
        return String.join("\t", cells) + "\n";
    }

    private static String conclusionText(final Classification c, final UnaryOperator<String> label) {
        final String by_distance = c.isByDistance() ? " (by distance)" : "";
        switch (c.getConclusion()) {
            case MEMBER:
                return "member of clade " + label.apply(c.getConclusionClade()) + by_distance;
            case NOVEL_WITHIN:
                return "potential for novel sub-species within clade " + label.apply(c.getConclusionClade()) + by_distance;
            case OUTSIDE_SISTER_TO:
                return "outside all clades, sister to clade " + label.apply(c.getConclusionClade());
            case OUTSIDE:
                return "outside all clades";
            case NO_CONFIDENT_ASSIGNMENT:
            default:
                final List<String> best = new ArrayList<>();
                for (final Prefix p : c.getBestMatches()) {
                    best.add((p.getPrefix().equals(AnalysisMulti.UNKNOWN) ? "outside all clades" : "clade " + label.apply(p.getPrefix()))
                            + " " + Prefix.CONFIDENCE_FORMAT.format(p.getConfidence()));
                }
                return "no confident assignment (" + (best.size() == 1 ? "best match: " : "tie: ") + String.join(", ", best) + ")";
        }
    }

    private static List<String> notes(final Classification c, final UnaryOperator<String> label) {
        final List<String> notes = new ArrayList<>();
        if (!c.getCompetingSubclades().isEmpty()) {
            final List<String> l = new ArrayList<>();
            for (final Prefix p : c.getCompetingSubclades()) {
                l.add(label.apply(p.getPrefix()) + " " + Prefix.CONFIDENCE_FORMAT.format(p.getConfidence()));
            }
            notes.add("sub-clades of " + label.apply(c.getAssignment()) + " tie at the cutoff: " + String.join(", ", l));
        }
        if (c.isDistanceNotApplied()) {
            notes.add("no branch lengths: the distance threshold was not applied");
        }
        if (c.getNearestLeafOutsideClade() != null) {
            notes.add("the nearest reference leaf (" + label.apply(c.getConclusionClade()) + ") is not within the clade of the placements ("
                    + label.apply(c.getNearestLeafOutsideClade()) + ")");
        }
        if (!c.getSingleLeafSisters().isEmpty()) {
            final List<String> l = new ArrayList<>();
            for (final Map.Entry<String, Double> e : c.getSingleLeafSisters().entrySet()) {
                l.add(label.apply(e.getKey()) + ": " + Prefix.CONFIDENCE_FORMAT.format(e.getValue()));
            }
            if (l.size() == 1) {
                notes.add("sister to a single reference leaf (" + l.get(0) + "): membership in "
                        + label.apply(c.getSingleLeafSisters().keySet().iterator().next()) + " cannot be excluded");
            } else {
                notes.add("sister to single reference leaves (" + String.join(", ", l)
                        + "): membership in one of them cannot be excluded");
            }
        }
        return notes;
    }

    private final static void print_help() {
        System.out.println("Usage:");
        System.out.println();
        System.out.println(PRG_NAME + " [options] <input tree(s) file> [output table file]");
        System.out.println();
        System.out.println(" options:");
        System.out.println("  -" + SEP_OPTION + "=<separator>     : the annotation-separator to be used (default: \"" + SEP_DEFAULT + "\")");
        System.out.println("  -" + MAPPING_FILE_OPTION + "=<mapping table> : to map node names to appropriate annotations (tab-separated, two columns) (default: no mapping)");
        System.out.println("  -" + EXTRA_PROCESSING_OPTION1 + "                 : to enable extra processing of annotations (e.g. \"Q16611|A.1.1\" becomes \"A.1.1\")");
        System.out.println("  -" + EXTRA_PROCESSING1_SEP_OPTION + "=<separator>    : the separator for extra annotations (default: \"" + EXTRA_PROCESSING1_SEP_DEFAULT + "\")");
        System.out.println("  -" + EXTRA_PROCESSING1_KEEP_EXTRA_OPTION + "                : to keep extra annotations (e.g. \"Q16611|A.1.1\" becomes \"A.1.1.Q16611\")");
        System.out.println("  -" + SPECIAL_PROCESSING_OPTION + "=<pattern>       : special processing with pattern (e.g. \"(\\d+)([a-z]+)_.+\" for changing \"6q_EF42\" to \"6.q\")");
        System.out.println("  -" + REMOVE_ANNOT_SEP_OPTION + "                : to remove the annotation-separator in the output (e.g. the \"" + SEP_DEFAULT + "\")");
        System.out.println("  -" + SPLIT_QUERY_OPTION + "                : to split query names at \"" + QUERY_NAME_SPLIT_SEP + "\" and print one row per part (e.g. \"S1_S2\" gives rows for S1 and S2)");
        System.out.println("  -" + CUTOFF_OPTION + "=<cutoff>        : minimum summed placement confidence for assigning a clade (default: " + CUTOFF_DEFAULT + ")");
        System.out.println("  -" + NON_HOMOLOGOUS_FACTOR_OPTION + "=<factor>       : a query is reported as likely non-homologous when all its placements are at least <factor> times");
        System.out.println("                      as far from the root as the farthest reference leaf (default: " + NON_HOMOLOGOUS_FACTOR_DEFAULT + ", 0 turns the check off)");
        System.out.println("  -" + DISTANCE_THRESHOLD_OPTION + "=<distance>      : decide member vs. novel by distance: a query closer than <distance> to a reference leaf is a member of");
        System.out.println("                      that leaf's clade, one farther from every reference leaf is a novel lineage (default: by topology)");
        System.out.println("  -" + QUERY_PATTERN_OPTION + "=<pattern>       : expert option: the regular expression pattern for the query (default: \"" + QUERY_PATTERN_DEFAULT + "\" for pplacer output)");
        System.out.println();
        System.out.println("Examples:");
        System.out.println();
        System.out.println(" " + PRG_NAME + " pp_out_tree.sing.tre result.tsv");
        System.out.println(" " + PRG_NAME + " -s=. pp_out_tree.sing.tre result.tsv");
        System.out.println(" " + PRG_NAME + " -s=_ -m=map.tsv pp_out_trees.sing.tre result.tsv");
        System.out.println(" " + PRG_NAME + " -x -xs=& -xk pp_out_trees.sing.tre result.tsv");
        System.out.println(" " + PRG_NAME + " -x -xs=\"|\" pp_out_trees.sing.tre result.tsv");
        System.out.println(" " + PRG_NAME + " -x -xk -m=map.tsv pp_out_trees.sing.tre result.tsv");
        System.out.println(" " + PRG_NAME + " -m=map.tsv -S='(\\d+)([a-z?]*)_.+' pp_out_trees.sing.tre result.tsv");
        System.out.println();
    }
}
