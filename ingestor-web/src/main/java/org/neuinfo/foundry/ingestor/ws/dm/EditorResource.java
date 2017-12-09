package org.neuinfo.foundry.ingestor.ws.dm;

import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import com.wordnik.swagger.annotations.*;
import org.apache.activemq.ActiveMQConnectionFactory;
import org.bson.types.ObjectId;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.neuinfo.foundry.common.Constants;
import org.neuinfo.foundry.common.model.Source;
import org.neuinfo.foundry.common.util.JSONUtils;
import org.neuinfo.foundry.common.util.Utils;
import org.neuinfo.foundry.common.util.DiffRecord;
import org.neuinfo.foundry.consumers.common.EditDiffManager;
import org.neuinfo.foundry.common.util.JsonPathDiffHandler;
import org.neuinfo.foundry.ingestor.ws.MongoService;

import javax.jms.*;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.List;
import java.util.Properties;

import static org.neuinfo.foundry.consumers.common.Constants.*;

/**
 * Created by bozyurt on 11/3/17.
 */

@Path("cinergi/editing")
@Api(value = "cinergi/editing", description = "CINERGI edited document processing")
public class EditorResource {
    @POST
    @Path("/process")
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {@ApiResponse(code = 500, message = "An internal error occurred during submission of document for CINERGI pipeline processing"),
            @ApiResponse(code = 400, message = "Either missing apiKey or document already exists"),
            @ApiResponse(code = 403, message = "Not a valid API key")})
    @ApiOperation(value = "Submit a document for CINERGI pipeline processing",
            notes = "",
            response = String.class)
    public Response post(@ApiParam(value = "The primaryKey for the metadata document", required = true)
                         @QueryParam("primaryKey") String primaryKey,
                         @ApiParam(value = "sourceID", required = false) @QueryParam("sourceID") String sourceID,
                         @ApiParam(value = "API Key", required = true) @QueryParam("apiKey") String apiKey) {

        if (apiKey == null) {
            throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST).build());
        }
        String theApiKey = null;
        try {
            Properties properties = Utils.loadProperties("ingestor-web.properties");
            theApiKey = properties.getProperty("apiKey");
        } catch (IOException e) {
            e.printStackTrace();
            throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST).build());
        }
        if (theApiKey == null) {
            throw new WebApplicationException(Response.status(Response.Status.INTERNAL_SERVER_ERROR).build());
        }

        if (!apiKey.equals(theApiKey)) {
            throw new WebApplicationException(Response.status(Response.Status.FORBIDDEN).build());
        }

        MongoService mongoService = null;
        try {
            mongoService = new MongoService();
            String docId = primaryKey;
            if (docId.indexOf('"') == -1) {
                docId = (new StringBuilder()).append('"').append(docId).append('"').toString();
            }
            BasicDBObject editedDocWrapper = mongoService.findTheEditedDocument(sourceID, docId);
            if (editedDocWrapper == null) {
                return Response.status(Response.Status.NOT_FOUND)
                        .entity("No edited document with primaryKey:" + primaryKey + " is not found!").build();
            }
            BasicDBObject docWrapper;
            if (Utils.isEmpty(sourceID)) {
                docWrapper = mongoService.findTheDocument(primaryKey);
            } else {
                docWrapper = mongoService.findTheDocument(sourceID, primaryKey);
            }
            if (docWrapper == null) {
                return Response.status(Response.Status.NOT_FOUND)
                        .entity("No document with primaryKey:" + primaryKey + " is not found!").build();
            }
            BasicDBObject originalDoc = (BasicDBObject) docWrapper.get("OriginalDoc");
            JSONObject origDocJson = JSONUtils.toJSON(originalDoc, false);
            JSONObject editedDocJson = JSONUtils.clone(origDocJson);
            JSONObject editedDocWrapperJson = JSONUtils.toJSON(editedDocWrapper, false);
            BasicDBObject dataDoc = (BasicDBObject) docWrapper.get("Data");
            JSONObject dataSectionJson = null;
            if (dataDoc != null) {
                dataSectionJson = JSONUtils.toJSON(dataDoc, false);
            }

            List<DiffRecord> diffRecords = EditDiffManager.extractEditDiffInformation(editedDocWrapperJson);
            boolean modified = handleOrigDataEdit(editedDocJson, diffRecords);
            JSONArray keywordDiffs = null;
            if (dataSectionJson != null) {
                keywordDiffs = handleKeywordEdits(dataSectionJson, diffRecords);
            }

            boolean hasKeywordDiffs = (keywordDiffs != null && keywordDiffs.length() > 0);
            if (modified || hasKeywordDiffs) {
                if (modified) {
                    DBObject encoded = JSONUtils.encode(editedDocJson, true);
                    docWrapper.put("EditedDoc", encoded);
                }
                if (hasKeywordDiffs) {
                    dataSectionJson.put("keywordDiffs", keywordDiffs);
                    DBObject encoded = JSONUtils.encode(dataSectionJson, true);
                    docWrapper.put("Data", encoded);
                }
                // FIXME provenance
                mongoService.updateDocument(docWrapper);
                ObjectId oid = docWrapper.getObjectId(MONGODB_ID_FIELD);
                sendStartProccessingMessage(oid.toString());
            }

        } catch (Exception x) {
            x.printStackTrace();
            return Response.serverError().build();
        } finally {
            if (mongoService != null) {
                mongoService.shutdown();
            }
        }
        return null;
    }

    void sendStartProccessingMessage(String oid) throws Exception {
        Connection con = null;
        try {
            ConnectionFactory factory = new ActiveMQConnectionFactory("tcp://localhost:61616");
            con = factory.createConnection();
            Session session = con.createSession(false, Session.AUTO_ACKNOWLEDGE);
            MessageProducer producer = session.createProducer(null);
            String queueName = Constants.PIPELINE_MSG_QUEUE; // "foundry.consumer.head";
            Destination destination = session.createQueue(queueName);
            System.out.println("sending notification message to queue:" + queueName);
            JSONObject messageBodyJson = new JSONObject();
            messageBodyJson.put("oid", oid);
            messageBodyJson.put("status", "new.1");
            String jsonStr = messageBodyJson.toString();
            Message message = session.createObjectMessage(jsonStr);
            producer.send(destination, message);
        } finally {
            if (con != null) {
                try {
                    con.close();
                } catch (JMSException e) {
                    // ignore
                }
            }
        }
    }


    static class Result {
        Source source;
        ObjectId oid;

        public Result(Source source, ObjectId oid) {
            this.source = source;
            this.oid = oid;
        }

        public Source getSource() {
            return source;
        }

        public ObjectId getOid() {
            return oid;
        }
    }

    JSONArray handleKeywordEdits(JSONObject dataSectionJSON, List<DiffRecord> diffRecords) {
        JSONArray keywordDiffs = new JSONArray();
        for (DiffRecord diffRecord : diffRecords) {
            List<JsonPathDiffHandler.JsonPathNode> path = JsonPathDiffHandler.parseJsonPath(diffRecord.getJsonPath());
            if (path == null || path.isEmpty()) {
                continue;
            }
            if (path.get(0).getFullName().equals("Data")) {
                path = path.subList(1, path.size());
                JsonPathDiffHandler.createUpdateKeywordDiffJson(path, diffRecord, dataSectionJSON, keywordDiffs);
            }
        }
        return keywordDiffs;
    }

    boolean handleOrigDataEdit(JSONObject editedDocJson, List<DiffRecord> diffRecords) {
        boolean modified = false;
        for (DiffRecord diffRecord : diffRecords) {
            List<JsonPathDiffHandler.JsonPathNode> path = JsonPathDiffHandler.parseJsonPath(diffRecord.getJsonPath());
            if (path == null || path.isEmpty()) {
                continue;
            }
            if (path.get(0).getFullName().equals("OriginalDoc")) {
                path = path.subList(1, path.size());
                try {
                    JsonPathDiffHandler.modifyJson(path, diffRecord, editedDocJson);
                    modified = true;
                } catch (JSONException je) {
                    System.err.println(je.getMessage());
                }
            }

        }
        return modified;
    }
}
