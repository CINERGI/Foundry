package org.neuinfo.foundry.common.ingestion;

import com.mongodb.MongoClient;
import com.mongodb.ServerAddress;
import com.mongodb.WriteConcern;
import org.neuinfo.foundry.common.config.IMongoConfig;
import org.neuinfo.foundry.common.config.ServerInfo;
import org.neuinfo.foundry.common.util.MongoUtils;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by bozyurt on 5/27/14.
 */
public class BaseIngestionService {
    protected String dbName;
    protected MongoClient mongoClient;

    public void start(IMongoConfig conf) throws UnknownHostException {
        this.dbName = conf.getMongoDBName();
        List<ServerAddress> servers = new ArrayList<ServerAddress>(conf.getServers().size());
        String user = null;
        String pwd = null;
        for (ServerInfo si : conf.getServers()) {
            if (si.getUser() != null) {
                user = si.getUser();
                pwd = si.getPwd();
            }
            InetAddress inetAddress = InetAddress.getByName(si.getHost());
            servers.add(new ServerAddress(inetAddress, si.getPort()));
        }
        mongoClient = MongoUtils.createMongoClient(servers, user, pwd, dbName);
    }

    public void shutdown() {
        if (mongoClient != null) {
            mongoClient.close();
        }
    }
}
