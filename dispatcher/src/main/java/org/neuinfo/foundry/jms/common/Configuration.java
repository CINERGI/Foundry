package org.neuinfo.foundry.jms.common;

import org.neuinfo.foundry.common.config.ServerInfo;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Created by bozyurt on 4/24/14.
 */
public class Configuration {
    Map<String, Object> mongoListenerSettings;
    List<Workflow> workflows = new ArrayList<Workflow>(5);
    List<WorkflowMapping> workflowMappings = new ArrayList<WorkflowMapping>(5);
    File checkpointXmlFile;
    String brokerURL;
    List<ServerInfo>  mongoServers = new ArrayList<ServerInfo>(3);
    String mongoDBName;
    String collectionName;

    public Map<String, Object> getMongoListenerSettings() {
        return mongoListenerSettings;
    }

    public void setMongoListenerSettings(Map<String, Object> mongoListenerSettings) {
        this.mongoListenerSettings = mongoListenerSettings;
    }

    public File getCheckpointXmlFile() {
        return checkpointXmlFile;
    }

    public void setCheckpointXmlFile(File checkpointXmlFile) {
        this.checkpointXmlFile = checkpointXmlFile;
    }

    public String getBrokerURL() {
        return brokerURL;
    }

    public void setBrokerURL(String brokerURL) {
        this.brokerURL = brokerURL;
    }

    public List<Workflow> getWorkflows() {
        return workflows;
    }

    public List<WorkflowMapping> getWorkflowMappings() {
        return workflowMappings;
    }

    public List<ServerInfo> getMongoServers() {
        return mongoServers;
    }

    public String getMongoDBName() {
        return mongoDBName;
    }

    public void setMongoServers(List<ServerInfo> mongoServers) {
        this.mongoServers = mongoServers;
    }

    public void setMongoDBName(String mongoDBName) {
        this.mongoDBName = mongoDBName;
    }

    public String getCollectionName() {
        return collectionName;
    }

    public void setCollectionName(String collectionName) {
        this.collectionName = collectionName;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("Configuration{");
        // sb.append("mongoListenerSettings=").append(mongoListenerSettings);
        sb.append("brokerURL=").append(brokerURL);
        sb.append(",workflows=").append(workflows);
        sb.append(", checkpointXmlFile=").append(checkpointXmlFile);
        sb.append('}');
        return sb.toString();
    }
}
