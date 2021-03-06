package org.neuinfo.foundry.common.util;

import com.mongodb.DBObject;
import org.jdom2.Comment;
import org.jdom2.Content;
import org.jdom2.Element;
import org.jdom2.Namespace;
import org.json.JSONArray;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Created by bozyurt on 12/10/14.
 */
public class CinergiXMLUtils {
    static Namespace gmd = Namespace.getNamespace("gmd", "http://www.isotc211.org/2005/gmd");
    static Namespace gco = Namespace.getNamespace("gco", "http://www.isotc211.org/2005/gco");
    static Namespace gmx = Namespace.getNamespace("gmx", "http://www.isotc211.org/2005/gmx");
    static Namespace xlink = Namespace.getNamespace("xlink", "http://www.w3.org/1999/xlink");


    static Map<String, String> thesaurusMap = new HashMap<String, String>(7);

    static {
        thesaurusMap.put("instrument", "Instrument from NASA/Global Change Master Directory (GCMD) Earth Science Keywords.");
        thesaurusMap.put("theme", "Science keywords from NASA/Global Change Master Directory (GCMD) Earth Science Keywords.");
        thesaurusMap.put("dataCenter", "Datacenter from NASA/Global Change Master Directory (GCMD) Earth Science Keywords.");
        thesaurusMap.put("platform", "Platforms from NASA/Global Change Master Directory (GCMD) Earth Science Keywords.");
        thesaurusMap.put("organization", "Virtual International Authority File (VIAF) Corporate Names");
    }

    private CinergiXMLUtils() {
    }

    public static Element createBoundaryBox(String wbLongVal, String ebLongVal,
                                            String sbLatVal, String nbLatVal, String description) {

        Element extEl = new Element("extent", gmd);
        Comment comment = new Comment("CINERGI spatial enhancer generated extent at " + new Date());
        extEl.addContent(comment);
        Element eXEl = new Element("EX_Extent", gmd);
        extEl.addContent(eXEl);
        Element descriptionEl = new Element("description", gmd);
        eXEl.addContent(descriptionEl);
        descriptionEl.addContent(new Element("CharacterString", gco).setText(description));
        Element geoElem = new Element("geographicElement", gmd);
        eXEl.addContent(geoElem);
        Element gbx = new Element("EX_GeographicBoundingBox", gmd);
        geoElem.addContent(gbx);
        Element wblong = new Element("westBoundLongitude", gmd);
        gbx.addContent(wblong);
        wblong.addContent(new Element("Decimal", gco).setText(wbLongVal));
        Element eblong = new Element("eastBoundLongitude", gmd);
        gbx.addContent(eblong);
        eblong.addContent(new Element("Decimal", gco).setText(ebLongVal));
        Element sblat = new Element("southBoundLatitude", gmd);
        gbx.addContent(sblat);
        sblat.addContent(new Element("Decimal", gco).setText(sbLatVal));
        Element nblat = new Element("northBoundLatitude", gmd);
        gbx.addContent(nblat);
        nblat.addContent(new Element("Decimal", gco).setText(nbLatVal));
        return extEl;
    }

    public static Element createKeywordTag(String keyword, String category, KeywordInfo kwi) {
      /*  <gmx:Anchor xlink:href="http://example.com/cinergi/vocabulary/term"
                 xlink:actuate="onRequest">Geothermal</gmx:Anchor>
        */
        Element kwdEl = new Element("keyword", gmd);
        //kwdEl.addContent(createCharString(keyword));
        Element anchorEl = new Element("Anchor", gmx);
        String anchorURL = kwi.getId();

        // errors of undefined URI's in 'enchanced' KW
        if (anchorURL != null && !anchorURL.isEmpty()) {
            anchorEl.setAttribute("href", anchorURL, xlink);
//        } else {
//          anchorEl.setAttribute("href", "http://ec-scigraph.sdsc.edu:9000/scigraph/vocabulary/term/thing", xlink);
        }
        // anchorEl.setAttribute("actuate", "onRequest", xlink);
        anchorEl.setText(keyword);
        kwdEl.addContent(anchorEl);
        return kwdEl;
    }

