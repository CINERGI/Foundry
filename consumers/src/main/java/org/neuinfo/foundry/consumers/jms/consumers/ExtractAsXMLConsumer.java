package org.neuinfo.foundry.consumers.jms.consumers;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import org.apache.commons.cli.*;
import org.apache.log4j.Logger;
import org.bson.types.ObjectId;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;
import org.json.JSONObject;
import org.neuinfo.foundry.common.util.JSONUtils;
import org.neuinfo.foundry.common.util.Utils;
import org.neuinfo.foundry.common.util.XML2JSONConverter;
import org.neuinfo.foundry.consumers.common.Constants;

/**
 * Created by bozyurt on 9/16/14.
 */
public class ExtractAsXMLConsumer extends JMSConsumerSupport {
    private final static Logger logger = Logger.getLogger(ExtractAsXMLConsumer.class);
    public ExtractAsXMLConsumer() {
        super("foundy.extractxml");
    }

    void extractAsXml(String objectId) throws Exception {
        logger.info("in ExtractAsXMLConsumer");
        DB db = mongoClient.getDB(super.mongoDbName);

        DBCollection records = db.getCollection("records");

        BasicDBObject query = new BasicDBObject(Constants.MONGODB_ID_FIELD, new ObjectId(objectId));

        DBObject theDoc = records.findOne(query);
        if (theDoc != null) {
            System.out.println("found doc");
            BasicDBObject dataBlock = (BasicDBObject) theDoc.get("Data");
            final JSONObject json = JSONUtils.toJSON(dataBlock, false);
            // System.out.println("dataBlock:" + json.toString(2));
            if (json.has("TransformedDoc")) {
                System.out.println("has TransformedDoc");
                JSONObject transformedDoc = json.getJSONObject("TransformedDoc");
                XML2JSONConverter converter = new XML2JSONConverter();

                Element docEl = converter.toXML(transformedDoc);

                XMLOutputter xmlOutputter = new XMLOutputter(Format.getPrettyFormat());

                Document doc = new Document();
                doc.setRootElement(docEl);
                xmlOutputter.output(doc, System.out);
                System.out.println("==============================================");
                Utils.saveXML(docEl, "/tmp/" + objectId + ".xml");
            }
        }
    }

    public static void usage(Options options) {
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp("ExtractAsXMLConsumer", options);
        System.exit(1);
    }

    public static void main(String[] args) throws Exception {
        Option help = new Option("h", "print this message");
        Option objIdOption = OptionBuilder.withArgName("object id").hasArg().
                withDescription("object id to extract as xml").create('i');
        objIdOption.setRequired(true);
        Options options = new Options();
        options.addOption(help);
        options.addOption(objIdOption);
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
        String objectId = line.getOptionValue("i");
        ExtractAsXMLConsumer consumer = new ExtractAsXMLConsumer();
        try {
            String configFile = "consumers-cfg.xml";
            consumer.startup(configFile);

            consumer.extractAsXml(objectId);
        } finally {
            consumer.shutdown();
        }
    }
}
