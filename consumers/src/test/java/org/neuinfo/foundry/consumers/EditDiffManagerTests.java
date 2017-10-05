package org.neuinfo.foundry.consumers;

import junit.framework.TestCase;
import org.json.JSONException;
import org.json.JSONObject;
import org.neuinfo.foundry.common.util.Utils;
import org.neuinfo.foundry.consumers.common.DiffRecord;
import org.neuinfo.foundry.consumers.common.EditDiffManager;
import org.neuinfo.foundry.consumers.common.JsonPathJDOMHandler;

import java.net.URL;
import java.util.List;

/**
 * Created by bozyurt on 9/22/17.
 */
public class EditDiffManagerTests extends TestCase {

    public EditDiffManagerTests(String name) {
        super(name);
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
                System.out.println(diffRecord.getUpdatePath());
                List<JsonPathJDOMHandler.JsonPathNode> path = JsonPathJDOMHandler.parseJsonPath(diffRecord.getUpdatePath());
                path = path.subList(1, path.size());
                try {
                    JsonPathJDOMHandler.modifyJson(path, diffRecord, originalDoc);
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
