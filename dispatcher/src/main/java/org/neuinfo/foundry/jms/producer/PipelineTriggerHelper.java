package org.neuinfo.foundry.jms.producer;

import com.mongodb.*;
import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.log4j.Logger;
import org.bson.types.ObjectId;
import org.json.JSONObject;
import org.neuinfo.foundry.common.config.ServerInfo;
import org.neuinfo.foundry.common.ingestion.DocumentIngestionService;
import org.neuinfo.foundry.common.model.Source;
import org.neuinfo.foundry.common.util.MongoUtils;
import org.neuinfo.foundry.common.util.Utils;
import org.neuinfo.foundry.jms.common.ConfigLoader;
import org.neuinfo.foundry.jms.common.Configuration;
import org.neuinfo.foundry.jms.common.WorkflowMapping;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.*;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

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
        mongoClient = new MongoClient(servers);

        mongoClient.setWriteConcern(WriteConcern.SAFE);
        ConnectionFactory factory = new ActiveMQConnectionFactory(config.getBrokerURL());
        this.con = factory.createConnection();


        docService = new DocumentIngestionService();

        docService.start(this.config);
        // docService.initialize(this.dbName, mongoClient);
    }


    public void shutdown() {
        if (mongoClient != null) {
            mongoClient.close();
        }
        try {
            if (con != null) {
                logger.info("closing JMS connection");
                con.close();
            }
        } catch (JMSException x) {
            logger.error("shutdown", x);
            x.printStackTrace();
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
                        " to queue:" + this.queueName);
            }
            Message message = session.createObjectMessage(messageBody.toString());
            producer.send(destination, message);
        } finally {
            if (session != null) {
                session.close();
            }
        }
    }


    public void triggerPipeline(Source source, String status2Match, String queue2Send, String newStatus) throws Exception {
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

    public JSONObject prepareMessageBody(String oid, String status) {
        JSONObject json = new JSONObject();
        json.put("oid", oid);
        json.put("status", status);
        return json;
    }

    public JSONObject prepareMessageBody(String cmd, Source source) throws Exception {
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

    DocumentIngestionService getDocService() {
        return this.docService;
    }

    public String getCollectionName() {
        return config.getCollectionName();
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
}//;
