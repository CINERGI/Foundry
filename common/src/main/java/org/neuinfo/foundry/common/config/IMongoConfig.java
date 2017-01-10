package org.neuinfo.foundry.common.config;

import com.mongodb.MongoCredential;

import java.util.List;

/**
 * Created by bozyurt on 10/2/14.
 */
public interface IMongoConfig {
    public String getMongoDBName();
    public List<ServerInfo> getServers();
    public List<MongoCredential> getCredentialsList();

}
