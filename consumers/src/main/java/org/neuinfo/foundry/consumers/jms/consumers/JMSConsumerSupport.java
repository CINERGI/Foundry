package org.neuinfo.foundry.consumers.jms.consumers;

import org.apache.activemq.ActiveMQConnectionFactory;

import javax.jms.*;

/**
 * Created by bozyurt on 5/8/14.
 */
public abstract class JMSConsumerSupport extends ConsumerSupport {
    //   private static String brokerURL = "tcp://localhost:61616";
    protected transient ConnectionFactory factory;
    protected transient Connection con;
    protected transient Session session;


    public JMSConsumerSupport(String queueName) {
        super(queueName);
    }

    @Override
    public void startup(String configFile) throws Exception {
        super.startup(configFile);
        this.factory = new ActiveMQConnectionFactory(config.getBrokerURL());
        this.con = factory.createConnection();
        con.start();
        session = con.createSession(false, Session.AUTO_ACKNOWLEDGE);
    }

    @Override
    public void shutdown() {
        super.shutdown();
        try {
            if (this.con != null) {
                con.close();
            }
        } catch (JMSException x) {
            x.printStackTrace();
        }
    }

    public void handleMessages(MessageListener listener) throws JMSException {
        Destination destination = this.session.createQueue(queueName);
        MessageConsumer messageConsumer = this.session.createConsumer(destination);
        messageConsumer.setMessageListener(listener);
    }

}
