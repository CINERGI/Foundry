package org.neuinfo.foundry.common.ingestion;

import com.mongodb.MongoCredential;
import org.neuinfo.foundry.common.config.IMongoConfig;
import org.neuinfo.foundry.common.config.ServerInfo;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by bozyurt on 5/27/14.
 */
public class Configuration implements IMongoConfig {
    private String mongoDBName;
    private List<ServerInfo> servers = new ArrayList<ServerInfo>(3);
    private SourceConfig sourceConfig;
    private String mongoUserName;
    private String mongoUserPassword;

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

    public void setMongoUserName(String mongoUserName) {
        this.mongoUserName =  mongoUserName;
    }

    public void setMongoUserPassword(String mongoUserPassword) {
        this.mongoUserPassword =  mongoUserPassword;
    }

    public String getMongoUserName() {
        return mongoUserName;
    }

    public String getMongoUserPassword() {
        return mongoUserPassword;
    }

    @Override
    public List<MongoCredential> getCredentialsList() {
        if (! getMongoUserName().isEmpty()&& !getMongoUserPassword().isEmpty())
        { MongoCredential credentials = MongoCredential.createCredential(getMongoUserName(), getMongoDBName(), getMongoUserPassword().toCharArray());
            return Arrays.asList(credentials);
        } else {
            return null;
        }
    }

    public SourceConfig getSourceConfig() {
        return sourceConfig;
    }

    public void setSourceConfig(SourceConfig sourceConfig) {
        this.sourceConfig = sourceConfig;
    }



}
