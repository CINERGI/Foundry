package org.neuinfo.foundry.jms.producer;

import com.mongodb.*;
import org.apache.log4j.Logger;
import org.neuinfo.foundry.common.config.Configuration;
import org.neuinfo.foundry.river.*;
import org.neuinfo.foundry.utils.ImmutableMap;

import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedTransferQueue;

/**
 * Created by bozyurt on 4/24/14.
 */
@Deprecated
public class MongoOpLogListener {
    public final static String TYPE = "mongodb";
    public final static String MONGODB_LOCAL_DATABASE = "local";
    public final static String MONGODB_ADMIN_DATABASE = "admin";
    public final static String MONGODB_CONFIG_DATABASE = "config";

    public final static String OPLOG_COLLECTION = "oplog.rs";
    public final static String OPLOG_NAMESPACE = "ns";
    public final static String OPLOG_NAMESPACE_COMMAND = "$cmd";
    public final static String OPLOG_ADMIN_COMMAND = "admin." + OPLOG_NAMESPACE_COMMAND;
    public final static String OPLOG_OBJECT = "o";
    public final static String OPLOG_UPDATE = "o2";
    public final static String OPLOG_OPERATION = "op";
    public final static String OPLOG_UPDATE_OPERATION = "u";
    public final static String OPLOG_INSERT_OPERATION = "i";
    public final static String OPLOG_DELETE_OPERATION = "d";
    public final static String OPLOG_COMMAND_OPERATION = "c";
    public final static String OPLOG_DROP_COMMAND_OPERATION = "drop";
    public final static String OPLOG_DROP_DATABASE_COMMAND_OPERATION = "dropDatabase";
    public final static String OPLOG_RENAME_COLLECTION_COMMAND_OPERATION = "renameCollection";
    public final static String OPLOG_TO = "to";
    public final static String OPLOG_TIMESTAMP = "ts";
    public final static String OPLOG_FROM_MIGRATE = "fromMigrate";
    public final static String GRIDFS_FILES_SUFFIX = ".files";
    public final static String GRIDFS_CHUNKS_SUFFIX = ".chunks";
    static Logger logger = Logger.getLogger(MongoOpLogListener.class);

    protected final MongoDBRiverDefinition definition;
    private Mongo mongo;
    private DB adminDb;
    protected final Context context;
    protected final String riverName;
    protected volatile boolean startInvoked = false;
    protected Thread tailerThread = null;
    private Configuration configuration;
    private Thread consumerThread;

    public MongoOpLogListener(String riverName, Configuration configuration) {
        if (logger.isTraceEnabled()) {
            logger.trace("initializing MongoOpLogListener");
        }
        this.configuration = configuration;
        this.riverName = riverName;
        this.definition = MongoDBRiverDefinition.parseSettings(riverName, riverName,
                null /* configuration.getMongoListenerSettings() */);
        BlockingQueue<QueueEntry> stream = definition.getThrottleSize() == -1 ? new LinkedTransferQueue<QueueEntry>()
                : new ArrayBlockingQueue<QueueEntry>(definition.getThrottleSize());

        this.context = new Context(Status.STOPPED, stream);
    }


