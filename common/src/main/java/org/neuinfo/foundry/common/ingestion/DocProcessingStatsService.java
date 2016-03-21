package org.neuinfo.foundry.common.ingestion;

import com.mongodb.*;

import java.io.Serializable;
import java.util.*;


/**
 * Created by bozyurt on 12/9/15.
 */
public class DocProcessingStatsService extends BaseIngestionService {

    public void setMongoClient(MongoClient mc) {
        super.mongoClient = mc;
    }

    public void setDbName(String dbName) {
        super.dbName = dbName;
    }

    public List<SourceStats> getDocCountsPerStatusPerSource2(String collectionName) {
        DB db = mongoClient.getDB(dbName);
         DBCollection collection = db.getCollection(collectionName);
        List sourceIDs = collection.distinct("SourceInfo.SourceID");
        List statusList = collection.distinct("Processing.status");
        List<SourceStats> ssList = new ArrayList<SourceStats>(sourceIDs.size());
        for(Object sourceID : sourceIDs) {
            // System.out.println(sourceID);
            SourceStats ss = new SourceStats(sourceID.toString());
            int totCount = 0;
            for(Object status : statusList) {
                BasicDBObject query = new BasicDBObject("SourceInfo.SourceID", sourceID)
                        .append("Processing.status", status);
                int count = (int) collection.count(query);
                String statusStr = status.toString();
                if (count > 0) {
                    totCount += count;
                    ss.put(statusStr, count);
                } else {
                    if (statusStr.equals("error")) {
                        ss.put(statusStr, count);
                    }
                }
            }
            if (totCount > 0) {
                ssList.add(ss);
            }
        }
        return ssList;
    }

    public static class SourceStats implements Serializable{
        final String sourceID;
        Map<String, Integer> statusCountMap = new HashMap<String, Integer>(3);

        public SourceStats(String sourceID) {
            this.sourceID = sourceID;
        }

        public void put(String status, int count) {
            statusCountMap.put(status, count);
        }

        public String getSourceID() {
            return sourceID;
        }

        public Map<String, Integer> getStatusCountMap() {
            return statusCountMap;
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder(80);
            sb.append("SourceStats::[").append("sourceID:").append(sourceID);
            for (String status : statusCountMap.keySet()) {
                sb.append("\n\t").append(status).append(": ").append(statusCountMap.get(status));
            }
            sb.append("]");
            return sb.toString();
        }
    }
}
