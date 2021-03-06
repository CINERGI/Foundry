package org.neuinfo.foundry.ingestor.ws;

import org.bson.types.ObjectId;
import org.json.JSONObject;
import org.neuinfo.foundry.common.model.Organization;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * Created by bozyurt on 10/17/14.
 */

@Path("cinergi")
public class OrganizationSource {

    @POST
    @Path("organization")
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.APPLICATION_JSON)
    public Response post(String content, @QueryParam("action") String action) {
        System.out.println("action:" + action);
        System.out.println("content:" + content);
        System.out.println("=============================");
        MongoService mongoService = null;
        try {
            mongoService = new MongoService();
            JSONObject orgJSON = new JSONObject(content);
            if (action.equals("create")) {
                String orgName = orgJSON.getString("organization-name");
                ObjectId objectId = mongoService.saveOrganization(orgName);

                JSONObject js = new JSONObject();
                js.put("id", objectId.toHexString());
                return Response.ok(js.toString(2)).build();
            } else if (action.equals("delete")) {

                String orgName = orgJSON.has("organization-name") ? orgJSON.getString("organization-name") : null;
                String id = orgJSON.has("id") ? orgJSON.getString("id") : null;
                System.out.println("id:" + id);
                mongoService.removeOrganization(orgName, id);
                return Response.ok().build();
            }

        } catch (Exception x) {
            x.printStackTrace();
        } finally {
            if (mongoService != null) {
                mongoService.shutdown();
            }
        }
        return null;
    }

    @Path("organization/{orgId}")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getOrganization(@PathParam("orgId") String orgId) {
        MongoService mongoService = null;
        try {
            mongoService = new MongoService();
            Organization organization = mongoService.findOrganization(null, orgId);
            if (organization == null) {
                return Response.status(Response.Status.NOT_FOUND)
                        .entity("No organization with the id:" + orgId).build();
            }
            JSONObject js = organization.toJSON();
            return Response.ok(js.toString(2)).build();

        } catch (Exception x) {
            x.printStackTrace();
            return Response.serverError().build();
        } finally {
            if (mongoService != null) {
                mongoService.shutdown();
            }
        }
    }

    @Path("organization/search")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response searchOrganization(@QueryParam("organization-name") String orgName) {
        MongoService mongoService = null;
        try {
            mongoService = new MongoService();
            Organization organization = mongoService.findOrganization(orgName, null);
            if (organization == null) {
                return Response.status(Response.Status.NOT_FOUND)
                        .entity("No organization with the name:" + orgName).build();
            }
            JSONObject js = organization.toJSON();
            return Response.ok(js.toString(2)).build();

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
