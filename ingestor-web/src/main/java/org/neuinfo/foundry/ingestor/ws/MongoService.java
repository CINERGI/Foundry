package org.neuinfo.foundry.ingestor.ws;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.fge.jackson.JsonLoader;
import com.github.fge.jsonschema.core.report.ProcessingReport;
import com.github.fge.jsonschema.main.JsonSchema;
import com.github.fge.jsonschema.main.JsonSchemaFactory;
import com.mongodb.*;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;
import org.json.JSONArray;
import org.json.JSONObject;
import org.neuinfo.foundry.common.config.ServerInfo;
import org.neuinfo.foundry.common.ingestion.Configuration;
import org.neuinfo.foundry.common.ingestion.SourceIngestionService;
import org.neuinfo.foundry.common.model.*;
import org.neuinfo.foundry.common.util.Assertion;
import org.neuinfo.foundry.common.util.JSONPathProcessor;
import org.neuinfo.foundry.common.util.JSONUtils;
import org.neuinfo.foundry.ingestor.common.ConfigLoader;

import java.net.InetAddress;
import java.util.*;

import static com.mongodb.client.model.Filters.and;
import static com.mongodb.client.model.Filters.eq;

/**
 * Created by bozyurt on 7/17/14.
 */
public class MongoService {
    MongoClient mongoClient;
    String dbName;


    public MongoClient getMongoClient() {
        return mongoClient;
    }

    public String getDbName() {
        return dbName;
    }

    public MongoService() throws Exception {
        Configuration conf = ConfigLoader.load("ingestor-cfg.xml", false);
        this.dbName = conf.getMongoDBName();
        List<ServerAddress> servers = new ArrayList<ServerAddress>(conf.getServers().size());
        String user = conf.getServers().get(0).getUser();
        String pwd = conf.getServers().get(0).getPwd();
        for (ServerInfo si : conf.getServers()) {
            InetAddress inetAddress = InetAddress.getByName(si.getHost());
            servers.add(new ServerAddress(inetAddress, si.getPort()));
        }
        if (user != null && pwd != null) {
            MongoCredential credential = MongoCredential.createCredential(user, conf.getMongoDBName(), pwd.toCharArray());
            mongoClient = new MongoClient(servers, Arrays.asList(credential));
        } else {
            mongoClient = new MongoClient(servers);
        }
        mongoClient.setWriteConcern(WriteConcern.ACKNOWLEDGED);
    }

    public void shutdown() {
        if (mongoClient != null) {
            mongoClient.close();
            mongoClient = null;
        }
    }

    public List<JSONObject> getAllSources() {
        List<JSONObject> list = new LinkedList<JSONObject>();
        MongoDatabase db = mongoClient.getDatabase(dbName);
        MongoCollection<Document> sources = db.getCollection("sources");
        MongoCursor<Document> cursor = sources.find().iterator();
        try {
            while (cursor.hasNext()) {
                Document sourceDoc = cursor.next();
                final JSONObject js = JSONUtils.toJSON(sourceDoc, true);
                list.add(js);
            }
        } finally {
            cursor.close();
        }
        return list;
    }

    public JSONObject getSource(String nifId) {
        MongoDatabase db = mongoClient.getDatabase(dbName);
        MongoCollection<Document> sources = db.getCollection("sources");
        Document source = sources.find(eq("sourceInformation.resourceID", nifId)).first();

        // BasicDBObject source = (BasicDBObject) sources.findOne(query);
        if (source == null) {
            return null;
        }
        return JSONUtils.toJSON(source, true);
    }

    public JSONArray findDocumentIds4Source(String nifId, Set<String> statusSet) {
        DB db = mongoClient.getDB(dbName);
        DBCollection records = db.getCollection("records");
        BasicDBObject query = new BasicDBObject("SourceInfo.SourceID", nifId);
        if (statusSet != null && !statusSet.isEmpty()) {
            List<String> statusList = new ArrayList<String>(statusSet);
            query.append("Processing.status", new BasicDBObject("$in", statusList));
        }
        BasicDBObject keys = new BasicDBObject("primaryKey", 1);
        DBCursor cursor = records.find(query, keys);
        JSONArray jsArr = new JSONArray();
        try {
            while (cursor.hasNext()) {
                BasicDBObject dbObject = (BasicDBObject) cursor.next();
                JSONObject js = JSONUtils.toJSON(dbObject, false);
                jsArr.put(js.getString("primaryKey"));
            }
        } finally {
            cursor.close();
        }
        return jsArr;
    }


    public JSONObject findDocument(String nifId, String docId) {
        MongoDatabase db = mongoClient.getDatabase(dbName);
        MongoCollection<Document> records = db.getCollection("records");
        //BasicDBObject query = new BasicDBObject("primaryKey", docId).append("SourceInfo.SourceID", nifId);
        Bson query = and(eq("primaryKey", docId), eq("SourceInfo.SourceID", nifId));
        Document document = records.find(query).first();
        if (document == null) {
            return null;
        }
        return JSONUtils.toJSON(document, true);
    }

