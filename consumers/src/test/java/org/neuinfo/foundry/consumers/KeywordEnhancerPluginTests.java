package org.neuinfo.foundry.consumers;

import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.jdom2.Element;
import org.json.JSONArray;
import org.json.JSONObject;
import org.neuinfo.foundry.common.Constants;
import org.neuinfo.foundry.common.model.Keyword;
import org.neuinfo.foundry.common.util.*;
import org.neuinfo.foundry.common.util.KeywordInfo;
import org.neuinfo.foundry.consumers.jms.consumers.plugins.KeywordEnhancer;
import org.neuinfo.foundry.consumers.jms.consumers.plugins.KeywordEnhancer2;
import org.neuinfo.foundry.consumers.plugin.IPlugin;
import org.neuinfo.foundry.consumers.plugin.Result;
import org.neuinfo.foundry.consumers.util.Helper;

import java.io.File;
import java.util.*;

/**
 * Created by bozyurt on 12/10/14.
 */
public class KeywordEnhancerPluginTests extends TestCase {
    public KeywordEnhancerPluginTests(String name) {
        super(name);
    }


    public void testCategoryHierarchy() throws Exception {
        CategoryHierarchyHandler chh = CategoryHierarchyHandler.getInstance();
        assertEquals("Process > Atmospheric Process", chh.getCinergiCategory("atmospheric process"));
        assertEquals("Realm > Other", chh.getCinergiCategory("Location"));
        assertEquals("Feature > Hydrologic Feature", chh.getCinergiCategory("Water body"));
    }

    public void testRelatedKeywords() throws Exception {
        Map<String, Keyword> keywordMap = new HashMap<String, Keyword>();
        KeywordEnhancer enhancer = new KeywordEnhancer();
        // enhancer.getRelatedKeywords("ENVO_00002006", keywordMap);
        //  enhancer.getRelatedKeywords("bedload", keywordMap);
        enhancer.getRelatedKeywords("2f0c733d-f669-4ecb-b955-1ff004318a62", keywordMap);
        assertFalse(keywordMap.isEmpty());
        for (Keyword k : keywordMap.values()) {
            System.out.println(k);
        }
    }

    public void testAddKeyword() throws Exception {
        Element docEl = Utils.loadXML("/tmp/00C9D45F-F6DF-4B80-B24B-B10883A282CB.xml");
        List<KeywordInfo> kwiList = new ArrayList<KeywordInfo>(10);
        KeywordInfo kwi = new KeywordInfo("", "geyser",
                "theme", null);
        kwiList.add(kwi);
        kwi = new KeywordInfo("", "Laser", "instrument", null);
        kwiList.add(kwi);
        Map<String, List<KeywordInfo>> category2KWIListMap = new HashMap<String, List<KeywordInfo>>(7);
        category2KWIListMap.put("instrument", kwiList);
        FacetHierarchyHandler fhh = FacetHierarchyHandler.getInstance(Constants.SCIGRAPH_URL);
        docEl = CinergiXMLUtils.addKeywords(docEl, category2KWIListMap, fhh, null);
        File enhancedXmlFile = new File("/tmp/kwd_test.xml");
        Utils.saveXML(docEl, enhancedXmlFile.getAbsolutePath());
        System.out.println("saved enhancedXmlFile to " + enhancedXmlFile);
    }


