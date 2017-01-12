package org.neuinfo.foundry.consumers;

import com.mongodb.*;
import junit.framework.TestCase;
import org.json.JSONObject;
import org.neuinfo.foundry.common.util.JSONUtils;
import org.neuinfo.foundry.consumers.common.ConfigLoader;
import org.neuinfo.foundry.consumers.common.Configuration;
import org.neuinfo.foundry.consumers.river.MongoDBRiverDefinition;

import java.util.Arrays;
import java.util.List;

/**
 * Created by bozyurt on 11/6/14.
 */
public class DocWrapperTest extends TestCase {
    MongoClient mongoClient;
    String mongoDbName;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        Configuration config = ConfigLoader.load("consumers-cfg.xml");
        config.getMongoListenerSettings();
        MongoDBRiverDefinition definition = MongoDBRiverDefinition.parseSettings("consumer", "consumer",
                config.getMongoListenerSettings());
        List<ServerAddress> mongoServers = definition.getMongoServers();
        String user = config.getServers().get(0).getUser();
        String pwd = config.getServers().get(0).getPwd();
        if (user != null && pwd != null) {
            MongoCredential credential = MongoCredential.createCredential(user, config.getMongoDBName(), pwd.toCharArray());
            mongoClient = new MongoClient(mongoServers, Arrays.asList(credential));
        } else {

            mongoClient = new MongoClient(mongoServers);
        }
        this.mongoDbName = definition.getMongoDb();
    }

    @Override
    protected void tearDown() throws Exception {
        if (mongoClient != null) {
            mongoClient.close();
        }
        super.tearDown();
    }

    public void testIt() {
        DB db = mongoClient.getDB(this.mongoDbName);
        DBCollection collection = db.getCollection("records");
        DBObject one = collection.findOne();
        System.out.println(one);
    }

    public void testGetDocWrapper() {
        DB db = mongoClient.getDB(this.mongoDbName);
        DBCollection collection = db.getCollection("nifRecords");
        String primaryKey = "4"; //
        String nifId = "nif-0000-00241";
        primaryKey = "http://www.wired.com/?p=1622077";
        nifId = "nlx_144196";

        primaryKey = "40";
        nifId = "nlx_152590";

        primaryKey = "oai:datadryad.org:10255/dryad.180";
        nifId = "nlx_149486";
        BasicDBObject query = new BasicDBObject("primaryKey", primaryKey)
                .append("SourceInfo.SourceID", nifId);
        BasicDBObject dbo = (BasicDBObject) collection.findOne(query);
        JSONObject json = JSONUtils.toJSON(dbo, true);
        System.out.println(json.toString(2));
    }
}
