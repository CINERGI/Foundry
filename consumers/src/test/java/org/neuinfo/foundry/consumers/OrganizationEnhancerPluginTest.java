package org.neuinfo.foundry.consumers;

import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import junit.framework.TestCase;
import org.neuinfo.foundry.consumers.jms.consumers.plugins.OrganizationEnhancer;
import org.neuinfo.foundry.consumers.plugin.IPlugin;
import org.neuinfo.foundry.consumers.plugin.Result;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by bozyurt on 2/5/15.
 */
public class OrganizationEnhancerPluginTest extends TestCase {
    public OrganizationEnhancerPluginTest(String name) {
        super(name);
    }

    public void testOrganizationEnhancer() throws Exception {
        Helper helper = new Helper("");
        try {
            helper.startup("cinergi-consumers-cfg.xml");

            List<BasicDBObject> docWrappers = helper.getDocWrappers("cinergi-0001");

            IPlugin orgEnhancer = new OrganizationEnhancer();

            Map<String, String> optionMap = new HashMap<String, String>();

            new File("/tmp/cinergi/org").mkdirs();
            optionMap.put("workDir", "/tmp/cinergi/org");
            optionMap.put("scriptPath", "/home/bozyurt/dev/python/some_cinergi_enhancers");

            orgEnhancer.initialize(optionMap);
            int count = 0;
            for (BasicDBObject docWrapper : docWrappers) {
                String primaryKey = docWrapper.get("primaryKey").toString();
                System.out.println(primaryKey);
                Result result = orgEnhancer.handle(docWrapper);
                if (result.getStatus() == Result.Status.OK_WITH_CHANGE) {
                    DBObject dw = result.getDocWrapper();
                    DBObject data = (DBObject) dw.get("Data");

                    System.out.println(data.toString());

                }
               // break;
                if (count > 1000) {
                    break;
                }
                count++;

            }
        } finally {
            helper.shutdown();
        }

    }
}
