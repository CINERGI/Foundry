package org.neuinfo.foundry.consumers.jms.consumers.ingestors;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.jdom2.input.SAXBuilder;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.neuinfo.foundry.common.util.Utils;
import org.neuinfo.foundry.common.util.WAFUtils;
import org.neuinfo.foundry.common.util.XML2JSONConverter;
import org.neuinfo.foundry.consumers.plugin.Ingestor;
import org.neuinfo.foundry.consumers.plugin.Result;

import java.io.IOException;
import java.io.StringReader;
import java.net.URI;
import java.util.*;

/**
 * Created by bozyurt on 11/10/14.
 */
public class WAFIngestor implements Ingestor {
    String ingestURL;
    boolean allowDuplicates = false;
    /** if provided select what to be ingested from this file */
    String includeFile = null;
    Map<String, String> optionMap;
    Iterator<String> docLinkIterator;
    int maxNumDocs2Ingest = -1;
    int count = 0;

    @Override
    public void initialize(Map<String, String> options) throws Exception {
        this.ingestURL = options.get("ingestURL");
        this.allowDuplicates = options.containsKey("allowDuplicates") ?
                Boolean.parseBoolean(options.get("allowDuplicates")) : false;
        this.optionMap = options;
        if (options.containsKey("maxDocs")) {
            this.maxNumDocs2Ingest = Utils.getIntValue(options.get("maxDocs"), -1);
        }
        includeFile = options.get("includeFile");
    }

    @Override
    public void startup() throws Exception {
        List<String> links = new LinkedList<String>();

        final Iterator<Element> it = WAFUtils.extractAnchors(this.ingestURL);
        while (it.hasNext()) {
            Element ae = it.next();
            final String href = ae.attr("abs:href");
            if (href != null && href.endsWith(".xml")) {
                links.add(href);
            } else if (href.length() > this.ingestURL.length()){
                WAFUtils.collectLinks(href, links);
                if (maxNumDocs2Ingest > 0 && links.size() >= maxNumDocs2Ingest) {
                    break;
                }
            }
        }
        if (includeFile != null) {
            List<String> includeList = Utils.loadList(includeFile);
            Set<String> includeSet = new HashSet<String>(includeList);
            includeList = null;
            for(Iterator<String> iter = links.iterator(); iter.hasNext();) {
                String link = iter.next();
                if (!includeSet.contains(link)) {
                    iter.remove();
                }
            }
        }
        System.out.println("# of xml docs:" + links.size());
        this.docLinkIterator = links.iterator();
    }



    @Override
    public Result prepPayload() {
        try {
            String docURL = this.docLinkIterator.next();
            this.count++;
            SAXBuilder builder = new SAXBuilder();
            String xmlContent = WAFUtils.getXMLContent(docURL);
            org.jdom2.Document doc = builder.build(new StringReader(xmlContent));
            org.jdom2.Element rootEl = doc.getRootElement();
            XML2JSONConverter converter = new XML2JSONConverter();
            JSONObject json = converter.toJSON(rootEl);
            return new Result(json, Result.Status.OK_WITH_CHANGE);
        } catch (Throwable t) {
            t.printStackTrace();
        }
        return null;
    }

    @Override
    public String getName() {
        return "WAFIngestor";
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
        // no op
    }

    @Override
    public boolean hasNext() {
        if (this.maxNumDocs2Ingest > 0 && count >= this.maxNumDocs2Ingest) {
            return false;
        }
        return this.docLinkIterator.hasNext();
    }


}
