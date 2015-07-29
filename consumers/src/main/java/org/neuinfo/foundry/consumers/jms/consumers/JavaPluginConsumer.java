package org.neuinfo.foundry.consumers.jms.consumers;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import org.apache.log4j.Logger;
import org.bson.types.ObjectId;
import org.json.JSONObject;
import org.neuinfo.foundry.common.util.JSONUtils;
import org.neuinfo.foundry.consumers.common.Constants;
import org.neuinfo.foundry.consumers.plugin.IPlugin;
import org.neuinfo.foundry.consumers.plugin.Pluggable;
import org.neuinfo.foundry.consumers.plugin.Result;

import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.ObjectMessage;

/**
 * Created by bozyurt on 10/27/14.
 */
public class JavaPluginConsumer extends JMSConsumerSupport implements MessageListener, Pluggable {
    private IPlugin plugin;
    private final static Logger logger = Logger.getLogger(JavaPluginConsumer.class);

    public JavaPluginConsumer(String queueName) {
        super(queueName);
    }

    void handle(String objectId) throws Exception {
        DB db = mongoClient.getDB(super.mongoDbName);
        DBCollection collection = db.getCollection(getCollectionName());
        BasicDBObject query = new BasicDBObject(Constants.MONGODB_ID_FIELD, new ObjectId(objectId));
        DBObject theDoc = collection.findOne(query);
        if (theDoc != null) {
            DBObject pi = (DBObject) theDoc.get("Processing");
            if (pi != null) {
                System.out.println("pi:" + pi);
                String status = (String) pi.get("status");
                BasicDBObject origDoc = (BasicDBObject) theDoc.get("OriginalDoc");
                if (origDoc != null && status != null && status.equals(getInStatus())) {
                    try {
                        Result result = getPlugin().handle(theDoc);

                        if (result.getStatus() == Result.Status.OK_WITH_CHANGE) {
                            theDoc = result.getDocWrapper();
                            pi = (DBObject) theDoc.get("Processing");
                            pi.put("status", getOutStatus());
                            collection.update(query, theDoc);
                        } else if (result.getStatus() == Result.Status.OK_WITHOUT_CHANGE) {
                            pi.put("status", getOutStatus());
                            collection.update(query, theDoc);
                        } else {
                            pi.put("status", "error");
                            logger.info("updating pi:" + pi.toString());
                            collection.update(query, theDoc);
                        }

                    } catch (Throwable t) {
                        logger.error("Error", t);
                       // t.printStackTrace();
                        if (pi != null) {
                            pi.put("status", "error");
                            logger.info("updating pi:" + pi.toString());
                            collection.update(query, theDoc);
                        }
                    }
                }
            }
        } else {
            logger.warn("Cannot find object with id:" + objectId);
        }
    }

    @Override
    public void onMessage(Message message) {
        try {
            ObjectMessage om = (ObjectMessage) message;
            String payload = (String) om.getObject();
            System.out.println("payload:" + payload);
            JSONObject json = new JSONObject(payload);
            String status = json.getString("status");
            String objectId = json.getString("oid");
            System.out.format("status:%s objectId:%s%n", status, objectId);
            handle(objectId);
        } catch (Exception x) {
            //TODO proper error handling
            x.printStackTrace();
        }
    }

    @Override
    public void setPlugin(IPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public IPlugin getPlugin() {
        return this.plugin;
    }
}
