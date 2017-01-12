package org.neuinfo.foundry.common.util;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.Namespace;
import org.jdom2.filter.Filters;
import org.jdom2.input.SAXBuilder;
import org.jdom2.xpath.XPathExpression;
import org.jdom2.xpath.XPathFactory;
import org.neuinfo.foundry.common.config.ServerInfo;
import org.neuinfo.foundry.common.ingestion.Configuration;
import org.neuinfo.foundry.common.ingestion.SourceIngestionService;
import org.neuinfo.foundry.common.model.Source;

import java.io.IOException;
import java.io.StringReader;
import java.net.UnknownHostException;
import java.util.*;

/**
 * Created by bozyurt on 7/28/16.
 */
public class ISOXMLFileHandler {
    private static Namespace gmd = Namespace.getNamespace("gmd", "http://www.isotc211.org/2005/gmd");
    private static Namespace gmi = Namespace.getNamespace("gmi", "http://www.isotc211.org/2005/gmi");
    private static Namespace gco = Namespace.getNamespace("gco", "http://www.isotc211.org/2005/gco");


    public static List<String> getDocUrls(String ingestURL) throws IOException {
        List<String> links = new LinkedList<String>();
        final Iterator<org.jsoup.nodes.Element> it = WAFUtils.extractAnchors(ingestURL);
        while (it.hasNext()) {
            org.jsoup.nodes.Element ae = it.next();
            String href = ae.attr("abs:href");
            if (href != null && href.endsWith(".xml")) {
                links.add(href);
            } else if (href.length() > ingestURL.length()) {
                WAFUtils.collectLinks(href, links);
            }
        }
        return links;
    }

    public static String extractFileIdentifier(String docURL) throws Exception {
        String xmlContent = WAFUtils.getXMLContent(docURL);
        SAXBuilder builder = new SAXBuilder();
        Document doc = builder.build(new StringReader(xmlContent));
        Element rootEl = doc.getRootElement();
        XPathFactory factory = XPathFactory.instance();
        XPathExpression<Element> expr = factory.compile("//gmd:fileIdentifier",
                Filters.element(), null, gmd);
        List<Element> elements = expr.evaluate(rootEl);
        if (!elements.isEmpty()) {
            Element csEl = elements.get(0).getChild("CharacterString", gco);
            if (csEl != null) {
                return csEl.getTextTrim();
            }
        }
        return null;
    }

    public static List<String> getIngestURLs4Sources() throws Exception {
        Configuration conf = new Configuration("discotest");
        ServerInfo si = new ServerInfo("132.249.238.128", 27017, null, null);
        conf.addServer(si);
        SourceIngestionService sis = new SourceIngestionService();
        List<String> ingestURLs = new ArrayList<String>();
        try {
            sis.start(conf);
            List<Source> sources = sis.getAllSources();
            for (Source source : sources) {
                String ingestURL = source.getIngestConfiguration().getString("ingestURL");
                ingestURLs.add(ingestURL);
            }
            return ingestURLs;
        } finally {
            sis.shutdown();
        }
    }

    public static void main(String[] args) throws Exception {
        String[] ingestURLArr = {
              //  "http://hydro10.sdsc.edu/metadata/USGS_Coastal_and_Marine_Geology_Program/",
              //  "http://hydro10.sdsc.edu/metadata/National_Climatic_Data_Center/",
              //  "http://hydro10.sdsc.edu/metadata/U.S._Geoscience_Information_Network/",
            //    "http://hydro10.sdsc.edu/metadata/ecogeo/",
           //     "http://hydro10.sdsc.edu/metadata/SEN/",
           //     "http://hydro10.sdsc.edu/metadata/c4p/",
           //     "http://hydro10.sdsc.edu/metadata/databib/",
           //     "http://hydro10.sdsc.edu/metadata/OpenTopography/",
           //     "http://hydro10.sdsc.edu/metadata/NOAA_NGDC/",
            //    "http:///hydro10.sdsc.edu/metadata/NGDS_Geo-portal/",
            //     "http://hydro10.sdsc.edu/metadata/ci/", not there
            //    "http://hydro10.sdsc.edu/metadata/cuahsi/",
            //    "http://hydro10.sdsc.edu/metadata/Geoscience_Australia/",
            //    "http://hydro10.sdsc.edu/metadata/IEDA/",
                "http://hydro10.sdsc.edu/metadata/CZO_Datasets/",
                "http://hydro10.sdsc.edu/metadata/NODC/",
                "http://maxim.ucsd.edu/waf/sciencebase/",
        };
        List<String> ingestURLs = Arrays.asList(ingestURLArr);
        ChangedRecordRegistry registry = new ChangedRecordRegistry();


        // List<String> ingestURLs = ISOXMLFileHandler.getIngestURLs4Sources();

        for (String ingestURL : ingestURLs) {
            System.out.println(ingestURL);
        }

        System.out.println("========================");
        try {
            for (String ingestURL : ingestURLs) {
                //List<String> docUrls = ISOXMLFileHandler.getDocUrls("http://hydro10.sdsc.edu/metadata/IEDA/");
                List<String> docUrls = ISOXMLFileHandler.getDocUrls(ingestURL);
                int count = 0;
                for (String docURL : docUrls) {
                    count++;
                    try {
                        String fileIdentifier = ISOXMLFileHandler.extractFileIdentifier(docURL);
                        if (fileIdentifier != null) {
                            System.out.println(fileIdentifier + " , " + docURL + " (" + count + " of " + docUrls.size() + ")");
                            registry.addURL(fileIdentifier, docURL);
                        }
                    } catch (Throwable t) {
                        System.err.println(t.getMessage());
                    }
                }
                System.out.println(" done with " + ingestURL + " (" + docUrls.size() + ")");
            }

        } finally {
            registry.shutdown();
        }

    }

}
