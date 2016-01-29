package org.neuinfo.foundry.consumers.common;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.Namespace;
import org.jdom2.filter.Filters;
import org.jdom2.input.SAXBuilder;
import org.jdom2.xpath.XPathExpression;
import org.jdom2.xpath.XPathFactory;
import org.json.JSONArray;
import org.neuinfo.foundry.common.model.EntityInfo;
import org.neuinfo.foundry.common.model.Keyword;
import org.neuinfo.foundry.common.util.Assertion;
import org.neuinfo.foundry.common.util.CinergiXMLUtils;
import org.neuinfo.foundry.common.util.KeywordInfo;
import org.neuinfo.foundry.common.util.Utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.*;

/**
 * Created by bozyurt on 11/17/15.
 */
public class OrganizationChecker {
    private static Namespace gmd = Namespace.getNamespace("gmd", "http://www.isotc211.org/2005/gmd");
    private static Namespace gco = Namespace.getNamespace("gco", "http://www.isotc211.org/2005/gco");

    public void handle(List<File> xmlFiles) throws Exception {
        Set<String> noViafOrgNames = new LinkedHashSet<String>();
        int viafCount = 0;
        int orgCount = 0;
        for (File xmlFile : xmlFiles) {
            List<String> orgNames = extractOrganizations(xmlFile);
            if (!orgNames.isEmpty()) {
                orgCount++;
                KeywordData kd = runOrgEnhancer(xmlFile);
                System.out.println(xmlFile.getName() + " (" + xmlFile.getParentFile().getName() + ")");
                System.out.println("---------------");
                String viafInfo = prepOrgInfo(kd);
                System.out.println(orgNames + viafInfo);
                if (viafInfo.length() == 0) {
                    for (String orgName : orgNames) {
                        noViafOrgNames.add(orgName);
                    }
                } else {
                    //TODO
                    if (orgNames.size() > 1) {
                        for (String orgName : orgNames) {
                            boolean found = false;
                            for (String k : kd.keywordMap.keySet()) {
                                int idx = k.indexOf(':');
                                String key = k.substring(0, idx).trim();
                                if (key.equals(orgName)) {
                                    found = true;
                                    break;
                                }
                            }
                            if (!found) {
                                noViafOrgNames.add(orgName);
                            }
                        }
                    }
                    viafCount++;
                }
                System.out.println("===============");
            }
        }
        System.out.println("# of organizations with VIAF url: " + viafCount + " total # of docs:" + xmlFiles.size() +
                " # docs with organization:" + orgCount);
        System.out.println("++++++++++++++++++++++++++++");
        for (String orgName : noViafOrgNames) {
            System.out.println(orgName);
        }

    }

    String prepOrgInfo(KeywordData kd) {
        if (kd == null) {
            return "";
        }
        return " (" + kd.keywordMap.keySet().iterator().next() + ")";
    }

    KeywordData runOrgEnhancer(File isoXmlFile) throws Exception {
        File workDir = new File("/tmp/cinergi/org");
        if (!workDir.isDirectory()) {
            workDir.mkdirs();
        }
        Assertion.assertTrue(workDir.isDirectory());
        String scriptPath = "/home/bozyurt/dev/python/some_cinergi_enhancers";
        Assertion.assertNotNull(scriptPath);
        Assertion.assertTrue(new File(scriptPath).isDirectory());
        File scriptFile = new File(scriptPath, "org_enhancer.py");
        Assertion.assertNotNull(scriptFile.isFile());
        String prefix = isoXmlFile.getName().replaceFirst("\\.xml", "").replaceAll("\\s+", "_").replaceAll("/+", "_");
        File outFile = new File(workDir, prefix + "_out.xml");
        List<String> cmdList = new ArrayList<String>(10);
        cmdList.add(scriptFile.getAbsolutePath());
        cmdList.add("-i");
        cmdList.add(isoXmlFile.getAbsolutePath());
        cmdList.add("-o");
        cmdList.add(outFile.getAbsolutePath());

        ProcessBuilder pb = new ProcessBuilder(cmdList);

        Map<String, String> env = pb.environment();
        env.put("PATH", env.get("PATH") + ":" + scriptPath);
        //System.out.println("scriptPath:" + scriptPath);
        Process process = pb.start();
        BufferedReader ein = new BufferedReader(new InputStreamReader(process.getErrorStream()));
        String line;

        BufferedReader bin = new BufferedReader(new InputStreamReader(
                process.getInputStream()));
        while ((line = bin.readLine()) != null) {
            //  System.out.println(line);
        }
        while ((line = ein.readLine()) != null) {
            // System.err.println(line);
        }

        int rc = process.waitFor();
        if (rc == 0 && outFile.exists()) {
            KeywordData kd = addOrganizations(outFile);
            if (!kd.keywordMap.isEmpty()) {
                return kd;
            }
        }
        return null;
    }

