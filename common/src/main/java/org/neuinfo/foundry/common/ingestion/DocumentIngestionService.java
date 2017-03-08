package org.neuinfo.foundry.common.ingestion;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.fge.jackson.JsonLoader;
import com.github.fge.jsonschema.core.exceptions.ProcessingException;
import com.github.fge.jsonschema.core.report.ProcessingReport;
import com.github.fge.jsonschema.main.JsonSchema;
import com.github.fge.jsonschema.main.JsonSchemaFactory;
import com.mongodb.*;
import org.bson.types.ObjectId;
import org.json.JSONArray;
import org.json.JSONObject;
import org.neuinfo.foundry.common.model.*;
import org.neuinfo.foundry.common.util.Assertion;
import org.neuinfo.foundry.common.util.JSONPathProcessor;
import org.neuinfo.foundry.common.util.JSONUtils;
import org.neuinfo.foundry.common.util.MongoUtils;

import java.io.IOException;
import java.util.Date;
import java.util.List;

/**
 * Created by bozyurt on 5/27/14.
 */
public class DocumentIngestionService extends BaseIngestionService {
    private Source source;
    private JsonSchema schema;


    /**
     * to be used for web services, other use <code>startup()</code>,
     * <code>shutdown()</code> lifecycle methods
     *
     * @param dbName
     * @param mongoClient
     */
    public void initialize(String dbName,
                           MongoClient mongoClient) {
        this.dbName = dbName;
        this.mongoClient = mongoClient;
    }

    public void beginBatch(Source source, String batchId) {
        SourceIngestionService sis = new SourceIngestionService();
        sis.setMongoClient(this.mongoClient);
        sis.setMongoDBName(this.dbName);
        BatchInfo bi = new BatchInfo(batchId, Status.IN_PROCESS);
        bi.setIngestedCount(0);
        bi.setSubmittedCount(0);
        bi.setUpdatedCount(0);
        bi.setIngestionStatus(Status.IN_PROCESS);
        bi.setIngestionStartDatetime(new Date());
        sis.addUpdateBatchInfo(source.getResourceID(), source.getDataSource(), bi);
    }

    public void endBatch(Source source, String batchId,
                         int ingestedCount, int submittedCount, int updatedCount) {
        SourceIngestionService sis = new SourceIngestionService();
        sis.setMongoClient(this.mongoClient);
        sis.setMongoDBName(this.dbName);
        BatchInfo bi = new BatchInfo(batchId, Status.FINISHED);
        bi.setSubmittedCount(submittedCount);
        bi.setIngestedCount(ingestedCount);
        bi.setUpdatedCount(updatedCount);
        bi.setIngestionStatus(Status.FINISHED);
        bi.setIngestionEndDatetime(new Date());
        sis.addUpdateBatchInfo(source.getResourceID(), source.getDataSource(), bi);
    }

    public Source findSource(String nifId, String dataSource) {
        BasicDBObject query = new BasicDBObject("sourceInformation.resourceID", nifId);
        if (dataSource != null) {
            query.append("sourceInformation.dataSource", dataSource);
        }
        DB db = mongoClient.getDB(dbName);
        DBCollection sources = db.getCollection("sources");
        return MongoUtils.getSource(query, sources);

    }

    public void deleteDocuments4Resource(String collectionName, String resourceId, String dataSource) {
        BasicDBObject query = new BasicDBObject("SourceInfo.SourceID", resourceId);
        if (dataSource != null) {
            query.append("SourceInfo.DataSource", dataSource);
        }
        DB db = mongoClient.getDB(dbName);
        DBCollection records = db.getCollection(collectionName);
        long count = records.count(query);
        System.out.println("found " + count + " records.");
        System.out.println("deleting...");
        records.remove(query);
    }

    public void removeDocument(DBObject document, String collectionName) {
        BasicDBObject query = new BasicDBObject();
        query.put("_id", document.get("_id"));
        DB db = mongoClient.getDB(dbName);
        DBCollection records = db.getCollection(collectionName);
        records.remove(query);
    }

