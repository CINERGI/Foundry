package org.neuinfo.foundry.ingestor.ws.enhancers;

import org.glassfish.grizzly.http.server.HttpServer;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.neuinfo.foundry.ingestor.ws.Main;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * Created by bozyurt on 5/8/15.
 */
public class NotificationResourceTest {
    private HttpServer server;
    private WebTarget target;
    private Client c;

    @Before
    public void setUp() throws Exception {
        // start the server
        server = Main.startServer();
        // create the client
        c = ClientBuilder.newClient();
        target = c.target(Main.BASE_URI);
    }

    @After
    public void tearDown() throws Exception {
        server.shutdownNow();
    }

    @Test
    public void testResourceEntered2Scicrunch() throws Exception {
        Response response = target.path("cinergi/notification/resourceEntered2Scicrunch")
                .queryParam("apiKey", "72b6afb31ba46a4e797c3f861c5a945f78dfaa81")
                .request(MediaType.APPLICATION_JSON_TYPE).get();
        System.out.println(response.getStatus());
        System.out.println(response.readEntity(String.class));
    }
}
