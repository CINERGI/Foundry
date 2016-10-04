package org.neuinfo.foundry.jms.producer;

import org.apache.commons.cli.*;
import org.apache.log4j.Logger;
import org.neuinfo.foundry.common.config.ConfigLoader;
import org.neuinfo.foundry.common.config.Configuration;

/**
 * Created by bozyurt on 7/9/15.
 */
public class PipelineMessageDispatcher {
    private Configuration config;
    private PipelineMessageListener messageListener;
    private final static Logger logger = Logger.getLogger(PipelineMessageDispatcher.class);

    public void startup(String configFile) throws Exception {
        if (configFile != null) {
            this.config = ConfigLoader.load(configFile);
        } else {
            this.config = ConfigLoader.load("dispatcher-cfg.xml");
        }
        System.out.println(this.config);

        this.messageListener = new PipelineMessageListener(config);

        this.messageListener.startup();
        Runtime.getRuntime().addShutdownHook(new Thread() {
            public void run() {
                shutdown();
            }
        });
    }

    private void shutdown() {
        if (messageListener != null) {
            logger.info("shutting down PipelineMessageListener...");
            messageListener.shutdown();
        }
    }

    public static void usage(Options options) {
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp("Dispatcher", options);
        System.exit(1);
    }

    public static void main(String[] args) throws Exception {
        Option help = new Option("h", "print this message");
        Option configFileOption = OptionBuilder.withArgName("config-file")
                .hasArg().withDescription("config-file e.g. cinergi-dispatcher-cfg.xml").create('c');

        Options options = new Options();
        options.addOption(help);
        options.addOption(configFileOption);
        CommandLineParser cli = new GnuParser();
        CommandLine line = null;
        try {
            line = cli.parse(options, args);
        } catch (Exception x) {
            System.err.println(x.getMessage());
            usage(options);
        }

        if (line.hasOption("h")) {
            usage(options);
        }

        String configFile = null;
        if (line.hasOption('c')) {
            configFile = line.getOptionValue('c');
        }

        PipelineMessageDispatcher dispatcher = new PipelineMessageDispatcher();
        dispatcher.startup(configFile);
    }


}
