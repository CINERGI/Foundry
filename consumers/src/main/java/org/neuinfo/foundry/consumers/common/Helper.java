package org.neuinfo.foundry.consumers.common;

import com.mongodb.*;
import org.bson.types.ObjectId;
import org.json.JSONObject;
import org.neuinfo.foundry.common.model.User;
import org.neuinfo.foundry.common.util.JSONUtils;
import org.neuinfo.foundry.consumers.jms.consumers.ConsumerSupport;

import javax.jms.JMSException;
import javax.jms.MessageListener;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by bozyurt on 4/7/15.
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

    public void saveUser(String username, String pwd) {
        DB db = mongoClient.getDB(super.mongoDbName);
        DBCollection users = db.getCollection("users");
        User user = new User.Builder(username, pwd, "").build();
        JSONObject json = user.toJSON();
        DBObject dbo = JSONUtils.encode(json, true);
        users.save(dbo);
    }

    public List<BasicDBObject> getDocWrappers(String sourceID) {
        DB db = mongoClient.getDB(super.mongoDbName);
        DBCollection collection = db.getCollection("records");
        DBCursor dbCursor;
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

    public DBObject getDocWrapper(String oid) {
        DB db = mongoClient.getDB(super.mongoDbName);
        DBCollection collection = db.getCollection("records");
        BasicDBObject query = new BasicDBObject("_id", new ObjectId(oid));
        return collection.findOne(query);
    }

    public List<String> getDocWrapperIds(String sourceID) {
        DB db = mongoClient.getDB(super.mongoDbName);
        DBCollection collection = db.getCollection("records");
        BasicDBObject query = new BasicDBObject("SourceInfo.SourceID", sourceID)
                .append("Processing.status", "finished");

        DBCursor cursor = collection.find(query, new BasicDBObject("Processing.status", 1));
        List<String> oidList = new LinkedList<String>();
        try {
            while (cursor.hasNext()) {
                DBObject dbo = cursor.next();
                String oid = dbo.get("_id").toString();
                oidList.add(oid);
            }
        } finally {
            cursor.close();
        }
        return oidList;
    }

    @Override
    public void handleMessages(MessageListener listener) throws JMSException {
        // no op
    }
}
