package org.neuinfo.foundry.ingestor.ws.dm;

import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiResponse;
import com.wordnik.swagger.annotations.ApiResponses;
import org.json.JSONObject;
import org.neuinfo.foundry.common.provenance.ProvUtils;
import org.neuinfo.foundry.common.util.JSONUtils;
import org.neuinfo.foundry.ingestor.ws.MongoService;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;

/**
 * Created by bozyurt on 6/1/16.
 */

@Path("cinergi/prov")
@Api(value = "cinergi/prov", description = "Provenance")
public class ProvResource {

    @Path("/doc")
    @GET
    @Produces("application/json")
    @ApiResponses(value = {@ApiResponse(code = 500, message = "An internal error occurred while retrieving provenance data"),
            @ApiResponse(code = 404, message = "No metadata document or no provenance information is found for the given document ID")})
    @ApiOperation(value = "Retrieve provenance information as JSON for a given document id for visualization",
            notes = "",
            response = String.class)
    public Response getProvenance(@QueryParam("id") String docId) {
        MongoService mongoService = null;
        try {
            mongoService = new MongoService();
            System.out.println("docId:" + docId);
            BasicDBObject docWrapper = mongoService.findTheDocument(docId);
            if (docWrapper == null) {
                return Response.status(Response.Status.NOT_FOUND)
                        .entity("No document with id:" + docId + " is not found!").build();
            }
            BasicDBObject history = (BasicDBObject) docWrapper.get("History");
            BasicDBObject prov = (BasicDBObject) history.get("prov");
            if (prov == null) {
                return Response.status(Response.Status.NOT_FOUND)
                        .entity("No provenance for the document with id:" + docId + " is found!").build();
            }
            BasicDBList events = (BasicDBList) prov.get("events");
            JSONObject combined = ProvUtils.prepare4Viewer(events);

            String jsonStr = combined.toString(2);
            return Response.ok(jsonStr).build();
        } catch (Exception x) {
            x.printStackTrace();
            return Response.serverError().build();
        } finally {
            if (mongoService != null) {
                mongoService.shutdown();
            }
        }
    }


}
