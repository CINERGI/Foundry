package org.neuinfo.foundry.ingestor;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import org.apache.commons.cli.*;

/**
 * Created by bozyurt on 6/4/14.
 */
public class MongoUtilsCLI extends IngestorSupport {

    public static void usage(Options options) {
        HelpFormatter
                formatter = new HelpFormatter();
        formatter.printHelp("MongoUtilsCLI", options);
        System.exit(1);
    }

    public void deleteAllSources() {
        DB db = mongoClient.getDB(this.dbName);

        DBCollection records = db.getCollection("sources");
        records.remove(new BasicDBObject());
    }

    public static void cli(String[] args) throws Exception {
        Option help = new Option("h", "print this message");
        Option removeOption = new Option("r", "remove all records from mongodb");
        Option removeForcedOption = new Option("rf", "remove all records from mongodb without asking");
        Option removeSourcesOption = new Option("s", "remove all sources from mongodb without asking");

        Options options = new Options();
        options.addOption(help);
        options.addOption(removeOption);
        options.addOption(removeForcedOption);
        options.addOption(removeSourcesOption);
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
        if (line == null ||
                (!line.hasOption('r') && !line.hasOption("rf") && !line.hasOption('s'))) {
            usage(options);
        }

        MongoUtilsCLI utils = new MongoUtilsCLI();
        try {
            utils.start();
            boolean deleteOp = line.hasOption('r');
            if (deleteOp) {
                System.out.print("Do you really want to delete all records in records collection?(y/[n]):");
                int ch = System.in.read();
                if (ch == 'y') {
                    utils.deleteAllRecords();
                }
            }
            final boolean rf = line.hasOption("rf");
            if (rf) {
                utils.deleteAllRecords();
            }
            boolean delSources = line.hasOption("s");
            if (delSources) {
                utils.deleteAllRecords();
            }
        } finally {
            utils.shutdown();
        }
    }

    public static void main(String[] args) throws Exception {
        cli(args);
    }
}
