package org.neuinfo.foundry.ingestor;

import org.apache.commons.cli.*;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.input.SAXBuilder;
import org.json.JSONObject;
import org.neuinfo.foundry.common.util.XML2JSONConverter;

import java.io.File;
import java.util.List;

/**
 * Created by bozyurt on 4/29/14.
 */
public class NIFXMLIngestor extends IngestorSupport {


    public JSONObject prepDocJsonPayload(Element docEl) throws Exception {
        XML2JSONConverter converter = new XML2JSONConverter();
        final JSONObject json = converter.toJSON(docEl);

        return json;
    }

    public List<Element> getDocElements(File xmlFile, String topElName, String docElName) throws Exception {
        SAXBuilder builder = new SAXBuilder();
        Document doc = builder.build(xmlFile);
        Element rootEl = doc.getRootElement();
        Element topEl;
        if (rootEl.getName().equals(topElName)) {
            topEl = rootEl;
        } else {
            topEl = rootEl.getChild(topElName);
        }
        assert topEl != null;
        List<Element> children = topEl.getChildren(docElName);

        return children;
    }

    public static void usage(Options options) {
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp("NIFXMLIngestor", options);
        System.exit(1);
    }

    public static void cli(String[] args) throws Exception {
        Option help = new Option("h", "print this message");
        Option fileOption = OptionBuilder.withArgName("xml-file").hasArg()
                .withDescription("xml-file to convert to JSON docs for ingestion").create('f');
        Option topElOption = OptionBuilder.withArgName("top-elem").hasArg()
                .withDescription("XMl tag for the element under which the documents reside").create('t');
        Option docElOption = OptionBuilder.withArgName("doc-elem").hasArg()
                .withDescription("XML tag for the document").create('d');
        Option removeOption = new Option("r", "remove all records from mongodb");

        //fileOption.setRequired(true);
        Options options = new Options();
        options.addOption(help);
        options.addOption(fileOption);
        options.addOption(topElOption);
        options.addOption(docElOption);
        options.addOption(removeOption);
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

        String xmlFile = line.getOptionValue('f');
        String topElName = line.getOptionValue('t');
        String docElName = line.getOptionValue('d');
        boolean deleteOp = line.hasOption('r');
        if (deleteOp) {
            System.out.print("Do you really wan to delete all records?(y/[n]):");
            int ch = System.in.read();
            if (ch != 'y') {
                System.exit(1);
            }
        } else {
            if (xmlFile == null || topElName == null || docElName == null) {
                usage(options);
            }
        }


        NIFXMLIngestor ingestor = new NIFXMLIngestor();
        try {
            ingestor.start();
            if (deleteOp) {
                ingestor.deleteAllRecords();
                System.out.println("deleted all records from mongodb");
            } else {
                List<Element> docElements = ingestor.getDocElements(
                        new File(xmlFile), topElName, docElName);
                for (Element docEl : docElements) {
                    JSONObject json = ingestor.prepDocJsonPayload(docEl);
                    System.out.println(json.toString(2));
                    System.out.println("===============================");
                    ingestor.insertDoc(ingestor.prepareDocument(json));
                }
            }

        } finally {
            ingestor.shutdown();
        }
    }


    public static void main(String[] args) throws Exception {
       cli(args);
    }

    private static void test() throws Exception {
        boolean deleteOp = false;
        NIFXMLIngestor ingestor = new NIFXMLIngestor();
        try {
            ingestor.start();
            if (deleteOp) {
                ingestor.deleteAllRecords();
            } else {
                List<Element> docElements = ingestor.getDocElements(
                        new File("/tmp/open_source_brain_projects.xml"), "projects", "project");
                for (Element docEl : docElements) {

                    JSONObject json = ingestor.prepDocJsonPayload(docEl);
                    System.out.println(json.toString(2));
                    System.out.println("===============================");
                    ingestor.insertDoc(ingestor.prepareDocument(json));
                }
            }

        } finally {
            ingestor.shutdown();
        }
    }


}