    public static Element createKeywords(List<Element> keywords, String category, KeywordType keywordType,
                                         Date date, FacetHierarchyHandler fhh) {
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
        if (keywordType == KeywordType.Keyword) {
            titleEl.addContent(createCharString(category));
            //titleEl.addContent(createCharString(thesaurusMap.get(category)));
        } else if (keywordType == KeywordType.Organization) {
            titleEl.addContent(createCharString(thesaurusMap.get("organization")));
        }
        /*
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ") {
            public Date parse(String source, ParsePosition pos) {
                return super.parse(source.replaceFirst(":(?=[0-9]{2}$)", ""), pos);
            }
        };
        */
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd");

        /*
         <gmd:CI_Date>
                  <gmd:date>
                    <gco:DateTime>2015-07-30T16:04:37</gco:DateTime>
                  </gmd:date>
                  <gmd:dateType>
                    <gmd:CI_DateTypeCodecodeList="http://www.isotc211.org/2005/resources/Codelist/gmxCodelists.xml#CI_DateTypeCode"codeListValue="publication">publication</gmd:CI_DateTypeCode>
                  </gmd:dateType>
                             
               </gmd:CI_Date>
         */

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

        /*
        <gmd:identifier>
            <gmd:MD_Identifier>
              <gmd:code>
                <gmx:Anchor>http://example.com/cinergi/vocabulary/</gmx:Anchor>
               </gmd:code>
             </gmd:MD_Identifier>
        </gmd:identifier>
         */
        Element identifierEl = new Element("identifier", gmd);
        Element mdIdentifierEl = new Element("MD_Identifier", gmd);
        identifierEl.addContent(mdIdentifierEl);
        Element codeEl = new Element("code", gmd);
        mdIdentifierEl.addContent(codeEl);
        Element anchorEl = new Element("Anchor", gmx);
        String url = fhh.getOntologyId(category);
        anchorEl.setText(url);
        codeEl.addContent(anchorEl);


        Element otherCitationDetailsEl = new Element("otherCitationDetails", gmd);
        otherCitationDetailsEl.addContent(createCharString("Cinergi keyword enhanced at " + date));

        citationEl.addContent(outDateEl);
        // not used 9/5/2015
        //  citationEl.addContent(identifierEl);
        citationEl.addContent(otherCitationDetailsEl);

        for (Element keyword : keywords) {
            mdKWEl.addContent(keyword);
        }
        mdKWEl.addContent(typeEl);
        mdKWEl.addContent(thesaurusEl);
        return mdKWEl;
    }


    public static Element createCharString(String value) {
        Element csEl = new Element("CharacterString", gco);
        csEl.setText(value);
        return csEl;
    }


    public static Set<String> getExistingKeywords(Element docEl) {
        Element identificationInfo = docEl.getChild("identificationInfo", gmd);
        Element dataIdentification = identificationInfo.getChild("MD_DataIdentification", gmd);
        Set<String> existingKeywordSet = new HashSet<String>();
        if (dataIdentification != null) {
            List<Element> descriptiveKeywords = dataIdentification.getChildren("descriptiveKeywords", gmd);
            if (descriptiveKeywords != null && !descriptiveKeywords.isEmpty()) {
                for (Element dkEl : descriptiveKeywords) {

                    Element mdkEl = dkEl.getChild("MD_Keywords", gmd);
                    if (mdkEl != null) {
                        // skip any Cinergi keywords
                        Element thesaurusNameEl = mdkEl.getChild("thesaurusName", gmd);
                        if (thesaurusNameEl != null) {
                            Element citationEl = thesaurusNameEl.getChild("CI_Citation", gmd);
                            if (citationEl != null) {
                                Element otherCDEl = citationEl.getChild("otherCitationDetails", gmd);
                                if (otherCDEl != null) {

                                    Element csEl = otherCDEl.getChild("CharacterString", gco);
                                    if (csEl.getTextTrim().toLowerCase().indexOf("cinergi") != -1) {
                                        continue;
                                    }
                                }
                            }
                        }
                    }
                    if (mdkEl != null) {
                        List<Element> kwEls = mdkEl.getChildren("keyword", gmd);
                        for (Element kwEl : kwEls) {

                            Element anchorEl = kwEl.getChild("Anchor", gmx);
                            if (anchorEl != null) {
                                existingKeywordSet.add(anchorEl.getTextTrim());
                            } else {
                                Element csEl = kwEl.getChild("CharacterString", gco);
                                if (csEl != null) {
                                    existingKeywordSet.add(csEl.getTextTrim());
                                }
                            }
                        }
                    }
                }
            }
        }
        return existingKeywordSet;
    }

