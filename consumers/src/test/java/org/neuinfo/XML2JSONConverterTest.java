package org.neuinfo;

import org.custommonkey.xmlunit.Diff;
import org.custommonkey.xmlunit.XMLTestCase;
import org.custommonkey.xmlunit.XMLUnit;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.input.SAXBuilder;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;
import org.json.JSONObject;
import org.neuinfo.foundry.common.util.Utils;
import org.neuinfo.foundry.common.util.XML2JSONConverter;

import java.io.File;
import java.io.StringWriter;

/**
 * Created by bozyurt on 5/6/14.
 */
public class XML2JSONConverterTest extends XMLTestCase {
    public XML2JSONConverterTest(String name) {
        super(name);
    }


    public void testRoundtrip() throws Exception {
        String xmlFile = "/tmp/open_source_brain_projects.xml";
        SAXBuilder builder = new SAXBuilder();
        Document doc = builder.build(new File(xmlFile));
        Element rootEl = doc.getRootElement();

        XML2JSONConverter converter = new XML2JSONConverter();
        JSONObject json = converter.toJSON(rootEl);

        Element docEl = converter.toXML(json);

        XMLOutputter xmlOutputter = new XMLOutputter(Format.getPrettyFormat());

        doc = new Document();
        doc.setRootElement(docEl);
        StringWriter sw = new StringWriter(50000);
        xmlOutputter.output(doc, sw);

        Utils.saveXML(rootEl, "/tmp/reconstructed.xml");

        String origXml = Utils.loadAsString(xmlFile);
        String reconstructedXml = sw.toString();
        XMLUnit.setIgnoreWhitespace(true);
        Diff diff = new Diff(origXml, reconstructedXml);

        assertTrue("XML identical " + diff.toString(), diff.identical());
    }

    public void testRoundtripWithNamespaces() throws Exception {
        String xmlFile = "/tmp/053B250F-3EAB-4FA5-B7D0-52ED907A6526_formatted.xml";
        SAXBuilder builder = new SAXBuilder();
        Document doc = builder.build(new File(xmlFile));
        Element rootEl = doc.getRootElement();

        XML2JSONConverter converter = new XML2JSONConverter();
        JSONObject json = converter.toJSON(rootEl);

        Element docEl = converter.toXML(json);

        XMLOutputter xmlOutputter = new XMLOutputter(Format.getPrettyFormat());

        doc = new Document();
        doc.setRootElement(docEl);
        StringWriter sw = new StringWriter(50000);
        xmlOutputter.output(doc, sw);

        Utils.saveXML(rootEl, "/tmp/reconstructed_cinergi.xml");

        String origXml = Utils.loadAsString(xmlFile);
        String reconstructedXml = sw.toString();
        XMLUnit.setIgnoreWhitespace(true);
        Diff diff = new Diff(origXml, reconstructedXml);

        assertTrue("XML identical " + diff.toString(), diff.identical());
    }

    // international characters.
    //Stöbe, W
    // working
    public void testRoundtripWithNamespacesFromURL() throws Exception {
        String xmlSource = "http://132.249.238.169:8080/metadata/pangaea_datacite/pangaea_iso/oai.datacite.org/00001/oai_oai.datacite.org_2423966.xml";

        SAXBuilder builder = new SAXBuilder();
        Document doc = builder.build(xmlSource);
        Element rootEl = doc.getRootElement();

        XML2JSONConverter converter = new XML2JSONConverter();
        JSONObject json = converter.toJSON(rootEl);

        Element docEl = converter.toXML(json);

        XMLOutputter xmlOutputter = new XMLOutputter(Format.getPrettyFormat());

        Document doc2 = new Document();
        doc2.setRootElement(docEl);
        StringWriter sw = new StringWriter(50000);
        xmlOutputter.output(doc2, sw);

        //Utils.saveXML(rootEl, "reconstructed_cinergi.xml");
        StringWriter sw2 = new StringWriter(50000);
        xmlOutputter.output(doc, sw2);
        //  String origXml = Utils.loadAsString(xmlFile);
        String origXml = sw2.toString();
        String reconstructedXml = sw.toString();
        XMLUnit.setIgnoreWhitespace(true);
        Diff diff = new Diff(origXml, reconstructedXml);

        assertTrue("XML identical " + diff.toString(), diff.identical());
    }
}