    public JSONObject findOriginalDocument(String nifId, String docId) {
        DB db = mongoClient.getDB(dbName);
        DBCollection records = db.getCollection("records");
        BasicDBObject query = new BasicDBObject("primaryKey", docId).append("SourceInfo.SourceID", nifId);
        BasicDBObject fields = new BasicDBObject("OriginalDoc", 1);
        final BasicDBObject dbObject = (BasicDBObject) records.findOne(query);
        if (dbObject == null) {
            return null;
        }
        return JSONUtils.toJSON((BasicDBObject) dbObject.get("OriginalDoc"), false);
    }

    public BasicDBObject findTheDocument(String resourceId, String docId) {
        DB db = mongoClient.getDB(dbName);
        DBCollection records = db.getCollection("records");
        BasicDBObject query = new BasicDBObject("primaryKey", docId).append("SourceInfo.SourceID", resourceId);
        return (BasicDBObject) records.findOne(query);
    }

    public BasicDBObject findTheDocument(String docId) {
        DB db = mongoClient.getDB(dbName);
        DBCollection records = db.getCollection("records");
        BasicDBObject query = new BasicDBObject("primaryKey", docId);
        return (BasicDBObject) records.findOne(query);
    }


    public boolean hasDocument(String nifId, String docId) {
        //DB db = mongoClient.getDB(dbName);
        MongoDatabase db = mongoClient.getDatabase(dbName);
        MongoCollection<Document> records = db.getCollection("records");
        // BasicDBObject query = new BasicDBObject("primaryKey", docId).append("SourceInfo.SourceID", nifId);
        Bson query = and(eq("primaryKey", docId), eq("SourceInfo.SourceID", nifId));
        // BasicDBObject fields = new BasicDBObject("primaryKey", 1);
        // return records.findOne(query, fields) != null;
        return records.count(query) > 0;
    }

    public Source findSource(String resourceID) {
        // BasicDBObject query = new BasicDBObject("sourceInformation.resourceID", resourceID);
        MongoDatabase db = mongoClient.getDatabase(dbName);
        MongoCollection<Document> sources = db.getCollection("sources");
        MongoCursor<Document> cursor = sources.find(eq("sourceInformation.resourceID", resourceID)).iterator();

        Source source = null;
        try {
            if (cursor.hasNext()) {
                Document sourceDoc = cursor.next();
                // source = Source.fromDBObject(dbObject);
                source = Source.fromJSON(new JSONObject(sourceDoc.toJson()));
            }
        } finally {
            cursor.close();
        }
        return source;
    }


    public Organization findOrganization(String orgName, String objectId) {
        //DB db = mongoClient.getDB(dbName);
        MongoDatabase db = mongoClient.getDatabase(dbName);
        MongoCollection<Document> organizations = db.getCollection("organizations");
        /*
        BasicDBObject query;
        if (objectId != null) {
            query = new BasicDBObject("_id", new ObjectId(objectId));
        } else {
            query = new BasicDBObject("name", orgName);
        }
        */
        Bson query;
        if (objectId != null) {
            query = eq("_id", new ObjectId(objectId));
        } else {
            query = eq("name", orgName);
        }

        MongoCursor<Document> cursor = organizations.find(query).iterator();
        Organization org = null;
        try {
            Document doc = cursor.next();
            org = Organization.fromDocument(doc);
        } finally {
            cursor.close();
        }
        return org;
    }

    public ObjectId saveOrganization(String orgName) throws Exception {
        MongoDatabase db = mongoClient.getDatabase(dbName);
        MongoCollection<Document> organizations = db.getCollection("organizations");
        Organization org = new Organization(orgName);
        JSONObject json = org.toJSON();
        Document doc = Document.parse(json.toString());
        //DBObject dbObject = JSONUtils.encode(json, true);
        organizations.insertOne(doc);
        // ObjectId id = (ObjectId) dbObject.get("_id");
        ObjectId id = doc.getObjectId("_id");
        return id;
    }


    public void removeOrganization(String orgName, String objectId) throws Exception {
        DB db = mongoClient.getDB(dbName);
        DBCollection organizations = db.getCollection("organizations");
        BasicDBObject query;
        if (objectId != null) {
            query = new BasicDBObject("_id", new ObjectId(objectId));
        } else {
            query = new BasicDBObject("name", orgName);
        }

        WriteResult remove = organizations.remove(query);
        System.out.println("remove:" + remove);
    }

    public ObjectId saveUser(User user) throws Exception {
        DB db = mongoClient.getDB(dbName);
        DBCollection users = db.getCollection("users");
        JSONObject json = user.toJSON();
        DBObject dbo = JSONUtils.encode(json, true);
        users.save(dbo);
        ObjectId id = (ObjectId) dbo.get("_id");
        return id;
    }

