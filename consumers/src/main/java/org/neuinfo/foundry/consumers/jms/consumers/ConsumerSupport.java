package org.neuinfo.foundry.consumers.jms.consumers;

import com.mongodb.MongoClient;
import com.mongodb.ServerAddress;
import org.neuinfo.foundry.consumers.common.ConfigLoader;
import org.neuinfo.foundry.consumers.common.Configuration;
import org.neuinfo.foundry.consumers.coordinator.IConsumer;
import org.neuinfo.foundry.consumers.river.MongoDBRiverDefinition;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by bozyurt on 10/29/14.
 */
public abstract class ConsumerSupport implements IConsumer {
    protected String queueName;
    protected MongoClient mongoClient;
    protected String mongoDbName;
    protected String successMessageQueueName;
    protected String failureMessageQueueName;
    protected Map<String, String> paramsMap = new HashMap<String, String>(17);
    protected Configuration config;
    /**
     * the name of the collection to consume document(s) from
     */
    protected String collectionName;
    protected String inStatus;
    protected String outStatus;
    MongoDBRiverDefinition definition;

    public ConsumerSupport(String queueName) {
        this.queueName = queueName;
    }

    public String getSuccessMessageQueueName() {
        return successMessageQueueName;
    }

    public void setSuccessMessageQueueName(String successMessageQueueName) {
        this.successMessageQueueName = successMessageQueueName;
    }

    public String getFailureMessageQueueName() {
        return failureMessageQueueName;
    }

    public void setFailureMessageQueueName(String failureMessageQueueName) {
        this.failureMessageQueueName = failureMessageQueueName;
    }

    public void startup(String configFile) throws Exception {
        this.config = ConfigLoader.load(configFile); // "consumers-cfg.xml");

        config.getMongoListenerSettings();
        this.definition = MongoDBRiverDefinition.parseSettings("consumer", "consumer",
                config.getMongoListenerSettings());
        List<ServerAddress> mongoServers = definition.getMongoServers();
        mongoClient = new MongoClient(mongoServers);
        this.mongoDbName = definition.getMongoDb();
    }

    public void shutdown() {
        System.out.println("shutting down the consumer...");

        if (mongoClient != null) {
            mongoClient.close();
        }
    }

    public void setParam(String name, String value) {
        paramsMap.put(name, value);
    }

    public String getParam(String name) {
        return paramsMap.get(name);
    }

    public void setCollectionName(String name) {
        this.collectionName = name;
    }

    public String getCollectionName() {
        return collectionName;
    }

    @Override
    public String getInStatus() {
        return inStatus;
    }

    @Override
    public void setInStatus(String inStatus) {
        this.inStatus = inStatus;
    }

    @Override
    public String getOutStatus() {
        return outStatus;
    }

    @Override
    public void setOutStatus(String outStatus) {
        this.outStatus = outStatus;
    }

    public void stop() {
        // no op
    }
}
