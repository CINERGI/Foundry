package org.neuinfo.foundry.ingestor.ws;

import junit.framework.Assert;
import org.bson.types.ObjectId;
import org.glassfish.jersey.server.internal.scanning.PackageNamesScanner;
import org.junit.After;
import org.junit.Ignore;
import org.junit.Test;
import org.neuinfo.foundry.common.model.Organization;

/**
 * Created by bozyurt on 10/16/14.
 */
public class MongoServiceTests {

    @Test
    public void saveFindOrganization() throws Exception {
        MongoService mongoService = null;
        try {
            mongoService = new MongoService();
            ObjectId id = mongoService.saveOrganization("UCSD");

            System.out.println("inserted id:" + id);

            Organization org = mongoService.findOrganization(null, id.toHexString());

            Assert.assertNotNull(org);
            System.out.println(org);
        } finally {
            if (mongoService != null) {
                mongoService.shutdown();
            }
        }
    }

    @Ignore
    public void removeOrganization() throws Exception {
        MongoService mongoService = null;
        try {
            mongoService = new MongoService();
            mongoService.removeOrganization("UCSD", null);
        } finally {
            if (mongoService != null) {
                mongoService.shutdown();
            }
        }
    }
}