    public void start() {
        try {
            logger.info("Starting MongoOpLogListener ");
            final TimeCheckPointManager timeCheckPointManager = TimeCheckPointManager.getInstance();
            this.context.setStatus(Status.RUNNING);
            for (ServerAddress server : definition.getMongoServers()) {
                logger.debug(String.format("Using mongodb server(s): host [%s], port [%s]",
                        server.getHost(), server.getPort()));
            }
            logger.info("MongoOpLogListener - v 0.1");
            logger.info(
                    String.format("starting MongoOpLogListener with options: secondaryreadpreference [%s], drop_collection [%s], include_collection [%s], throttlesize [%s], gridfs [%s], filter [%s], db [%s], collection [%s], script [%s], indexing to [%s]/[%s]",
                            definition.isMongoSecondaryReadPreference(), definition.isDropCollection(), definition.getIncludeCollection(),
                            definition.getThrottleSize(), definition.isMongoGridFS(), definition.getMongoOplogFilter(), definition.getMongoDb(),
                            definition.getMongoCollection(), definition.getScript(), definition.getIndexName(), definition.getTypeName())
            );

            Thread consumerThread;
            if (isMongos()) {
                throw new UnsupportedOperationException("sharding is not supported yet!");
            } else {
                tailerThread = new Thread(
                        new Slurper(definition.getMongoServers(), definition, context, timeCheckPointManager.getLastCheckPointTime()));
                tailerThread.setDaemon(true);
            }
            OplogQueueEntryConsumer consumer = new OplogQueueEntryConsumer(configuration, context);
            consumerThread = new Thread(consumer);
            consumerThread.setDaemon(true);

            if (tailerThread != null) {
                tailerThread.start();
            }

            consumerThread.start();
        } catch (Throwable t) {
            logger.warn("Fail to start MongoOpLogListener " + riverName, t);
            context.setStatus(Status.START_FAILED);
        } finally {
            startInvoked = true;
        }

    }

    private boolean isMongos() throws Exception {
        DB adminDb = getAdminDb();
        if (adminDb == null) {
            return false;
        }
        logger.trace(String.format("Found %s database", MONGODB_ADMIN_DATABASE));

        DBObject command = BasicDBObjectBuilder.start(
                ImmutableMap.builder().put("serverStatus", 1).put("asserts", 0).put("backgroundFlushing", 0).put("connections", 0)
                        .put("cursors", 0).put("dur", 0).put("extra_info", 0).put("globalLock", 0).put("indexCounters", 0).put("locks", 0)
                        .put("metrics", 0).put("network", 0).put("opcounters", 0).put("opcountersRepl", 0).put("recordStats", 0)
                        .put("repl", 0).build()
        ).get();
        logger.trace("About to execute: " + command);
        CommandResult cr = adminDb.command(command);
        logger.trace("Command executed return : " + cr);

        logger.info("MongoDB version - " + cr.get("version"));
        if (logger.isTraceEnabled()) {
            logger.trace("serverStatus: " + cr);
        }

        if (!cr.ok()) {
            logger.warn("serverStatus returns error: " + cr.getErrorMessage());
            return false;
        }

        if (cr.get("process") == null) {
            logger.warn("serverStatus.process return null.");
            return false;
        }
        String process = cr.get("process").toString().toLowerCase();
        if (logger.isTraceEnabled()) {
            logger.trace("process: " + process);
        }
        // Fix for https://jira.mongodb.org/browse/SERVER-9160
        return (process.contains("mongos"));
    }

    private DB getAdminDb() throws Exception {
        if (adminDb == null) {
            if (!definition.getMongoAdminUser().isEmpty()
                    && !definition.getMongoAdminPassword().isEmpty()
                    ) {
                MongoCredential credential = MongoCredential.createCredential(definition.getMongoAdminUser(),
                        MONGODB_ADMIN_DATABASE, definition.getMongoAdminPassword().toCharArray());
                adminDb = getMongoClient(credential).getDB(MONGODB_ADMIN_DATABASE);
            } else {
                adminDb = new MongoClient(definition.getMongoServers(), definition.getMongoClientOptions()).getDB(MONGODB_ADMIN_DATABASE);

            }
        }
//        if (adminDb == null) {
//            adminDb = getMongoClient().getDB(MONGODB_ADMIN_DATABASE);
//            if (logger.isTraceEnabled()) {
//                logger.trace(String.format("MongoAdminUser: % - authenticated: %", definition.getMongoAdminUser(), adminDb.isAuthenticated()));
//            }
//            if (!definition.getMongoAdminUser().isEmpty() && !definition.getMongoAdminPassword().isEmpty() && !adminDb.isAuthenticated()) {
//                logger.info(String.format("Authenticate %s with %s", MONGODB_ADMIN_DATABASE, definition.getMongoAdminUser()));
//
//                try {
//                    CommandResult cmd = adminDb.authenticateCommand(definition.getMongoAdminUser(), definition.getMongoAdminPassword()
//                            .toCharArray());
//                    if (!cmd.ok()) {
//                        logger.error(String.format("Authenticatication failed for %s: %s",
//                                MONGODB_ADMIN_DATABASE, cmd.getErrorMessage()));
//                    } else {
//                        logger.trace(String.format("authenticateCommand: %s - isAuthenticated: %s",
//                                cmd, adminDb.isAuthenticated()));
//                    }
//                } catch (MongoException mEx) {
//                    logger.warn("getAdminDb() failed", mEx);
//                }
//            }
//        }
        if (adminDb == null) {
            throw new Exception(String.format("Could not get %s database from MongoDB", MONGODB_ADMIN_DATABASE));
        }
        return adminDb;
    }

