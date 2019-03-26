package org.neuinfo.foundry.consumers.common;

import org.neuinfo.foundry.common.config.ConsumerConfig;
import org.neuinfo.foundry.common.config.IMongoConfig;
import org.neuinfo.foundry.common.config.ServerInfo;
import org.neuinfo.foundry.common.util.ConfigUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Created by bozyurt on 4/24/14.
 * dwv 2019-03-26. Environment Variables can be used in configuration
 * ${envVar:defaultValue}
 * not all properties support this, at present. No int values (aka ports).
 * only params in workflows*/
public class Configuration implements IMongoConfig {
    Map<String, Object> mongoListenerSettings;
    String brokerURL;
    List<ConsumerConfig> consumerConfigs;
    private String mongoDBName;
    private List<ServerInfo> servers = new ArrayList<ServerInfo>(3);
    private String pluginDir;
    private String libDir;
    private String collectionName;

    public List<ConsumerConfig> getConsumerConfigs() {
        return consumerConfigs;
    }

    public void setConsumerConfigs(List<ConsumerConfig> consumerConfigs) {
        this.consumerConfigs = consumerConfigs;
    }

    public Map<String, Object> getMongoListenerSettings() {
        return mongoListenerSettings;
    }

    public void setMongoListenerSettings(Map<String, Object> mongoListenerSettings) {
        this.mongoListenerSettings = mongoListenerSettings;
    }

    public String getBrokerURL() {
        return brokerURL;
    }

    public void setBrokerURL(String brokerURL) {
        this.brokerURL = ConfigUtils.envVarParser(brokerURL);
    }

    public String getPluginDir() {
        return pluginDir;
    }

    public String getLibDir() {
        return libDir;
    }

    public void setPluginDir(String pluginDir) {
        this.pluginDir = ConfigUtils.envVarParser(pluginDir);
    }

    public void setLibDir(String libDir) {
        this.libDir = ConfigUtils.envVarParser(libDir)+ File.pathSeparator + "lib";
    }

    public String getCollectionName() {
        return collectionName;
    }

    public void setCollectionName(String collectionName) {
        this.collectionName = ConfigUtils.envVarParser(collectionName);
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("Configuration{");
        sb.append("mongoListenerSettings=").append(mongoListenerSettings);
        sb.append("brokerURL=").append(brokerURL);
        sb.append('}');
        return sb.toString();
    }


    public void setMongoDBName(String mongoDBName) {
        this.mongoDBName = ConfigUtils.envVarParser(mongoDBName);
    }

    public void addServerInfo(ServerInfo si) {
        this.servers.add(si);
    }

    @Override
    public String getMongoDBName() {
        return this.mongoDBName;
    }

    @Override
    public List<ServerInfo> getServers() {
        return this.servers;
    }
}
