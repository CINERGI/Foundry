package org.neuinfo.foundry.jms.common;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.input.SAXBuilder;
import org.neuinfo.foundry.common.config.ServerInfo;
import org.neuinfo.foundry.river.MongoDBRiverDefinition;
import org.neuinfo.foundry.common.util.Utils;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by bozyurt on 5/1/14.
 */
public class ConfigLoader {

    public static Configuration load(String xmlFile) throws Exception {
        SAXBuilder builder = new SAXBuilder();

        Configuration conf = new Configuration();
        InputStream in = null;
        try {
            in = ConfigLoader.class.getClassLoader().getResourceAsStream(xmlFile); // "dispatcher-cfg.xml");

            Document doc = builder.build(in);
            Element docRoot = doc.getRootElement();
            Element mcEl = docRoot.getChild("mongo-config");
            conf.setMongoListenerSettings(extractMongoSettings(mcEl));
            conf.setMongoDBName( mcEl.getAttributeValue("db"));
            conf.setCollectionName( mcEl.getAttributeValue("collection"));
            List<ServerInfo> siList = new ArrayList<ServerInfo>(3);
            List<Element> children = mcEl.getChild("servers").getChildren("server");
            for(Element c : children) {
                String host = c.getAttributeValue("host");
                int port = Utils.getIntValue( c.getAttributeValue("port"), -1);
                ServerInfo si = new ServerInfo(host, port);
                siList.add(si);
            }
            conf.setMongoServers(siList);

            Map<String, QueueInfo> qiMap = new HashMap<String, QueueInfo>(7);
            if (docRoot.getChild("queues") != null) {
                final List<Element> qEls = docRoot.getChild("queues").getChildren("queue");
                for (Element qEl : qEls) {
                    final QueueInfo qi = QueueInfo.fromXml(qEl);
                    qiMap.put(qi.getName(), qi);
                }
            }
            if (docRoot.getChild("workflows") != null) {
                List<Element> wfEls = docRoot.getChild("workflows").getChildren("workflow");
                for (Element wfEl : wfEls) {
                    Workflow wf = Workflow.fromXml(wfEl, qiMap);
                    conf.getWorkflows().add(wf);
                }
            }

            if (docRoot.getChild("wf-mappings") != null) {
                List<Element> wmEls = docRoot.getChild("wf-mappings").getChildren("wf-mapping");
                for(Element wmEl : wmEls) {
                    WorkflowMapping wm = WorkflowMapping.fromXml(wmEl);
                    conf.getWorkflowMappings().add(wm);
                }
            }
            /*
            if (docRoot.getChild("routes") != null) {
                List<Element> routeEls = docRoot.getChild("routes").getChildren("route");
                for (Element routeEl : routeEls) {
                    Route route = Route.fromXml(routeEl, qiMap);
                    conf.getRoutes().add(route);
                }
            }
            */
            final Element cpEl = docRoot.getChild("checkpoint-file");
            conf.setCheckpointXmlFile(new File(cpEl.getTextTrim()));
            final Element amqEl = docRoot.getChild("activemq-config");
            String brokerURL = amqEl.getChildTextTrim("brokerURL");
            conf.setBrokerURL(brokerURL);
            return conf;
        } finally {
            Utils.close(in);
        }
    }


    static Map<String, Object> extractMongoSettings(Element mcEl) {
        Map<String, Object> settings = new HashMap<String, Object>(7);

        Map<String, Object> typeMap = new HashMap<String, Object>(17);
        settings.put(Constants.TYPE, typeMap);
        String db = mcEl.getAttributeValue("db");
        String collectionName = mcEl.getAttributeValue("collection");


        List<Element> children = mcEl.getChild("servers").getChildren("server");
        for (Element child : children) {
            String hostName = child.getAttributeValue("host");
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
