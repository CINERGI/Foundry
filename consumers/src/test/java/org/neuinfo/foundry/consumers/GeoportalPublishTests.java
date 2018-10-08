package org.neuinfo.foundry.consumers;

import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import junit.framework.Assert;
import junit.framework.TestCase;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.neuinfo.foundry.common.model.Keyword;
import org.neuinfo.foundry.common.util.*;
import org.neuinfo.foundry.consumers.common.EditDiffManager;
import org.neuinfo.foundry.consumers.jms.consumers.plugins.Geoportal2Exporter;
import org.neuinfo.foundry.consumers.jms.consumers.plugins.KeywordEnhancer2;
import org.neuinfo.foundry.consumers.plugin.IPlugin;
import org.neuinfo.foundry.consumers.plugin.Result;
import org.neuinfo.foundry.consumers.util.Helper;

import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by valentine.
 */
public class GeoportalPublishTests extends TestCase {

    public GeoportalPublishTests(String name) {
        super(name);
    }


    public void testPublishGPT() throws Exception {
        JSONObject docWrapperJson = new JSONObject(loadAsStringFromClassPath("testdata/edit_doc47_edit_record.json"));
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

                Assert.fail("publish to GPT failed");
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
