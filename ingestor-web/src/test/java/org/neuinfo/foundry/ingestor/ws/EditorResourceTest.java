package org.neuinfo.foundry.ingestor.ws;

import org.glassfish.grizzly.http.server.HttpServer;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * Created by bozyurt on 12/8/17.
 */
public class EditorResourceTest {
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
    public void testEditedDocProcessing() throws Exception {
        Response response = target.path("cinergi/editing/process")
                .queryParam("apiKey", "72b6afb31ba46a4e797c3f861c5a945f78dfaa81")
                .queryParam("primaryKey", "gov.noaa.nodc:0117727")
                .request(MediaType.APPLICATION_JSON_TYPE).post(Entity.text(""));
        System.out.println(response.getStatus());
        System.out.println(response.readEntity(String.class));
    }
}
