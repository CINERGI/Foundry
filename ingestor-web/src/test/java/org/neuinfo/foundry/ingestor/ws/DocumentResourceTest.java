package org.neuinfo.foundry.ingestor.ws;

import org.glassfish.grizzly.http.server.HttpServer;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * Created by bozyurt on 2/9/15.
 */
public class DocumentResourceTest {
    private HttpServer server;
    private WebTarget target;
    private Client c;


    @Before
    public void setup() throws Exception {
        server = Main.startServer();
        c = ClientBuilder.newClient();
        target = c.target(Main.BASE_URI);
    }

    @After
    public void tearDown() throws Exception {
        server.shutdownNow();
    }

    @Test
    public void testGetDocumentIds() throws Exception {
        Response response = target.path("cinergi/docs/cinergi-0006").request(MediaType.APPLICATION_JSON_TYPE).get();
        System.out.println(response.getStatus());
        System.out.println(response.readEntity(String.class));
    }

    @Test
    public void testGetDocument() throws Exception {
        Response response = target.path("cinergi/docs/cinergi-0006/gov.noaa.nodc:7301155").request(MediaType.APPLICATION_XML).get();
        System.out.println(response.getStatus());
        System.out.println(response.readEntity(String.class));
    }

    @Test
    public void testGetKeywordHierarchies() throws Exception {
        String docId = "4f4e4ae1e4b07f02db6887ce";
        docId = "CORDC";
        docId = "4f4e48b4e4b07f02db532964";
        docId = "4f4e4a51e4b07f02db62a174";
        Response response = target.path("cinergi/docs/keyword/hierarchies/").queryParam("id", docId)
                .request(MediaType.APPLICATION_JSON_TYPE).get();
        System.out.println(response.getStatus());
        System.out.println(response.readEntity(String.class));
    }
}
