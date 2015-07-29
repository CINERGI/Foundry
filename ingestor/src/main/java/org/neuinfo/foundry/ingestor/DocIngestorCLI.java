package org.neuinfo.foundry.ingestor;

import org.apache.commons.cli.*;
import org.jdom2.Element;
import org.json.JSONObject;
import org.neuinfo.foundry.common.model.Source;
import org.neuinfo.foundry.ingestor.common.ConfigLoader;
import org.neuinfo.foundry.common.ingestion.Configuration;
import org.neuinfo.foundry.common.ingestion.SourceConfig;
import org.neuinfo.foundry.common.ingestion.DocumentIngestionService;

import java.io.File;
import java.util.List;

/**
 * Created by bozyurt on 5/27/14.
 */
public class DocIngestorCLI {

    public static void ingestOBPDocs(String batchId) throws Exception {
        Configuration conf = ConfigLoader.load("dev/ingestor-cfg.xml");
        DocumentIngestionService dis = new DocumentIngestionService();
        try {
            dis.start(conf);

            final SourceConfig sourceConfig = conf.getSourceConfig();
            String dataSource = "";
            final Source source = dis.findSource(sourceConfig.getNifId(), dataSource); //  "nlx_152590");

            dis.setSource(source);

            NIFXMLIngestor nxi = new NIFXMLIngestor();
            final File xmlFile = new File(sourceConfig.getPath()); //   "/tmp/open_source_brain_projects.xml");
            //final List<Element> docElements = nxi.getDocElements(xmlFile, "projects", "project");
            final List<Element> docElements = nxi.getDocElements(xmlFile, sourceConfig.getRootEl(), sourceConfig.getDocEl());

            // batch ingestion

            dis.beginBatch(source, batchId);
            int submittedCount = docElements.size();
            int ingestedCount = 0;
            for (Element docEl : docElements) {
                JSONObject json = nxi.prepDocJsonPayload(docEl);
                System.out.println(json.toString(2));
                try {
                    dis.saveDocument(json, batchId, source, "new", "records");
                    ingestedCount++;
                } catch (Exception x) {
                    x.printStackTrace();
                }
                System.out.println("=============================================");
            }

            dis.endBatch(source, batchId, ingestedCount, submittedCount);

        } finally {
            dis.shutdown();
        }
    }

    public static void usage(Options options) {
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp("DocIngestorCLI", options);
        System.exit(1);
    }

    public static void main(String[] args) throws Exception {
        Option help = new Option("h", "print this message");
        Option batchIdOption = OptionBuilder.withArgName("batchId e.g. 20140528").hasArg()
                .withDescription("batchId in YYYYMMDD format").create('b');
        batchIdOption.setRequired(true);

        Options options = new Options();
        options.addOption(help);
        options.addOption(batchIdOption);
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
        final String batchId = line.getOptionValue('b');
        System.out.println("Using batchId:" + batchId);
        DocIngestorCLI.ingestOBPDocs(batchId);
    }
}