    public void testStopwordHandler() throws Exception {
        String text = "The Ismay?Desert Creek interval and Cane Creek cycle of the Alkali Gulch interval of the Middle Pennsylvanian Paradox Formation in the Paradox Basin of Utah and Colorado contain excellent organic-rich source rocks having total organic carbon contents ranging from 0.5 to 11.0 percent. The source rocks in both intervals contain types I, II, and III organic matter and are potential source rocks for both oil and gas. Organic matter in the Ismay?Desert Creek interval and Cane Creek cycle of the Alkali Gulch interval (hereinafter referred to in this report as the ?Cane Creek cycle?) probably is more terrestrial in origin in the eastern part of the basin and is interpreted to have contributed to some of the gas produced there. Thermal maturity increases from southwest to northeast for both the Ismay?Desert Creek interval and Cane Creek cycle, following structural and burial trends throughout the basin. In the northernmost part of the basin, the combination of a relatively thick Tertiary sedimentary sequence and high basinal heat flow has produced very high thermal maturities. Although general thermal maturity trends are similar for both the Ismay?Desert Creek interval and Cane Creek cycle, actual maturity levels are higher for the Cane Creek due to the additional thickness (as much as several thousand feet) of Middle Pennsylvanian section. Throughout most of the basin, the Ismay?Desert Creek interval is mature and in the petroleum-generation window (0.10 to 0.50 production index (PI)), and both oil and gas are produced; in the south-central to southwestern part of the basin, however, the interval is marginally mature (&amp;lt;0.10 PI) for petroleum generation, and mainly oil is produced. In contrast, the more mature Cane Creek cycle contains no marginally immature areas?it is mature (&amp;gt;0.10 PI) in the central part of the basin and is overmature (past the petroleum-generation window (&amp;gt;0.50 PI)) throughout most of the eastern part of the basin. The Cane Creek cycle generally produces oil and associated gas throughout the western and central parts of the basin and thermogenic gas in the eastern part of the basin. Burial and thermal-history models were constructed for six different areas of the Paradox Basin. In the Monument upwarp area, the least mature part of the basin, the Ismay?Desert Creek interval and Cane Creek cycle have thermal maturities of 0.10 and 0.20 PI and were buried to 13,400 ft and 14,300 ft, respectively. A constant heat flow through time of 40 mWm?2 (milliwatts per square meter) is postulated for this area. Significant petroleum generation began at 45 Ma for the Ismay?Desert Creek interval and at 69 Ma for the Cane Creek cycle. In the area around the confluence of the Green and Colorado Rivers, the Ismay?Desert Creek interval and Cane Creek cycle have thermal maturities of 0.20 and 0.25 PI and were buried to 13,000 ft and 14,200 ft, respectively. A constant heat flow through time of 42 mWm?2 is postulated for this area. Significant petroleum generation began at 60 Ma for the Ismay?Desert Creek interval and at 75 Ma for the Cane Creek cycle. In the area around the town of Green River, Utah, the Ismay?Desert Creek interval and Cane Creek cycle have thermal maturities of 0.60 and greater and were buried to 14,000 ft and 15,400 ft, respectively. A constant heat flow through time of 53 mWm?2 is proposed for this area. Significant petroleum generation began at 82 Ma for the Ismay?Desert Creek interval and at 85 Ma for the Cane Creek cycle. Around Moab, Utah, in the deeper, eastern part of the basin, the Ismay?Desert Creek interval and Cane Creek cycle have thermal maturities of 0.30 and around 0.35 PI and were buried to 18,250 ft and 22,000 ft, respectively. A constant heat flow through time of 40 mWm?2 is postulated for this area. Significant petroleum generation began at 79 Ma for the Ismay?Desert Creek interval and at 90 Ma for the Cane Creek cycle. At Lisbon Valley, also in the structurally deeper part of the basin, the Ismay?";
        Helper helper = new Helper("");
        try {
            helper.startup("cinergi-consumers-cfg.xml");

            List<BasicDBObject> docWrappers = helper.getDocWrappers("cinergi-0001");

            StopWordsHandler handler = StopWordsHandler.getInstance();
            for (BasicDBObject docWrapper : docWrappers) {
                String primaryKey = docWrapper.get("primaryKey").toString();
                if (primaryKey.equals("4f4e48b4e4b07f02db532964")) {
                    DBObject data = (DBObject) docWrapper.get("Data");
                    if (data.containsField("keywords")) {
                        DBObject kwDBO = (DBObject) data.get("keywords");
                        JSONArray jsArr = JSONUtils.toJSONArray((BasicDBList) kwDBO);
                        Map<String, Keyword> keywordMap = new HashMap<String, Keyword>();
                        for (int i = 0; i < jsArr.length(); i++) {
                            JSONObject kwJson = jsArr.getJSONObject(i);
                            Keyword kw = Keyword.fromJSON(kwJson);
                            keywordMap.put(kw.getTerm(), kw);
                        }

                        handler.postFilter(text, keywordMap);
                    }
                    break;
                }
            }
        } finally {
            helper.shutdown();
        }
    }

