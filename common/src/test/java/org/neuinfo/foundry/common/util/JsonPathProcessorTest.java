package org.neuinfo.foundry.common.util;

import junit.framework.TestCase;
import org.json.JSONObject;

import java.util.List;

/**
 * Created by bozyurt on 5/27/14.
 */
public class JsonPathProcessorTest extends TestCase {

    public void testFindPrimaryKey() throws Exception {
        final String s = Utils.loadAsString("/tmp/osbp.json");
        JSONObject js = new JSONObject(s);

        JSONPathProcessor processor = new JSONPathProcessor();

        final List<Object> objects = processor.find("$.project.identifier.'_$'", js);

        assertTrue(objects.size() == 1);
        String identifier = objects.get(0).toString();

        assertEquals(identifier,"vogelsetal2011");
    }
}
