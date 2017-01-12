package org.neuinfo.foundry.common.util;

import com.mongodb.*;
import org.neuinfo.foundry.common.model.Source;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by bozyurt on 11/4/15.
 */
public class MongoUtils {
    public static Source getSource(BasicDBObject query, DBCollection sources) {
        final DBCursor cursor = sources.find(query);

        Source source = null;
        try {
            if (cursor.hasNext()) {
                DBObject dbObject = cursor.next();
                source = Source.fromDBObject(dbObject);
            }
        } finally {
            cursor.close();
        }
        return source;
    }

    public static MongoClient createMongoClient(List<ServerAddress> servers, String user, String pwd, String database) {
        MongoClientOptions mco = new MongoClientOptions.Builder().socketKeepAlive(false).
                maxConnectionIdleTime(60000).connectionsPerHost(10).build();
        MongoClient mongoClient = null;
        if (pwd != null &&  user != null) {
            MongoCredential credential = MongoCredential.createCredential(user, database, pwd.toCharArray());

            mongoClient = new MongoClient(servers, Arrays.asList(credential), mco);
        } else {
            mongoClient = new MongoClient(servers, mco);
        }
        mongoClient.setWriteConcern(WriteConcern.SAFE);
        return mongoClient;
    }
}
