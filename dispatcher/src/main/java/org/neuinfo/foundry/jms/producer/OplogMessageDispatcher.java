package org.neuinfo.foundry.jms.producer;

import org.apache.commons.cli.*;
import org.neuinfo.foundry.jms.common.ConfigLoader;
import org.neuinfo.foundry.jms.common.Configuration;

/**
 * <p/>
 * Listens to MongoDB replicaSet oplog messages to create messages to be dispatched
 * to persistent message queues to be consumed by remote consumer processes.
 * <p/>
 * Created by bozyurt on 4/24/14.
 */
public class OplogMessageDispatcher {
    private MongoOpLogListener opLogListener;
    private UserMessageService ums;
    private Configuration config;

    public void startup(String configFile) throws Exception {
        if (configFile != null) {
           this.config = ConfigLoader.load(configFile);
        } else {
            this.config = ConfigLoader.load("dispatcher-cfg.xml");
        }
        System.out.println(this.config);
        TimeCheckPointManager.getInstance(this.config.getCheckpointXmlFile());

        // start periodic checkpointing
        Thread checkpointer = new Thread(new TimeCheckPointScheduler());
        checkpointer.start();

        opLogListener = new MongoOpLogListener("dispatcher", config);

        opLogListener.start();
        // start
        //this.ums = new UserMessageService(this.config.getBrokerURL(), "foundry.dispatcher");
        //Thread umsThread = new Thread(ums);
        //umsThread.start();

        Runtime.getRuntime().addShutdownHook(new Thread() {
            public void run() {
                shutdown();
            }
        });
    }

    private void shutdown() {
        if (this.ums != null) {
            ums.close();
        }
        if (opLogListener != null) {
            System.out.println("shutting down OplogMessageDispatcher...");
            opLogListener.shutdown();
        }

        try {
            TimeCheckPointManager.getInstance().checkpoint();
        } catch (Exception e) {
            e.printStackTrace();
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
        OplogMessageDispatcher dispatcher = new OplogMessageDispatcher();

        dispatcher.startup(configFile);

        //   System.out.print("Press a key to exit:");
        //   System.in.read();
        //   dispatcher.shutdown();
    }


}
