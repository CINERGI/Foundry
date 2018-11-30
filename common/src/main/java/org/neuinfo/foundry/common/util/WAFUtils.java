package org.neuinfo.foundry.common.util;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.apache.http.entity.ContentType;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.net.URI;
import java.util.Iterator;
import java.util.List;

/**
 * Created by bozyurt on 7/28/16.
 */
public class WAFUtils {


    public static Iterator<Element> extractAnchors(String url) throws IOException {
        Document doc = Jsoup.connect(url).timeout(15000).maxBodySize(0).get();
        final Elements anchorEls = doc.select("a");
        return anchorEls.iterator();
    }

    public static void collectLinks(String subDirHref, List<String> links) throws IOException {
        int startSize = links.size();
        Document doc = Jsoup.connect(subDirHref).timeout(15000).maxBodySize(0).get();
        Elements anchorEls = doc.select("a");
        Iterator<Element> it = anchorEls.iterator();
        while (it.hasNext()) {
            String href = it.next().attr("abs:href");
            if (href != null && href.endsWith(".xml")) {
                links.add(href);
            }
        }
        int numAdded = links.size() - startSize;
        System.out.println("Added " + numAdded + " documents from " + subDirHref);
    }

     public static String getXMLContent(String docURL) throws Exception {
        HttpClient client = new DefaultHttpClient();
        URIBuilder builder = new URIBuilder(docURL);
        URI uri = builder.build();
        HttpGet httpGet = new HttpGet(uri);
        try {
            HttpResponse response = client.execute(httpGet);
            HttpEntity entity = response.getEntity();

            ContentType ct = ContentType.get(entity);
            String charset = null;
            if (ct.getCharset() == null){
                charset="utf8";
            }
            else {
                charset=ct.getCharset().toString();
            }

            if (entity != null) {
                String xmlStr = EntityUtils.toString(entity,charset);
                return xmlStr;
            }


        } finally {
            if (httpGet != null) {
                httpGet.releaseConnection();
            }
        }
        return null;
    }

}
