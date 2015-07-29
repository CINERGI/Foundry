package org.neuinfo.foundry.common.config;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.input.SAXBuilder;
import org.neuinfo.foundry.common.util.Utils;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by bozyurt on 9/30/14.
 */
public class ConsumerConfigReader {

    public static List<ConsumerConfig> loadConfig(String xmlFile) throws Exception {
        SAXBuilder builder = new SAXBuilder();
        List<ConsumerConfig> cfList = new ArrayList<ConsumerConfig>();
        InputStream in = null;
        try {
            in = ConsumerConfigReader.class.getClassLoader().getResourceAsStream(xmlFile);
            Document doc = builder.build(in);
            Element docRoot = doc.getRootElement();
            List<Element> children = docRoot.getChildren("consumer-cfg");
            for (Element e : children) {
                cfList.add(ConsumerConfig.fromXml(e));
            }
        } finally {
            Utils.close(in);
        }

        return cfList;
    }
}
