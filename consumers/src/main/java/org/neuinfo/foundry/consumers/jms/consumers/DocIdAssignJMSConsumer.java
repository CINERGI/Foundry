package org.neuinfo.foundry.consumers.jms.consumers;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import org.apache.log4j.Logger;
import org.bson.types.ObjectId;
import org.json.JSONObject;
import org.neuinfo.foundry.consumers.common.Constants;

import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.ObjectMessage;
import java.util.UUID;

/**
 * Created by bozyurt on 5/1/14.
 */
public class DocIdAssignJMSConsumer extends JMSConsumerSupport implements MessageListener {

    private final static Logger logger = Logger.getLogger(DocIdAssignJMSConsumer.class);

    public DocIdAssignJMSConsumer() {
        super("foundry.new");
    }

    public DocIdAssignJMSConsumer(String queueName) {
        super(queueName);
    }

    private void addDocId(String objectId) {
        logger.info("in addDocId");
        DB db = mongoClient.getDB(super.mongoDbName);

        DBCollection collection = db.getCollection(getCollectionName()); //  "records");

        BasicDBObject query = new BasicDBObject(Constants.MONGODB_ID_FIELD, new ObjectId(objectId));

        DBObject theDoc = collection.findOne(query);
        if (theDoc != null) {
            DBObject pi = (DBObject) theDoc.get("Processing");

            if (pi != null) {
                System.out.println("pi:" + pi);
                String status = (String) pi.get("status");
                if (status.equals(getInStatus())) { //  "new")) {
                    final UUID uuid = UUID.randomUUID();
                    pi.put("docId", uuid.toString());
                    pi.put("status", getOutStatus());
                    System.out.println("updating");
                    collection.update(query, theDoc);
                    System.out.println("updated " + theDoc);
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
            addDocId(objectId);
        } catch (Exception x) {
            //TODO proper error handling
            x.printStackTrace();
        }
    }

    public static void main(String[] args) throws Exception {
        DocIdAssignJMSConsumer consumer = new DocIdAssignJMSConsumer();
        String configFile = "consumers-cfg.xml";
        try {
            consumer.startup(configFile);

            consumer.handleMessages(consumer);

            System.out.print("Press a key to exit:");
            System.in.read();
        } finally {
            consumer.shutdown();
        }
    }


}
