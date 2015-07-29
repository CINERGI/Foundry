package org.neuinfo.foundry.jms.producer;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.json.JSONObject;

import javax.jms.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

/**
 * Reads user commands from the command prompt to start ingesting etc for example via MQ
 * Created by bozyurt on 9/30/14.
 */
public class UserMessageService implements Runnable {
    private transient Connection con;
    private transient Session session;
    private transient MessageProducer producer;
    private String queueName;

    public UserMessageService(String brokerURL, String queueName) throws JMSException {
        this.queueName = queueName;
        ConnectionFactory factory = new ActiveMQConnectionFactory(brokerURL);
        this.con = factory.createConnection();
        session = con.createSession(false, Session.AUTO_ACKNOWLEDGE);
        this.producer = session.createProducer(null);
    }

    void sendMessage(String cmd, Map<String,String> paramMap) throws JMSException {
        Destination destination = session.createQueue(this.queueName);
        JSONObject json = new JSONObject();
        json.put("cmd", cmd);
        for(String name : paramMap.keySet()) {
            json.put(name, paramMap.get(name));
        }
        System.out.println("sending user JMS message with payload:" + json.toString(2) +
                " to queue:" + this.queueName);
        Message message = session.createObjectMessage(json.toString());
        this.producer.send(destination, message);
    }

    @Override
    public void run() {

        BufferedReader in = new BufferedReader( new InputStreamReader(System.in));
        boolean finished = false;
        while(!finished) {
            try {
                String ans = in.readLine();
                ans = ans.trim().toLowerCase();
                if (ans.startsWith("ingest")) {
                    String[] toks = ans.split("\\s+");
                    if (toks.length == 3) {
                        Map<String,String> pm = new HashMap<String, String>(7);
                        pm.put("srcNifId", toks[1]);
                        pm.put("batchId", toks[2]);
                        sendMessage("ingest", pm);
                    }
                }

            } catch(Exception x) {
                x.printStackTrace();
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
