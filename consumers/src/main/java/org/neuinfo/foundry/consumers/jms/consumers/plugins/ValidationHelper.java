package org.neuinfo.foundry.consumers.jms.consumers.plugins;

import bnlpkit.util.FileUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.jdom2.Element;
import org.json.JSONObject;
import org.neuinfo.foundry.common.model.TeamEngineValidationRec;
import org.neuinfo.foundry.common.util.Assertion;
import org.neuinfo.foundry.common.util.Utils;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URI;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by bozyurt on 2/18/15.
 */
public class ValidationHelper {
    String serviceURL = "http://cite-dev-03.opengeospatial.org/teamengine/rest/suites/iso19139/1.0/run";


    // http://cite-dev-03.opengeospatial.org/teamengine/rest/suites/iso19139/1.0/run?
    // iut=http://hydro10.sdsc.edu/metadata/Raquel_Files/1E97BD2D-0FDD-4BAC-8DEA-FEB57AB53A6E.xml
    // &sch=http://cite-dev-03.opengeospatial.org/teamengine/checkScopeOfXmlFile.sch


    public TeamEngineValidationRec validate(String isoXml) throws Exception {
        HttpClient client = new DefaultHttpClient();
        URIBuilder builder = new URIBuilder(this.serviceURL);
        URI uri = builder.build();
        HttpPost httpPost = new HttpPost(uri);
        System.out.println("uri:" + uri);
        // httpPost.addHeader("Content-Type","multipart/form-data");
        httpPost.addHeader("Content-Type", "text/plain; charset=utf-8");
        // httpPost.addHeader("Accept", "application/xml");
        try {
            List<NameValuePair> params = new ArrayList<NameValuePair>(3);
            params.add(new BasicNameValuePair("iut", isoXml));
            //   params.add(new BasicNameValuePair("sch",
            //           "http://cite-dev-03.opengeospatial.org/teamengine/checkScopeOfXmlFile.sch"));
            JSONObject json = new JSONObject();
            json.put("iut", isoXml);
            httpPost.setEntity(new StringEntity(json.toString()));
            final HttpResponse response = client.execute(httpPost);
            if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
                System.out.println(response);
                final HttpEntity entity = response.getEntity();
                StringBuilder sb = new StringBuilder(4096);
                String newline = System.getProperty("line.separator");
                if (entity != null) {
                    BufferedReader bin = null;
                    try {
                        bin = new BufferedReader(new InputStreamReader(entity.getContent(), Charset.forName("UTF-8")));
                        String line;
                        while ((line = bin.readLine()) != null) {
                            System.out.println(line);
                            sb.append(line).append(newline);
                        }
                    } finally {
                        FileUtils.close(bin);
                    }
                }
                return process(sb.toString().trim());
            }

        } finally {
            if (httpPost != null) {
                httpPost.releaseConnection();
            }
        }
        return null;
    }

    TeamEngineValidationRec process(String xmlStr) throws Exception {
        Element rootEl = Utils.readXML(xmlStr);
        String failedStr = rootEl.getAttributeValue("failed");
        int numFailed = Utils.getIntValue(failedStr, -1);
        Assertion.assertTrue(numFailed != -1);
        Element repOutEl = rootEl.getChild("reporter-output");
        List<Element> lineEls = repOutEl.getChildren("line");
        Date ruleSetDate = null;
        String ruleSetName = null;
        for (Element lineEl : lineEls) {
            String text = lineEl.getTextTrim();
            if (text.indexOf("**DATE AND TIME PERFORMED  :") != -1) {
                Pattern p = Pattern.compile("\\*\\*DATE AND TIME PERFORMED  :(\\d{4,4}/\\d{2,2}/\\d{2,2} \\d{2,2}:\\d{2,2}:\\d{2,2})");
                Matcher m = p.matcher(text);
                if (m.find()) {
                    ruleSetDate = TeamEngineValidationRec.toDate(m.group(1));
                }
            } else if (text.indexOf("**TEST NAME AND VERSION") != -1) {
                Pattern p = Pattern.compile("\\*\\*TEST NAME AND VERSION\\s+:([\\w\\-\\.]+)");
                Matcher m = p.matcher(text);
                if (m.find()) {
                    ruleSetName = m.group(1);
                }
            }
        }

        TeamEngineValidationRec rec = new TeamEngineValidationRec(ruleSetName, ruleSetDate, xmlStr, numFailed == 0);

        return rec;
    }

    public static void main(String[] args) throws Exception {
        String HOME_DIR = System.getProperty("user.home");
        String isoXML = Utils.loadAsString(HOME_DIR +
                "/dev/java/Foundry/ingestor/etc/053B250F-3EAB-4FA5-B7D0-52ED907A6526.xml");

        ValidationHelper vh = new ValidationHelper();

        TeamEngineValidationRec validationRec = vh.validate(isoXML);
        System.out.println("=======================");
        System.out.println(validationRec);
    }
}
