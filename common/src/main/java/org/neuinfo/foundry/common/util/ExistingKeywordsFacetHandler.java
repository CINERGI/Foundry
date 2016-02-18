package org.neuinfo.foundry.common.util;

import com.mongodb.DBObject;
import org.apache.log4j.Logger;
import org.jdom2.Element;
import org.neuinfo.foundry.common.model.EntityInfo;
import org.neuinfo.foundry.common.model.Keyword;
import org.neuinfo.foundry.common.util.ScigraphMappingsHandler.FacetNode;

import java.io.File;
import java.util.*;

/**
 * Created by bozyurt on 10/13/15.
 */
public class ExistingKeywordsFacetHandler {
    File isoXmlFile;
    Element docEl;
    private final static Logger logger = Logger.getLogger("ExistingKeywordsFacetHandler");

    static {
        try {
            ScigraphMappingsHandler smHandler = ScigraphMappingsHandler.getInstance();
            ScigraphUtils.setHandler(smHandler);
        } catch (Exception x) {
            x.printStackTrace();
        }
    }

    public ExistingKeywordsFacetHandler(Element docEl) {
        this.docEl = docEl;
    }

    public ExistingKeywordsFacetHandler(File isoXmlFile) throws Exception {
        this.isoXmlFile = isoXmlFile;
        docEl = Utils.loadXML(isoXmlFile.getAbsolutePath());
    }

    public void handleAndSave() throws Exception {
        handle(null);
        String path = isoXmlFile.getAbsolutePath().replaceFirst("\\.xml", "_existing.xml");
        File enhancedXmlFile = new File(path);
        System.out.println(enhancedXmlFile);
        Utils.saveXML(docEl, enhancedXmlFile.getAbsolutePath());
    }

    public Element handle(DBObject docWrapper) throws Exception {
        Set<String> existingKeywords = CinergiXMLUtils.getExistingKeywords(docEl);
        if (existingKeywords.isEmpty()) {
            return docEl;
        }
        StringBuilder sb = new StringBuilder(existingKeywords.size() * 30);
        for (Iterator<String> it = existingKeywords.iterator(); it.hasNext(); ) {
            sb.append(it.next());
            if (it.hasNext()) {
                sb.append(" , ");
            }
        }
        Set<String> existingKeywordsLC = new HashSet<String>();
        for (String ekw : existingKeywords) {
            existingKeywordsLC.add(ekw.toLowerCase());
        }
        Map<String, Keyword> keywordMap = new HashMap<String, Keyword>();

        ScigraphUtils.annotateEntities(null, sb.toString(), keywordMap, false);
        Map<String, List<KeywordInfo>> category2KWIListMap = null;
        List<Keyword> filteredKeywordList = new ArrayList<Keyword>(keywordMap.size());
        for (Keyword kw : keywordMap.values()) {
            if (existingKeywordsLC.contains(kw.getTerm().toLowerCase())) {
                filteredKeywordList.add(kw);
                logger.info(kw);
            }
        }

        if (!filteredKeywordList.isEmpty()) {
            category2KWIListMap = prepKeywordFacets(filteredKeywordList);
            FacetHierarchyHandler fhh = FacetHierarchyHandler.getInstance();

            docEl = CinergiXMLUtils.addFacets2ExistingKeywords(docEl, category2KWIListMap, fhh, docWrapper);
        }
        return docEl;
    }

    static Map<String, List<KeywordInfo>> prepKeywordFacets(List<Keyword> kwList) throws Exception {
        Inflector inflector = new Inflector();
        Map<String, List<KeywordInfo>> category2KWIListMap = new HashMap<String, List<KeywordInfo>>(7);
        List<String> unmatchedList = new ArrayList<String>(kwList.size());
        for (Keyword kw : kwList) {
            boolean matched = false;
            String singularCCTerm = Inflector.toCamelCase(inflector.toSingular(kw.getTerm()));
            if (!singularCCTerm.equals(kw.getTerm())) {
                Keyword kwNew = new Keyword(singularCCTerm);
                for (EntityInfo ei : kw.getEntityInfos()) {
                    kwNew.addEntityInfo(ei);
                }
                kw = kwNew;
            }
            for (String id : kw.getIds()) {
                List<List<ScigraphMappingsHandler.FacetNode>> fnListList = ScigraphUtils.getKeywordFacetHierarchy(id);
                for (List<FacetNode> fnList : fnListList) {
                    String category = ScigraphUtils.toCinergiCategory(fnList);
                    KeywordInfo kwi = new KeywordInfo(id, kw.getTerm(), category, null);
                    List<KeywordInfo> kwiList = category2KWIListMap.get(category);
                    if (kwiList == null) {
                        kwiList = new ArrayList<KeywordInfo>(10);
                        category2KWIListMap.put(category, kwiList);
                    }
                    if (!kwiList.contains(kwi)) {
                        matched = true;
                        kwiList.add(kwi);
                    }
                }
            }
            if (!matched) {
                unmatchedList.add(kw.getTerm());
            }
        }
        if (!unmatchedList.isEmpty()) {
            Utils.appendToFile("/tmp/no_facet_keywords.txt", unmatchedList);
        }
        return category2KWIListMap;
    }

    public static void main(String[] args) throws Exception {
        String HOME_DIR = System.getProperty("user.home");
        File isoXmlFile = new File(HOME_DIR + "/work/Foundry/029E458C-96A7-4D17-BC46-DF84CA30DFA8.xml");
        File rootDir = new File("/tmp/waf/Data.gov");
        isoXmlFile = new File("/tmp/waf/Data.gov/9066b916-9dc0-46bc-8a73-b52eb10bdd0e.xml");
        isoXmlFile = new File("/tmp/waf/Data.gov/f6e16a7a-d433-46c6-a2fd-a70ea80f26db.xml");

        //ExistingKeywordsFacetHandler handler = new ExistingKeywordsFacetHandler(isoXmlFile);

        // handler.handleAndSave();

        File[] files = rootDir.listFiles();
        for (File f : files) {
            if (f.getName().endsWith(".xml") && !f.getName().endsWith("_existing.xml")) {
                ExistingKeywordsFacetHandler handler = new ExistingKeywordsFacetHandler(f);
                handler.handleAndSave();

            }
        }

    }
}
