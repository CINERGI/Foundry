package org.neuinfo.foundry.common.util;

import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import org.jdom2.Element;
import org.jdom2.Namespace;
import org.json.JSONArray;
import org.json.JSONObject;
import org.neuinfo.foundry.common.Constants;
import org.neuinfo.foundry.common.model.Keyword;
import org.neuinfo.foundry.common.util.ScigraphMappingsHandler.FacetNode;

import java.util.*;

/**
 * given a mongodb document generates an enhanced version of the original ISO XML document.
 * <p/>
 * Created by bozyurt on 2/11/15.
 */
public class ISOXMLGenerator {
    private Namespace gmd = Namespace.getNamespace("gmd", "http://www.isotc211.org/2005/gmd");
    private Namespace gmi = Namespace.getNamespace("gmi", "http://www.isotc211.org/2005/gmi");
    private Namespace xlink = Namespace.getNamespace("xlink", "http://www.w3.org/1999/xlink");
    private Namespace gmx = Namespace.getNamespace("gmx", "http://www.isotc211.org/2005/gmx");

    public Element generate(DBObject docWrapper) throws Exception {
        DBObject originalDoc = (DBObject) docWrapper.get("OriginalDoc");

        DBObject data = (DBObject) docWrapper.get("Data");
        DBObject spatial = (DBObject) data.get("spatial");
        JSONObject originalDocJson = JSONUtils.toJSON((BasicDBObject) originalDoc, false);
        XML2JSONConverter converter = new XML2JSONConverter();
        Element docEl = converter.toXML(originalDocJson);
        // MI_Metadata
        if (!docEl.getName().equals("MI_Metadata")) {
            docEl.setName("MI_Metadata");
            docEl.setNamespace(gmi);
            if (docEl.getNamespace("gmd") == null) {
                docEl.addNamespaceDeclaration(gmd);
            }
        }
        if (docEl.getNamespace("xlink") == null) {
            docEl.addNamespaceDeclaration(xlink);
        }
        if (docEl.getNamespace("gmx") == null) {
            docEl.addNamespaceDeclaration(gmx);
        }
        if (spatial != null) {
            JSONObject spatialJson = JSONUtils.toJSON((BasicDBObject) spatial, false);
            Object bbObj = spatialJson.get("bounding_boxes");
            if ((bbObj instanceof JSONArray)) {
                docEl = ISOXMLGeneratorSupport.addSpatialExtent(docEl, spatialJson);
            }
        }


        Map<String, List<KeywordInfo>> category2KWIListMap = new HashMap<String, List<KeywordInfo>>(7);
        if (data.containsField("orgKeywords")) {
            DBObject kwDBO = (DBObject) data.get("orgKeywords");
            JSONArray jsArr = JSONUtils.toJSONArray((BasicDBList) kwDBO);
            for (int i = 0; i < jsArr.length(); i++) {
                JSONObject kwJson = jsArr.getJSONObject(i);
                Keyword kw = Keyword.fromJSON(kwJson);
                Set<String> categories = kw.getCategories();
                if (categories.size() == 1) {
                    String category = categories.iterator().next();
                    Set<String> ids = kw.getIds();
                    if (!ids.isEmpty()) {
                        KeywordInfo kwi = new KeywordInfo(ids.iterator().next(),
                                kw.getTerm(), category, null,
                                CinergiXMLUtils.KeywordType.Organization);
                        List<KeywordInfo> kwiList = category2KWIListMap.get(category);
                        if (kwiList == null) {
                            kwiList = new ArrayList<KeywordInfo>(10);
                            category2KWIListMap.put(category, kwiList);
                        }
                        kwiList.add(kwi);
                    }
                }
            }
        }
        FacetHierarchyHandler fhh = FacetHierarchyHandler.getInstance(Constants.SCIGRAPH_URL);
        if (data.containsField("keywords")) {
            //CategoryHierarchyHandler chh = CategoryHierarchyHandler.getInstance();

            DBObject kwDBO = (DBObject) data.get("keywords");
            JSONArray jsArr = JSONUtils.toJSONArray((BasicDBList) kwDBO);
            List<String> unmatchedList = new ArrayList<String>(jsArr.length());
            for (int i = 0; i < jsArr.length(); i++) {
                JSONObject kwJson = jsArr.getJSONObject(i);
                Keyword kw = Keyword.fromJSON(kwJson);
                boolean matched = false;
                for (String id : kw.getIds()) {
                    List<List<FacetNode>> fnListList = ScigraphUtils.getKeywordFacetHierarchy(id, kw.getTerm());
                    for (List<FacetNode> fnList : fnListList) {
                        String category = ScigraphUtils.toCinergiCategory(fnList);
                        KeywordInfo kwi = new KeywordInfo(id, kw.getTerm(), category, null);
                        List<KeywordInfo> kwiList = category2KWIListMap.get(category);
                        if (kwiList == null) {
                            kwiList = new ArrayList<KeywordInfo>(10);
                            category2KWIListMap.put(category, kwiList);
                        }
                        if (!kwiList.contains(kwi)) {
                            kwiList.add(kwi);
                            matched = true;
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

        }
        if (!category2KWIListMap.isEmpty()) {
            for (List<KeywordInfo> kwiList : category2KWIListMap.values()) {
                CinergiXMLUtils.filterPlurals(kwiList);
            }

            docEl = CinergiXMLUtils.addKeywords(docEl, category2KWIListMap, fhh, docWrapper);
        }
        // fix anchor problem if exists
        docEl = ISOXMLFixer.fixAnchorProblem(docEl);
        return docEl;
    }




}
