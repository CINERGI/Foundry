package org.neuinfo.foundry.consumers;

import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import junit.framework.TestCase;

import java.util.List;

/**
 * Created by bozyurt on 4/21/15.
 */
public class KeywordHierarchyHandlerTest extends TestCase {
    public KeywordHierarchyHandlerTest(String name) {
        super(name);
    }

    public void testKeywordHierarchyHandler() throws Exception {
        Helper helper = new Helper("");
        try {
            helper.startup("cinergi-consumers-cfg.xml");
            List<BasicDBObject> docWrappers = helper.getDocWrappers("cinergi-0001");
            for (BasicDBObject docWrapper : docWrappers) {
                BasicDBObject data = (BasicDBObject) docWrapper.get("Data");
                BasicDBList keywords = (BasicDBList) data.get("keywords");
                if (keywords != null) {
                   //TODO
                }
            }
        } finally {
            helper.shutdown();
        }

    }
}
