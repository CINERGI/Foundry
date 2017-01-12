package org.neuinfo.foundry.ingestor;

import com.mongodb.*;
import org.json.JSONException;
import org.json.JSONObject;
import org.neuinfo.foundry.common.config.Configuration;
import org.neuinfo.foundry.common.util.JSONUtils;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * to create dummy documents for testing
 * Created by bozyurt on 4/8/14.
 */
public class SimpleIngestor {
    MongoClient mongoClient;
    private Configuration configuration;


    public void start() throws UnknownHostException {
        List<ServerAddress> servers = new ArrayList<ServerAddress>(2);
        InetAddress inetAddress = InetAddress.getByName("burak.crbs.ucsd.edu");
        servers.add(new ServerAddress(inetAddress, 27017));
        servers.add(new ServerAddress(inetAddress, 27018));

      // going to need to add auth
         mongoClient = new MongoClient(servers);
      //  mongoClient = new MongoClient(servers, configuration.getCredentialsList());

        mongoClient.setWriteConcern(WriteConcern.SAFE);
    }


    public void shutdown() {
        if (mongoClient != null) {
            mongoClient.close();
        }
    }


    public void insertDoc(JSONObject doc) {
        DB db = mongoClient.getDB("discotest");

        DBCollection records = db.getCollection("records");

        DBObject dbObject = JSONUtils.encode(doc);

        records.insert(dbObject);
    }

    public void deleteAllRecords() {
        DB db = mongoClient.getDB("discotest");

        DBCollection records = db.getCollection("records");
        records.remove(new BasicDBObject());
    }

    public JSONObject prepareDocument(JSONObject payload) throws JSONException {
        JSONObject js = new JSONObject();
        JSONObject sourceInfoJS = new JSONObject();
        JSONObject crawlInfoJS = new JSONObject();
        crawlInfoJS.put("crawledDate", new Date());
        crawlInfoJS.put("crawler", "SimpleIngestor");
        sourceInfoJS.put("status", "new");

        js.put("crawlInfo", crawlInfoJS);
        js.put("sourceInfo", sourceInfoJS);
        js.put("original", payload);

        return js;
    }


    public JSONObject createRecordJSON() throws JSONException {
        JSONObject js = new JSONObject();
        for (int i = 1; i < 10; i++) {
            js.put("column" + i, i);
        }
        return js;
    }

    public static void main(String[] args) throws Exception {
        SimpleIngestor ingestor = new SimpleIngestor();
        try {
            ingestor.start();

            JSONObject doc = ingestor.prepareDocument(ingestor.createRecordJSON());

             ingestor.insertDoc(doc);

           // ingestor.deleteAllRecords();

        } finally {
            ingestor.shutdown();
        }

    }
}
