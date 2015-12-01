package org.neuinfo.foundry.consumers.common;

import au.com.bytecode.opencsv.CSVReader;
import org.json.JSONArray;
import org.neuinfo.foundry.common.model.EntityInfo;
import org.neuinfo.foundry.common.model.Keyword;
import org.neuinfo.foundry.common.util.Utils;

import java.io.BufferedReader;
import java.io.EOFException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by bozyurt on 11/24/15.
 */
public class OrgLookupHandler {
    static Map<String, Organization> orgMap = new HashMap<String, Organization>();

    static {
        InputStream in = Utils.class.getClassLoader().getResourceAsStream(
                "org_2_viaf_map.csv");
        try {
            BufferedReader bin = new BufferedReader(new InputStreamReader(in));
            CSVReader csvReader = new CSVReader(bin, ',', '"', 1);

            try {
                String[] row = null;
                while ((row = csvReader.readNext()) != null) {
                    if (row.length >= 2) {
                        Organization org = new Organization();
                        String key = row[0];
                        org.name = row[0];
                        org.viaf = row[1];
                        if (row[2].length() > 0) {
                            org.preferredLabel = row[2];
                        } else {
                            org.preferredLabel = org.name;
                        }
                        orgMap.put(key, org);
                    }
                }
            } catch (Exception x) {
                if (!(x instanceof EOFException)) {
                    x.printStackTrace();
                }
            }
        } finally {
            Utils.close(in);
        }
    }


    public Keyword getOrganizationVIAF(String orgName, Map<String, Keyword> keywordMap) {
        Organization org = orgMap.get(orgName);
        if (org != null) {
            String combined = org.name + ": " + org.viaf;
            if (!keywordMap.containsKey(combined)) {
                String term = combined;
                if (org.preferredLabel != null) {
                    term = org.preferredLabel + ": " + org.viaf;
                }
                Keyword kw = new Keyword(term);
                EntityInfo ei = new EntityInfo("", "", -1, -1, "theme");
                kw.addEntityInfo(ei);
                return kw;
            }
        }
        return null;
    }

    public void findOrganizations(String text, Map<String, Keyword> keywordMap, JSONArray jsArr) {
        for (String key : orgMap.keySet()) {
            if (text.indexOf(key) != -1) {
                Organization org = orgMap.get(key);
                if (org == null) {
                    continue;
                }
                String combined = org.name + ": " + org.viaf;
                if (!keywordMap.containsKey(combined)) {
                    String term = combined;
                    if (org.preferredLabel != null) {
                        term = org.preferredLabel + ": " + org.viaf;
                    }
                    Keyword kw = new Keyword(term);
                    EntityInfo ei = new EntityInfo("", "", -1, -1, "theme");
                    kw.addEntityInfo(ei);
                    keywordMap.put(combined, kw);
                    jsArr.put(kw.toJSON());
                }
            }
        }
    }


    public static class Organization {
        String name;
        String preferredLabel;
        String viaf;
    }

    public static void main(String[] args) throws Exception {
        OrgLookupHandler handler = new OrgLookupHandler();
        Map<String, Keyword> kwMap = new HashMap<String, Keyword>();
        JSONArray jsArr = new JSONArray();
        handler.findOrganizations("USGS some text", kwMap, jsArr);
        System.out.println(kwMap);
    }
}
