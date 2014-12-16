/*
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package dk.statsbiblioteket.netark.dvenabler;

import org.apache.commons.cli.*;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.index.FieldInfo;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Command-line interface for DVEnabler.
 */
public class Command {
    public static final String HELP =    "help";
    public static final String VERBOSE = "verbose";
    public static final String INPUT =   "input";
    public static final String OUTPUT =  "output";
    public static final String LIST =    "list";
    public static final String CONVERT = "convert";
    public static final String FIELDS =  "fields";
    
    @SuppressWarnings("CallToPrintStackTrace")
    public static void main(String[] args) throws IOException {
        CommandLine cli;
        try {
            cli = new GnuParser().parse(getOptions(), args);
        } catch (ParseException e) {
            System.err.println("Exception parsing arguments");
            e.printStackTrace();
            usage();
            return;
        }

        if (cli.hasOption(HELP)) {
            usage();
            return;
        }

        if (!cli.hasOption(INPUT)) {
            System.err.println("input index must be specified");
            usage();
            return;
        }

        final File in = new File(cli.getOptionValue(INPUT));
        if (!in.exists() || !in.isDirectory()) {
            System.err.println("Unable to access index folder '" + in + "'");
            usage();
            return;
        }

        final boolean verbose = cli.hasOption(VERBOSE);
        if (cli.hasOption(LIST)) {
            list(in, verbose);
            return;
        }

        if (cli.hasOption(CONVERT)) {
            if (!cli.hasOption(OUTPUT)) {
                System.err.println("convert is specified but output is missing");
                usage();
                return;
            }
            final File out = new File(cli.getOptionValue(OUTPUT));
            if (!cli.hasOption(FIELDS)) {
                System.err.println("convert is specified but no fields are defined.\n" +
                                   "Use '.' to signal that no fields should be changed");
                usage();
                return;
            }
            final String[] rawFields = cli.getOptionValues(FIELDS);
            List<DVConfig> dvFields = getTweakedFields(in, rawFields);
            if (dvFields == null) {
                System.err.println("Invalid field specification");
                usage();
                return;
            }
            convert(in, out, dvFields, verbose);
            return;
        }

        System.out.println("Nothing to do");
        usage();
    }

    private static void convert(File in, File out, List<DVConfig> dvFields, boolean verbose) throws IOException {
        if (verbose) {
            System.out.println(String.format("Adjusting from %s to %s with adjustment fields", in, out));
            for (DVConfig dvConfig: dvFields) {
                System.out.println(dvConfig.toString(true));
            }
        } else {
            System.out.println(String.format("Adjusting from %s to %s with %d adjustment fields",
                                             in, out, dvFields.size()));
        }
        long processTime = -System.nanoTime();
        IndexUtils.convert(in, out, dvFields);
        processTime += System.nanoTime();
        System.out.println("Finished conversion successfully in " + (processTime/1000000/1000) + " seconds");
    }

    private static List<DVConfig> getTweakedFields(File index, String[] rawFields) throws IOException {
        List<DVConfig> config = new ArrayList<DVConfig>();
        if (".".equals(rawFields[0])) {
            System.out.println("'.' specified for field: No fields will be adjusted");
            return config;
        }
        List<DVConfig> dvConfigs = IndexUtils.getDVConfigs(index);
        List<DVConfig> adjustedConfigs = new ArrayList<>(rawFields.length);
        raw:
        for (String rawField: rawFields) {
            String[] tokens = rawField.replace(")", "").split("[(]", 2);
            if (tokens.length == 1) {
                System.err.println("No docvalues type specified for '" + rawField + "'");
                return null;
            }
            final String field = tokens[0];
            String docValuesType = tokens[1]; // NUMERIC(INT))

            for (DVConfig dvConfig: dvConfigs) {
                if (field.equals(dvConfig.getName())) {
                    String[] numTokens = docValuesType.split("[(]", 2);
                    try {
                        if ("NONE".equals(numTokens[0])) {
                            dvConfig.set(null);
                            adjustedConfigs.add(dvConfig);
                            continue raw;
                        }
                        final FieldInfo.DocValuesType dvType = FieldInfo.DocValuesType.valueOf(numTokens[0]);
                        if (FieldInfo.DocValuesType.NUMERIC != dvType) {
                            dvConfig.set(dvType);
                        } else {
                            try {
                                final FieldType.NumericType numType = FieldType.NumericType.valueOf(numTokens[1]);
                                dvConfig.set(dvType, numType);
                            } catch (IllegalArgumentException e) {
                                System.err.println("The numeric type '" + numTokens[1] + "' is invalid");
                                return null;
                            }
                        }
                        adjustedConfigs.add(dvConfig);
                    } catch (IllegalArgumentException e) {
                        System.err.println("The DocValueType '" + numTokens[0] + " for field "
                                           + dvConfig.getName() + " is unknown");
                        return null;
                    }
                    continue raw;
                }
            }
            System.err.println("The field '" + field + "' could not be located in the index. Ignoring field");
        }
        return adjustedConfigs;
    }

    private static void list(File index, boolean verbose) throws IOException {
        List<DVConfig> dvConfigs = IndexUtils.getDVConfigs(index);
        System.out.println("Lucene index at " + index + " contains " + dvConfigs.size() + " fields");
        for (DVConfig dvConfig: dvConfigs) {
            System.out.println(dvConfig.toString(verbose));
        }
    }

    private static Options getOptions() {
        Options options = new Options();
        options.addOption("h", HELP, false, "Print help message and exit");
        options.addOption("v", VERBOSE, false, "Enable verbose output");
        options.addOption("l", LIST, false, "Lists fields in index");
        options.addOption("c", CONVERT, false, "Convert index with the given field adjustments");
        {
            Option iOption = new Option(
                    "f", FIELDS, true,
                    "The fields to adjust.\n"
                    + "entry: fieldname(docvaluetype)\n"
                    + "docvaluetype: "
                    + "NONE" + " | "
                    + FieldInfo.DocValuesType.NUMERIC + "(numerictype) | "
                    + FieldInfo.DocValuesType.BINARY + " | "
                    + FieldInfo.DocValuesType.SORTED + " | "
                    + FieldInfo.DocValuesType.SORTED_SET + "\n"
                    + "numerictype: "
                    + FieldType.NumericType.INT + " | "
                    + FieldType.NumericType.LONG + " | "
                    + FieldType.NumericType.FLOAT + " | "
                    + FieldType.NumericType.DOUBLE + "\n"
                    + "Sample: title(SORTED) year(NUMERIC(INT)) author(SORTED_SET) deprecated(NONE)");
            iOption.setArgs(Option.UNLIMITED_VALUES);
            options.addOption(iOption);
        }
        {
            Option iOption = new Option("i", INPUT, true, "Input folder with Lucene index");
            iOption.setArgs(1);
            options.addOption(iOption);
        }
        {
            Option iOption = new Option("o", OUTPUT, true, "Destination folder for adjusted Lucene index");
            iOption.setArgs(1);
            options.addOption(iOption);
        }

        return options;
    }

    private static void usage() {
        String usage = "java -cp dvenabler.jar dk.statsbiblioteket.netark.DVEnabler.Command [options]";

        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp(usage, getOptions());
    }
}