    public List<String> extractOrganizations(File isoXmlFile) throws Exception {
        SAXBuilder builder = new SAXBuilder();
        BufferedReader in = null;
        Document doc = null;
        try {
            in = org.neuinfo.foundry.common.util.Utils.newUTF8CharSetReader(isoXmlFile.getAbsolutePath());
            doc = builder.build(in);
        } finally {
            org.neuinfo.foundry.common.util.Utils.close(in);
        }
        XPathFactory factory = XPathFactory.instance();
        XPathExpression<Element> expr = factory.compile("//gmd:organisationName",
                Filters.element(), null, gmd);
        List<Element> orgEls = expr.evaluate(doc);
        List<String> orgNames = new ArrayList<String>(1);
        if (!orgEls.isEmpty()) {
            String orgName = orgEls.get(0).getChildTextTrim("CharacterString", gco);
            if (!orgName.equalsIgnoreCase("unknown")) {
                orgNames.add(orgName);
            }
        }
        return orgNames;
    }

    KeywordData addOrganizations(File outFile) throws Exception {
        Element docEl = Utils.loadXML(outFile.getAbsolutePath());
        Map<String, Keyword> keywordMap = new LinkedHashMap<String, Keyword>();
        List<KeywordInfo> organizationKeywords = CinergiXMLUtils.getOrganizationKeywords(docEl);
        JSONArray jsArr = new JSONArray();
        if (!organizationKeywords.isEmpty()) {
            for (KeywordInfo kwi : organizationKeywords) {
                Keyword kw = new Keyword(kwi.getTerm());
                EntityInfo ei = new EntityInfo("", "", -1, -1, kwi.getCategory());
                kw.addEntityInfo(ei);
                keywordMap.put(kw.getTerm(), kw);
                jsArr.put(kw.toJSON());
            }
        }
        KeywordData kd = new KeywordData();
        kd.jsArr = jsArr;
        kd.keywordMap = keywordMap;
        return kd;
    }

    static class KeywordData {
        JSONArray jsArr;
        Map<String, Keyword> keywordMap;
    }

    public static void main(String[] args) throws Exception {
        OrganizationChecker oc = new OrganizationChecker();
        String ROOT = "/var/data/cinergi/waf/hydro10.sdsc.edu/metadata/";
        List<String> paths = Arrays.asList(ROOT + "ScienceBase_WAF_dump",
                ROOT + "SEN", ROOT + "databib", ROOT + "ecogeo",
                ROOT + "Geoscience_Australia", ROOT + "c4p", ROOT + "Data.gov_csw",
                ROOT + "NOAA_data.noaa.gov_catalog", ROOT + "EPA_Environmental_Dataset_Gateway");
        paths = Arrays.asList(ROOT + "Data.gov_csw");
        // paths = Arrays.asList(ROOT + "SEN");
        List<File> xmlFiles = ConsumerUtils.getXMLFiles(paths);
        oc.handle(xmlFiles);
    }
}
