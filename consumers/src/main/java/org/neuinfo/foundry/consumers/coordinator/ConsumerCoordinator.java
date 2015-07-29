package org.neuinfo.foundry.consumers.coordinator;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.commons.cli.*;
import org.apache.log4j.Logger;
import org.json.JSONObject;
import org.neuinfo.foundry.common.config.ConsumerConfig;
import org.neuinfo.foundry.common.util.Assertion;
import org.neuinfo.foundry.consumers.common.ConfigLoader;
import org.neuinfo.foundry.consumers.common.Configuration;
import org.neuinfo.foundry.consumers.jms.consumers.plugins.ProvenanceHelper;

import javax.jms.*;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Created by bozyurt on 9/30/14.
 */
public class ConsumerCoordinator implements MessageListener {
    private transient Connection con;
    private transient Session session;
    private Configuration config;
    private String configFile;
    private int maxDocs = -1;
    private boolean runInTestMode = false;
    private final ExecutorService executorService = Executors
            .newFixedThreadPool(10);
    List<ConsumerWorker> consumerWorkers = new ArrayList<ConsumerWorker>();
    private final static Logger logger = Logger.getLogger("ConsumerCoordinator");

    public ConsumerCoordinator(Configuration config, String configFile) {
        this.config = config;
        this.configFile = configFile;
    }

    public boolean isRunInTestMode() {
        return runInTestMode;
    }

    public void setRunInTestMode(boolean runInTestMode) {
        this.runInTestMode = runInTestMode;
    }

    public void startup() throws Exception {
        ActiveMQConnectionFactory factory = new ActiveMQConnectionFactory(config.getBrokerURL());
        this.con = factory.createConnection();
        con.start();
        session = con.createSession(false, Session.AUTO_ACKNOWLEDGE);

        String libDir = config.getLibDir();
        String pluginDir = config.getPluginDir();
        JavaPluginCoordinator.getInstance(pluginDir, libDir);
    }

    public void shutdown() {
        System.out.println("shutting down the ConsumerCoordinator...");
        try {
            if (this.con != null) {
                con.close();
            }
        } catch (JMSException x) {
            x.printStackTrace();
        }
    }

    public void handle() throws Exception {
        ConsumerFactory factory = ConsumerFactory.getInstance();
        for (ConsumerConfig cf : config.getConsumerConfigs()) {
            IConsumer consumer = factory.createConsumer(cf, this.runInTestMode);
            if (consumer != null) {
                ConsumerWorker worker = new ConsumerWorker(consumer,
                        (MessageListener) consumer, this.configFile);
                consumerWorkers.add(worker);
            } else {
                logger.warn("Cannot create consumer " + cf.getName());
            }
        }
        for (ConsumerWorker worker : consumerWorkers) {
            Assertion.assertNotNull(worker.consumer);
            logger.info("starting consumer " + worker.consumer.getClass().getName());
            executorService.submit(worker);
        }

        Runtime.getRuntime().addShutdownHook(new Thread() {
            public void run() {
                shutdownAndAwaitTermination();
            }
        });

        handleMessages(this);
    }

    public void handleMessages(MessageListener listener) throws JMSException {
        Destination destination = this.session.createQueue("foundry.consumer.head");
        MessageConsumer messageConsumer = this.session.createConsumer(destination);
        messageConsumer.setMessageListener(listener);
    }


