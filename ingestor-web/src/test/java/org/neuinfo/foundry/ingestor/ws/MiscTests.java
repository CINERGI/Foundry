package org.neuinfo.foundry.ingestor.ws;

import org.jdom2.Element;
import org.junit.Test;
import org.neuinfo.foundry.common.util.Utils;

import java.net.URL;

import static org.junit.Assert.assertNotNull;

/**
 * Created by bozyurt on 4/2/18.
 */
public class MiscTests {


    @Test
    public void testForm2ISOXMLGeneration() throws Exception {
        String xmlStr = loadAsStringFromClassPath("testdata/form_6922925790434181084.xml");

        Element rootEl = Utils.readXML(xmlStr);
        assertNotNull(rootEl);
    }

    public static String loadAsStringFromClassPath(String classpath) throws Exception {
        URL url = MiscTests.class.getClassLoader().getResource(classpath);
        String path = url.toURI().getPath();
        return Utils.loadAsString(path);
    }
}
