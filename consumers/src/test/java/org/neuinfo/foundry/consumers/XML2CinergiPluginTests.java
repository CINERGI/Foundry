package org.neuinfo.foundry.consumers;

import com.mongodb.DBObject;
import junit.framework.Assert;
import junit.framework.TestCase;
import org.neuinfo.foundry.consumers.coordinator.JavaPluginCoordinator;
import org.neuinfo.foundry.consumers.jms.consumers.plugins.XML2CinergiPlugin;
import org.neuinfo.foundry.consumers.plugin.IPlugin;
import org.neuinfo.foundry.consumers.plugin.Result;
import org.neuinfo.foundry.consumers.util.Helper;

/**
 * Created by bozyurt on 11/20/14.
 */
public class XML2CinergiPluginTests extends TestCase {
    static final String HOME_DIR = System.getProperty("user.home");

    public XML2CinergiPluginTests(String name) {
        super(name);
    }

    public void testDynamicPluginCreation() throws Exception {
        String libDir = HOME_DIR + "/etc/foundry_plugins/lib";
        String pluginDir = HOME_DIR + "/etc/foundry_plugins/plugins";

        JavaPluginCoordinator c = JavaPluginCoordinator.getInstance(pluginDir, libDir);

        c.createInstance("org.neuinfo.foundry.consumers.jms.consumers.plugins.XML2CinergiPlugin");
    }


    public void testPlugin() throws Exception {
        Helper helper = new Helper("");
        try {
            helper.startup("cinergi-consumers-cfg.xml");

            DBObject docWrapper = helper.getDocWrapper();
            IPlugin plugin = new XML2CinergiPlugin();

            Result result = plugin.handle(docWrapper);

            Assert.assertEquals(result.getStatus(), Result.Status.OK_WITH_CHANGE);
            System.out.println(result.getDocWrapper().toString());
        } finally {
            helper.shutdown();
        }

    }

}
