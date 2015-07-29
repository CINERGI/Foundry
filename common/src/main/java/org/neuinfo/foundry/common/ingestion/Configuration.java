package org.neuinfo.foundry.common.ingestion;

import org.neuinfo.foundry.common.config.IMongoConfig;
import org.neuinfo.foundry.common.config.ServerInfo;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by bozyurt on 5/27/14.
 */
public class Configuration implements IMongoConfig {
    private String mongoDBName;
    private List<ServerInfo> servers = new ArrayList<ServerInfo>(3);
    private SourceConfig sourceConfig;


    public Configuration(String mongoDBName) {
        this.mongoDBName = mongoDBName;
    }

    public void addServer(ServerInfo si) {
        servers.add(si);
    }

    public String getMongoDBName() {
        return mongoDBName;
    }

    public List<ServerInfo> getServers() {
        return servers;
    }

    public SourceConfig getSourceConfig() {
        return sourceConfig;
    }

    public void setSourceConfig(SourceConfig sourceConfig) {
        this.sourceConfig = sourceConfig;
    }

}