    void shutdownAndAwaitTermination() {
        for (ConsumerWorker cw : this.consumerWorkers) {
            cw.setFinished(true);
        }
        executorService.shutdown();
        try {
            if (executorService.awaitTermination(1, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
                if (!executorService.awaitTermination(30, TimeUnit.SECONDS)) {
                    logger.warn("Consumer pool did not terminate");
                    System.err.println("Consumer pool did not terminate");
                }
            }
        } catch (InterruptedException x) {
            executorService.shutdownNow();
            // preserve interrupt status
            Thread.currentThread().interrupt();
        }
        shutdown();
    }

    public int getMaxDocs() {
        return maxDocs;
    }

    public void setMaxDocs(int maxDocs) {
        this.maxDocs = maxDocs;
    }

    @Override
    public void onMessage(Message message) {
        try {
            ObjectMessage om = (ObjectMessage) message;
            String payload = (String) om.getObject();
            logger.info("payload:" + payload);
            JSONObject json = new JSONObject(payload);
            String command = json.getString("cmd");
            String srcNifId = json.getString("srcNifId");
            String batchId = json.getString("batchId");
            String dataSource = json.getString("dataSource");
            String ingestorOutStatus = json.getString("ingestorOutStatus");
            String updateOutStatus = json.getString("updateOutStatus");

            logger.info("received command '" + command + "' [srcNifId:"
                    + srcNifId + ", batchId:" + batchId + "]");
            JSONObject ingestConfigJS = json.getJSONObject("ingestConfiguration");

            JSONObject contentSpecJS = json.getJSONObject("contentSpecification");

            Map<String, String> optionMap = new HashMap<String, String>();
            optionMap.put("srcNifId", srcNifId);
            optionMap.put("batchId", batchId);
            optionMap.put("dataSource", dataSource);
            optionMap.put("ingestorOutStatus", ingestorOutStatus);
            optionMap.put("updateOutStatus", updateOutStatus);

            String ingestMethod = ingestConfigJS.getString("ingestMethod");
            optionMap.put("ingestURL", ingestConfigJS.getString("ingestURL"));
            optionMap.put("allowDuplicates", ingestConfigJS.getString("allowDuplicates"));
            if (this.maxDocs > 0) {
                optionMap.put("maxDocs", String.valueOf(maxDocs));
            }
            if (this.runInTestMode) {
                optionMap.put("testMode", Boolean.TRUE.toString());
            }

            for (Iterator<String> it = contentSpecJS.keys(); it.hasNext(); ) {
                String name = it.next();
                optionMap.put(name, contentSpecJS.getString(name));
            }


            IConsumer harvester = ConsumerFactory.getInstance().createHarvester(ingestMethod,
                    "genIngestor", config.getCollectionName(), optionMap);
            IngestorWorker worker = new IngestorWorker(harvester, this.configFile);
            logger.info("starting harvester " + worker.consumer.getClass().getName());
            executorService.submit(worker);

        } catch (Exception x) {
            //TODO proper error handling
            x.printStackTrace();
        }
    }

    public static void usage(Options options) {
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp("ConsumerCoordinator", options);
        System.exit(1);
    }

    public static void main(String[] args) throws Exception {
        Option help = new Option("h", "print this message");
        Option configFileOption = OptionBuilder.withArgName("config-file")
                .hasArg().withDescription("config-file e.g. cinergi-consumers-cfg.xml").create('c');
        Option fullOption = OptionBuilder.withDescription("full data set default is 100 documents").create('f');
        Option provOption = OptionBuilder.withDescription("send provenance data to prov server").create('p');
        Option numOption = OptionBuilder.withDescription("Max number of documents to ingest").hasArg()
                .withArgName("max number of docs").create("n");
        Option testOption = OptionBuilder.withDescription("run ingestors in test mode").create('t');
        Options options = new Options();
        options.addOption(help);
        options.addOption(configFileOption);
        options.addOption(fullOption);
        options.addOption(provOption);
        options.addOption(numOption);
        options.addOption(testOption);
        CommandLineParser cli = new GnuParser();
        CommandLine line = null;
        try {
            line = cli.parse(options, args);
        } catch (Exception x) {
            System.err.println(x.getMessage());
            usage(options);
        }

        if (line.hasOption("h")) {
            usage(options);
        }
        String configFile = "consumers-cfg.xml";
        configFile = "cinergi-consumers-cfg.xml";
        int numDocs = 100;
        boolean processInFull = false;
        if (line.hasOption('c')) {
            configFile = line.getOptionValue('c');
        }
        processInFull = line.hasOption('f');

        if (line.hasOption('p')) {
            ProvenanceHelper.TEST_MODE = false;
        }
        if (line.hasOption('n')) {
            String v = line.getOptionValue('n');
            try {
                numDocs = Integer.parseInt(v);
            } catch (NumberFormatException x) {
                // no op
            }
            if (numDocs <= 0) {
                numDocs = 100;
            }
        }
        boolean runInTestMode = false;
        if (line.hasOption('t')) {
            runInTestMode = true;
        }

        Configuration config = ConfigLoader.load(configFile);
        ConsumerCoordinator cc = new ConsumerCoordinator(config, configFile);
        cc.setRunInTestMode(runInTestMode);
        if (!processInFull) {
            cc.setMaxDocs(numDocs);
        }
        cc.startup();

        cc.handle();
    }

}
