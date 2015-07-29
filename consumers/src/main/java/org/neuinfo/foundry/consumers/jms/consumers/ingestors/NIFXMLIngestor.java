package org.neuinfo.foundry.consumers.jms.consumers.ingestors;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.input.SAXBuilder;
import org.json.JSONObject;
import org.neuinfo.foundry.common.util.Assertion;
import org.neuinfo.foundry.common.util.Utils;
import org.neuinfo.foundry.common.util.XML2JSONConverter;
import org.neuinfo.foundry.consumers.plugin.Ingestor;
import org.neuinfo.foundry.consumers.plugin.Result;

import java.io.StringReader;
import java.net.URI;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Created by bozyurt on 10/28/14.
 */
public class NIFXMLIngestor implements Ingestor {
    String ingestURL;
    boolean allowDuplicates = false;
    String totElName;
    String docElName;
    List<Element> elements;
    Iterator<Element> recordIterator;
    Map<String, String> optionMap;

    @Override
    public void initialize(Map<String, String> options) throws Exception {
        this.ingestURL = options.get("ingestURL");
        this.allowDuplicates = options.containsKey("allowDuplicates") ?
                Boolean.parseBoolean(options.get("allowDuplicates")) : false;
        this.totElName = options.get("topElement");
        this.docElName = options.get("documentElement");

        this.optionMap = options;
    }

    @Override
    public void startup() throws Exception {
        SAXBuilder builder = new SAXBuilder();

        //String xmlContent = getXMLContent(this.ingestURL);

        // FIXME test only
        String xmlContent = Utils.loadAsString("/tmp/projects.xml");
        Document doc = builder.build( new StringReader(xmlContent) );
        Element rootEl = doc.getRootElement();
        Element topEl;
        if (rootEl.getName().equals(this.totElName)) {
            topEl = rootEl;
        } else {
            topEl = rootEl.getChild(this.totElName);
        }
        Assertion.assertNotNull(topEl);
        this.elements = topEl.getChildren(this.docElName);
        this.recordIterator = elements.iterator();
    }

    public static String getXMLContent(String ingestURL) throws Exception {
        HttpClient client = new DefaultHttpClient();
        URIBuilder builder = new URIBuilder(ingestURL);
        URI uri = builder.build();
        HttpGet httpGet = new HttpGet(uri);
        try {
            HttpResponse response = client.execute(httpGet);
            HttpEntity entity = response.getEntity();
            if (entity != null) {
                String xmlStr = EntityUtils.toString(entity);
                return xmlStr;
            }

        } finally {
            if (httpGet != null) {
                httpGet.releaseConnection();
            }
        }
        return null;
    }

    @Override
    public Result prepPayload() {
        try {
            Element el = this.recordIterator.next();
            XML2JSONConverter converter = new XML2JSONConverter();
            JSONObject json = converter.toJSON(el);
            return new Result(json, Result.Status.OK_WITH_CHANGE);
        } catch (Throwable t) {
            t.printStackTrace();
            return new Result(null, Result.Status.ERROR, t.getMessage());
        }

    }

    @Override
    public String getName() {
        return "NIFXMLIngestor";
    }

    @Override
    public int getNumRecords() {
        return this.elements.size();
    }

    @Override
    public String getOption(String optionName) {
        return this.optionMap.get(optionName);
    }

    @Override
    public void shutdown() {
        // no op
    }

    @Override
    public boolean hasNext() {
        return this.recordIterator.hasNext();
    }

}
