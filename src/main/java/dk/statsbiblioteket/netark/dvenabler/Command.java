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
import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.Log;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * Command-line interface for DVEnabler.
 */
public class Command {
    private static Log log = LogFactory.getLog(Command.class);

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

        if (cli.hasOption("help")) {
            usage();
            return;
        }

        final File in = new File(cli.getOptionValue("index"));
        if (!in.exists() || !in.isDirectory()) {
            System.err.println("Unable to access index folder '" + in + "'");
            usage();
            return;
        }

        final boolean verbose = cli.hasOption("verbose");
        if (cli.hasOption("list")) {
            list(in, verbose);
            return;
        }
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
        options.addOption("h", "help", false, "Print help message and exit");
        options.addOption("v", "verbose", false, "Enable verbose output");
        options.addOption("l", "list", false, "Lists fields in index");


        Option iOption = new Option("i", "index", true, "Folder with Lucene index");
        iOption.setRequired(true);
        iOption.setArgs(1);
        options.addOption(iOption);
        return options;
    }

    private static void usage() {
        String usage = "java -cp dvenabler.jar dk.statsbiblioteket.netark.DVEnabler.Command [options]";

        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp(usage, getOptions());
    }
}
