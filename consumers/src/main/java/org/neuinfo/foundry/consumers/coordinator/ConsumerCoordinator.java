package org.neuinfo.foundry.consumers.coordinator;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.commons.cli.*;
import org.apache.log4j.Logger;
import org.json.JSONObject;
import org.neuinfo.foundry.common.config.ConsumerConfig;
import org.neuinfo.foundry.common.util.Assertion;
import org.neuinfo.foundry.common.util.ScigraphMappingsHandler;
import org.neuinfo.foundry.common.util.ScigraphUtils;
import org.neuinfo.foundry.consumers.common.ConfigLoader;
import org.neuinfo.foundry.consumers.common.Configuration;
import org.neuinfo.foundry.consumers.common.ServiceFactory;
import org.neuinfo.foundry.consumers.jms.consumers.plugins.ProvenanceHelper;

import javax.jms.*;
import java.io.File;
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
    private boolean consumerMode = false;
    private int maxDocs = -1;
    private boolean runInTestMode = false;
    /**
     * file holding a list of urls for the data to be ingested disregarding the rest
     */
    private String includeFile;
    private boolean onlyErrors = false;
    private final ExecutorService executorService = Executors.newFixedThreadPool(10);
    List<ConsumerWorker> consumerWorkers = new ArrayList<ConsumerWorker>();

    private final static Logger logger = Logger.getLogger("ConsumerCoordinator");

    public ConsumerCoordinator(Configuration config, String configFile,
                               boolean consumerMode,
                               String includeFile, boolean onlyErrors) throws Exception {
        this.config = config;
        this.configFile = configFile;
        this.consumerMode = consumerMode;
        this.includeFile = includeFile;
        this.onlyErrors = onlyErrors;
        ServiceFactory.getInstance(configFile);
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
        ScigraphMappingsHandler smHandler = ScigraphMappingsHandler.getInstance();
        ScigraphUtils.setHandler(smHandler);
    }

    public void shutdown() {
        logger.info("shutting down the ConsumerCoordinator...");
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
            logger.info("starting consumer  inStatus:" + worker.consumer.getInStatus()
                    + " outStatus:" + worker.consumer.getOutStatus() +
                    " successQueue:" + worker.consumer.getSuccessMessageQueueName());
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
        if (!consumerMode) {
            Destination destination = this.session.createQueue("foundry.consumer.head");
            MessageConsumer messageConsumer = this.session.createConsumer(destination);
            messageConsumer.setMessageListener(listener);
        }
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
            if (includeFile != null) {
                optionMap.put("includeFile", includeFile);
            }
            optionMap.put("onlyErrors", String.valueOf(onlyErrors));

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
        Option configFileOption = Option.builder("c").argName("config-file")
                .hasArg().desc("config-file e.g. cinergi-consumers-cfg.xml").build();
        Option fullOption = Option.builder("f").desc("full data set default is 100 documents").build();
        Option provOption = Option.builder("p").desc("send provenance data to prov server").build();
        Option numOption = Option.builder("n").desc("Max number of documents to ingest").hasArg()
                .argName("max number of docs").build();
        Option testOption = Option.builder("t").desc("run ingestors in test mode").build();
        Option consumerModeOption = Option.builder("cm").desc("run in consumer mode (no ingestors)").build();
        Option includeFileOption = Option.builder("i").hasArg().argName("include-file")
                .desc("a list of urls to be processed rejecting the rest").build();
        Option onlyErrorsOpt = new Option("e", "process only records with errors");
        Options options = new Options();
        options.addOption(help);
        options.addOption(configFileOption);
        options.addOption(fullOption);
        options.addOption(provOption);
        options.addOption(numOption);
        options.addOption(testOption);
        options.addOption(consumerModeOption);
        options.addOption(includeFileOption);
        options.addOption(onlyErrorsOpt);
        CommandLineParser cli = new DefaultParser();
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
        boolean consumerMode = line.hasOption("cm");
        String includeFile = null;
        if (line.hasOption('i')) {
            includeFile = line.getOptionValue('i');
            if (!new File(includeFile).isFile()) {
                System.err.println("Not a valid includeFile: " + includeFile);
                usage(options);
            }
        }
        boolean onlyErrors = line.hasOption('e');
        
        Configuration config = ConfigLoader.load(configFile);
        ConsumerCoordinator cc = new ConsumerCoordinator(config, configFile,
                consumerMode, includeFile, onlyErrors);
        cc.setRunInTestMode(runInTestMode);
        if (!processInFull) {
            cc.setMaxDocs(numDocs);
        }
        cc.startup();

        cc.handle();
    }

}
