package org.neuinfo.foundry.consumers.util;

import com.mongodb.*;
import org.bson.types.ObjectId;
import org.neuinfo.foundry.consumers.common.Constants;
import org.neuinfo.foundry.consumers.jms.consumers.ConsumerSupport;

import javax.jms.JMSException;
import javax.jms.MessageListener;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by bozyurt on 12/10/14.
 */
public class Helper extends ConsumerSupport {

    public Helper(String queueName) {
        super(queueName);
    }

    public DBObject getDocWrapper() {
        DB db = mongoClient.getDB(super.mongoDbName);
        DBCollection collection = db.getCollection("records");
        return collection.findOne();
    }

    public void saveDocWrapper(DBObject dbo) {
        ObjectId oid = (ObjectId) dbo.get(Constants.MONGODB_ID_FIELD);
        DB db = mongoClient.getDB(super.mongoDbName);
        DBCollection collection = db.getCollection("records");
        BasicDBObject query = new BasicDBObject(Constants.MONGODB_ID_FIELD, oid);
        collection.update(query, dbo);
    }


    public DBObject findDocWrapper(String primaryKey) {
        DB db = mongoClient.getDB(super.mongoDbName);
        DBCollection collection = db.getCollection("records");
        BasicDBObject query = new BasicDBObject("primaryKey", primaryKey);

        return collection.findOne(query);
    }

    public List<BasicDBObject> getDocWrappers(String sourceID) {
        DB db = mongoClient.getDB(super.mongoDbName);
        DBCollection collection = db.getCollection("records");
        DBCursor dbCursor = null;
        if (sourceID != null) {
            BasicDBObject query = new BasicDBObject("SourceInfo.SourceID", sourceID);
            dbCursor = collection.find(query);
        } else {
            dbCursor = collection.find();
        }
        List<BasicDBObject> docWrappers = new ArrayList<BasicDBObject>(100);
        try {
            while (dbCursor.hasNext()) {
                docWrappers.add((BasicDBObject) dbCursor.next());
            }
        } finally {
            dbCursor.close();
        }
        return docWrappers;
    }

    @Override
    public void handleMessages(MessageListener listener) throws JMSException {
        // no op
    }
}