    private DB getConfigDb() throws Exception {
        DB configDb = null;
        if (!definition.getMongoAdminUser().isEmpty()
                && !definition.getMongoAdminPassword().isEmpty()
                ) {
            MongoCredential credential = MongoCredential.createCredential(definition.getMongoAdminUser(),
                    MONGODB_CONFIG_DATABASE, definition.getMongoAdminPassword().toCharArray());
            configDb = getMongoClient(credential).getDB(MONGODB_CONFIG_DATABASE);
        } else {
            configDb = new MongoClient(definition.getMongoServers(), definition.getMongoClientOptions()).getDB(MONGODB_CONFIG_DATABASE);
        }
//        DB configDb = getMongoClient().getDB(MONGODB_CONFIG_DATABASE);
//        if (!definition.getMongoAdminUser().isEmpty() && !definition.getMongoAdminPassword().isEmpty() && getAdminDb().isAuthenticated()) {
//            configDb = getAdminDb().getMongo().getDB(MONGODB_CONFIG_DATABASE);
//        }
        if (configDb == null) {
            throw new Exception(String.format("Could not get %s database from MongoDB", MONGODB_CONFIG_DATABASE));
        }
        return configDb;
    }


    private Mongo getMongoClient(MongoCredential credential) {
        if (mongo == null) {
            //mongo = new MongoClient(definition.getMongoServers(), definition.getMongoClientOptions());

            mongo = new MongoClient(definition.getMongoServers(), Arrays.asList(credential), definition.getMongoClientOptions());
        }
        return mongo;
    }

    private void closeMongoClient() {
        logger.info("Closing Mongo client");
        if (adminDb != null) {
            adminDb = null;
        }
        if (mongo != null) {
            mongo.close();
            mongo = null;
        }
    }

    private List<ServerAddress> getServerAddressForReplica(DBObject item) {
        String definition = item.get("host").toString();
        if (definition.contains("/")) {
            definition = definition.substring(definition.indexOf("/") + 1);
        }
        if (logger.isDebugEnabled()) {
            logger.debug("getServerAddressForReplica - definition: " + definition);
        }
        List<ServerAddress> servers = new ArrayList<ServerAddress>();
        for (String server : definition.split(",")) {
            try {
                servers.add(new ServerAddress(server));
            } catch (MongoTimeoutException uhEx) {
                logger.warn("failed to execute bulk" + uhEx);
            }
        }
        return servers;
    }


    public void shutdown() {
        logger.info("shutting down MongoOpLogListener");
        try {
            if (this.tailerThread != null) {
                this.tailerThread.interrupt();
                this.tailerThread = null;
            }
            if (this.consumerThread != null) {
                this.consumerThread.interrupt();
                this.consumerThread = null;
            }
            closeMongoClient();
        } catch (Throwable t) {
            logger.error("Fail to close MongoOpLogListener " + t.getMessage());
        } finally {
            this.context.setStatus(Status.STOPPED);
        }
    }

}
