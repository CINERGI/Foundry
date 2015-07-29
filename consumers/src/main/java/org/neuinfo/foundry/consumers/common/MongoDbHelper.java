package org.neuinfo.foundry.consumers.common;

import com.mongodb.*;
import org.neuinfo.foundry.common.model.BatchInfo;

/**
 * Created by bozyurt on 5/28/14.
 */
public class MongoDbHelper {
    private MongoClient mongoClient;
    private String dbName;

    public MongoDbHelper(String dbName, MongoClient mongoClient) {
        this.dbName = dbName;
        this.mongoClient = mongoClient;
    }

    public BatchInfo getBatchInfo(String nifId, String batchId) {
        DB db = mongoClient.getDB(dbName);
        DBCollection sources = db.getCollection("sources");
        BasicDBObject query = new BasicDBObject("nifId", nifId);
        BasicDBObject keys = new BasicDBObject("batchInfos", 1);
        final DBCursor cursor = sources.find(query, keys);
        BatchInfo bi = null;
        try {
            if (cursor.hasNext()) {
                DBObject dbObject = cursor.next();
                BasicDBList biList = (BasicDBList) dbObject.get("batchInfos");
                for (int i = 0; i < biList.size(); i++) {
                    final DBObject biDBO = (DBObject) biList.get(i);
                    final String aBatchId = (String) biDBO.get("batchId");
                    if (aBatchId.equals(batchId)) {
                        bi = BatchInfo.fromDbObject(biDBO);
                        break;
                    }
                }
            }
        } finally {
            cursor.close();
        }
        return bi;
    }
}
