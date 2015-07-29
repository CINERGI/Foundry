package org.neuinfo.foundry.jms.producer;

import com.mongodb.*;
import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.commons.cli.*;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONObject;
import org.neuinfo.foundry.common.config.ServerInfo;
import org.neuinfo.foundry.common.ingestion.DocumentIngestionService;
import org.neuinfo.foundry.common.model.Source;
import org.neuinfo.foundry.common.util.Utils;
import org.neuinfo.foundry.jms.common.ConfigLoader;
import org.neuinfo.foundry.jms.common.Configuration;
import org.neuinfo.foundry.jms.common.WorkflowMapping;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.URI;
import java.util.*;

/**
 * Created by bozyurt on 10/30/14.
 */
public class ManagementService {
    private transient Connection con;
    private transient Session session;
    private transient MessageProducer producer;
    private String queueName;
    private Configuration config;
    private String dbName;
    private MongoClient mongoClient;
    private DocumentIngestionService docService;

    public ManagementService(String queueName) {
        this.queueName = queueName;
    }

    public void startup(String configFile) throws Exception {
        if (configFile == null) {
            this.config = ConfigLoader.load("dispatcher-cfg.xml");
        } else {
            this.config = ConfigLoader.load(configFile);
        }

        this.dbName = config.getMongoDBName();
        List<ServerAddress> servers = new ArrayList<ServerAddress>(config.getMongoServers().size());
        for (ServerInfo si : config.getMongoServers()) {
            InetAddress inetAddress = InetAddress.getByName(si.getHost());
            servers.add(new ServerAddress(inetAddress, si.getPort()));
        }
        mongoClient = new MongoClient(servers);

        mongoClient.setWriteConcern(WriteConcern.SAFE);
        ConnectionFactory factory = new ActiveMQConnectionFactory(config.getBrokerURL());
        this.con = factory.createConnection();
        session = con.createSession(false, Session.AUTO_ACKNOWLEDGE);
        this.producer = session.createProducer(null);

        docService = new DocumentIngestionService();
        docService.initialize(this.dbName, mongoClient);
    }

    public void shutdown() {
        if (mongoClient != null) {
            mongoClient.close();
        }
        try {
            if (con != null) {
                con.close();
            }
        } catch (JMSException x) {
            x.printStackTrace();
        }
    }

    Source findSource(String sourceID) {
        DB db = mongoClient.getDB(dbName);
        DBCollection sources = db.getCollection("sources");
        BasicDBObject query = new BasicDBObject("sourceInformation.resourceID", sourceID);
        // FIXME finding only by NIF ID currently
        final DBCursor cursor = sources.find(query, null);
        Source source = null;
        try {
            if (cursor.hasNext()) {
                DBObject dbo = cursor.next();
                source = Source.fromDBObject(dbo);
            }
        } finally {
            cursor.close();
        }
        return source;
    }

    List<Source> findSources() {
        DB db = mongoClient.getDB(dbName);
        DBCollection sources = db.getCollection("sources");
        List<Source> srcList = new LinkedList<Source>();
        DBCursor cursor = sources.find();
        try {
            while(cursor.hasNext()) {
                DBObject dbo = cursor.next();
                Source source = Source.fromDBObject(dbo);
                srcList.add(source);
            }

        } finally {
            cursor.close();
        }
        return srcList;

    }

    void deleteDocuments(String sourceID) {
        docService.deleteDocuments4Resource(config.getCollectionName(), sourceID, null);
    }

    void sendMessage(JSONObject messageBody) throws JMSException {
        Destination destination = session.createQueue(this.queueName);
        System.out.println("sending user JMS message with payload:" + messageBody.toString(2) +
                " to queue:" + this.queueName);
        Message message = session.createObjectMessage(messageBody.toString());
        this.producer.send(destination, message);
    }

    JSONObject prepareMessageBody(String cmd, Source source) throws Exception {
        // check if source has a valid workflow

        WorkflowMapping wm = hasValidWorkflow(source);
        if (wm == null) {
            throw new Exception("No matching workflow for source " + source.getResourceID());
        }
        String batchId = Utils.prepBatchId(new Date());
        JSONObject json = new JSONObject();
        json.put("cmd", cmd);
        json.put("batchId", batchId);
        json.put("srcNifId", source.getResourceID());
        json.put("dataSource", source.getDataSource());
        json.put("ingestConfiguration", source.getIngestConfiguration());
        json.put("contentSpecification", source.getContentSpecification());

        json.put("ingestorOutStatus", wm.getIngestorOutStatus());
        json.put("updateOutStatus", wm.getUpdateOutStatus());

        return json;
    }

