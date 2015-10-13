package org.neuinfo.foundry.common.util;

import org.jdom2.Element;
import org.neuinfo.foundry.common.model.Keyword;

import java.io.File;
import java.util.*;

/**
 * Created by bozyurt on 10/13/15.
 */
public class ExistingKeywordsFacetHandler {
    Element docEl;

    public ExistingKeywordsFacetHandler(Element docEl) {
        this.docEl = docEl;
    }

    public ExistingKeywordsFacetHandler(File isoXmlFile) throws Exception {
        docEl = Utils.loadXML(isoXmlFile.getAbsolutePath());
    }


    public void handle() throws Exception {
        Set<String> existingKeywords = CinergiXMLUtils.getExistingKeywords(docEl);
        if (existingKeywords.isEmpty()) {
            return;
        }
        StringBuilder sb = new StringBuilder(existingKeywords.size() * 30);
        for(Iterator<String> it = existingKeywords.iterator(); it.hasNext();) {
            sb.append(it.next());
            if (it.hasNext()) {
                sb.append(" , ");
            }
        }
        Map<String,Keyword> keywordMap = new HashMap<String, Keyword>();

        ScigraphUtils.annotateEntities(null, sb.toString(), keywordMap);
        Map<String, List<CinergiXMLUtils.KeywordInfo>> category2KWIListMap = new HashMap<String, List<CinergiXMLUtils.KeywordInfo>>(7);
        for(Keyword kw : keywordMap.values()) {
            System.out.println(kw);
        }
    }

    public static void main(String[] args) throws Exception {
        String HOME_DIR = System.getProperty("user.home");
        File isoXmlFile = new File(HOME_DIR + "/work/Foundry/029E458C-96A7-4D17-BC46-DF84CA30DFA8.xml");
        ExistingKeywordsFacetHandler handler = new ExistingKeywordsFacetHandler(isoXmlFile);

        handler.handle();
    }
}