    public BasicDBObject findDocument(JSONObject payload, String collectionName) throws Exception {
        DB db = mongoClient.getDB(dbName);
        DBCollection collection = db.getCollection(collectionName);
        PrimaryKeyDef pkDef = source.getPrimaryKeyDef();

        String primaryKey = pkDef.prepPrimaryKey(payload);
        BasicDBObject query = new BasicDBObject("primaryKey", primaryKey)
                .append("SourceInfo.SourceID", source.getResourceID())
                .append("SourceInfo.DataSource", source.getDataSource());
        DBCursor cursor = collection.find(query);

        BasicDBObject docWrapper = null;
        try {
            if (cursor.hasNext()) {
                docWrapper = (BasicDBObject) cursor.next();
            }
        } finally {
            cursor.close();
        }
        return docWrapper;
    }

    public void setSource(Source source) throws IOException, ProcessingException {
        this.source = source;
        if (source.getSchema() != null) {
            JsonSchemaFactory factory = JsonSchemaFactory.byDefault();
            String jsonStr = source.getSchema().toString(2);
            System.out.println(jsonStr);
            JsonNode schemaJSON = JsonLoader.fromString(jsonStr);
            this.schema = factory.getJsonSchema(schemaJSON);
        }
    }

    public DocWrapper saveDocument(JSONObject payload, String batchId, Source source,
                                   String outStatus, String collectionName) throws Exception {
        return saveDocument(payload, batchId, source, outStatus, collectionName, true);
    }


    public void updateDocument(BasicDBObject docWrapper, String collectionName, String batchId) {
        ObjectId oid = (ObjectId) docWrapper.get("_id");
        BasicDBObject query = new BasicDBObject("_id", oid);

        DBObject historyDBO = (DBObject) docWrapper.get("History");
        historyDBO.put("batchId", batchId);
        DB db = mongoClient.getDB(dbName);
        DBCollection collection = db.getCollection(collectionName);

        collection.update(query, docWrapper, false, false, WriteConcern.SAFE);
    }

    public DocWrapper prepareDocWrapper(JSONObject payload, String batchId, Source source, String outStatus) throws Exception {
        PrimaryKeyDef pkDef = source.getPrimaryKeyDef();
        String primaryKey = pkDef.prepPrimaryKey(payload);

        DocWrapper.Builder builder = new DocWrapper.Builder(outStatus); // "new"
        final DocWrapper docWrapper = builder.batchId(batchId).payload(payload).version(1)
                .crawlDate(new Date()).sourceId(source.getResourceID())
                .sourceName(source.getName()).primaryKey(primaryKey)
                .dataSource(source.getDataSource()).build();
        return docWrapper;
    }

    public ObjectId saveDocument(DocWrapper docWrapper, String collectionName) throws Exception {
        DB db = mongoClient.getDB(dbName);
        DBCollection collection = db.getCollection(collectionName); // "records");

        // now save the doc
        JSONObject json = docWrapper.toJSON();
        DBObject dbObject = JSONUtils.encode(json, true);

        collection.insert(dbObject, WriteConcern.SAFE);
        return (ObjectId) dbObject.get("_id");
    }


    public DocWrapper saveDocument(JSONObject payload, String batchId, Source source, String outStatus,
                                   String collectionName, boolean validate) throws Exception {
        if (schema != null && validate) {
            // validate the payload
            final JsonNode json = JsonLoader.fromString(payload.toString());
            ProcessingReport report = schema.validate(json);
            if (!report.isSuccess()) {
                throw new Exception(report.toString());
            }
        }

        PrimaryKeyDef pkDef = source.getPrimaryKeyDef();
        String primaryKey = pkDef.prepPrimaryKey(payload);

        DocWrapper.Builder builder = new DocWrapper.Builder(outStatus); // "new"
        final DocWrapper docWrapper = builder.batchId(batchId).payload(payload).version(1)
                .crawlDate(new Date()).sourceId(source.getResourceID())
                .sourceName(source.getName()).primaryKey(primaryKey)
                .dataSource(source.getDataSource()).build();


        DB db = mongoClient.getDB(dbName);
        DBCollection collection = db.getCollection(collectionName); // "records");

        // now save the doc
        JSONObject json = docWrapper.toJSON();
        DBObject dbObject = JSONUtils.encode(json, true);

        collection.insert(dbObject, WriteConcern.SAFE);
        return docWrapper;
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
}
