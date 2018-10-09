package org.neuinfo.foundry.consumers;

import com.mongodb.BasicDBObject;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import org.neuinfo.foundry.common.util.Utils;
import org.neuinfo.foundry.consumers.jms.consumers.plugins.Geoportal2Exporter;
import org.neuinfo.foundry.consumers.plugin.IPlugin;
import org.neuinfo.foundry.consumers.plugin.Result;
import org.neuinfo.foundry.consumers.util.Helper;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Created by valentine.
 */
public class GeoportalPublishTests  {



    @Test
    public void testPublishGPT() throws Exception {
        JSONObject docWrapperJson = new JSONObject(loadAsStringFromClassPath("testdata/gpt_test_doc_wrapper.json"));
        assertTrue(docWrapperJson.has("OriginalDoc"));
        assertTrue(docWrapperJson.has("Processing"));
        Helper helper = new Helper("");
        try {
           // helper.startup("cinergi-consumers-cfg.xml");
            String user = helper.getParam("user");
            String password = helper.getParam("password");


            IPlugin plugin = new Geoportal2Exporter();
            Map<String, String> optionMap = new HashMap<String, String>();
            optionMap.put("gptURI", "http://132.249.238.169:8080/geoportal/");
            optionMap.put("elasticsearchIndex", "metadata");

            optionMap.put("user", "gptadmin");
            optionMap.put("password", "gptadmin");


            plugin.initialize(optionMap);
            BasicDBObject docWrapper = BasicDBObject.parse(
                    loadAsStringFromClassPath("testdata/gpt_test_doc_wrapper.json"));
            Result result = plugin.handle(docWrapper);
            if (result.getStatus() != Result.Status.OK_WITH_CHANGE) {

                fail("publish to GPT failed");
            }


        } finally {
           // helper.shutdown();
        }
    }




    public static String loadAsStringFromClassPath(String classpath) throws Exception {
        URL url = GeoportalPublishTests.class.getClassLoader().getResource(classpath);
        String path = url.toURI().getPath();
        return Utils.loadAsString(path);
    }
}
