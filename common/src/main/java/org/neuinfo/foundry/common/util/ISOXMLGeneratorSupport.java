package org.neuinfo.foundry.common.util;

import com.mongodb.DBObject;
import org.jdom2.Comment;
import org.jdom2.Content;
import org.jdom2.Element;
import org.jdom2.Namespace;
import org.json.JSONArray;
import org.json.JSONObject;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Created by bozyurt on 6/10/16.
 */
public class ISOXMLGeneratorSupport {
    private static Namespace gmd = Namespace.getNamespace("gmd", "http://www.isotc211.org/2005/gmd");
    static Namespace gco = Namespace.getNamespace("gco", "http://www.isotc211.org/2005/gco");
    static Namespace gmx = Namespace.getNamespace("gmx", "http://www.isotc211.org/2005/gmx");
    static Map<String, String> thesaurusMap = new HashMap<String, String>(7);

    static {
        thesaurusMap.put("instrument", "Instrument from NASA/Global Change Master Directory (GCMD) Earth Science Keywords.");
        thesaurusMap.put("theme", "Science keywords from NASA/Global Change Master Directory (GCMD) Earth Science Keywords.");
        thesaurusMap.put("dataCenter", "Datacenter from NASA/Global Change Master Directory (GCMD) Earth Science Keywords.");
        thesaurusMap.put("platform", "Platforms from NASA/Global Change Master Directory (GCMD) Earth Science Keywords.");
        thesaurusMap.put("organization", "Virtual International Authority File (VIAF) Corporate Names");
    }

    public static Element addSpatialExtent(Element docEl, JSONObject spatial) throws Exception {

        JSONArray boundingBoxes = spatial.getJSONArray("bounding_boxes");
        boolean hasBB = boundingBoxes.length() > 0;
        boolean hasBBFromPlaces = false;

        Element identificationInfo = docEl.getChild("identificationInfo", gmd);
        Element dataIdentification = identificationInfo.getChild("MD_DataIdentification", gmd);
        if (dataIdentification == null) {
            dataIdentification = new Element("MD_DataIdentification", gmd);
            identificationInfo.addContent(dataIdentification);
        }

        if (!hasBB) {
            JSONObject derivedBoundingBoxes = spatial.getJSONObject("derived_bounding_boxes_from_places");
            if (derivedBoundingBoxes.length() > 0) {
                for (String place : derivedBoundingBoxes.keySet()) {
                    JSONObject placeJson = derivedBoundingBoxes.getJSONObject(place);
                    JSONObject swJson = placeJson.getJSONObject("southwest");
                    JSONObject neJson = placeJson.getJSONObject("northeast");
                    String wbLongVal = String.valueOf(swJson.getDouble("lng"));
                    String sblatVal = String.valueOf(swJson.getDouble("lat"));
                    String ebLongVal = String.valueOf(neJson.getDouble("lng"));
                    String nbLatVal = String.valueOf(neJson.getDouble("lat"));
                    Element bbEl = CinergiXMLUtils.createBoundaryBox(wbLongVal, ebLongVal, sblatVal, nbLatVal, place);
                    dataIdentification.addContent(bbEl);
                }
                hasBBFromPlaces = true;
            }
        }
        if (!hasBB && !hasBBFromPlaces) {
            JSONObject derivedBoundingBoxes = spatial.getJSONObject("derived_bounding_boxes_from_derived_place");
            if (derivedBoundingBoxes.length() > 0) {
                for (String place : derivedBoundingBoxes.keySet()) {
                    JSONObject placeJson = derivedBoundingBoxes.getJSONObject(place);
                    JSONObject swJson = placeJson.getJSONObject("southwest");
                    JSONObject neJson = placeJson.getJSONObject("northeast");
                    String wbLongVal = String.valueOf(swJson.getDouble("lng"));
                    String sblatVal = String.valueOf(swJson.getDouble("lat"));
                    String ebLongVal = String.valueOf(neJson.getDouble("lng"));
                    String nbLatVal = String.valueOf(neJson.getDouble("lat"));
                    Element bbEl = CinergiXMLUtils.createBoundaryBox(wbLongVal, ebLongVal, sblatVal, nbLatVal, place);
                    dataIdentification.addContent(bbEl);
                }
            }
        }
        return docEl;
    }


