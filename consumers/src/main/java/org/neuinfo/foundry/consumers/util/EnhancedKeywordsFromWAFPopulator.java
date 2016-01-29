package org.neuinfo.foundry.consumers.util;

import com.mongodb.DBObject;
import org.jdom2.Attribute;
import org.jdom2.Element;
import org.jdom2.Namespace;
import org.jdom2.filter.Filters;
import org.jdom2.xpath.XPathExpression;
import org.jdom2.xpath.XPathFactory;
import org.json.JSONArray;
import org.neuinfo.foundry.common.util.JSONUtils;
import org.neuinfo.foundry.common.util.KeywordInfo;
import org.neuinfo.foundry.common.util.Utils;

import java.io.File;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Created by bozyurt on 1/28/16.
 */
public class EnhancedKeywordsFromWAFPopulator {
    Helper helper;
    String sourceID;
    List<File> wafFiles;
    static Namespace gco = Namespace.getNamespace("gco", "http://www.isotc211.org/2005/gco");
    static Namespace gmx = Namespace.getNamespace("gmx", "http://www.isotc211.org/2005/gmx");
    static Namespace gmd = Namespace.getNamespace("gmd", "http://www.isotc211.org/2005/gmd");
    static Namespace xlink = Namespace.getNamespace("xlink", "http://www.w3.org/1999/xlink");

    public EnhancedKeywordsFromWAFPopulator(String wafDir, String sourceID) {
        this.sourceID = sourceID;
        File[] files = new File(wafDir).listFiles();
        wafFiles = new ArrayList<File>(files.length);
        for (File f : files) {
            if (f.getName().endsWith(".xml")) {
                wafFiles.add(f);
            }
        }
        this.helper = new Helper("");
    }

    public void handle() throws Exception {
        try {
            helper.startup("cinergi-consumers-cfg.xml");
            XPathFactory factory = XPathFactory.instance();
            XPathExpression<Element> expr = factory.compile("//gmd:fileIdentifier", Filters.element(), null, gmd);


            for (File wafFile : wafFiles) {
                Element docEl = Utils.loadXML(wafFile.getAbsolutePath());
                List<Element> enhancedKeywordElements = getEnhancedKeywordElements(docEl, factory);
                if (enhancedKeywordElements.isEmpty()) {
                    continue;
                }
                List<Element> elements = expr.evaluate(docEl);
                if (!elements.isEmpty()) {
                    Element pel = elements.get(0);
                    Element csEl = pel.getChild("CharacterString", gco);
                    if (csEl != null) {
                        String fileIdentifier = csEl.getTextTrim();
                        DBObject docWrapper = helper.findDocWrapper(fileIdentifier);
                        if (docWrapper != null) {
                            List<KeywordInfo> kiList = fromISO2KeywordInfos(enhancedKeywordElements);
                            JSONArray jsArr = new JSONArray();
                            for (KeywordInfo ki : kiList) {
                                jsArr.put(ki.toJSON());
                            }
                            DBObject data = (DBObject) docWrapper.get("Data");
                            data.put("enhancedKeywords", JSONUtils.encode(jsArr));
                            System.out.println(jsArr.toString(2));
                            System.out.println("===========================");
                            // save docWrapper
                            helper.saveDocWrapper(docWrapper);
                        }
                    }
                }
            }


        } finally {
            helper.shutdown();
        }
    }


    List<KeywordInfo> fromISO2KeywordInfos(List<Element> enhancedDescriptiveKeywords) {
        DateFormat df = new SimpleDateFormat("EEE MMM dd HH:mm:ss z yyyy");
        List<KeywordInfo> kiList = new ArrayList<KeywordInfo>();
        for (Element el : enhancedDescriptiveKeywords) {
            Element mdKWEl = el.getChild("MD_Keywords", gmd);
            Element thesaurusEl = mdKWEl.getChild("thesaurusName", gmd);
            Element citationEl = thesaurusEl.getChild("CI_Citation", gmd);
            Element titleEl = citationEl.getChild("title", gmd);
            String category = titleEl.getChildText("CharacterString", gco);
            if (category.indexOf("VIAF") != -1) {
                category = "theme";
            }

            Element otherCitationDetailsEl = citationEl.getChild("otherCitationDetails", gmd);
            String text = otherCitationDetailsEl.getChildTextTrim("CharacterString", gco);
            String timeStampStr = text.substring(text.indexOf("at") + 2).trim();

            //Element outDateEl = citationEl.getChild("date", gmd);
            //Element ciDateEl = outDateEl.getChild("CI_Date", gmd);
            //Element gcoDateEl = ciDateEl.getChild("Date", gco);

            Date date = null;
            try {
                date = df.parse(timeStampStr);
            } catch (ParseException e) {
                e.printStackTrace();
            }
            List<Element> keywordEls = mdKWEl.getChildren("keyword", gmd);
            for (Element kel : keywordEls) {
                Element anchorEl = kel.getChild("Anchor", gmx);
                String term = anchorEl.getTextTrim();
                Attribute href = anchorEl.getAttribute("href", xlink);
                String id = href.getValue();

                KeywordInfo ki = new KeywordInfo(id, term, category, "");
                if (date != null) {
                    ki.setLastChangedDate(date);
                }
                kiList.add(ki);
            }
        }
        return kiList;
    }

    public List<Element> getEnhancedKeywordElements(Element docEl, XPathFactory factory) {
        XPathExpression<Element> expr = factory.compile("//gmd:descriptiveKeywords",
                Filters.element(), null, gmd);
        XPathExpression<Element> citationExpr = factory.compile("//gmd:CI_Citation", Filters.element(), null, gmd);
        List<Element> elements = expr.evaluate(docEl);
        if (elements.isEmpty()) {
            return Collections.emptyList();
        }
        List<Element> enhancedDescriptiveKeywords = new ArrayList<Element>(elements.size());
        for (Element dkEl : elements) {
            List<Element> citationEls = citationExpr.evaluate(dkEl);
            if (citationEls.isEmpty()) {
                continue;
            }
            boolean cinergiEnhanced = false;
            for (Element citationEl : citationEls) {
                Element otherCitationDetails = citationEl.getChild("otherCitationDetails", gmd);
                if (otherCitationDetails != null) {
                    String citationDetailsText = otherCitationDetails.getChildText("CharacterString", gco);
                    if (citationDetailsText != null && citationDetailsText.indexOf("Cinergi") != -1) {
                        cinergiEnhanced = true;
                        break;
                    }
                }
            }
            if (cinergiEnhanced) {
                enhancedDescriptiveKeywords.add(dkEl);
            }
        }
        return enhancedDescriptiveKeywords;
    }


    public static void main(String[] args) throws Exception {
        String homeDir = System.getProperty("user.home");
        String wafDir = homeDir + "/etc/OpenTopography";
        EnhancedKeywordsFromWAFPopulator populator = new EnhancedKeywordsFromWAFPopulator(wafDir, "cinergi-0012");

        populator.handle();


    }

}
