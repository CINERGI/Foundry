package org.neuinfo.foundry.river;

import com.mongodb.*;
import org.apache.log4j.Logger;
import org.neuinfo.foundry.river.util.MongoDBHelper;
import org.neuinfo.foundry.utils.ImmutableMap;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedTransferQueue;

/**
 * Created by bozyurt on 4/4/14.
 */
public class MongoDBRiver {
    public final static String TYPE = "mongodb";
    public final static String NAME = "mongodb-river";
    public final static String STATUS_ID = "_riverstatus";
    public final static String STATUS_FIELD = "status";
    public final static String DESCRIPTION = "MongoDB River Plugin";
    public final static String LAST_TIMESTAMP_FIELD = "_last_ts";
    public final static String MONGODB_LOCAL_DATABASE = "local";
    public final static String MONGODB_ADMIN_DATABASE = "admin";
    public final static String MONGODB_CONFIG_DATABASE = "config";
    public final static String MONGODB_ID_FIELD = "_id";
    public final static String MONGODB_IN_OPERATOR = "$in";
    public final static String MONGODB_OR_OPERATOR = "$or";
    public final static String MONGODB_AND_OPERATOR = "$and";
    public final static String MONGODB_NATURAL_OPERATOR = "$natural";
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
    static Logger logger = Logger.getLogger(MongoDBRiver.class);

    protected volatile List<Thread> tailerThreads = new ArrayList<Thread>();
    protected volatile Thread indexerThread;
    protected volatile Thread statusThread;
    protected volatile boolean startInvoked = false;
    protected final MongoDBRiverDefinition definition;
    private Mongo mongo;
    private DB adminDb;
    protected final Context context;
    protected final String riverName;

    public MongoDBRiver(String riverName, String riverIndexName, Map<String, Object> settings) {
        if (logger.isTraceEnabled()) {
            logger.trace("initializing river:" + riverName);
        }
        this.riverName = riverName;
        this.definition = MongoDBRiverDefinition.parseSettings(riverName, riverIndexName, settings);
        int throttleSize = definition.getThrottleSize();

        BlockingQueue<QueueEntry> stream = definition.getThrottleSize() == -1 ? new LinkedTransferQueue<QueueEntry>()
                : new ArrayBlockingQueue<QueueEntry>(definition.getThrottleSize());

        this.context = new Context(Status.STOPPED, stream);
    }

