package org.neuinfo.foundry.jms.producer;

import com.mongodb.DBObject;
import org.apache.activemq.ActiveMQConnectionFactory;
import org.json.JSONException;
import org.json.JSONObject;
import org.neuinfo.foundry.common.util.JSONUtils;
import org.neuinfo.foundry.jms.common.Constants;
import org.neuinfo.foundry.jms.common.QueueInfo;
import org.neuinfo.foundry.river.Operation;
import org.neuinfo.foundry.river.QueueEntry;

import javax.jms.*;

/**
 * Created by bozyurt on 5/1/14.
 */
public class Publisher {
    // private String brokerURL = "tcp://localhost:61616";
    private transient Connection con;
    private transient Session session;
    private transient MessageProducer producer;

    public Publisher(String brokerURL) throws JMSException {
        ConnectionFactory factory = new ActiveMQConnectionFactory(brokerURL);
        this.con = factory.createConnection();
        session = con.createSession(false, Session.AUTO_ACKNOWLEDGE);
        this.producer = session.createProducer(null);
    }


    public void sendMessage(QueueEntry entry, QueueInfo destQI, String status) throws JMSException, JSONException {
        String objectId = "";
        final DBObject data = entry.getData();
        if (data.get(Constants.MONGODB_ID_FIELD) != null) {
            objectId = data.get(Constants.MONGODB_ID_FIELD).toString();
        }
        Operation operation = entry.getOperation();
        if (operation == Operation.INSERT && objectId != null) {
            Destination destination = session.createQueue(destQI.getName());
            JSONObject json = new JSONObject();
            json.put("oid", objectId);
            json.put("status", status);
            addAdditionalFieldsIfAny(destQI, data, json);
            System.out.println("sending JMS message with payload:" + json.toString(2));
            Message message = session.createObjectMessage(json.toString());
            this.producer.send(destination, message);
        } else if (operation == Operation.UPDATE && objectId != null) {
            Destination destination = session.createQueue(destQI.getName());
            JSONObject json = new JSONObject();
            json.put("oid", objectId);
            json.put("status", status);
            addAdditionalFieldsIfAny(destQI, data, json);
            System.out.println("sending JMS message with payload:" + json.toString(2) + " to destination "
                    + destQI.getName());
            Message message = session.createObjectMessage(json.toString());
            this.producer.send(destination, message);
        }
    }

    void addAdditionalFieldsIfAny(QueueInfo destQI, DBObject data, JSONObject json) {
        if (destQI.hasHeaderFields()) {
            for (String fieldName : destQI.getHeaderFieldSet()) {
                Object o = data.get(fieldName);

                if (o != null) {
                    json.put(fieldName, o);
                } else {
                    if (fieldName.indexOf('.') != -1) {
                        String[] toks = fieldName.split("\\.");
                        o = JSONUtils.findNested(data, fieldName);
                        if (o != null) {
                            json.put(toks[toks.length - 1], o);
                        }

                    }
                }
            }
        }
    }

    public void close() {
        try {
            if (con != null) {
                con.close();
            }
        } catch (JMSException x) {
            x.printStackTrace();
        }
    }
}
