package org.neuinfo.foundry.ingestor.ws.enhancers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wordnik.swagger.annotations.*;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.jdom2.Element;
import org.jdom2.Namespace;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;
import org.json.JSONArray;
import org.json.JSONObject;
import org.neuinfo.foundry.common.Constants;
import org.neuinfo.foundry.common.util.CinergiXMLUtils;
import org.neuinfo.foundry.common.util.FacetHierarchyHandler;
import org.neuinfo.foundry.common.util.Utils;

import javax.ws.rs.*;
import javax.ws.rs.core.GenericEntity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.StringWriter;
import java.net.URI;

import org.cinergi.sdsc.metadata.enhancer.spatial.SpatialEnhancerResultSimple;
import org.cinergi.sdsc.metadata.enhancer.spatial.StanfordNEDLocationFinder;
import org.neuinfo.foundry.consumers.jms.consumers.plugins.SpatialEnhancer2;
import org.neuinfo.foundry.consumers.plugin.Result;

/**
 * Created by valentine on 11/12/19.
 */
@Path("cinergi/enhancers/spatial/2")
@Api(value = "cinergi/enhancers/spatial/2", description = "Spatial enhancement of text documents")
public class SpatialEnhancer2Resource {

    static StanfordNEDLocationFinder finder;
    static {
        try {
            finder = new StanfordNEDLocationFinder();
            finder.startup();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Return Spatial  extent for text",
            notes = "Uses text in the given document to detect the spatial extent",
            response = String.class)
    @ApiResponses(value = {@ApiResponse(code = 400, message = "no  document is supplied"),
            @ApiResponse(code = 500, message = "An internal error occurred during spatial enhancement")})
    public Response post(@ApiParam(value = "Text for spatial enhancements", required = true)
                         String textInput ) {

        if (textInput == null) {
            throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST).build());
        }
        try {
//            finder = new StanfordNEDLocationFinder();
//            finder.startup();

            SpatialEnhancerResultSimple sp = new SpatialEnhancerResultSimple(textInput, finder);
            ObjectMapper Obj = new ObjectMapper();
          //  Object value = Obj.writeValue(jsonTarget, sp);
            GenericEntity entity = new GenericEntity<SpatialEnhancerResultSimple>(sp){};
            return Response.ok().entity(Obj.writeValueAsString(sp)).build();
        } catch (Exception e) {
            e.printStackTrace();
            return Response.serverError().build();
        }
    }



}
