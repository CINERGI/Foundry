package org.neuinfo.foundry.consumers.jms.consumers;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import org.apache.commons.cli.*;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.apache.log4j.Logger;
import org.bson.types.ObjectId;
import org.json.JSONObject;
import org.neuinfo.foundry.common.util.JSONUtils;
import org.neuinfo.foundry.common.util.Utils;
import org.neuinfo.foundry.consumers.common.Constants;

import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.ObjectMessage;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URI;
import java.nio.charset.Charset;

/**
 * Created by bozyurt on 9/16/14.
 */
public class TransformerConsumer extends JMSConsumerSupport implements MessageListener {
    String serverURL;
    private final static Logger logger = Logger.getLogger(TransformerConsumer.class);

    public TransformerConsumer(String serverURL) {
        super("foundry.transform");
        this.serverURL = serverURL;
    }


    void doTransform(String objectId) {
        logger.info("in TransformerConsumer");
        DB db = mongoClient.getDB(super.mongoDbName);

        DBCollection records = db.getCollection("records");

        BasicDBObject query = new BasicDBObject(Constants.MONGODB_ID_FIELD, new ObjectId(objectId));

        DBObject theDoc = records.findOne(query);
        if (theDoc != null) {
            DBObject pi = (DBObject) theDoc.get("Processing");
            if (pi != null) {
                String status = (String) pi.get("status");
                // if (status.equals("transform")) {
                if (status.equals("new")) { // TEST
                    DBObject doc2Index = (DBObject) theDoc.get("OriginalDoc");
                    String jsonDocStr = doc2Index.toString();
                    try {

                        String transformedJS = send2TransformServer(jsonDocStr);
                        if (transformedJS != null) {
                            JSONObject trJson = new JSONObject(transformedJS);
                            pi.put("status", "transformed");
                            DBObject data = (DBObject) theDoc.get("Data");

                            data.put("TransformedDoc", JSONUtils.encode(trJson));
                            records.update(query, theDoc);
                            logger.info("");
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }

        }
    }

    private String send2TransformServer(String jsonDocStr) throws Exception {
        HttpClient client = new DefaultHttpClient();
        URIBuilder builder = new URIBuilder(serverURL);

        URI uri = builder.build();
        HttpPost httpPost = new HttpPost(uri);
        try {
            httpPost.addHeader("Accept", "application/json");
            httpPost.addHeader("Content-Type", "application/json");
            JSONObject json = new JSONObject();
            json.put("data", new JSONObject(jsonDocStr));

            StringEntity entity = new StringEntity(json.toString(), "UTF-8");
            httpPost.setEntity(entity);
            HttpResponse response = client.execute(httpPost);
            if (response.getStatusLine().getStatusCode() == 200) {
                String body = EntityUtils.toString(response.getEntity(), Charset.forName("UTF-8"));
                System.out.println(body);
                return body;
            } else {
                logger.error(response.getStatusLine().getStatusCode() +
                        " - " + response.getStatusLine().getReasonPhrase());
            }

        } finally {
            if (httpPost != null) {
                httpPost.releaseConnection();
            }
        }
        return null;
    }


    @Override
    public void onMessage(Message message) {
        try {
            ObjectMessage om = (ObjectMessage) message;
            String payload = (String) om.getObject();
            JSONObject json = new JSONObject(payload);
            String status = json.getString("status");
            String objectId = json.getString("oid");
            System.out.format("status:%s objectId:%s%n", status, objectId);
            doTransform(objectId);
        } catch (Exception x) {
            // TODO
            x.printStackTrace();
        }
    }

    public static void usage(Options options) {
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp("TransformerConsumer", options);
        System.exit(1);
    }

    public static void main(String[] args) throws Exception {
        Option help = new Option("h", "print this message");
        Option urlOption = OptionBuilder.withArgName("server URL").hasArg().
                withDescription("server url for the transformer service").create('u');
        urlOption.setRequired(true);
        Option objIdOption = OptionBuilder.withArgName("object id").hasArg().
                withDescription("object id to transform").create('i');
        Options options = new Options();
        options.addOption(help);
        options.addOption(urlOption);
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
        String serverUrl = line.getOptionValue("u");
        TransformerConsumer consumer = new TransformerConsumer(serverUrl);
        String configFile = "consumers-cfg.xml";
        try {
            consumer.startup(configFile);
            if (line.hasOption("i")) {
                String objectId = line.getOptionValue("i");
                consumer.doTransform(objectId);
            } else {
                consumer.handleMessages(consumer);

                System.out.print("Press a key to exit:");
                System.in.read();
            }
        } finally {
            consumer.shutdown();
        }
    }
}

