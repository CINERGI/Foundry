package org.neuinfo.foundry.jms.producer;

import com.mongodb.*;
import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.log4j.Logger;
import org.bson.types.ObjectId;
import org.json.JSONObject;
import org.neuinfo.foundry.common.config.*;
import org.neuinfo.foundry.common.ingestion.DocProcessingStatsService;
import org.neuinfo.foundry.common.ingestion.DocProcessingStatsService.SourceStats;
import org.neuinfo.foundry.common.ingestion.DocumentIngestionService;
import org.neuinfo.foundry.common.model.Source;
import org.neuinfo.foundry.common.util.MongoUtils;
import org.neuinfo.foundry.utils.MessagingUtils;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.*;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Created by bozyurt on 6/15/15.
 */
public class PipelineTriggerHelper {
    private transient Connection con;
    private String queueName;
    private Configuration config;
    private String dbName;
    private MongoClient mongoClient;
    private DocumentIngestionService docService;
    private final static Logger logger = Logger.getLogger(PipelineTriggerHelper.class);

    public PipelineTriggerHelper(String queueName) {
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
        String user = config.getServers().get(0).getUser();
        String pwd = config.getServers().get(0).getPwd();

        mongoClient = MongoUtils.createMongoClient(servers, user, pwd, dbName);
        ConnectionFactory factory = new ActiveMQConnectionFactory(config.getBrokerURL());
        this.con = factory.createConnection();

        docService = new DocumentIngestionService();
        docService.start(this.config);
        // docService.initialize(this.dbName, mongoClient);
    }


    public void shutdown() {
        if (mongoClient != null) {
            mongoClient.close();
            logger.debug("closed mongo connection");
        }
        try {
            if (con != null) {
                logger.debug("closing JMS connection");
                con.close();
                logger.debug("closed JMS connection");
            }
        } catch (JMSException x) {
            logger.error("shutdown", x);
            x.printStackTrace();
        }

        if (docService != null) {
            docService.shutdown();
        }
    }


    public void showWS(PrintWriter out) {
        for (Workflow wf : this.config.getWorkflows()) {
            out.println(wf.toString());
        }
    }

    public Source findSource(String sourceID) {
        DB db = mongoClient.getDB(dbName);
        DBCollection sources = db.getCollection("sources");
        BasicDBObject query = new BasicDBObject("sourceInformation.resourceID", sourceID);
        // FIXME finding only by NIF ID currently
        return MongoUtils.getSource(query, sources);
    }

    public List<Source> findSources() {
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

    public void sendMessage(JSONObject messageBody) throws JMSException {
        sendMessage(messageBody, this.queueName);
    }

    private void sendMessage(JSONObject messageBody, String queue2Send) throws JMSException {
        Session session = null;
        try {
            session = con.createSession(false, Session.AUTO_ACKNOWLEDGE);
            MessageProducer producer = session.createProducer(null);
            Destination destination = session.createQueue(queue2Send);
            if (logger.isInfoEnabled()) {
                logger.info("sending user JMS message with payload:" + messageBody.toString(2) +
                        " to queue:" + queue2Send);
            }
            Message message = session.createObjectMessage(messageBody.toString());
            producer.send(destination, message);
        } finally {
            if (session != null) {
                session.close();
            }
        }
    }


    public void runPipelineSteps(Source source, String status2Match, String stepName, boolean run2TheEnd) throws Exception {
        Workflow workflow = config.getWorkflows().get(0);
        List<Route> routes = workflow.getRoutes();
        Route theRoute = null;
        for (int i = 0; i < routes.size(); i++) {
            Route route = routes.get(i);
            QueueInfo queueInfo = route.getQueueNames().get(0);
            String queueName = queueInfo.getName();
            String[] tokens = queueName.split("\\.");
            if (tokens[1].equals(stepName)) {
                theRoute = route;
                break;
            }
        }

        if (theRoute == null) {
            System.err.println("No pipeline step named " + stepName);
            return;
        }

        String newStatus = theRoute.getCondition().getFirstPredicateValue();
        String queue2Send = theRoute.getQueueNames().get(0).getName();
        if (!run2TheEnd) {
            String newOutStatus = workflow.getFinishedStatus();
            System.out.println("status2Match:" + status2Match + " queue2Send:" + queue2Send + " newStatus:" + newStatus
                    + " newOutStatus:" + newOutStatus);
            triggerPipeline(source, status2Match, queue2Send, newStatus, newOutStatus);
        } else {
            System.out.println("status2Match:" + status2Match + " queue2Send:" + queue2Send + " newStatus:" + newStatus);
            triggerPipeline(source, status2Match, queue2Send, newStatus, null);
        }
    }

    public void triggerPipeline(Source source, String status2Match, String queue2Send, String newStatus,
                                String newOutStatus) throws Exception {
        DB db = mongoClient.getDB(dbName);
        DBCollection records = db.getCollection(this.getCollectionName());
        BasicDBObject query = new BasicDBObject("Processing.status", status2Match)
                .append("SourceInfo.SourceID", source.getResourceID());
        DBCursor cursor = records.find(query, new BasicDBObject("Processing.status", 1));
        if (newStatus == null) {
            try {
                while (cursor.hasNext()) {
                    DBObject dbo = cursor.next();
                    String oid = dbo.get("_id").toString();
                    JSONObject mb = prepareMessageBody(oid, status2Match, newOutStatus);
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
            Thread.sleep(1000);
            for (String oidStr : matchingOIdList) {
                ObjectId oid = new ObjectId(oidStr);
                BasicDBObject update = new BasicDBObject();
                update.append("$set", new BasicDBObject("Processing.status", newStatus));
                query = new BasicDBObject("_id", oid);
                records.update(query, update, false, false, WriteConcern.SAFE);
            }
            for (String oidStr : matchingOIdList) {
                JSONObject mb = prepareMessageBody(oidStr, newStatus, newOutStatus);
                sendMessage(mb, queue2Send);
            }
        }
    }

    public JSONObject prepareMessageBody(String oid, String status, String newOutStatus) {
        JSONObject json = new JSONObject();
        json.put("oid", oid);
        json.put("status", status);
        if (newOutStatus != null) {
            json.put("outStatus", newOutStatus);
        }
        return json;
    }

    public JSONObject prepareMessageBody(String cmd, Source source) throws Exception {
        return MessagingUtils.prepareMessageBody(cmd, source, this.config.getWorkflowMappings());
    }

    DocumentIngestionService getDocService() {
        return this.docService;
    }


    public List<SourceStats> getProcessingStats(String sourceID) {
        DocProcessingStatsService dpss = new DocProcessingStatsService();
        dpss.setMongoClient(this.mongoClient);
        dpss.setDbName(this.dbName);
        return dpss.getDocCountsPerStatusPerSource2(getCollectionName(), sourceID);
    }

    public Map<String, DocProcessingStatsService.WFStatusInfo> getWorkflowStatusInfo(String sourceID, List<SourceStats> ssList) {
        DocProcessingStatsService dpss = new DocProcessingStatsService();
        dpss.setMongoClient(this.mongoClient);
        dpss.setDbName(this.dbName);
        Workflow workflow = config.getWorkflows().get(0);
        String finishedStatus = workflow.getFinishedStatus();
        return dpss.getWorkflowStatusInfo(sourceID, finishedStatus, ssList);
    }

    public String getCollectionName() {
        return config.getCollectionName();
    }


}//;
