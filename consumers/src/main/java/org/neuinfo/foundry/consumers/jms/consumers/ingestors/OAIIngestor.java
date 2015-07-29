package org.neuinfo.foundry.consumers.jms.consumers.ingestors;

import org.jdom2.Element;
import org.json.JSONObject;
import org.neuinfo.foundry.common.util.Assertion;
import org.neuinfo.foundry.common.util.PullJDOMXmlHandler;
import org.neuinfo.foundry.common.util.XML2JSONConverter;
import org.neuinfo.foundry.consumers.plugin.Ingestor;
import org.neuinfo.foundry.consumers.plugin.Result;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.Map;

/**
 * Created by bozyurt on 11/5/14.
 */
public class OAIIngestor implements Ingestor {
    String ingestURL;
    boolean allowDuplicates = false;
    String totElName;
    String docElName;
    Map<String, String> optionMap;
    PullJDOMXmlHandler xmlHandler;
    Element currentRecEl;

    @Override
    public void initialize(Map<String, String> options) throws Exception {
        this.optionMap = options;
        this.ingestURL = options.get("ingestURL");
        this.allowDuplicates = options.containsKey("allowDuplicates") ?
                Boolean.parseBoolean(options.get("allowDuplicates")) : false;
        this.totElName = options.get("topElement");
        this.docElName = options.get("documentElement");
    }

    @Override
    public void startup() throws Exception {
        // FIXME get it form OAIPMHHarvester

        String xmlFile = "/tmp/DryadDataRepository/DryadDataRepository_hdl_10255_dryad.148_11-05-2014.xml";
        xmlHandler = new PullJDOMXmlHandler(xmlFile);
        Element el = xmlHandler.nextElementStart();
        System.out.println("el:" + el.getName());
        Assertion.assertTrue(el.getName().equals(totElName));
    }

    @Override
    public Result prepPayload() {
        try {
            Assertion.assertNotNull(this.currentRecEl);
            XML2JSONConverter converter = new XML2JSONConverter();
            JSONObject json = converter.toJSON(this.currentRecEl);
            return new Result(json, Result.Status.OK_WITH_CHANGE);
        } catch(Throwable t) {
            t.printStackTrace();
            return new Result(null, Result.Status.ERROR, t.getMessage());
        }
    }

    @Override
    public String getName() {
        return "OAIIngestor";
    }

    @Override
    public int getNumRecords() {
        return -1;
    }

    @Override
    public String getOption(String optionName) {
        return optionMap.get(optionName);
    }

    @Override
    public void shutdown() {
        if (xmlHandler != null) {
            xmlHandler.shutdown();
        }
    }

    @Override
    public boolean hasNext() {
        try {
            this.currentRecEl = xmlHandler.nextElement(this.docElName);
            return this.currentRecEl != null;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}
