package org.neuinfo.foundry.consumers.common;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.input.SAXBuilder;
import org.neuinfo.foundry.common.config.ConsumerConfig;
import org.neuinfo.foundry.common.config.ServerInfo;
import org.neuinfo.foundry.common.util.Assertion;
import org.neuinfo.foundry.common.util.ConfigUtils;
import org.neuinfo.foundry.consumers.river.MongoDBRiverDefinition;
import org.neuinfo.foundry.common.util.Utils;

import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by bozyurt on 5/1/14.
 * dwv 2019-03-26. Environment Variables can be used in configuration
 * ${envVar:defaultValue}
 * not all properties support this, at present. No int values (aka ports).
 * only params in workflows
 * */
public class ConfigLoader {

    public static Configuration loadFromFile(String filename) throws Exception {
        InputStream in = null;
        in =  new FileInputStream(
                filename) ; // "dispatcher-cfg.xml");
        return build(in);
    }

    public static Configuration load(String xmlFile) throws Exception {

        InputStream in = null;
        in = ConfigLoader.class.getClassLoader().getResourceAsStream(xmlFile); // "dispatcher-cfg.xml");
        return build(in);
    }
    public static Configuration build(InputStream in) throws Exception {
        SAXBuilder builder = new SAXBuilder();

        Configuration conf = new Configuration();
        try {

            Document doc = builder.build(in);
            Element docRoot = doc.getRootElement();
            Element mcEl = docRoot.getChild("mongo-config");
            conf.setMongoListenerSettings(extractMongoSettings(mcEl));

            conf.setPluginDir(docRoot.getChildTextTrim("pluginDir"));
            conf.setLibDir(docRoot.getChildTextTrim("libDir"));

            conf.setMongoDBName(mcEl.getAttributeValue("db"));
            conf.setCollectionName(mcEl.getAttributeValue("collection"));
            List<Element> sels = mcEl.getChild("servers").getChildren("server");
            for (Element sel : sels) {
                String host = sel.getAttributeValue("host");
                int port = Utils.getIntValue(sel.getAttributeValue("port"), -1);
                Assertion.assertTrue(port != -1);
                String user = null;
                String pwd = null;
                if (sel.getAttribute("user") != null) {
                    user = sel.getAttributeValue("user");
                }
                if (sel.getAttribute("pwd") != null) {
                    pwd = sel.getAttributeValue("pwd");
                }
                ServerInfo si = new ServerInfo(host, port, user, pwd);
                conf.addServerInfo(si);
            }

            final Element amqEl = docRoot.getChild("activemq-config");
            String brokerURL = amqEl.getChildTextTrim("brokerURL");
            conf.setBrokerURL(brokerURL);
            if (docRoot.getChild("consumers") != null) {
                List<ConsumerConfig> cfList = new ArrayList<ConsumerConfig>();
                List<Element> children = docRoot.getChild("consumers").getChildren("consumer-cfg");
                for (Element e : children) {
                    cfList.add(ConsumerConfig.fromXml(e, conf.getCollectionName()));
                }
                conf.setConsumerConfigs(cfList);
            }
            return conf;
        } finally {
            Utils.close(in);
        }
    }


    static Map<String, Object> extractMongoSettings(Element mcEl) {
        Map<String, Object> settings = new HashMap<String, Object>(7);

        Map<String, Object> typeMap = new HashMap<String, Object>(17);
        settings.put(Constants.TYPE, typeMap);
        String db = ConfigUtils.envVarParser(mcEl.getAttributeValue("db"));
        String collectionName = ConfigUtils.envVarParser(mcEl.getAttributeValue("collection"));


        List<Element> children = mcEl.getChild("servers").getChildren("server");
        for (Element child : children) {
            String hostName = ConfigUtils.envVarParser(child.getAttributeValue("host"));
            int port = Utils.getIntValue(child.getAttributeValue("port"), -1);
            assert port != -1;
            Map<String, Object> serverMap = new HashMap<String, Object>(7);
            List<Map<String, Object>> servers = new ArrayList<Map<String, Object>>(2);
            servers.add(serverMap);
            typeMap.put(MongoDBRiverDefinition.SERVERS_FIELD, servers);
            serverMap.put(MongoDBRiverDefinition.HOST_FIELD, hostName);
            serverMap.put(MongoDBRiverDefinition.PORT_FIELD, port);
        }

        typeMap.put(MongoDBRiverDefinition.DB_FIELD, db);
        typeMap.put(MongoDBRiverDefinition.COLLECTION_FIELD, collectionName);
        return settings;
    }
}