    public static Map<String, List<KeywordInfo>> getNewKeywords(Element docEl, Map<String, List<KeywordInfo>> category2KwiListMap) {
        Set<String> existingKeywords = getExistingKeywords(docEl);
        if (!existingKeywords.isEmpty()) {
            // remove duplicates
            for (List<KeywordInfo> kwiList : category2KwiListMap.values()) {
                for (Iterator<KeywordInfo> it = kwiList.iterator(); it.hasNext(); ) {
                    KeywordInfo kwi = it.next();
                    if (existingKeywords.contains(kwi.getTerm())) {
                        it.remove();
                    }
                }
            }
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

        return category2KwiListMap;
    }


    static String getThesaurusNameCharString(Element thesaurusEl) {
        Element citationEl = thesaurusEl.getChild("CI_Citation", gmd);
        if (citationEl != null) {
            Element titleEl = citationEl.getChild("title", gmd);
            if (titleEl != null) {
                return titleEl.getChildTextTrim("CharacterString", gco);
            }
        }
        return null;
    }

    public static List<KeywordInfo> getOrganizationKeywords(Element docEl) {
        List<KeywordInfo> kwiList = new ArrayList<KeywordInfo>(5);
        Element identificationInfo = docEl.getChild("identificationInfo", gmd);
        Element dataIdentification = identificationInfo.getChild("MD_DataIdentification", gmd);
        if (dataIdentification != null) {
            List<Element> descriptiveKeywords = dataIdentification.getChildren("descriptiveKeywords", gmd);
            if (descriptiveKeywords != null && !descriptiveKeywords.isEmpty()) {
                for (Element dkwEl : descriptiveKeywords) {
                    List<Element> mdKeywordsList = dkwEl.getChildren("MD_Keywords", gmd);
                    if (mdKeywordsList != null && !mdKeywordsList.isEmpty()) {
                        for (Element mdKeywordsEl : mdKeywordsList) {
                            Element thesaurusNameEl = mdKeywordsEl.getChild("thesaurusName", gmd);
                            if (thesaurusNameEl != null) {
                                String thesaurusNameStr = getThesaurusNameCharString(thesaurusNameEl);
                                if (thesaurusNameStr != null && thesaurusNameStr.indexOf("(VIAF)") != -1) {
                                    List<Element> keywordElList = mdKeywordsEl.getChildren("keyword", gmd);
                                    if (keywordElList != null) {
                                        for (Element keywordEl : keywordElList) {
                                            String keyword = keywordEl.getChildTextTrim("CharacterString", gco);
                                            KeywordInfo kwi = new KeywordInfo("", keyword, "theme", "");
                                            kwiList.add(kwi);
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        return kwiList;
    }

    public static Element addFacets2ExistingKeywords(Element docEl, Map<String, List<KeywordInfo>> category2KwiListMap,
                                                     FacetHierarchyHandler fhh, DBObject docWrapper) {
        Element identificationInfo = docEl.getChild("identificationInfo", gmd);
        Element dataIdentification = identificationInfo.getChild("MD_DataIdentification", gmd);
        Assertion.assertNotNull(dataIdentification);
        List<Element> descriptiveKeywords = dataIdentification.getChildren("descriptiveKeywords", gmd);
        Assertion.assertTrue(descriptiveKeywords != null && !descriptiveKeywords.isEmpty());
        List<Content> contents = dataIdentification.getContent();
        int pivot = getPivot(contents);
        Assertion.assertTrue(pivot != -1);
        addKeywords(category2KwiListMap, contents, pivot, fhh, docWrapper);
        return docEl;
    }

    public static int getPivot(List<Content> contents) {
        int pivot = -1;
        for (int i = 0; i < contents.size(); i++) {
            Content content = contents.get(i);
            if (content instanceof Element) {
                Element e = (Element) content;
                if (e.getName().equals("descriptiveKeywords")) {
                    pivot = i;
                }
            }
        }
        return pivot;
    }

    public static Element addKeywords(Element docEl, Map<String, List<KeywordInfo>> category2KwiListMap,
                                      FacetHierarchyHandler fhh, DBObject docWrapper) {
        Element identificationInfo = docEl.getChild("identificationInfo", gmd);
        Element dataIdentification = identificationInfo.getChild("MD_DataIdentification", gmd);
        if (dataIdentification == null) {
            dataIdentification = new Element("MD_DataIdentification", gmd);
            identificationInfo.addContent(dataIdentification);
        }
        List<Element> descriptiveKeywords = dataIdentification.getChildren("descriptiveKeywords", gmd);
        List<Content> contents = dataIdentification.getContent();
        if (descriptiveKeywords != null && !descriptiveKeywords.isEmpty()) {
            int pivot = getPivot(contents);

            Assertion.assertTrue(pivot != -1);
            Set<String> existingKeywords = getExistingKeywords(docEl);
            if (!existingKeywords.isEmpty()) {
                // remove duplicates
                for (List<KeywordInfo> kwiList : category2KwiListMap.values()) {
                    for (Iterator<KeywordInfo> it = kwiList.iterator(); it.hasNext(); ) {
                        KeywordInfo kwi = it.next();
                        if (existingKeywords.contains(kwi.getTerm())) {
                            it.remove();
                        }
                    }
                }
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
            addKeywords(category2KwiListMap, contents, pivot, fhh, docWrapper);
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
            addKeywords(category2KwiListMap, contents, pivot, fhh, docWrapper);
        }
        return docEl;
    }

    private static void addKeywords(Map<String, List<KeywordInfo>> category2KwiListMap,
                                    List<Content> contents, int pivot, FacetHierarchyHandler fhh, DBObject docWrapper) {
        DBObject data = (DBObject) docWrapper.get("Data");
        JSONArray enhancedKeywords = new JSONArray();

        for (String category : category2KwiListMap.keySet()) {
            List<KeywordInfo> kwiList = category2KwiListMap.get(category);
            Element dkEl = new Element("descriptiveKeywords", gmd);
            Date now = new Date();
            Comment comment = new Comment("Cinergi keyword enhanced at " + now);
            dkEl.addContent(comment);
            List<Element> keywords = new ArrayList<Element>(kwiList.size());
            for (KeywordInfo kwi : kwiList) {
                kwi.setLastChangedDate(now);
                keywords.add(createKeywordTag(kwi.getTerm(), kwi.getCategory(), kwi));
                // descriptiveKeywords.add(dkEl);
                enhancedKeywords.put(kwi.toJSON());
            }

            KeywordType type = kwiList.get(0).getType();
            Element keywordEl = createKeywords(keywords, category, type, now, fhh);
            dkEl.addContent(keywordEl);
            contents.add(pivot + 1, dkEl);
        }
        // store the enhanced keywords to the document wrapper data subdocument
        data.put("enhancedKeywords", JSONUtils.encode(enhancedKeywords));
    }

    public static void filterPlurals(List<KeywordInfo> kwiList) {
        Set<KeywordInfo> plurals = new HashSet<KeywordInfo>();
        for (int i = 0; i < kwiList.size(); i++) {
            KeywordInfo kwiRef = kwiList.get(i);
            if (plurals.contains(kwiRef)) {
                continue;
            }
            if (kwiRef.getTerm().length() == 1 || kwiRef.getTerm().endsWith(" .")) {
                plurals.add(kwiRef);
                continue;
            }
            for (int j = i + 1; j < kwiList.size(); j++) {
                KeywordInfo kwi2 = kwiList.get(j);
                if (kwiRef.getTerm().equalsIgnoreCase(kwi2.getTerm())) {
                    plurals.add(kwi2);
                } else if (kwiRef.getTerm().toUpperCase().equals(kwi2.getTerm().toUpperCase() + "S")) {
                    plurals.add(kwi2);
                } else if (kwi2.getTerm().toUpperCase().equals(kwiRef.getTerm().toUpperCase() + "S")) {
                    plurals.add(kwiRef);
                }
            }
        }

        if (!plurals.isEmpty()) {
            System.out.println(plurals);
            for (Iterator<KeywordInfo> it = kwiList.iterator(); it.hasNext(); ) {
                KeywordInfo kwi = it.next();
                if (plurals.contains(kwi)) {
                    it.remove();
                }
            }
        }
    }

    public static enum KeywordType {
        Keyword, Organization
    }

}
