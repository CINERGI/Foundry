package org.neuinfo.foundry.jms.producer;

import com.mongodb.*;
import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.commons.cli.*;
import org.apache.commons.lang.*;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.log4j.Logger;
import org.bson.types.ObjectId;
import org.json.JSONObject;
import org.neuinfo.foundry.common.config.ServerInfo;
import org.neuinfo.foundry.common.ingestion.DocProcessingStatsService;
import org.neuinfo.foundry.common.ingestion.DocumentIngestionService;
import org.neuinfo.foundry.common.model.Source;
import org.neuinfo.foundry.common.util.Assertion;
import org.neuinfo.foundry.common.util.MongoUtils;
import org.neuinfo.foundry.jms.common.ConfigLoader;
import org.neuinfo.foundry.jms.common.Configuration;
import org.neuinfo.foundry.utils.MessagingUtils;

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
    private String queueName;
    private Configuration config;
    private String dbName;
    private MongoClient mongoClient;
    private DocumentIngestionService docService;
    private final static Logger logger = Logger.getLogger(ManagementService.class);

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

        mongoClient = MongoUtils.createMongoClient(servers);
        ConnectionFactory factory = new ActiveMQConnectionFactory(config.getBrokerURL());
        this.con = factory.createConnection();

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
            while (cursor.hasNext()) {
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
        sendMessage(messageBody, this.queueName);
    }

    void sendMessage(JSONObject messageBody, String queue2Send) throws JMSException {
        Session session = null;
        try {
            session = con.createSession(false, Session.AUTO_ACKNOWLEDGE);
            MessageProducer producer = session.createProducer(null);
            Destination destination = session.createQueue(queue2Send);
            System.out.println("sending user JMS message with payload:" + messageBody.toString(2) +
                    " to queue:" + queue2Send);
            Message message = session.createObjectMessage(messageBody.toString());
            producer.send(destination, message);
        } finally {
            if (session != null) {
                session.close();
            }
        }
    }


    JSONObject prepareMessageBody(String cmd, Source source) throws Exception {
        return MessagingUtils.prepareMessageBody(cmd, source, config.getWorkflowMappings());
    }

    public JSONObject prepareMessageBody(String oid, String status) {
        JSONObject json = new JSONObject();
        json.put("oid", oid);
        json.put("status", status);
        return json;
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

    void triggerPipeline(Source source, String status2Match, String queue2Send, String newStatus) throws Exception {
        DB db = mongoClient.getDB(dbName);
        DBCollection records = db.getCollection(this.config.getCollectionName());
        BasicDBObject query = new BasicDBObject("Processing.status", status2Match)
                .append("SourceInfo.SourceID", source.getResourceID());
        DBCursor cursor = records.find(query, new BasicDBObject("Processing.status", 1));
        if (newStatus == null) {
            try {
                while (cursor.hasNext()) {
                    DBObject dbo = cursor.next();
                    String oid = dbo.get("_id").toString();
                    JSONObject mb = prepareMessageBody(oid, status2Match);
                    sendMessage(mb, queue2Send);
                }

            } finally {
                cursor.close();
            }
        } else {
            List<String> matchingOIdList = new LinkedList<String>();
            try {
                while (cursor.hasNext()) {
                    DBObject dbo = cursor.next();
                    String oid = dbo.get("_id").toString();
                    matchingOIdList.add(oid);
                }
            } finally {
                cursor.close();
            }
            logger.info("updating status of " + matchingOIdList.size() + " records to " + newStatus);
            for (String oidStr : matchingOIdList) {
                ObjectId oid = new ObjectId(oidStr);
                BasicDBObject update = new BasicDBObject();
                update.append("$set", new BasicDBObject("Processing.status", newStatus));
                query = new BasicDBObject("_id", oid);
                records.update(query, update, false, false, WriteConcern.SAFE);
            }
            for (String oidStr : matchingOIdList) {
                JSONObject mb = prepareMessageBody(oidStr, newStatus);
                sendMessage(mb, queue2Send);
            }
        }
    }

    void showProcessingStats(String sourceID) {
        DocProcessingStatsService dpss = new DocProcessingStatsService();
        dpss.setMongoClient(this.mongoClient);
        dpss.setDbName(this.dbName);
        List<DocProcessingStatsService.SourceStats> processingStats = dpss.getDocCountsPerStatusPerSource2(
                config.getCollectionName());
        if (sourceID == null) {
            for (DocProcessingStatsService.SourceStats ss : processingStats) {
                showSourceStats(ss);
            }
        } else {
            for (DocProcessingStatsService.SourceStats ss : processingStats) {
                if (ss.getSourceID().equals(sourceID)) {
                    showSourceStats(ss);
                    break;
                }
            }
        }
    }

    void showSourceStats(DocProcessingStatsService.SourceStats ss) {
        StringBuilder sb = new StringBuilder(128);
        sb.append(ss.getSourceID()).append("\t");
        Map<String, Integer> statusCountMap = ss.getStatusCountMap();
        int totCount = 0;
        for (Integer count : statusCountMap.values()) {
            totCount += count;
        }
        sb.append("total:").append(StringUtils.leftPad(String.valueOf(totCount), 10)).append("\t");
        Integer finishedCount = statusCountMap.get("finished");
        Integer errorCount = statusCountMap.get("error");
        finishedCount = finishedCount == null ? 0 : finishedCount;
        errorCount = errorCount == null ? 0 : errorCount;
        sb.append("finished:").append(StringUtils.leftPad(finishedCount.toString(), 10)).append("\t");
        sb.append("error:").append(StringUtils.leftPad(errorCount.toString(), 10)).append("\t");
        for (String status : statusCountMap.keySet()) {
            if (status.equals("finished") || status.equals("error")) {
                continue;
            }
            Integer statusCount = statusCountMap.get(status);
            sb.append(status).append(':').append(StringUtils.leftPad(statusCount.toString(), 10)).append("\t");
        }
        System.out.println(sb.toString().trim());
    }

    public static void showHelp() {
        System.out.println("Available commands");
        System.out.println("\thelp - shows this message.");
        System.out.println("\tingest <src-nif-id>");
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
                    for (Source source : sources) {
                        System.out.println(String.format("%s - (%s)", source.getResourceID(), source.getName()));
                    }
                } else if (ans.startsWith("trigger")) {
                    String[] toks = ans.split("\\s+");
                    if (toks.length == 4 || toks.length == 5) {
                        String srcNifId = toks[1];
                        String status2Match = toks[2];
                        String toQueue = toks[3];
                        String newStatus = null;
                        if (toks.length == 5) {
                            newStatus = toks[4];
                        }
                        Source source = ms.findSource(srcNifId);
                        Assertion.assertNotNull(source);
                        ms.triggerPipeline(source, status2Match, toQueue, newStatus);
                        history.add(ans);
                    }
                } else if (ans.startsWith("status")) {
                    String[] toks = ans.split("\\s+");
                    if (toks.length == 1 || toks.length == 2) {
                        if (toks.length == 2) {
                            String srcNifId = toks[1];
                            ms.showProcessingStats(srcNifId);
                        } else {
                            ms.showProcessingStats(null);
                        }
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