    public static Element addKeywords(Element docEl, Map<String, List<KeywordInfo>> category2KwiListMap,
                                       DBObject docWrapper) {
        Element identificationInfo = docEl.getChild("identificationInfo", gmd);
        Element dataIdentification = identificationInfo.getChild("MD_DataIdentification", gmd);
        if (dataIdentification == null) {
            dataIdentification = new Element("MD_DataIdentification", gmd);
            identificationInfo.addContent(dataIdentification);
        }
        List<Element> descriptiveKeywords = dataIdentification.getChildren("descriptiveKeywords", gmd);
        List<Content> contents = dataIdentification.getContent();
        if (descriptiveKeywords != null && !descriptiveKeywords.isEmpty()) {
            int pivot = CinergiXMLUtils.getPivot(contents);

            Assertion.assertTrue(pivot != -1);
            Set<String> existingKeywords = CinergiXMLUtils.getExistingKeywords(docEl);
            if (!existingKeywords.isEmpty()) {
                /*
                // remove duplicates
                for (List<KeywordInfo> kwiList : category2KwiListMap.values()) {
                    for (Iterator<KeywordInfo> it = kwiList.iterator(); it.hasNext(); ) {
                        KeywordInfo kwi = it.next();
                        if (existingKeywords.contains(kwi.getTerm())) {
                            it.remove();
                        }
                    }
                }
                */
                //remove any now empty categories
                Set<String> badCategorySet = new HashSet<String>(7);
                for (String category : category2KwiListMap.keySet()) {
                    List<KeywordInfo> kwiList = category2KwiListMap.get(category);
                    if (kwiList.isEmpty()) {
                        badCategorySet.add(category);
                    }
                }
                if (!badCategorySet.isEmpty()) {
                    for (String category : badCategorySet) {
                        category2KwiListMap.remove(category);
                    }
                }
            }
            addKeywords(category2KwiListMap, contents, pivot, docWrapper);
        } else {
            int pivot = -1;
            for (int i = 0; i < contents.size(); i++) {
                Content content = contents.get(i);
                if (content instanceof Element) {
                    Element e = (Element) content;
                    String name = e.getName();
                    if (name.equals("resourceFormat")) {
                        pivot = i;
                    } else if (name.equals("graphicOverview")) {
                        pivot = i;
                    } else if (name.equals("resourceMaintenance")) {
                        pivot = i;
                    } else if (name.equals("pointOfContact")) {
                        pivot = i;
                    } else if (name.equals("status")) {
                        pivot = i;
                    } else if (name.equals("credit")) {
                        pivot = i;
                    } else if (name.equals("purpose")) {
                        pivot = i;
                    } else if (name.equals("abstract")) {
                        pivot = i;
                    }
                }
            }
            Assertion.assertTrue(pivot != -1);
            addKeywords(category2KwiListMap, contents, pivot, docWrapper);
        }
        return docEl;
    }

    private static void addKeywords(Map<String, List<KeywordInfo>> category2KwiListMap,
                                    List<Content> contents, int pivot,  DBObject docWrapper) {
        DBObject data = (DBObject) docWrapper.get("Data");
        JSONArray enhancedKeywords = new JSONArray();

        for (String category : category2KwiListMap.keySet()) {
            List<KeywordInfo> kwiList = category2KwiListMap.get(category);
            Element dkEl = new Element("descriptiveKeywords", gmd);
            Date now = new Date();
            Comment comment = new Comment("Cinergi keyword enhanced at " + now);
            dkEl.addContent(comment);
            List<Element> keywords = new ArrayList<Element>(kwiList.size());
            String ontId = null;
            for (KeywordInfo kwi : kwiList) {
                kwi.setLastChangedDate(now);
                keywords.add( CinergiXMLUtils.createKeywordTag(kwi.getTerm(), kwi.getCategory(), kwi));
                // descriptiveKeywords.add(dkEl);
                enhancedKeywords.put(kwi.toJSON());
                ontId = kwi.getId();
            }

            CinergiXMLUtils.KeywordType type = kwiList.get(0).getType();
            Element keywordEl = createKeywords(keywords, category, type, now, ontId);
            dkEl.addContent(keywordEl);
            contents.add(pivot + 1, dkEl);
        }
        // store the enhanced keywords to the document wrapper data subdocument
        data.put("enhancedKeywords", JSONUtils.encode(enhancedKeywords));
    }


