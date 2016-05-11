package org.neuinfo.foundry.consumers;

import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import junit.framework.TestCase;
import org.json.JSONArray;
import org.json.JSONObject;
import org.neuinfo.foundry.common.util.JSONPathProcessor;
import org.neuinfo.foundry.common.util.JSONUtils;
import org.neuinfo.foundry.common.util.Utils;
import org.neuinfo.foundry.consumers.jms.consumers.ingestors.WAFIngestor;
import org.neuinfo.foundry.consumers.plugin.Ingestor;
import org.neuinfo.foundry.consumers.plugin.Result;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by bozyurt on 11/28/15.
 */
public class WAFIngestorTest extends TestCase {
    public WAFIngestorTest(String name) {
        super(name);
    }

    public void testWAFSubdirs() throws Exception {
        Map<String, String> options = new HashMap<String, String>(3);
        options.put("ingestURL", "http://maxim.ucsd.edu/waf/data.gov_all");
        Ingestor ingestor = new WAFIngestor();
        ingestor.initialize(options);
        ingestor.startup();
    }


    public static String getPrimaryKey(JSONObject json) throws Exception {
        String[] jsonPaths = {"$.'gmd:MD_Metadata'.'gmd:fileIdentifier'.'gco:CharacterString'.'_$'",
                "$.'gmi:MI_Metadata'.'gmd:fileIdentifier'.'gco:CharacterString'.'_$'"};
        JSONPathProcessor processor = new JSONPathProcessor();
        for (String jsonPath : jsonPaths) {
            List<Object> objects = processor.find(jsonPath, json);
            if (objects != null && !objects.isEmpty()) {
                  return (String) objects.get(0);
            }
        }
        return null;
    }

    public void testWAFIngest() throws Exception {
        Map<String, String> options = new HashMap<String, String>(3);
        //options.put("ingestURL", "http://hydro10.sdsc.edu/metadata/NODC/");
        options.put("ingestURL", "http://hydro10.sdsc.edu/metadata/CZO_Datasets/");
        List<String> jsonPaths = new ArrayList<String>(2);
        List<String> titleJsonPaths = new ArrayList<String>(2);
        jsonPaths.add("$..'gmd:abstract'.'gco:CharacterString'.'_$'");
        titleJsonPaths.add("$..'gmd:citation'.'gmd:CI_Citation'.'gmd:title'.'gco:CharacterString'.'_$'");
        jsonPaths.add("$..'abstract'.'gco:CharacterString'.'_$'");
        titleJsonPaths.add("$..'title'.'gco:CharacterString'.'_$'");
        Ingestor ingestor = new WAFIngestor();
        ingestor.initialize(options);
        ingestor.startup();


        JSONArray jsArr = new JSONArray();
        while (ingestor.hasNext()) {
            Result result = ingestor.prepPayload();
            if (result.getStatus() != Result.Status.ERROR) {
                JSONPathProcessor processor = new JSONPathProcessor();
                JSONObject json = result.getPayload();
                JSONObject content = new JSONObject();
                String primaryKey = getPrimaryKey(json);
                assertNotNull(primaryKey);
                content.put("id", primaryKey);
                content.put("abstract", "");
                content.put("title", "");
                for (String jsonPath : jsonPaths) {
                    List<Object> objects = processor.find(jsonPath, json);
                    if (objects != null && !objects.isEmpty()) {
                        String text2Annotate = (String) objects.get(0);
                        if (!text2Annotate.equals("REQUIRED FIELD")) {
                            content.put("abstract", text2Annotate);
                        }
                    }
                }
                for (String jsonPath : titleJsonPaths) {
                    List<Object> objects = processor.find(jsonPath, json);
                    if (objects != null && !objects.isEmpty()) {
                        String text2Annotate = (String) objects.get(0);
                        if (!text2Annotate.equals("REQUIRED FIELD")) {
                            content.put("title", text2Annotate);
                        }
                    }
                }
                jsArr.put(content);
            }

        }
        Utils.saveText(jsArr.toString(2), "/tmp/czo_contents.json");
    }
}
