package org.neuinfo.foundry.jms.producer;

import org.apache.log4j.Logger;
import org.bson.BSONObject;
import org.bson.types.BSONTimestamp;
import org.neuinfo.foundry.jms.common.Configuration;
import org.neuinfo.foundry.jms.common.QueueInfo;
import org.neuinfo.foundry.jms.common.Route;
import org.neuinfo.foundry.jms.common.Workflow;
import org.neuinfo.foundry.river.Context;
import org.neuinfo.foundry.river.QueueEntry;
import org.neuinfo.foundry.river.Status;

import javax.jms.JMSException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * <p>
 * Consumes <code>QueueEntry</code> records from the blocking queue,
 * creates a JMS message and sends it to a queue based on its status.
 * </p>
 * Created by bozyurt on 5/1/14.
 */
public class OplogQueueEntryConsumer implements Runnable {
    Configuration configuration;
    private final Context context;
    private Publisher publisher;
    private final static Logger logger = Logger.getLogger(OplogQueueEntryConsumer.class);

    public OplogQueueEntryConsumer(Configuration configuration, Context context) throws JMSException {
        this.configuration = configuration;
        this.context = context;
        publisher = new Publisher(configuration.getBrokerURL());
    }

    @Override
    public void run() {

        Map<String, String> paramsMap = new HashMap<String, String>();
        try {
            BSONTimestamp oplogBsonTimestamp;
            final TimeCheckPointManager tcmMan = TimeCheckPointManager.getInstance();
            while (context.getStatus() == Status.RUNNING) {
                try {
                    QueueEntry entry = context.getStream().take();
                    oplogBsonTimestamp = entry.getOplogTimestamp();
                    BSONObject po = (BSONObject) entry.getData().get("Processing");
                    if (po != null) {
                        String status = (String) po.get("status");
                        boolean ok = false;
                        if (!status.equals("finished")) {
                            paramsMap.put("processing.status", status);
                            logger.info("status:" + status + " " + po.toString());
                            for (Workflow wf : configuration.getWorkflows()) {
                                List<Route> routes = wf.getRoutes();
                                for (Route route : routes) {
                                    if (route.getCondition().isSatisfied(paramsMap)) {
                                        final List<QueueInfo> queueNames = route.getQueueNames();
                                        for (QueueInfo qi : queueNames) {
                                            try {
                                                publisher.sendMessage(entry, qi, status);
                                                ok = true;
                                            } catch (Exception e) {
                                                //TODO proper error handling
                                                e.printStackTrace();
                                                ok = false;
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        if (ok) {
                            tcmMan.setCheckPointTime(oplogBsonTimestamp);
                        }
                    }

                } catch (InterruptedException x) {
                    logger.info("OplogQueueEntryConsumer is interrupted");
                    try {
                        TimeCheckPointManager.getInstance().checkpoint();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        } finally {
            publisher.close();
        }
    }
}
