package org.neuinfo.foundry.common.config;

import org.neuinfo.foundry.common.util.ConfigUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by bozyurt on 4/24/14.
 * dwv 2019-03-26. Environment Variables can be used in configuration
 * ${envVar:defaultValue}
 * not all properties support this, at present. No int values (aka ports).
 * only params in workflows
 * */
public class Configuration implements IMongoConfig {
    List<Workflow> workflows = new ArrayList<Workflow>(5);
    List<WorkflowMapping> workflowMappings = new ArrayList<WorkflowMapping>(5);
    //File checkpointXmlFile;
    String brokerURL;
    List<ServerInfo> mongoServers = new ArrayList<ServerInfo>(3);
    String mongoDBName;
    String collectionName;

    /*
    public void setCheckpointXmlFile(File checkpointXmlFile) {
        this.checkpointXmlFile = checkpointXmlFile;
    }
    */

    public String getBrokerURL() {
        return brokerURL;
    }

    public void setBrokerURL(String brokerURL) {
        this.brokerURL = ConfigUtils.envVarParser(brokerURL);
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
        return ConfigUtils.envVarParser(mongoDBName);
    }

    @Override
    public List<ServerInfo> getServers() {
        return mongoServers;
    }

    public void setMongoServers(List<ServerInfo> mongoServers) {
        this.mongoServers = mongoServers;
    }

    public void setMongoDBName(String mongoDBName) {
        this.mongoDBName = ConfigUtils.envVarParser(mongoDBName);
    }

    public String getCollectionName() {
        return collectionName;
    }

    public void setCollectionName(String collectionName) {
        this.collectionName = ConfigUtils.envVarParser(collectionName);
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("Configuration::[");
        sb.append("brokerURL=").append(brokerURL);
        for(Workflow wf : workflows) {
            sb.append("\n").append(wf);
        }
        sb.append("\n]");
        return sb.toString();
    }
}
