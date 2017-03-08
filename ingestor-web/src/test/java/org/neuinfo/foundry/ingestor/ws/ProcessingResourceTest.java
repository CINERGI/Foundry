package org.neuinfo.foundry.ingestor.ws;

import org.glassfish.grizzly.http.server.HttpServer;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.neuinfo.foundry.common.util.Utils;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.net.URL;

/**
 * Created by bozyurt on 3/7/17.
 */
public class ProcessingResourceTest {
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
    public void testFormProcessingSubmission() throws Exception {
        String formJsonStr = loadAsStringFromClassPath("testdata/form_test_data.json");
        Response response = target.path("cinergi/processing")
                .queryParam("apiKey", "72b6afb31ba46a4e797c3f861c5a945f78dfaa81")
                .request(MediaType.APPLICATION_JSON_TYPE).post(Entity.text(formJsonStr));
        System.out.println(response.getStatus());
        System.out.println(response.readEntity(String.class));
    }

    public static String loadAsStringFromClassPath(String classpath) throws Exception {
        URL url = ProcessingResourceTest.class.getClassLoader().getResource(classpath);
        String path = url.toURI().getPath();
        return Utils.loadAsString(path);
    }

}