    public User findUser(String userName, String objectId) {
        DB db = mongoClient.getDB(dbName);
        DBCollection users = db.getCollection("users");
        BasicDBObject query;
        if (objectId != null) {
            query = new BasicDBObject("_id", new ObjectId(objectId));
        } else {
            query = new BasicDBObject("username", userName);
        }
        DBCursor cursor = users.find(query);
        User user = null;
        try {
            DBObject dbObject = cursor.next();
            user = User.fromDBObject(dbObject);
        } finally {
            cursor.close();
        }
        return user;
    }

    public void removeUser(String userName, String objectId) throws Exception {
        DB db = mongoClient.getDB(dbName);
        DBCollection users = db.getCollection("users");
        BasicDBObject query;
        if (objectId != null) {
            query = new BasicDBObject("_id", new ObjectId(objectId));
        } else {
            query = new BasicDBObject("username", userName);
        }
        users.remove(query);
    }


    public void saveDocument(JSONObject payload, String batchId, String sourceId,
                             String sourceName, boolean validate,
                             Source source, String primaryKey) throws Exception {
        JsonSchema schema = null;

        if (validate && source.getSchema() != null) {
            JsonSchemaFactory factory = JsonSchemaFactory.byDefault();
            String jsonStr = source.getSchema().toString(2);
            System.out.println(jsonStr);
            JsonNode schemaJSON = JsonLoader.fromString(jsonStr);
            schema = factory.getJsonSchema(schemaJSON);
        }

        if (schema != null && validate) {
            // validate the payload
            final JsonNode json = JsonLoader.fromString(payload.toString());
            ProcessingReport report = schema.validate(json);
            if (!report.isSuccess()) {
                throw new Exception(report.toString());
            }
        }

        if (primaryKey == null) {
            //          JSONObject pkJS = source.getPrimaryKey();
            PrimaryKeyDef pkDef = source.getPrimaryKeyDef();
            primaryKey = pkDef.prepPrimaryKey(payload);
//            primaryKey = prepPrimaryKey(payload, pkJS.getJSONArray("key"));
        }

        DocWrapper.Builder builder = new DocWrapper.Builder("new");
        final DocWrapper docWrapper = builder.batchId(batchId).payload(payload).version(1)
                .crawlDate(new Date()).sourceId(sourceId)
                .sourceName(sourceName).primaryKey(primaryKey).build();


        DB db = mongoClient.getDB(dbName);
        DBCollection records = db.getCollection("cinergiRecords");

        // now save the doc
        JSONObject json = docWrapper.toJSON();
        DBObject dbObject = JSONUtils.encode(json, true);

        records.insert(dbObject);
    }


    public static String prepPrimaryKey(JSONObject payload, JSONArray pkJSArr) throws Exception {
        StringBuilder sb = new StringBuilder();
        JSONPathProcessor processor = new JSONPathProcessor();
        int len = pkJSArr.length();
        for (int i = 0; i < len; i++) {
            String jsonPathKey = pkJSArr.getString(i);
            final List<Object> objects = processor.find(jsonPathKey, payload);
            Assertion.assertTrue(objects.size() == 1);
            final Object o = objects.get(0);
            sb.append(o.toString());
            if ((i + 1) < len) {
                sb.append("__");
            }
        }
        return sb.toString();
    }

    public void beginBatch(String nifId, String dataSource, String batchId, boolean successfulIngestion) {
        SourceIngestionService sis = new SourceIngestionService();
        sis.setMongoClient(this.mongoClient);
        sis.setMongoDBName(this.dbName);
        BatchInfo bi = new BatchInfo(batchId, Status.IN_PROCESS);
        bi.setIngestedCount(0);
        if (successfulIngestion) {
            bi.setIngestedCount(1);
        }
        bi.setSubmittedCount(1);

        sis.addUpdateBatchInfo(nifId, dataSource, bi);
    }

    public void updateBatch(String nifId, String dataSource, String batchId, boolean successfulIngestion) {
        SourceIngestionService sis = new SourceIngestionService();
        sis.setMongoClient(this.mongoClient);
        sis.setMongoDBName(this.dbName);

        final BatchInfo bi = sis.getBatchInfo(nifId, batchId);
        bi.setSubmittedCount(bi.getSubmittedCount() + 1);
        if (successfulIngestion) {
            bi.setIngestedCount(bi.getIngestedCount() + 1);
        }
        sis.addUpdateBatchInfo(nifId, dataSource, bi);
    }

    public void endBatch(String nifId, String dataSource, String batchId) {
        SourceIngestionService sis = new SourceIngestionService();
        sis.setMongoClient(this.mongoClient);
        sis.setMongoDBName(this.dbName);
        final BatchInfo bi = sis.getBatchInfo(nifId, batchId);
        bi.setStatus(Status.FINISHED);
        sis.addUpdateBatchInfo(nifId, dataSource, bi);
    }

}