    WorkflowMapping hasValidWorkflow(Source source) {
        List<String> workflowSteps = source.getWorkflowSteps();
        List<WorkflowMapping> workflowMappings = this.config.getWorkflowMappings();
        for (WorkflowMapping wm : workflowMappings) {
            if (workflowSteps.size() == wm.getSteps().size()) {
                for (int i = 0; i < workflowSteps.size(); i++) {
                    String step = workflowSteps.get(i);
                    if (!step.equals(wm.getSteps().get(i))) {
                        break;
                    }
                }
                return wm;
            }
        }
        return null;
    }

    public static String ensureIndexPathStartsWithSlash(String indexPath) {
        if (!indexPath.startsWith("/")) {
            indexPath = "/" + indexPath;
        }
        return indexPath;
    }

    boolean deleteIndex(String url) throws Exception {
        HttpClient client = new DefaultHttpClient();
        URIBuilder builder = new URIBuilder(url);
        URI uri = builder.build();
        System.out.println("uri:" + uri);
        HttpDelete httpDelete = new HttpDelete(uri);
        boolean ok = false;
        try {
            final HttpResponse response = client.execute(httpDelete);
            if (response.getStatusLine().getStatusCode() == 200) {
                ok = true;
            }
        } finally {
            if (httpDelete != null) {
                httpDelete.releaseConnection();
            }
        }
        return ok;
    }

    public static void showHelp() {
        System.out.println("Available commands");
        System.out.println("\thelp - shows this message.");
        System.out.println("\tingest <src-nif-id>");
        System.out.println("\th - show all command history");
        System.out.println("\tdelete <url> - [e.g. http://localhost:9200/nif]");
        System.out.println("\tdd <sourceID>  - delete docs for a sourceID");
        System.out.println("\tlist - lists all of the existing sources.");
        System.out.println("\texit - exits the management client.");
    }

    public static void usage(Options options) {
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp("ManagementService", options);
        System.exit(1);
    }

    static boolean confirm(BufferedReader in, String message) throws IOException {
        System.out.print(message + " (y/[n])? ");
        String ans = in.readLine();
        ans = ans.trim();
        if (ans.equalsIgnoreCase("y")) {
            return true;
        }
        return false;
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

        ManagementService ms = new ManagementService("foundry.consumer.head");
        Set<String> history = new LinkedHashSet<String>();
        try {
            ms.startup(configFile);
            BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
            boolean finished = false;
            while (!finished) {
                System.out.print("Foundry:>> ");
                String ans = in.readLine();
                ans = ans.trim();
                if (ans.equals("help")) {
                    showHelp();
                } else if (ans.startsWith("ingest")) {
                    String[] toks = ans.split("\\s+");
                    if (toks.length == 2) {
                        history.add(ans);
                        String srcNifId = toks[1];
                        Source source = ms.findSource(srcNifId);
                        JSONObject json = ms.prepareMessageBody("ingest", source);
                        ms.sendMessage(json);

                    }
                } else if (ans.equals("history") || ans.equals("h")) {
                    for (String h : history) {
                        System.out.println(h);
                    }
                } else if (ans.startsWith("delete")) {
                    String[] toks = ans.split("\\s+");
                    if (toks.length == 2) {
                        String url = toks[1];
                        ms.deleteIndex(url);
                    }
                } else if (ans.startsWith("dd")) {
                    String[] toks = ans.split("\\s+");
                    if (toks.length == 2) {
                        String sourceID = toks[1];
                        if (confirm(in, "Do you want to delete docs for " + sourceID + "?")) {
                            ms.deleteDocuments(sourceID);
                        }
                    }
                } else if (ans.startsWith("list")) {
                    List<Source> sources = ms.findSources();
                    for(Source source : sources) {
                        System.out.println(String.format("%s - (%s)", source.getResourceID() , source.getName()));
                    }
                } else if (ans.equals("exit")) {
                    break;
                }
            }

        } finally {
            ms.shutdown();
        }

    }
}
