package org.neuinfo.foundry.consumers;

import com.mongodb.*;
import org.apache.log4j.Logger;
import org.bson.types.BSONTimestamp;
import org.bson.types.ObjectId;
import org.neuinfo.foundry.consumers.river.*;

import java.util.List;
import java.util.UUID;

/**
 * Created by bozyurt on 4/8/14.
 */
@Deprecated
public class DocIdAssigner implements Runnable {
    private final MongoDBRiverDefinition definition;
    private final Context context;
    private final static Logger logger = Logger.getLogger(DocIdAssigner.class);
    MongoClient mongoClient;

    public DocIdAssigner(MongoDBRiverDefinition definition, Context context) {
        this.definition = definition;
        this.context = context;

        List<ServerAddress> mongoServers = definition.getMongoServers();
        mongoClient = new MongoClient(mongoServers);
        // mongoClient.setWriteConcern(WriteConcern.SAFE);
    }

    @Override
    public void run() {
        try {
            while (context.getStatus() == Status.RUNNING) {
                try {
                    QueueEntry entry = context.getStream().take();

                    processQueueEntry(entry);

                } catch (InterruptedException x) {
                    logger.info("DocIdAssigner is interrupted");
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        } finally {
            if (mongoClient != null) {
                mongoClient.close();
            }
        }
    }

    private BSONTimestamp processQueueEntry(QueueEntry entry) {
        Operation operation = entry.getOperation();
        if (entry.getData().get(MongoDBRiverDefinition.MONGODB_ID_FIELD) == null &&
                (operation == Operation.INSERT || operation == Operation.UPDATE || operation == Operation.DELETE)) {
            logger.warn(String.format("Cannot get object id. Skip the current item: [%s]", entry.getData()));
            return null;
        }

        BSONTimestamp lastTimestamp = entry.getOplogTimestamp();
        String type;

        String objectId = "";
        if (entry.getData().get(MongoDBRiverDefinition.MONGODB_ID_FIELD) != null) {
            objectId = entry.getData().get(MongoDBRiverDefinition.MONGODB_ID_FIELD).toString();
        }

        if (operation == Operation.INSERT) {
            addDocId(objectId);
        }


        return lastTimestamp;
    }


    private void addDocId(String objectId) {
        logger.info("in addDocId");
        DB db = mongoClient.getDB("discotest");

        DBCollection records = db.getCollection("records");

        /*
        DBCursor cursor = records.find();
        try {
            while (cursor.hasNext()) {
                System.out.println(cursor.next());
            }
        } finally {
            cursor.close();
        }
        */


        BasicDBObject query = new BasicDBObject(MongoDBRiverDefinition.MONGODB_ID_FIELD, new ObjectId(objectId));

        DBObject theDoc = records.findOne(query);
        if (theDoc != null) {
            //System.out.println("theDoc:" + theDoc);
            // DBObject pi = (DBObject) theDoc.get("sourceInfo"); // orig
            DBObject pi = (DBObject) theDoc.get("Processing");


            if (pi != null) {
                System.out.println("pi:" + pi);
                String status = (String) pi.get("status");
                if (status.equals("new")) {
                    final UUID uuid = UUID.randomUUID();


                    pi.put("docId", uuid.toString());
                    pi.put("status", "id_added");
                    System.out.println("updating");
                    records.update(query, theDoc);
                    //System.out.println("updated " + theDoc);
                }
            }
        } else {
            logger.warn("Cannot find object with id:" + objectId);
        }
    }


}
