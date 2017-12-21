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
import java.util.Properties;

/**
 * Created by bozyurt on 3/7/17.
 */
public class ProcessingResourceTest {
    private HttpServer server;
    private WebTarget target;
    private Client c;
    String apiKey;

    @Before
    public void setup() throws Exception {
        server = Main.startServer();
        c = ClientBuilder.newClient();
        target = c.target(Main.BASE_URI);
        Properties properties = Utils.loadProperties("ingestor-web.properties");
        this.apiKey = properties.getProperty("apiKey");
    }

    @After
    public void tearDown() throws Exception {
        server.shutdownNow();
    }

    @Test
    public void testFormProcessingSubmission() throws Exception {
        String formJsonStr = loadAsStringFromClassPath("testdata/form_test_data2.json");
        Response response = target.path("cinergi/processing")
                .queryParam("apiKey", apiKey)
                .request(MediaType.APPLICATION_JSON_TYPE).post(Entity.text(formJsonStr));
        System.out.println(response.getStatus());
        System.out.println(response.readEntity(String.class));
    }

    public static String loadAsStringFromClassPath(String classpath) throws Exception {
        URL url = ProcessingResourceTest.class.getClassLoader().getResource(classpath);
        String path = url.toURI().getPath();
        return Utils.loadAsString(path);
    }


    @Test
    public void testDocStatusCheck() throws Exception {
        Response response = target.path("cinergi/processing/status")
                .queryParam("apiKey", apiKey)
                .queryParam("docId", "5717c2a1cb74d89cc0a65238")
                .request(MediaType.APPLICATION_JSON_TYPE).get();
        System.out.println(response.getStatus());
        System.out.println(response.readEntity(String.class));
    }

}
