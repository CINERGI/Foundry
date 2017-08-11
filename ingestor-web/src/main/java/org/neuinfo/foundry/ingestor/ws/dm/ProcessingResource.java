package org.neuinfo.foundry.ingestor.ws.dm;

import com.mongodb.BasicDBObject;
import com.wordnik.swagger.annotations.*;
import org.apache.activemq.ActiveMQConnectionFactory;
import org.bson.types.ObjectId;
import org.jdom2.Element;
import org.json.JSONObject;
import org.neuinfo.foundry.common.Constants;
import org.neuinfo.foundry.common.ingestion.DocumentIngestionService;
import org.neuinfo.foundry.common.model.CinergiFormRec;
import org.neuinfo.foundry.common.model.DocWrapper;
import org.neuinfo.foundry.common.model.Source;
import org.neuinfo.foundry.common.util.TemplateISOXMLGenerator;
import org.neuinfo.foundry.common.util.Utils;
import org.neuinfo.foundry.common.util.XML2JSONConverter;
import org.neuinfo.foundry.consumers.jms.consumers.plugins.ProvenanceHelper;
import org.neuinfo.foundry.ingestor.ws.MongoService;

import javax.jms.*;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.Date;
import java.util.Properties;
import java.util.UUID;

/**
 * Created by bozyurt on 3/1/17.
 */
@Path("cinergi/processing")
@Api(value = "cinergi/processing", description = "CINERGI Form based processing")
public class ProcessingResource {
    @POST
    @Path("/")
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {@ApiResponse(code = 500, message = "An internal error occurred during submission of document for CINERGI pipeline processing"),
            @ApiResponse(code = 400, message = "Either missing apiKey or document already exists"),
            @ApiResponse(code = 403, message = "Not a valid API key")})
    @ApiOperation(value = "Submit a document for CINERGI pipeline processing",
            notes = "",
            response = String.class)
    public Response post(@ApiParam(value = "A JSON object from the form submission", required = true) String content,
                         @ApiParam(value = "API Key", required = true) @QueryParam("apiKey") String apiKey) {
        System.out.println("apiKey:" + apiKey);
        System.out.println("content:" + content);
        if (apiKey == null) {
            throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST).build());
        }
        // System.out.println("apiKey:" + apiKey);
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

        Connection con = null;
        try {
            JSONObject json = new JSONObject(content);
            CinergiFormRec cfr = CinergiFormRec.fromJSON(json);
            ConnectionFactory factory = new ActiveMQConnectionFactory("tcp://localhost:61616");
            Result result = handle(cfr);
            if (result == null) {
                throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST).entity("Already exists").build());
            }
            con = factory.createConnection();
            Session session = con.createSession(false, Session.AUTO_ACKNOWLEDGE);
            MessageProducer producer = session.createProducer(null);
            String queueName = Constants.PIPELINE_MSG_QUEUE; // "foundry.consumer.head";
            Destination destination = session.createQueue(queueName);
            System.out.println("sending notification message to queue:" + queueName);

            JSONObject messageBodyJson = new JSONObject();
            messageBodyJson.put("oid", result.getOid().toString());
            messageBodyJson.put("status", "new.1");
            String jsonStr = messageBodyJson.toString();
            Message message = session.createObjectMessage(jsonStr);
            producer.send(destination, message);
            return Response.ok(result.getResultJson().toString(2)).build();
        } catch (Exception e) {
            e.printStackTrace();
            return Response.serverError().build();
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

    Result handle(CinergiFormRec cfRec) throws Exception {
        MongoService mongoService = null;
        DocumentIngestionService dis;
        try {
            mongoService = new MongoService();
            dis = new DocumentIngestionService();
            dis.initialize(mongoService.getDbName(), mongoService.getMongoClient());

            Source source = dis.findSource("cinergi-0000", null);
            dis.setSource(source);
            TemplateISOXMLGenerator generator = new TemplateISOXMLGenerator();
            Date startDate = new Date();
            String batchId = Utils.prepBatchId(startDate);
            String guid = UUID.randomUUID().toString();
            Element docEl = generator.createISOXMLDoc(cfRec, guid);
            XML2JSONConverter converter = new XML2JSONConverter();
            JSONObject json = converter.toJSON(docEl);
            BasicDBObject docWrapper = dis.findDocument(json, "records");
            if (docWrapper == null) {
                DocWrapper dw = dis.prepareDocWrapper(json, batchId, source, "new.1");
                // save provenance
                ProvenanceHelper.ProvData provData = new ProvenanceHelper.ProvData(dw.getPrimaryKey(),
                        ProvenanceHelper.ModificationType.Ingested);
                provData.setSourceName(dw.getSourceName()).setSrcId(dw.getSourceId());
                // first cleanup any previous provenance data
                ProvenanceHelper.removeProvenance(dw.getPrimaryKey());
                ProvenanceHelper.saveIngestionProvenance("ingestion",
                        provData, startDate, dw);
                ObjectId oid = dis.saveDocument(dw, "records");
                JSONObject result = new JSONObject();
                result.put("docId", guid);
                result.put("batchId", batchId);
                result.put("sourceId", "cinergi-0000");
                return new Result(result, source, oid);
            }
            return null;
        } finally {
            if (mongoService != null) {
                mongoService.shutdown();
            }
        }
    }

    static class Result {
        JSONObject resultJson;
        Source source;
        ObjectId oid;

        public Result(JSONObject resultJson, Source source, ObjectId oid) {
            this.resultJson = resultJson;
            this.source = source;
            this.oid = oid;
        }

        public JSONObject getResultJson() {
            return resultJson;
        }

        public Source getSource() {
            return source;
        }

        public ObjectId getOid() {
            return oid;
        }
    }
}
