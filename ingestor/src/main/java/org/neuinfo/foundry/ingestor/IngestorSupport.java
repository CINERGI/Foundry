package org.neuinfo.foundry.ingestor;

import com.mongodb.*;
import org.json.JSONException;
import org.json.JSONObject;
import org.neuinfo.foundry.common.config.ServerInfo;
import org.neuinfo.foundry.common.util.JSONUtils;
import org.neuinfo.foundry.ingestor.common.ConfigLoader;
import org.neuinfo.foundry.common.ingestion.Configuration;

import java.net.InetAddress;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Created by bozyurt on 4/29/14.
 */
public class IngestorSupport {
    protected MongoClient mongoClient;
    protected String dbName;
    protected String collectionName = "records";


    public IngestorSupport() {
    }

    public IngestorSupport(String collectionName) {
        this.collectionName = collectionName;
    }

    public void start() throws Exception {
        Configuration conf = ConfigLoader.load("ingestor-cfg.xml");
        this.dbName = conf.getMongoDBName();
        List<ServerAddress> servers = new ArrayList<ServerAddress>(conf.getServers().size());
        for (ServerInfo si : conf.getServers()) {
            InetAddress inetAddress = InetAddress.getByName(si.getHost());
            servers.add(new ServerAddress(inetAddress, si.getPort()));
        }
        mongoClient = new MongoClient(servers);
        mongoClient.setWriteConcern(WriteConcern.SAFE);
    }

    public void shutdown() {
        if (mongoClient != null) {
            mongoClient.close();
        }
    }

    public void insertDoc(JSONObject doc) {
        DB db = mongoClient.getDB(this.dbName);
        DBCollection records = db.getCollection(this.collectionName);
        DBObject dbObject = JSONUtils.encode(doc);
        records.insert(dbObject);
    }

    public void deleteAllRecords() {
        DB db = mongoClient.getDB(this.dbName);

        DBCollection records = db.getCollection("records");
        records.remove(new BasicDBObject());
    }

    public JSONObject prepareDocument(JSONObject payload) throws JSONException {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX");
        JSONObject js = new JSONObject();
        JSONObject sourceInfoJS = new JSONObject();
        JSONObject historyJS = new JSONObject();
        JSONObject dataJS = new JSONObject();
        JSONObject processingJS = new JSONObject();

        js.put("Version", "1");
        js.put("CrawlDate", sdf.format(new Date()));
        js.put("CrawlMethod", "unknown");
        js.put("IndexDate", sdf.format(new Date()));

        sourceInfoJS.put("SourceID", "nif-0000-99999");
        sourceInfoJS.put("ViewID", "nif-0000-99999-1");
        sourceInfoJS.put("Name", "CINERGI");


        processingJS.put("status", "new");
        js.put("SourceInfo", sourceInfoJS);
        js.put("Data", dataJS);
        js.put("OriginalDoc", payload);

        js.put("Processing", processingJS);
        js.put("History", historyJS);

        return js;
    }
}
