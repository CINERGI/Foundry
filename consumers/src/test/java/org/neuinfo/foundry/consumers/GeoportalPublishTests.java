package org.neuinfo.foundry.consumers;

import com.mongodb.BasicDBObject;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
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
 * mvn -D gptuser=lester -D gptpassword=youwish -D gpturi=http://localhost:8080/geoportal/ -D esmetadata=metadata test
 *
 * In Intellij add to run's vm options:
 * -Dgptuser={user} -Dgptpassword={password} -
 * -Dgptuser={user} -Dgptpassword={password} -Dgpturi=http://{host}:8080/geoportal/
 */
public class GeoportalPublishTests  {
     String gptuser;
     String gptpassword;
     String gpturi;
     String esmetadata;

    @BeforeEach
    public  void initClass() {
        gptuser = System.getProperty("gptuser");
        gptpassword = System.getProperty("gptpassword");
        gpturi = System.getProperty("gpturi");
        esmetadata = System.getProperty("esmetadata");

        if (gpturi == null){
            gpturi = "http://localhost:8080/geoportal/";
        }
        if (esmetadata == null ){
            esmetadata = "metadata";
        }
        if ((gptuser == null) || (gptpassword == null)) {
            throw new RuntimeException("Invalid configuration. Make sure to set gptuser, gptpassword, !");
        }
    }

    @DisplayName("Submit files. Should be no dupes.")
    @ParameterizedTest
    @ValueSource(strings = {
            "testdata/gpt_test_doc_wrapper.json",
            "testdata/gpt_test_doc_colon_wrapper.json"
    }
    )
    public void testPublishGPT(String file) throws Exception {
        JSONObject docWrapperJson = new JSONObject(loadAsStringFromClassPath(file));
        assertTrue(docWrapperJson.has("OriginalDoc"));
        assertTrue(docWrapperJson.has("Processing"));
        Helper helper = new Helper("");
        try {
           // helper.startup("cinergi-consumers-cfg.xml");
            String user = helper.getParam("user");
            String password = helper.getParam("password");


            IPlugin plugin = new Geoportal2Exporter();
            Map<String, String> optionMap = new HashMap<String, String>();
            optionMap.put("gptURI", gpturi);
            optionMap.put("elasticsearchIndex", esmetadata);

            optionMap.put("user", gptuser);
            optionMap.put("password", gptpassword);


            plugin.initialize(optionMap);
            BasicDBObject docWrapper = BasicDBObject.parse(
                    loadAsStringFromClassPath(file));
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
