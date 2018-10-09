package org.neuinfo.foundry.consumers;

import junit.framework.TestCase;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.neuinfo.foundry.common.util.Utils;
import org.neuinfo.foundry.common.util.DiffRecord;
import org.neuinfo.foundry.consumers.common.EditDiffManager;
import org.neuinfo.foundry.common.util.JsonPathDiffHandler;

import java.net.URL;
import java.util.List;

/**
 * Created by bozyurt on 9/22/17.
 */
public class EditDiffManagerTests extends TestCase {

    public EditDiffManagerTests(String name) {
        super(name);
    }


    public void testCreateUpdateKeywordDiffJson() throws Exception {
        JSONObject docWrapper = new JSONObject(loadAsStringFromClassPath("testdata/edit_doc47_edit_record.json"));
        assertTrue(docWrapper.has("Data"));
        JSONObject dataJson = docWrapper.getJSONObject("Data");
        DiffRecord diffRecord = new DiffRecord("update","Data.enhancedKeywords.6.validation","", "false");
        List<JsonPathDiffHandler.JsonPathNode> path = JsonPathDiffHandler.parseJsonPath(diffRecord.getJsonPath());
        path = path.subList(1, path.size());
        JSONArray dest = new JSONArray();
        JsonPathDiffHandler.createUpdateKeywordDiffJson(path, diffRecord, dataJson, dest);

        JsonPathDiffHandler.createUpdateKeywordDiffJson(path, diffRecord, dataJson, dest);
        System.out.println(dest.toString(2));

    }
    public void testModifyJson() throws Exception {
        JSONObject docWrapper = new JSONObject(loadAsStringFromClassPath("testdata/edit_test_doc_wrapper.json"));

        assertTrue(docWrapper.has("OriginalDoc"));
        JSONObject originalDoc = docWrapper.getJSONObject("OriginalDoc");

        JSONObject editRec = new JSONObject( loadAsStringFromClassPath("testdata/test_metadataRecordLineageItem2.json"));
        assertNotNull(editRec);

        List<DiffRecord> diffRecords = EditDiffManager.extractEditDiffInformation(editRec);
        assertNotNull(diffRecords);
        for(DiffRecord diffRecord : diffRecords) {
           // if (diffRecord.getType().equalsIgnoreCase("update")) {
                System.out.println(diffRecord.getJsonPath());
                List<JsonPathDiffHandler.JsonPathNode> path = JsonPathDiffHandler.parseJsonPath(diffRecord.getJsonPath());
                path = path.subList(1, path.size());
                try {
                    JsonPathDiffHandler.modifyJson(path, diffRecord, originalDoc);
                } catch(JSONException je) {
                    System.err.println(je.getMessage());
                }
          //  }
        }
        System.out.println(originalDoc.toString(2));

    }


    public static String loadAsStringFromClassPath(String classpath) throws Exception {
        URL url = EditDiffManagerTests.class.getClassLoader().getResource(classpath);
        String path = url.toURI().getPath();
        return Utils.loadAsString(path);
    }
}
