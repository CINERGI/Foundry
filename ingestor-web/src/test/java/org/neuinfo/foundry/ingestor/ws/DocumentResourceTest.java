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
        docId = "08b87e44-b48e-59f8-e054-00144fdd4fa6";
        docId = "505ba4bbe4b08c986b320541";
        docId = "http://www.czo.arizona.edu/data/pub/valle/Towers/Vcp/Tower_Vcp_metadata_2009.hdr";

        Response response = target.path("cinergi/docs/keyword/hierarchies/").queryParam("id", docId)
                .request(MediaType.APPLICATION_JSON_TYPE).get();
        System.out.println(response.getStatus());
        System.out.println(response.readEntity(String.class));
    }

    @Test
    public void testGetKeywordHierarchiesRemote() throws Exception {
        String docId = "4f4e4ae1e4b07f02db6887ce";
        docId = "4f4e48b4e4b07f02db532964";
        WebTarget remoteTarget = c.target("http://ec-pipe-stage.cloudapp.net:8080/foundry/api/");
        Response response = remoteTarget.path("cinergi/docs/keyword/hierarchies/").queryParam("id", docId)
                .request(MediaType.APPLICATION_JSON_TYPE).get();
        System.out.println(response.getStatus());
        System.out.println(response.readEntity(String.class));
    }

    @Test
    public void testGetProvenance() throws Exception {
        String docId = "http://www.czo.arizona.edu/data/pub/valle/MetStation/met1_daily/data_daily_met1_metadata.hdr";
        Response response = target.path("cinergi/prov/doc").queryParam("id", docId)
                .request(MediaType.APPLICATION_JSON_TYPE).get();
        System.out.println(response.getStatus());
        System.out.println(response.readEntity(String.class));
    }
}
