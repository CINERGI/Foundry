package org.neuinfo.foundry.ingestor;

import org.apache.commons.cli.*;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.neuinfo.foundry.common.model.Source;
import org.neuinfo.foundry.common.util.Assertion;
import org.neuinfo.foundry.common.util.Utils;
import org.neuinfo.foundry.ingestor.common.ConfigLoader;
import org.neuinfo.foundry.common.ingestion.Configuration;
import org.neuinfo.foundry.common.ingestion.DocumentIngestionService;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Created by bozyurt on 6/4/14.
 */
public class CINERGIIngestorCLI {

    public static List<String> getDocumentList(String rootUrl) throws IOException {
        List<String> links = new ArrayList<String>();
        final Document doc = Jsoup.connect(rootUrl).get();
        final Elements anchorEls = doc.select("a");
        final Iterator<Element> it = anchorEls.iterator();
        while (it.hasNext()) {
            Element ae = it.next();
            final String href = ae.attr("abs:href");
            if (href != null && href.endsWith(".xml")) {
                links.add(href);
            }
        }
        return links;
    }

    public static void ingestWAFDocs(String batchId, List<String> docUrlList) throws Exception {
        Configuration conf = ConfigLoader.load("dev/ingestor-cfg.xml");
        DocumentIngestionService dis = new DocumentIngestionService();
        try {
            dis.start(conf);

            String nifId = "nlx_999999";
            String dataSource = "";

            final Source source = dis.findSource(nifId, dataSource);
            Assertion.assertNotNull(source, "Cannot find source for nifId:" + nifId);

            dis.setSource(source);

            int submittedCount = docUrlList.size();
            int ingestedCount = 0;
            dis.beginBatch(source, batchId);
            for (String docUrl : docUrlList) {
                final JSONObject json = GMIXMLIngestor.prepNOAAJsonPayload(docUrl);
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
        formatter.printHelp("CINERGIIngestorCLI", options);
        System.exit(1);
    }

    public static void main(String[] args) throws Exception {

        Option help = new Option("h", "print this message");
        Option batchIdOption = OptionBuilder.withArgName("batchId e.g. 20140528").hasArg()
                .withDescription("batchId in YYYYMMDD format").create('b');
        batchIdOption.setRequired(true);

        Option numOfDocsOption = OptionBuilder.withArgName("numDocs").hasArg()
                .withDescription("number of documents to WAF documents to index [1-100] default:10")
                .create('n');

        int numDocs = 10;
        Options options = new Options();
        options.addOption(help);
        options.addOption(batchIdOption);
        options.addOption(numOfDocsOption);
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
        if (line.hasOption('n')) {
            numDocs = Utils.getIntValue(line.getOptionValue('n'), 10);
            if (numDocs > 100) {
                numDocs = 100;
            }
            if (numDocs <= 0) {
                numDocs = 10;
            }
        }

        final List<String> documentList = CINERGIIngestorCLI.getDocumentList("http://hydro10.sdsc.edu/metadata/ScienceBase_WAF_dump/");
        final List<String> docUrlList = documentList.subList(0, numDocs - 1);
        for (String docUrl : docUrlList) {
            System.out.println(docUrl);
        }

        CINERGIIngestorCLI.ingestWAFDocs(batchId, docUrlList);
    }
}
