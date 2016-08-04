package org.neuinfo.foundry.common.ingestion;

import com.mongodb.*;
import org.json.JSONObject;
import org.neuinfo.foundry.common.model.BatchInfo;
import org.neuinfo.foundry.common.model.Source;
import org.neuinfo.foundry.common.util.Assertion;
import org.neuinfo.foundry.common.util.JSONUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by bozyurt on 5/27/14.
 */
public class SourceIngestionService extends BaseIngestionService {

    public void setMongoClient(MongoClient mc) {
        Assertion.assertTrue(this.mongoClient == null);
        this.mongoClient = mc;
    }

    public void setMongoDBName(String dbName) {
        this.dbName = dbName;
    }

    public void saveSource(Source source) {
        DB db = mongoClient.getDB(dbName);
        DBCollection sources = db.getCollection("sources");
        final JSONObject json = source.toJSON();

        final DBObject sourceDbObj = JSONUtils.encode(json, true);
        sources.insert(sourceDbObj);
    }

    public void deleteSource(Source source) {
        DB db = mongoClient.getDB(dbName);
        DBCollection sources = db.getCollection("sources");
        BasicDBObject query = new BasicDBObject("sourceInformation.resourceID", source.getResourceID());
        sources.remove(query);
    }

    public List<Source> getAllSources() {
        DB db = mongoClient.getDB(dbName);
        DBCollection sources = db.getCollection("sources");
        DBCursor cursor = sources.find();
        List<Source> sourceList = new ArrayList<Source>();
        try {
            while (cursor.hasNext()) {
                BasicDBObject dbo = (BasicDBObject) cursor.next();
                sourceList.add(Source.fromDBObject(dbo));
            }
            return sourceList;
        } finally {
            cursor.close();
        }
    }

    public Source findOrAssignIDandSaveSource(Source source) {
        DB db = mongoClient.getDB(dbName);
        DBCollection sources = db.getCollection("sources");
        BasicDBObject query = new BasicDBObject("sourceInformation.name", source.getName());
        BasicDBObject srcDBO = (BasicDBObject) sources.findOne(query);
        if (srcDBO != null) {
            return Source.fromDBObject(srcDBO);
        }

        ResourceID rid = getLatestResourceID(sources);
        String newResourceID = "cinergi-" + String.format("%04d", rid.id + 1);
        source.setResourceID(newResourceID);

        final JSONObject json = source.toJSON();

        final DBObject sourceDbObj = JSONUtils.encode(json, true);

        try {
            WriteResult wr = sources.insert(sourceDbObj, WriteConcern.JOURNAL_SAFE);
        } catch (MongoException me) {
            me.printStackTrace();
            return null;
        }
        return source;
    }

    ResourceID getLatestResourceID(DBCollection sources) {
        BasicDBObject keys = new BasicDBObject("sourceInformation.resourceID", 1);
        DBCursor cursor = sources.find(new BasicDBObject(), keys);
        ResourceID latestResourceID = null;
        try {
            while (cursor.hasNext()) {
                DBObject dbObject = cursor.next();
                BasicDBObject dbo = (BasicDBObject) dbObject.get("sourceInformation");
                String resourceID = dbo.getString("resourceID");
                if (resourceID.startsWith("cinergi")) {
                    if (latestResourceID == null) {
                        latestResourceID = new ResourceID(resourceID);
                    } else {
                        ResourceID rid = new ResourceID(resourceID);
                        if (rid.compareTo(latestResourceID) > 0) {
                            latestResourceID = rid;
                        }
                    }
                }
            }
        } finally {
            cursor.close();
        }
        return latestResourceID;
    }


    public static class ResourceID implements Comparable<ResourceID> {
        final String resourceID;
        int id;

        public ResourceID(String resourceID) {
            this.resourceID = resourceID;
            int idx = resourceID.indexOf('-');
            Assertion.assertTrue(idx != -1);
            this.id = Integer.parseInt(resourceID.substring(idx + 1));
        }

        public String getResourceID() {
            return resourceID;
        }

        @Override
        public int compareTo(ResourceID other) {
            return id - other.id;
        }
    }

    public BatchInfo getBatchInfo(String nifId, String batchId) {
        DB db = mongoClient.getDB(dbName);
        DBCollection sources = db.getCollection("sources");
        BasicDBObject query = new BasicDBObject("sourceInformation.resourceID", nifId);
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

    public void addUpdateBatchInfo(String nifId, String dataSource, BatchInfo bi) {
        DB db = mongoClient.getDB(dbName);
        DBCollection sources = db.getCollection("sources");
        BasicDBObject query = new BasicDBObject("sourceInformation.resourceID", nifId)
                .append("sourceInformation.dataSource", dataSource);
        BasicDBObject keys = new BasicDBObject("batchInfos", 1);
        final DBCursor cursor = sources.find(query, keys);
        boolean updated = false;
        BasicDBList biList = null;
        try {
            if (cursor.hasNext()) {
                DBObject dbObject = cursor.next();
                biList = (BasicDBList) dbObject.get("batchInfos");
                for (int i = 0; i < biList.size(); i++) {
                    final DBObject biDBO = (DBObject) biList.get(i);
                    final String aBatchId = (String) biDBO.get("batchId");
                    if (aBatchId.equals(bi.getBatchId())) {
                        biDBO.put("status", bi.getStatus().getCode());
                        biDBO.put("submittedCount", bi.getSubmittedCount());
                        biDBO.put("ingestedCount", bi.getIngestedCount());
                        updated = true;
                        break;
                    }
                }
            }
        } finally {
            cursor.close();
        }
        //
        if (updated) {
            BasicDBObject update = new BasicDBObject("$set", new BasicDBObject("batchInfos", biList));
            sources.update(query, update);
        } else {
            final DBObject dbObject = JSONUtils.encode(bi.toJSON());
            BasicDBObject update = new BasicDBObject("$push", new BasicDBObject("batchInfos", dbObject));
            sources.update(query, update);
        }
    }

    /**
     * @param nifId
     * @param bi
     * @deprecated
     */
    public void updateBatchInfo(String nifId, BatchInfo bi) {
        DB db = mongoClient.getDB(dbName);
        DBCollection sources = db.getCollection("sources");
        BasicDBObject query = new BasicDBObject("sourceInformation.resourceID", nifId);
        BasicDBObject keys = new BasicDBObject("batchInfos", 1);
        final DBCursor cursor = sources.find(query, keys);
        boolean updated = false;
        BasicDBList biList = null;
        try {
            if (cursor.hasNext()) {
                DBObject dbObject = cursor.next();
                biList = (BasicDBList) dbObject.get("batchInfos");
                for (int i = 0; i < biList.size(); i++) {
                    final DBObject biDBO = (DBObject) biList.get(i);
                    final String aBatchId = (String) biDBO.get("batchId");
                    if (aBatchId.equals(bi.getBatchId())) {
                        biDBO.put("status", bi.getStatus().getCode());
                        biDBO.put("submittedCount", bi.getSubmittedCount());
                        biDBO.put("ingestedCount", bi.getIngestedCount());
                        updated = true;
                    }
                }
            }
        } finally {
            cursor.close();
        }
        if (updated) {
            BasicDBObject update = new BasicDBObject("$set", new BasicDBObject("batchInfos", biList));
            sources.update(query, update);
        }
    }
}