    public void testKeywordEnhancer1() throws Exception {
        String thePrimaryKey = "505b9142e4b08c986b3197e9";
        thePrimaryKey = "4f4e48b4e4b07f02db532964";
        thePrimaryKey = "4f4e4a51e4b07f02db62a174";
        thePrimaryKey = "org.marine-geo:metadata:10000";
        boolean filter = true;
        // runKeywordEnhancer("cinergi-0001", thePrimaryKey, filter, new File("/tmp/kw2"));
        //runKeywordEnhancer("cinergi-0011", thePrimaryKey, filter, new File("/tmp/NOAA_NGDC_Sonar_Water_Column"));
        //runKeywordEnhancer("cinergi-0012", thePrimaryKey, filter, new File("/tmp/NOAA_NGDC_Collection"));
        // runKeywordEnhancer("cinergi-0007", thePrimaryKey, filter, new File("/tmp/NOAA_NGDC"));
        runKeywordEnhancer("cinergi-0023", thePrimaryKey, filter, new File("/tmp/IEDA"));
    }

    public void testBadKeywords() throws Exception {
        String thePrimaryKey = "SEN:0112";
        boolean filter = true;
        runKeywordEnhancer("cinergi-0015", thePrimaryKey, filter, new File("/tmp/SEN"));
    }

    public void testKeywordEnhancer2() throws Exception {
        String thePrimaryKey = "org.marine-geo:metadata:10000";
        boolean filter = true;
        runKeywordEnhancer2("cinergi-0023", thePrimaryKey, filter, new File("/tmp/IEDA"));
    }

    void runKeywordEnhancer2(String sourceID, String thePrimaryKey, boolean filter, File outDir) throws Exception {
        Helper helper = new Helper("");
        try {
            ScigraphMappingsHandler smHandler = ScigraphMappingsHandler.getInstance();
            ScigraphUtils.setHandler(smHandler);

            helper.startup("cinergi-consumers-cfg.xml");

            List<BasicDBObject> docWrappers = helper.getDocWrappers(sourceID);
            IPlugin plugin = new KeywordEnhancer2();
            Map<String, String> optionMap = new HashMap<String, String>();
            optionMap.put("serviceURL", "http://tikki.neuinfo.org:9000");
            plugin.initialize(optionMap);
            for (BasicDBObject docWrapper : docWrappers) {
                String primaryKey = docWrapper.get("primaryKey").toString();
                if (!filter || primaryKey.equalsIgnoreCase(thePrimaryKey)) {
                    Result result = plugin.handle(docWrapper);
                    if (result.getStatus() == Result.Status.OK_WITH_CHANGE) {
                        DBObject dw = result.getDocWrapper();
                        DBObject data = (DBObject) dw.get("Data");
                        if (data.containsField("keywords")) {
                            DBObject kwDBO = (DBObject) data.get("keywords");
                            JSONArray jsArr = JSONUtils.toJSONArray((BasicDBList) kwDBO);
                            System.out.println(jsArr.toString(2));
                        }
                    }
                    if (filter) {
                        break;
                    }
                }
            }

        } finally {
            helper.shutdown();
        }
    }

    void runKeywordEnhancer(String sourceID, String thePrimaryKey, boolean filter, File outDir) throws Exception {
        Helper helper = new Helper("");
        try {
            ScigraphMappingsHandler smHandler = ScigraphMappingsHandler.getInstance();
            ScigraphUtils.setHandler(smHandler);

            helper.startup("cinergi-consumers-cfg.xml");

            List<BasicDBObject> docWrappers = helper.getDocWrappers(sourceID);
            IPlugin plugin = new KeywordEnhancer();

            Map<String, String> optionMap = new HashMap<String, String>();
            optionMap.put("stopwordsUrl", "file:///var/data/cinergi/stopwords.txt");
            optionMap.put("serviceURL",
                    "http://ec-scigraph.sdsc.edu:9000/scigraph/annotations/entities");
            plugin.initialize(optionMap);

            ((KeywordEnhancer) plugin).setUseNER(false);
            int count = 0;
            for (BasicDBObject docWrapper : docWrappers) {
                String primaryKey = docWrapper.get("primaryKey").toString();
                if (!filter || primaryKey.equalsIgnoreCase(thePrimaryKey)) {
                    Result result = plugin.handle(docWrapper);
                    if (result.getStatus() == Result.Status.OK_WITH_CHANGE) {
                        DBObject dw = result.getDocWrapper();
                        DBObject data = (DBObject) dw.get("Data");
                        if (data.containsField("keywords")) {
                            DBObject kwDBO = (DBObject) data.get("keywords");
                            JSONArray jsArr = JSONUtils.toJSONArray((BasicDBList) kwDBO);
                            System.out.println(jsArr.toString(2));
                            ISOXMLGenerator generator = new ISOXMLGenerator();
                            Element docEl = generator.generate(docWrapper);
                            File enhancedXmlFile;
                            if (filter) {
                                enhancedXmlFile = new File("/tmp/kwd_test.xml");
                            } else {
                                if (!outDir.isDirectory()) {
                                    outDir.mkdir();
                                }
                                String filename = primaryKey.replaceAll("/", "_");
                                enhancedXmlFile = new File(outDir, filename + "_test.xml");
                            }
                            Utils.saveXML(docEl, enhancedXmlFile.getAbsolutePath());
                            System.out.println("saved enhancedXmlFile to " + enhancedXmlFile);
                        }
                    }
                    if (filter) {
                        break;
                    }
                }
                count++;

            }

        } finally {
            helper.shutdown();
        }
    }