    public void start() {
        try {
            logger.info("Starting river ");
            this.context.setStatus(Status.RUNNING);
            for (ServerAddress server : definition.getMongoServers()) {
                logger.debug(String.format("Using mongodb server(s): host [%s], port [%s]",
                        server.getHost(), server.getPort()));
            }
            // http://stackoverflow.com/questions/5270611/read-maven-properties-file-inside-jar-war-file
            logger.info(String.format("%s - %s", DESCRIPTION, MongoDBHelper.getRiverVersion()));
            logger.info(
                    String.format("starting mongodb stream. options: secondaryreadpreference [%s], drop_collection [%s], include_collection [%s], throttlesize [%s], gridfs [%s], filter [%s], db [%s], collection [%s], script [%s], indexing to [%s]/[%s]",
                            definition.isMongoSecondaryReadPreference(), definition.isDropCollection(), definition.getIncludeCollection(),
                            definition.getThrottleSize(), definition.isMongoGridFS(), definition.getMongoOplogFilter(), definition.getMongoDb(),
                            definition.getMongoCollection(), definition.getScript(), definition.getIndexName(), definition.getTypeName()));


            Thread tailerThread = null;
            Thread consumerThread = null;
            if (isMongos()) {
                throw new UnsupportedOperationException("sharding is not supported yet!");
            } else {
                logger.trace("Not mongos");
                tailerThread = new Thread(new Slurper(definition.getMongoServers(), definition, context));
                tailerThread.setDaemon(true);

            }

           // consumerThread = new Thread(new DocIdAssigner(definition, context));
           // consumerThread.setDaemon(true);

            if (tailerThread != null) {
                tailerThread.start();
            }

            if (consumerThread != null) {
                consumerThread.start();
            }


        } catch (Throwable t) {
            logger.warn("Fail to start river " + riverName, t);
            // MongoDBHelper.setRiverStatus(client, definition.getRiverName(), Status.START_FAILED);
            this.context.setStatus(Status.START_FAILED);

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
                        .put("repl", 0).build()).get();
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
            adminDb = getMongoClient().getDB(MONGODB_ADMIN_DATABASE);
            if (logger.isTraceEnabled()) {
                logger.trace(String.format("MongoAdminUser: % - authenticated: %", definition.getMongoAdminUser(), adminDb.isAuthenticated()));
            }
            if (!definition.getMongoAdminUser().isEmpty() && !definition.getMongoAdminPassword().isEmpty() && !adminDb.isAuthenticated()) {
                logger.info(String.format("Authenticate %s with %s", MONGODB_ADMIN_DATABASE, definition.getMongoAdminUser()));

                try {
                    CommandResult cmd = adminDb.authenticateCommand(definition.getMongoAdminUser(), definition.getMongoAdminPassword()
                            .toCharArray());
                    if (!cmd.ok()) {
                        logger.error(String.format("Authenticatication failed for %s: %s",
                                MONGODB_ADMIN_DATABASE, cmd.getErrorMessage()));
                    } else {
                        logger.trace(String.format("authenticateCommand: %s - isAuthenticated: %s",
                                cmd, adminDb.isAuthenticated()));
                    }
                } catch (MongoException mEx) {
                    logger.warn("getAdminDb() failed", mEx);
                }
            }
        }
        if (adminDb == null) {
            throw new Exception(String.format("Could not get %s database from MongoDB", MONGODB_ADMIN_DATABASE));
        }
        return adminDb;
    }

    private DB getConfigDb() throws Exception {
        DB configDb = getMongoClient().getDB(MONGODB_CONFIG_DATABASE);
        if (!definition.getMongoAdminUser().isEmpty() && !definition.getMongoAdminPassword().isEmpty() && getAdminDb().isAuthenticated()) {
            configDb = getAdminDb().getMongo().getDB(MONGODB_CONFIG_DATABASE);
        }
        if (configDb == null) {
            throw new Exception(String.format("Could not get %s database from MongoDB", MONGODB_CONFIG_DATABASE));
        }
        return configDb;
    }


    private Mongo getMongoClient() {
        if (mongo == null) {
            mongo = new MongoClient(definition.getMongoServers(), definition.getMongoClientOptions());
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


    public void close() {
        logger.info("Closing river");
        try {
            if (statusThread != null) {
                statusThread.interrupt();
                statusThread = null;
            }
            for (Thread thread : tailerThreads) {
                thread.interrupt();
                thread = null;
            }
            tailerThreads.clear();
            if (indexerThread != null) {
                indexerThread.interrupt();
                indexerThread = null;
            }
            closeMongoClient();
        } catch (Throwable t) {
            logger.error("Fail to close river " + t.getMessage());
        } finally {
            this.context.setStatus(Status.STOPPED);
        }
    }


    public static Map<String, Object> prepSettings() {
        Map<String, Object> settings = new HashMap<String, Object>(7);

        Map<String, Object> typeMap = new HashMap<String, Object>(17);
        settings.put(MongoDBRiver.TYPE, typeMap);

        Map<String, Object> serverMap = new HashMap<String, Object>(7);
        List<Map<String, Object>> servers = new ArrayList<Map<String, Object>>(2);
        servers.add(serverMap);
        typeMap.put(MongoDBRiverDefinition.SERVERS_FIELD, servers);
        serverMap.put(MongoDBRiverDefinition.HOST_FIELD, "burak.crbs.ucsd.edu");
        serverMap.put(MongoDBRiverDefinition.PORT_FIELD, 27017);

        serverMap = new HashMap<String, Object>(7);
        typeMap.put(MongoDBRiverDefinition.SERVERS_FIELD, servers);
        serverMap.put(MongoDBRiverDefinition.HOST_FIELD, "burak.crbs.ucsd.edu");
        serverMap.put(MongoDBRiverDefinition.PORT_FIELD, 27018);
        servers.add(serverMap);

        typeMap.put(MongoDBRiverDefinition.DB_FIELD, "discotest");
        typeMap.put(MongoDBRiverDefinition.COLLECTION_FIELD, "records");
        return settings;
    }

    public static void main(String[] args) throws Exception {

        Map<String, Object> settings = prepSettings();

        MongoDBRiver river = new MongoDBRiver("discotest", "test_index", settings);

        river.start();

        String line;
        while (true) {
            System.out.println("Type q to quit:");
            BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
            line = reader.readLine();
            if (line.equalsIgnoreCase("q")) {
                break;
            }
        }
        System.out.println("exiting");
    }

}
