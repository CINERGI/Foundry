package org.neuinfo.foundry.ingestor.ws.dm;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.json.JSONObject;

import javax.jms.*;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * Created by bozyurt on 5/8/15.
 */

@Path("cinergi/notification")
public class NotificationResource {
    private static String theApiKey = "72b6afb31ba46a4e797c3f861c5a945f78dfaa81";

    @Path("/resourceEntered2Scicrunch")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response resourceEntered2Scicrunch(@QueryParam("apiKey") String apiKey) {
        if (apiKey == null) {
            throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST).build());
        }
        System.out.println("apiKey:" + apiKey);
        if (!apiKey.equals(theApiKey)) {
            throw new WebApplicationException(Response.status(Response.Status.FORBIDDEN).build());
        }
        ConnectionFactory factory = new ActiveMQConnectionFactory("tcp://localhost:61616");
        Connection con = null;
        try {
            con = factory.createConnection();
            Session session = con.createSession(false, Session.AUTO_ACKNOWLEDGE);
            MessageProducer producer = session.createProducer(null);
            String queueName = "foundry.scicrunchIn";
            Destination destination = session.createQueue(queueName);
            System.out.println("sending notification message to queue:" + queueName);
            Message message = session.createObjectMessage(new JSONObject().toString());
            producer.send(destination, message);

            JSONObject js = new JSONObject();
            String jsonStr = js.toString();
            return Response.ok(jsonStr).build();
        } catch (Exception x) {
            x.printStackTrace();
            return Response.serverError().build();
        } finally {
            if (con != null) {
                try {
                    con.close();
                } catch (JMSException e) {
                    //ignore
                }
            }
        }
    }
}