    public void testKeywordEnhancer() throws Exception {
        Helper helper = new Helper("");
        try {
            helper.startup("cinergi-consumers-cfg.xml");

            List<BasicDBObject> docWrappers = helper.getDocWrappers("cinergi-0001");
            IPlugin plugin = new KeywordEnhancer();

            FacetHierarchyHandler fhh = FacetHierarchyHandler.getInstance(Constants.SCIGRAPH_URL);
            plugin.initialize(new HashMap<String, String>(1));
            int count = 0;
            for (BasicDBObject docWrapper : docWrappers) {
                String primaryKey = docWrapper.get("primaryKey").toString();
                Result result = plugin.handle(docWrapper);
                if (result.getStatus() == Result.Status.OK_WITH_CHANGE) {
                    DBObject dw = result.getDocWrapper();
                    DBObject data = (DBObject) dw.get("Data");
                    if (data.containsField("keywords")) {
                        DBObject kwDBO = (DBObject) data.get("keywords");
                        JSONArray jsArr = JSONUtils.toJSONArray((BasicDBList) kwDBO);
                        System.out.println(jsArr.toString(2));
                        Map<String, List<KeywordInfo>> category2KWIListMap = new HashMap<String, List<KeywordInfo>>(7);
                        for (int i = 0; i < jsArr.length(); i++) {
                            JSONObject kwJson = jsArr.getJSONObject(i);
                            Keyword kw = Keyword.fromJSON(kwJson);
                            Set<String> categories = kw.getCategories();
                            if (categories.size() == 1) { // && categories.iterator().next().equalsIgnoreCase("instrument")) {
                                String category = categories.iterator().next();
                                KeywordInfo kwi = new KeywordInfo("", kw.getTerm(),
                                        category, null);
                                List<KeywordInfo> kwiList = category2KWIListMap.get(category);
                                if (kwiList == null) {
                                    kwiList = new ArrayList<KeywordInfo>(10);
                                    category2KWIListMap.put(category, kwiList);
                                }
                                kwiList.add(kwi);
                            }

                        }
                        if (!category2KWIListMap.isEmpty()) {
                            DBObject originalDoc = (DBObject) dw.get("OriginalDoc");
                            JSONObject originalDocJson = JSONUtils.toJSON((BasicDBObject) originalDoc, false);
                            XML2JSONConverter converter = new XML2JSONConverter();
                            Element docEl = converter.toXML(originalDocJson);
                            docEl = CinergiXMLUtils.addKeywords(docEl, category2KWIListMap, fhh, docWrapper);
                            File enhancedXmlFile = new File("/tmp/kwd_" + primaryKey + ".xml");
                            Utils.saveXML(docEl, enhancedXmlFile.getAbsolutePath());
                            System.out.println("saved enhancedXmlFile to " + enhancedXmlFile);
                        }
                    }
                }
                count++;
                if (count > 10) {
                    break;
                }
                System.out.println(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>");
            }

        } finally {
            helper.shutdown();
        }
    }

    public static TestSuite suite() {
        TestSuite suite = new TestSuite();
        suite.addTest(new KeywordEnhancerPluginTests("testKeywordEnhancer"));
        return suite;
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }
}
