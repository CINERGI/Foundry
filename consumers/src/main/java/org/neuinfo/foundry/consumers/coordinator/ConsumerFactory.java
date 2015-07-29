package org.neuinfo.foundry.consumers.coordinator;

import org.neuinfo.foundry.common.config.ConsumerConfig;
import org.neuinfo.foundry.common.util.Assertion;
import org.neuinfo.foundry.consumers.jms.consumers.*;
import org.neuinfo.foundry.consumers.jms.consumers.ingestors.*;
import org.neuinfo.foundry.consumers.plugin.IPlugin;
import org.neuinfo.foundry.consumers.plugin.Ingestable;
import org.neuinfo.foundry.consumers.plugin.Ingestor;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by bozyurt on 9/30/14.
 */
public class ConsumerFactory {
    private static ConsumerFactory instance;
    Map<String, Class<?>> registeredConsumerMap = new HashMap<String, Class<?>>();
    Map<String, Class<?>> registeredIngestorMap = new HashMap<String, Class<?>>();
    Map<String, Class<?>> registeredIngestorPluginMap = new HashMap<String, Class<?>>();


    public synchronized static ConsumerFactory getInstance() {
        if (instance == null) {
            instance = new ConsumerFactory();
        }
        return instance;
    }

    private ConsumerFactory() {
        registeredConsumerMap.put("uuidGen", DocIdAssignJMSConsumer.class);
        registeredConsumerMap.put("bitbucket", BitBucketConsumer.class);
        registeredConsumerMap.put("entityAnnotator", EntityAnnotationJMSConsumer.class);
        registeredConsumerMap.put("indexCheckpointer", IndexCheckpointConsumer.class);
        registeredConsumerMap.put("elasticSearchIndexer", ElasticSearchIndexerJMSConsumer.class);
        registeredConsumerMap.put("scicrunchIn", ScicrunchResourceIngestorConsumer.class);

        // registeredConsumerMap.putRelatedKeywords("ingestor", SimpleIngestionConsumer.class);

        registeredIngestorMap.put("genIngestor", GenericIngestionConsumer.class);

        registeredIngestorPluginMap.put("xml", NIFXMLIngestor.class);
        registeredIngestorPluginMap.put("rss", RSSIngestor.class);
        registeredIngestorPluginMap.put("csv", NIFCSVIngestor.class);
        registeredIngestorPluginMap.put("oai", OAIIngestor.class);
        registeredIngestorPluginMap.put("waf", WAFIngestor.class);
    }

    public IConsumer createHarvester(String type, String ingestorName, String collectionName, Map<String, String> options) throws Exception {
        Class<?> aClass = registeredIngestorMap.get(ingestorName);
        if (aClass == null) {
            throw new Exception("Not a registered harvester:" + ingestorName);
        }
        IConsumer consumer = (IConsumer) aClass.getConstructor(String.class).newInstance((String) null);

        // ingestors generate different status for each unique workflow
        consumer.setOutStatus(options.get("ingestorOutStatus"));

        consumer.setCollectionName(collectionName); // "nifRecords");

        Ingestable harvester = (Ingestable) consumer;

        Class<?> pluginClazz = registeredIngestorPluginMap.get(type.toLowerCase());
        if (pluginClazz == null) {
            throw new Exception("Harvester type '" + type + "' is not recognized!");
        }
        Ingestor plugin = (Ingestor) pluginClazz.newInstance();

        plugin.initialize(options);

        harvester.setIngestor(plugin);

        return consumer;
    }


    public IConsumer createConsumer(ConsumerConfig config, boolean testMode) throws Exception {
        Assertion.assertNotNull(config);
        if (config.getType().equals("native")) {
            String consumerName = config.getName();
            if (consumerName.indexOf(".") != -1) {
                consumerName = consumerName.substring(0, consumerName.indexOf("."));
            }
            Class<?> aClass = registeredConsumerMap.get(consumerName);

            if (aClass == null) {
                throw new Exception("Not a registered consumer:" + consumerName);
            }
            IConsumer consumer = (IConsumer) aClass.getConstructor(String.class).newInstance(config.getListeningQueueName());

            consumer.setSuccessMessageQueueName(config.getSuccessMessageQueueName());
            consumer.setFailureMessageQueueName(config.getFailureMessageQueueName());
            consumer.setCollectionName(config.getCollectionName());
            consumer.setInStatus(config.getInStatus());
            consumer.setOutStatus(config.getOutStatus());
            if (!config.getParameters().isEmpty()) {
                for (ConsumerConfig.Parameter par : config.getParameters()) {
                    consumer.setParam(par.getName(), par.getValue());
                }
            }
            if (testMode) {
                consumer.setParam("testMode", "true");
            }
            return consumer;
        } else if (config.getType().equals("generic")) {
            String pluginClass = config.getPluginClass();
            Assertion.assertNotNull(pluginClass);
            IPlugin plugin = (IPlugin) JavaPluginCoordinator.getInstance().createInstance(pluginClass);

            Map<String, String> options = new HashMap<String, String>();
            for (ConsumerConfig.Parameter p : config.getParameters()) {
                options.put(p.getName(), p.getValue());
            }
            plugin.initialize(options);

            JavaPluginConsumer consumer = new JavaPluginConsumer(config.getListeningQueueName());

            consumer.setPlugin(plugin);
            consumer.setSuccessMessageQueueName(config.getSuccessMessageQueueName());
            consumer.setFailureMessageQueueName(config.getFailureMessageQueueName());
            consumer.setCollectionName(config.getCollectionName());
            consumer.setInStatus(config.getInStatus());
            consumer.setOutStatus(config.getOutStatus());

            return consumer;
        }

        return null;
    }


}
