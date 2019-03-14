package org.neuinfo.foundry.common.util;

import junit.framework.TestCase;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.input.SAXBuilder;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;
import org.json.JSONObject;

/**
 * Created by bozyurt on 3/3/15.
 */
public class XML2JsonConverterTests extends TestCase {

    public XML2JsonConverterTests(String name) {
        super(name);
    }

    public void testAustraliaMetaData() throws Exception {
        String xmlSource = "http://hydro10.sdsc.edu/metadata/geoscience_australia/043dbb8c-66de-6897-e054-00144fdd4fa6.xml";
        SAXBuilder builder = new SAXBuilder();
        Document doc = builder.build(xmlSource);
        Element rootEl = doc.getRootElement();


        XML2JSONConverter converter = new XML2JSONConverter();
        JSONObject json = converter.toJSON(rootEl);

        System.out.println(json.toString(2));

        Element docEl = converter.toXML(json);

        XMLOutputter xmlOutputter = new XMLOutputter(Format.getPrettyFormat());

        doc = new Document();
        doc.setRootElement(docEl);
        System.out.println("==============================================");
        xmlOutputter.output(doc, System.out);
    }

    // international characters.
    //Stöbe, W
    public void testPangaeaMetaData() throws Exception {
        String xmlSource = "http://132.249.238.169:8080/metadata/pangaea_datacite/pangaea_iso/oai.datacite.org/00001/oai_oai.datacite.org_2423966.xml";
        SAXBuilder builder = new SAXBuilder();
        Document doc = builder.build(xmlSource);
        Element rootEl = doc.getRootElement();


        XML2JSONConverter converter = new XML2JSONConverter();
        JSONObject json = converter.toJSON(rootEl);

        System.out.println(json.toString(2));

        Element docEl = converter.toXML(json);

        XMLOutputter xmlOutputter = new XMLOutputter(Format.getPrettyFormat());

        Document doc2 = new Document();
        doc2.setRootElement(docEl);
        System.out.println("==============================================");
        xmlOutputter.output(doc2, System.out);

        // need to assert that names in ci_responsible party are the same
    }
}