    public static Element createKeywords(List<Element> keywords, String category, CinergiXMLUtils.KeywordType keywordType,
                                         Date date, String ontId) {
        Element mdKWEl = new Element("MD_Keywords", gmd);
        Element typeCodeEl = new Element("MD_KeywordTypeCode", gmd);
        typeCodeEl.setAttribute("codeList", "http://www.isotc211.org/2005/resources/Codelist/gmxCodelists.xml#MD_KeywordTypeCode");
        typeCodeEl.setAttribute("codeListValue", "theme");
        typeCodeEl.setText("theme");
        Element typeEl = new Element("type", gmd);
        typeEl.addContent(typeCodeEl);
        // thesaurus
        Element thesaurusEl = new Element("thesaurusName", gmd);
        Element citationEl = new Element("CI_Citation", gmd);
        thesaurusEl.addContent(citationEl);
        Element titleEl = new Element("title", gmd);
        if (keywordType == CinergiXMLUtils.KeywordType.Keyword) {
            titleEl.addContent( CinergiXMLUtils.createCharString(category));
            //titleEl.addContent(createCharString(thesaurusMap.get(category)));
        } else if (keywordType == CinergiXMLUtils.KeywordType.Organization) {
            titleEl.addContent(CinergiXMLUtils.createCharString(thesaurusMap.get("organization")));
        }

        DateFormat df = new SimpleDateFormat("yyyy-MM-dd");



        citationEl.addContent(titleEl);

        Element ciDateEl = new Element("CI_Date", gmd);

        Element dateEl = new Element("date", gmd);
        // dateEl.setAttribute("nilReason", "unknown", gco);
        Element gcoDateEl = new Element("Date", gco);
        gcoDateEl.setText(df.format(date));
        dateEl.addContent(gcoDateEl);
        ciDateEl.addContent(dateEl);
        Element dateTypeEl = new Element("dateType", gmd);
        Element dateTypeCodeEl = new Element("CI_DateTypeCode", gmd);
        dateTypeCodeEl.setAttribute("codeList", "http://www.isotc211.org/2005/resources/Codelist/gmxCodelists.xml#CI_DateTypeCode");
        dateTypeCodeEl.setAttribute("codeListValue", "publication");
        dateTypeCodeEl.setText("publication");
        dateTypeEl.addContent(dateTypeCodeEl);
        ciDateEl.addContent(dateTypeEl);
        Element outDateEl = new Element("date", gmd);
        outDateEl.addContent(ciDateEl);
        Element identifierEl = new Element("identifier", gmd);
        Element mdIdentifierEl = new Element("MD_Identifier", gmd);
        identifierEl.addContent(mdIdentifierEl);
        Element codeEl = new Element("code", gmd);
        mdIdentifierEl.addContent(codeEl);
        Element anchorEl = new Element("Anchor", gmx);
        String url = ontId; //  fhh.getOntologyId(category);
        anchorEl.setText(url);
        codeEl.addContent(anchorEl);


        Element otherCitationDetailsEl = new Element("otherCitationDetails", gmd);
        otherCitationDetailsEl.addContent(CinergiXMLUtils.createCharString("Cinergi keyword enhanced at " + date));

        citationEl.addContent(outDateEl);

        citationEl.addContent(otherCitationDetailsEl);

        for (Element keyword : keywords) {
            mdKWEl.addContent(keyword);
        }
        mdKWEl.addContent(typeEl);
        mdKWEl.addContent(thesaurusEl);
        return mdKWEl;
    }
}
