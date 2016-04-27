package org.neuinfo.foundry.consumers.common;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.jdom2.Element;
import org.jdom2.Namespace;
import org.neuinfo.foundry.common.util.LRUCache;
import org.neuinfo.foundry.common.util.Utils;

import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by bozyurt on 4/27/16.
 */
public class Organization {
    private String name;
    private String uri;
    static Map<String, Organization> organizationCache = Collections.synchronizedMap(new LRUCache<String, Organization>(500));
    static Namespace srw = Namespace.getNamespace("", "http://www.loc.gov/zing/srw/");
    static Namespace viaf = Namespace.getNamespace("v", "http://viaf.org/viaf/terms#");
    static Organization NULL_ORG = new Organization(null);

    public Organization(String name, String uri) {
        this.name = name;
        this.uri = uri;
    }

    public Organization(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public String getUri() {
        return uri;
    }

    public static String normalizeOrgName(String name) {
        Pattern commonTermPattern = Pattern.compile("-*(\\s*(H|h)ome\\s*|(W|w)elcome\\s*|(T|t)he\\s*|\\s*(O|o)verview\\s*|\\s*(M|m)ain\\s*|\\s*(P|p)age\\s*)");
        Matcher matcher = commonTermPattern.matcher(name);
        name = matcher.replaceAll("");
        return name.replaceAll("'[^a-zA-Z0-9 -.]", "");
    }

    public static Organization validateInViaf(String orgName) throws Exception {
        Organization organization = organizationCache.get(orgName);
        if (organization != null) {
            if (organization == NULL_ORG) {
                return null;
            }
            return organization;
        }
        HttpClient client = new DefaultHttpClient();
        String encodedOrgName = orgName.replaceAll("\\s", "%2B");
        StringBuilder sb = new StringBuilder(200);
        sb.append("http://viaf.org/viaf/search?query=");
        String query = "local.corporateNames+all+" + encodedOrgName;
        sb.append(query).append("&");
        sb.append("recordSchema=BriefVIAF&sortKeys=holdingscount");

        URI uri = new URI(sb.toString());
        HttpGet httpGet = new HttpGet(uri);
        System.out.println("uri:" + uri);
        try {
            HttpResponse response = client.execute(httpGet);
            HttpEntity entity = response.getEntity();
            if (entity != null) {
                String xmlContent = EntityUtils.toString(entity);
              //  System.out.println(xmlContent);
                Element rootEl = Utils.readXML(xmlContent);
                Element records = rootEl.getChild("records", srw);
                if (records == null) {
                    organizationCache.put(orgName, NULL_ORG);
                    return null;
                }
                List<Element> recordList = records.getChildren("record", srw);
                String escapedOrgName = orgName.replaceAll("\\-", "\\-");
                Pattern p = Pattern.compile(escapedOrgName + "( \\(.*\\)$)*", Pattern.CASE_INSENSITIVE);
                for (Element record : recordList) {
                    Element recordData = record.getChild("recordData", srw);

                    if (recordData != null) {
                        Element viafCluster = recordData.getChild("VIAFCluster", viaf);
                        if (viafCluster != null) {
                            String ctitle = viafCluster.getChild("mainHeadings", viaf)
                                    .getChild("data", viaf).getChild("text", viaf).getTextTrim();
                            Matcher matcher = p.matcher(ctitle);
                            if (matcher.find()) {
                                Element viafId = viafCluster.getChild("viafID", viaf);
                                if (viafId != null) {
                                    String viafIdStr = viafId.getTextNormalize();
                                    String uriStr = "http://viaf.org/viaf/" + viafIdStr + "/";
                                    System.out.println(viafIdStr);
                                    System.out.println(uriStr);
                                    organization = new Organization(orgName, uriStr);
                                    organizationCache.put(orgName, organization);
                                    return organization;
                                }
                            }
                        }
                    }
                }
            }
            organizationCache.put(orgName, NULL_ORG);
        } finally {
            if (httpGet != null) {
                httpGet.releaseConnection();
            }
        }
        return null;
    }


    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("Organization{");
        sb.append("name='").append(name).append('\'');
        sb.append('}');
        return sb.toString();
    }

    public static void main(String[] args) throws Exception {
        Organization.validateInViaf("Doherty Earth Observatory");
    }
}
