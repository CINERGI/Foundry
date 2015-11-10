package org.neuinfo.foundry.common.util;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.Namespace;
import org.jdom2.filter.Filters;
import org.jdom2.input.SAXBuilder;
import org.jdom2.xpath.XPathExpression;
import org.jdom2.xpath.XPathFactory;
import org.neuinfo.foundry.common.model.Keyword;

import java.io.BufferedReader;
import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by bozyurt on 11/9/15.
 */
public class PhraseKWDetector {
    private static Namespace gmd = Namespace.getNamespace("gmd", "http://www.isotc211.org/2005/gmd");
    private static Namespace gco = Namespace.getNamespace("gco", "http://www.isotc211.org/2005/gco");

    public void handle(File rootDir) throws Exception {
        File[] files = rootDir.listFiles();
        for (File f : files) {
            if (f.isFile() && f.getName().endsWith(".xml")) {
                ISOText isoText = extractText(f);
                if (isoText.abstractText != null || isoText.title != null) {
                    if (isoText.abstractText != null && isoText.abstractText.indexOf("heat flux") != -1) {
                        System.out.println( Utils.formatText(isoText.abstractText, 100) );
                        System.out.println("======================");
                        String abs = isoText.abstractText;
                        Map<String, Keyword> keywordMap = new HashMap<String, Keyword>();
                        ScigraphUtils.annotateEntities(null, abs, keywordMap);
                        for(Keyword kw : keywordMap.values()) {
                            System.out.println(kw);
                        }
                    }
                }
            }
        }
    }


    public ISOText extractText(File isoXmlFile) throws Exception {
        SAXBuilder builder = new SAXBuilder();
        BufferedReader in = null;
        Document doc = null;
        try {
            in = Utils.newUTF8CharSetReader(isoXmlFile.getAbsolutePath());
            doc = builder.build(in);
        } finally {
            Utils.close(in);
        }
        XPathFactory factory = XPathFactory.instance();

        String abstractText = null;
        String title = null;
        XPathExpression<Element> expr = factory.compile("//gmd:abstract",
                Filters.element(), null, gmd);
        List<Element> absEls = expr.evaluate(doc);
        if (!absEls.isEmpty()) {
            abstractText = absEls.get(0).getChildTextTrim("CharacterString", gco);
            if (abstractText.equalsIgnoreCase("required field")) {
                abstractText = null;
            }
        }

        expr = factory.compile("//gmd:title", Filters.element(), null, gmd);
        List<Element> titleEls = expr.evaluate(doc);
        if (!titleEls.isEmpty()) {
            title = titleEls.get(0).getChildTextTrim("CharacterString", gco);
            if (title.equalsIgnoreCase("required field")) {
                title = null;
            }
        }
        return new ISOText(abstractText, title);
    }

    static class ISOText {
        String abstractText;
        String title;

        public ISOText(String abstractText, String title) {
            this.abstractText = abstractText;
            this.title = title;
        }

        @Override
        public String toString() {
            final StringBuilder sb = new StringBuilder("ISOText{");
            sb.append("abstractText='").append(abstractText).append('\'');
            sb.append(", title='").append(title).append('\'');
            sb.append('}');
            return sb.toString();
        }
    }

    public static void main(String[] args) throws Exception {
        PhraseKWDetector detector = new PhraseKWDetector();
        detector.handle(new File("/var/data/cinergi/waf/hydro10.sdsc.edu/metadata/ScienceBase_WAF_dump"));

    }
}
