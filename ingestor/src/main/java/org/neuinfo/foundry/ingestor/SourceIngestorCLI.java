package org.neuinfo.foundry.ingestor;

import org.apache.commons.cli.*;
import org.json.JSONObject;
import org.neuinfo.foundry.common.model.Source;
import org.neuinfo.foundry.common.util.JSONUtils;
import org.neuinfo.foundry.ingestor.common.ConfigLoader;
import org.neuinfo.foundry.common.ingestion.Configuration;
import org.neuinfo.foundry.common.ingestion.SourceIngestionService;

/**
 * Created by bozyurt on 5/27/14.
 */
public class SourceIngestorCLI {

    final static String HOME_DIR = System.getProperty("user.home");

    public static void usage(Options options) {
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp("SourceIngestorCLI", options);
        System.exit(1);
    }

    public static void ingestOBP(String[] args) throws Exception {
        Option help = new Option("h", "print this message");
        Option jsonOption = OptionBuilder.withArgName("source-json-file").hasArg().
                withDescription("harvest source description file").create("j");
        Option delOption = new Option("d", "delete the source given by the source-json-file");
        Option configFileOption = OptionBuilder.withArgName("config-file")
                .hasArg().withDescription("config-file e.g. pipeline-ingestor-cfg.xml (default: ingestor-cfg.xml)").create('c');

        jsonOption.setRequired(true);

        Options options = new Options();
        options.addOption(help);
        options.addOption(jsonOption);
        options.addOption(delOption);
        options.addOption(configFileOption);
        CommandLineParser cli = new GnuParser();
        CommandLine line = null;
        try {
            line = cli.parse(options, args);
        } catch (Exception x) {
            System.err.println(x.getMessage());
            usage(options);
        }
        if (line == null || line.hasOption("h")) {
            usage(options);
        }

        String jsonFilePath = line.getOptionValue('j');
        boolean delSource = line.hasOption('d');

        JSONObject js = JSONUtils.loadFromFile(jsonFilePath);

        Source source = Source.fromJSON(js);

        String configFile =   "ingestor-cfg.xml";
        if (line.hasOption('c')) {
            configFile = line.getOptionValue('c');
        }

        Configuration conf = ConfigLoader.load(configFile);

        SourceIngestionService sis = new SourceIngestionService();
        try {
            sis.start(conf);
            if (delSource) {
                System.out.println("Deleting source " + source.getResourceID() + " [" + source.getName() + "]...");
                sis.deleteSource(source);
            } else {
                sis.saveSource(source);
            }
        } finally {
            sis.shutdown();
        }
    }


    public static void main(String[] args) throws Exception {
        SourceIngestorCLI.ingestOBP(args);
    }
}
