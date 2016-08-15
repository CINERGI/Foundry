package org.neuinfo.foundry.ingestor;

import com.mongodb.*;
import org.apache.commons.cli.*;
import org.neuinfo.foundry.consumers.jms.consumers.plugins.WAFExporter;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by valentin on 7/8/2016.
 */
public  class WAFExporterCLI extends IngestorSupport {


    public static void showHelp() {
        System.out.println("Available commands");
        System.out.println("\thelp - shows this message.");
        System.out.println("\twaf <src-nif-id>");
        System.out.println("\th - show all command history");
        System.out.println("\tdelete <url> - [e.g. http://localhost:9200/nif]");
        System.out.println("\tdd <sourceID>  - delete docs for a sourceID");
        System.out.println("\ttrigger <sourceID> <status-2-match> <queue-2-send> [<new-status>] (e.g. trigger nif-0000-00135 new.1 foundry.uuid.1)");
        System.out.println("\tstatus [<sourceID>] - show processing status of data source(s)");
        System.out.println("\tlist - lists all of the existing sources.");
        System.out.println("\texit - exits the management client.");
    }

    public static void usage(Options options) {
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp("ManagementService", options);
        System.exit(1);
    }
    public static void main(String[] args) throws Exception {
        Option help = new Option("h", "print this message");
        Option cinergiSource = Option.builder("sourceid").argName("cinergiSource")
                .hasArg().desc("cinergi source identifier; eg cinergi-0022").required().
                        build();
        Option outputDirectory = Option.builder("output").argName("outputDir").required()
                .hasArg().desc("output directory").build();

        Options options = new Options();
        options.addOption(help);
        options.addOption(cinergiSource);
        options.addOption(outputDirectory);
        CommandLineParser cli = new DefaultParser();
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

        String src = null;
        if (line.hasOption("cinergiSource")) {
            src = line.getOptionValue("cinergiSource");
        }
        String outDir =  null;
        if (line.hasOption("outputDirectory")) {
            outDir = line.getOptionValue("outDirectory");
        }
        WAFExporter exporter = new WAFExporter();
        Map<String, String> WafOptions = new HashMap<String, String>() ;
        WafOptions.put("outDirectory",outDir);
        exporter.initialize(WafOptions);

        MongoUtilsCLI utils = new MongoUtilsCLI();

        MongoClient mongoService = utils.mongoClient;
        DB db = mongoService.getDB(utils.dbName);
        DBCollection records = db.getCollection(utils.collectionName);
        BasicDBObject query = new BasicDBObject("SourceInfo.SourceID", src);
//        if (statusSet != null && !statusSet.isEmpty()) {
//            List<String> statusList = new ArrayList<String>(statusSet);
//            query.append("Processing.status", new BasicDBObject("$in", statusList));
//        }
        BasicDBObject keys = new BasicDBObject("primaryKey", 1);
        DBCursor cursor = records.find(query, keys);

        try {
            while (cursor.hasNext()) {
                BasicDBObject dbObject = (BasicDBObject) cursor.next();
                BasicDBObject docQUery = new BasicDBObject("primaryKey", dbObject).append("SourceInfo.SourceID", src);
                BasicDBObject dbObjectDoc = (BasicDBObject) records.findOne(docQUery);
                exporter.handle(dbObjectDoc);
            }
        } finally {
            cursor.close();
        }


    }
}
