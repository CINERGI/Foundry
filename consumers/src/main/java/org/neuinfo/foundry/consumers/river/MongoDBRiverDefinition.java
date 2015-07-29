package org.neuinfo.foundry.consumers.river;

import com.mongodb.*;
import com.mongodb.util.JSON;
import org.apache.log4j.Logger;
import org.bson.BasicBSONObject;
import org.bson.types.BSONTimestamp;
import org.neuinfo.foundry.common.util.Utils;

import javax.net.SocketFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.net.UnknownHostException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.*;

/**
 * Created by bozyurt on 4/4/14.
 */
public class MongoDBRiverDefinition {
    static Logger logger = Logger.getLogger(MongoDBRiverDefinition.class);
    // defaults
    public final static String DEFAULT_DB_HOST = "localhost";
    public final static int DEFAULT_DB_PORT = 27017;
    public final static int DEFAULT_CONCURRENT_REQUESTS = Runtime.getRuntime().availableProcessors();
    public final static int DEFAULT_BULK_ACTIONS = 1000;
    //  public final static TimeValue DEFAULT_FLUSH_INTERVAL = TimeValue.timeValueMillis(10);
    //  public final static ByteSizeValue DEFAULT_BULK_SIZE = new ByteSizeValue(5, ByteSizeUnit.MB);
    public final static long DEFAULT_FLUSH_INTERVAL = 10;
    public final static long DEFAULT_BULK_SIZE = 5 * 1024 * 1024; // 5MB

    public final static String TYPE = "mongodb";
    public final static String MONGODB_ID_FIELD = "_id";

    public final static String OPLOG_UPDATE_OPERATION = "u";
    public final static String OPLOG_INSERT_OPERATION = "i";
    public final static String OPLOG_DELETE_OPERATION = "d";
    public final static String OPLOG_COMMAND_OPERATION = "c";

    // fields
    public final static String DB_FIELD = "db";
    public final static String SERVERS_FIELD = "servers";
    public final static String HOST_FIELD = "host";
    public final static String PORT_FIELD = "port";
    public final static String OPTIONS_FIELD = "options";
    public final static String SECONDARY_READ_PREFERENCE_FIELD = "secondary_read_preference";
    public final static String CONNECTION_TIMEOUT = "connect_timeout";
    public final static String SOCKET_TIMEOUT = "socket_timeout";
    public final static String SSL_CONNECTION_FIELD = "ssl";
    public final static String SSL_VERIFY_CERT_FIELD = "ssl_verify_certificate";
    public final static String DROP_COLLECTION_FIELD = "drop_collection";
    public final static String EXCLUDE_FIELDS_FIELD = "exclude_fields";
    public final static String INCLUDE_FIELDS_FIELD = "include_fields";
    public final static String INCLUDE_COLLECTION_FIELD = "include_collection";
    public final static String INITIAL_TIMESTAMP_FIELD = "initial_timestamp";
    public final static String INITIAL_TIMESTAMP_SCRIPT_TYPE_FIELD = "script_type";
    public final static String INITIAL_TIMESTAMP_SCRIPT_FIELD = "script";
    public final static String ADVANCED_TRANSFORMATION_FIELD = "advanced_transformation";
    public final static String SKIP_INITIAL_IMPORT_FIELD = "skip_initial_import";
    public final static String PARENT_TYPES_FIELD = "parent_types";
    public final static String STORE_STATISTICS_FIELD = "store_statistics";
    public final static String IMPORT_ALL_COLLECTIONS_FIELD = "import_all_collections";
    public final static String DISABLE_INDEX_REFRESH_FIELD = "disable_index_refresh";
    public final static String FILTER_FIELD = "filter";
    public final static String CREDENTIALS_FIELD = "credentials";
    public final static String USER_FIELD = "user";
    public final static String PASSWORD_FIELD = "password";
    public final static String SCRIPT_FIELD = "script";
    public final static String SCRIPT_TYPE_FIELD = "script_type";
    public final static String COLLECTION_FIELD = "collection";
    public final static String GRIDFS_FIELD = "gridfs";
    public final static String INDEX_OBJECT = "index";
    public final static String NAME_FIELD = "name";
    public final static String TYPE_FIELD = "type";
    public final static String LOCAL_DB_FIELD = "local";
    public final static String ADMIN_DB_FIELD = "admin";
    public final static String THROTTLE_SIZE_FIELD = "throttle_size";
    public final static String BULK_SIZE_FIELD = "bulk_size";
    public final static String BULK_TIMEOUT_FIELD = "bulk_timeout";
    public final static String CONCURRENT_BULK_REQUESTS_FIELD = "concurrent_bulk_requests";

    public final static String BULK_FIELD = "bulk";
    public final static String ACTIONS_FIELD = "actions";
    public final static String SIZE_FIELD = "size";
    public final static String CONCURRENT_REQUESTS_FIELD = "concurrent_requests";
    public final static String FLUSH_INTERVAL_FIELD = "flush_interval";

    // river
    private final String riverName;
    private final String riverIndexName;

    // mongodb.servers
    private final List<ServerAddress> mongoServers = new ArrayList<ServerAddress>();
    // mongodb
    private final String mongoDb;
    private final String mongoCollection;
    private final boolean mongoGridFS;
    private final BasicDBObject mongoOplogFilter;
    private final BasicDBObject mongoCollectionFilter;
    // mongodb.credentials
    private final String mongoAdminUser;
    private final String mongoAdminPassword;
    private final String mongoLocalUser;
    private final String mongoLocalPassword;

    // mongodb.options
    private final MongoClientOptions mongoClientOptions;
    private final int connectTimeout;
    private final int socketTimeout;
    private final boolean mongoSecondaryReadPreference;
    private final boolean mongoUseSSL;
    private final boolean mongoSSLVerifyCertificate;
    private final boolean dropCollection;
    private final Set<String> excludeFields;
    private final Set<String> includeFields;
    private final String includeCollection;
    private final BSONTimestamp initialTimestamp;
    private final String script;
    private final String scriptType;
    private final boolean advancedTransformation;
    private final boolean skipInitialImport;
    private final Set<String> parentTypes;
    private final boolean storeStatistics;
    private final String statisticsIndexName;
    private final String statisticsTypeName;
    private final boolean importAllCollections;
    private final boolean disableIndexRefresh;
    // index
    private final String indexName;
    private final String typeName;
    private final int throttleSize;

    // bulk
    private final Bulk bulk;

    public static class Builder {
        // river
        private String riverName;
        private String riverIndexName;

        // mongodb.servers
        private List<ServerAddress> mongoServers = new ArrayList<ServerAddress>();
        // mongodb
        private String mongoDb;
        private String mongoCollection;
        private boolean mongoGridFS;
        private BasicDBObject mongoOplogFilter;// = new BasicDBObject();
        private BasicDBObject mongoCollectionFilter = new BasicDBObject();
        // mongodb.credentials
        private String mongoAdminUser = "";
        private String mongoAdminPassword = "";
        private String mongoLocalUser = "";
        private String mongoLocalPassword = "";
        // mongodb.options
        private MongoClientOptions mongoClientOptions = null;
        private int connectTimeout = 0;
        private int socketTimeout = 0;
        private boolean mongoSecondaryReadPreference = false;
        private boolean mongoUseSSL = false;
        private boolean mongoSSLVerifyCertificate = false;
        private boolean dropCollection = false;
        private Set<String> excludeFields = null;
        private Set<String> includeFields = null;
        private String includeCollection = "";
        private BSONTimestamp initialTimestamp = null;
        private String script = null;
        private String scriptType = null;
        private boolean advancedTransformation = false;
        private boolean skipInitialImport;
        private Set<String> parentTypes = null;
        private boolean storeStatistics;
        private String statisticsIndexName;
        private String statisticsTypeName;
        private boolean importAllCollections;
        private boolean disableIndexRefresh;

        // index
        private String indexName;
        private String typeName;
        private int throttleSize = 1000; // IBO

        private Bulk bulk;

        public Builder mongoServers(List<ServerAddress> mongoServers) {
            this.mongoServers = mongoServers;
            return this;
        }

        public Builder riverName(String riverName) {
            this.riverName = riverName;
            return this;
        }

        public Builder riverIndexName(String riverIndexName) {
            this.riverIndexName = riverIndexName;
            return this;
        }

        public Builder mongoDb(String mongoDb) {
            this.mongoDb = mongoDb;
            return this;
        }

        public Builder mongoCollection(String mongoCollection) {
            this.mongoCollection = mongoCollection;
            return this;
        }

        public Builder mongoGridFS(boolean mongoGridFS) {
            this.mongoGridFS = mongoGridFS;
            return this;
        }

        public Builder mongoOplogFilter(BasicDBObject mongoOplogFilter) {
            this.mongoOplogFilter = mongoOplogFilter;
            return this;
        }

        public Builder mongoCollectionFilter(BasicDBObject mongoCollectionFilter) {
            this.mongoCollectionFilter = mongoCollectionFilter;
            return this;
        }

        public Builder mongoAdminUser(String mongoAdminUser) {
            this.mongoAdminUser = mongoAdminUser;
            return this;
        }

        public Builder mongoAdminPassword(String mongoAdminPassword) {
            this.mongoAdminPassword = mongoAdminPassword;
            return this;
        }

        public Builder mongoLocalUser(String mongoLocalUser) {
            this.mongoLocalUser = mongoLocalUser;
            return this;
        }

        public Builder mongoLocalPassword(String mongoLocalPassword) {
            this.mongoLocalPassword = mongoLocalPassword;
            return this;
        }

        public Builder mongoClientOptions(MongoClientOptions mongoClientOptions) {
            this.mongoClientOptions = mongoClientOptions;
            return this;
        }

        public Builder connectTimeout(int connectTimeout) {
            this.connectTimeout = connectTimeout;
            return this;
        }

        public Builder socketTimeout(int socketTimeout) {
            this.socketTimeout = socketTimeout;
            return this;
        }

        public Builder mongoSecondaryReadPreference(boolean mongoSecondaryReadPreference) {
            this.mongoSecondaryReadPreference = mongoSecondaryReadPreference;
            return this;
        }

        public Builder mongoUseSSL(boolean mongoUseSSL) {
            this.mongoUseSSL = mongoUseSSL;
            return this;
        }

        public Builder mongoSSLVerifyCertificate(boolean mongoSSLVerifyCertificate) {
            this.mongoSSLVerifyCertificate = mongoSSLVerifyCertificate;
            return this;
        }

        public Builder dropCollection(boolean dropCollection) {
            this.dropCollection = dropCollection;
            return this;
        }

        public Builder excludeFields(Set<String> excludeFields) {
            this.excludeFields = excludeFields;
            return this;
        }

        public Builder includeFields(Set<String> includeFields) {
            this.includeFields = includeFields;
            return this;
        }

        public Builder includeCollection(String includeCollection) {
            this.includeCollection = includeCollection;
            return this;
        }

        public Builder disableIndexRefresh(boolean disableIndexRefresh) {
            this.disableIndexRefresh = disableIndexRefresh;
            return this;
        }

        public Builder initialTimestamp(BSONTimestamp initialTimestamp) {
            this.initialTimestamp = initialTimestamp;
            return this;
        }

        public Builder advancedTransformation(boolean advancedTransformation) {
            this.advancedTransformation = advancedTransformation;
            return this;
        }

        public Builder skipInitialImport(boolean skipInitialImport) {
            this.skipInitialImport = skipInitialImport;
            return this;
        }

        public Builder parentTypes(Set<String> parentTypes) {
            this.parentTypes = parentTypes;
            return this;
        }

        public Builder storeStatistics(boolean storeStatistics) {
            this.storeStatistics = storeStatistics;
            return this;
        }

        public Builder statisticsIndexName(String statisticsIndexName) {
            this.statisticsIndexName = statisticsIndexName;
            return this;
        }

        public Builder statisticsTypeName(String statisticsTypeName) {
            this.statisticsTypeName = statisticsTypeName;
            return this;
        }

        public Builder importAllCollections(boolean importAllCollections) {
            this.importAllCollections = importAllCollections;
            return this;
        }

        public Builder script(String script) {
            this.script = script;
            return this;
        }

        public Builder scriptType(String scriptType) {
            this.scriptType = scriptType;
            return this;
        }

        public Builder indexName(String indexName) {
            this.indexName = indexName;
            return this;
        }

        public Builder typeName(String typeName) {
            this.typeName = typeName;
            return this;
        }

        public Builder throttleSize(int throttleSize) {
            this.throttleSize = throttleSize;
            return this;
        }

        public Builder bulk(Bulk bulk) {
            this.bulk = bulk;
            return this;
        }

        public MongoDBRiverDefinition build() {
            return new MongoDBRiverDefinition(this);
        }
    }

    static class Bulk {

        private final int concurrentRequests;
        private final int bulkActions;
        private final long bulkSize;
        private final long flushInterval;

        static class Builder {

            private int concurrentRequests = DEFAULT_CONCURRENT_REQUESTS;
            private int bulkActions = DEFAULT_BULK_ACTIONS;
            private long bulkSize = DEFAULT_BULK_SIZE;
            private long flushInterval = DEFAULT_FLUSH_INTERVAL;

            public Builder concurrentRequests(int concurrentRequests) {
                this.concurrentRequests = concurrentRequests;
                return this;
            }

            public Builder bulkActions(int bulkActions) {
                this.bulkActions = bulkActions;
                return this;
            }

            public Builder bulkSize(long bulkSize) {
                this.bulkSize = bulkSize;
                return this;
            }

            public Builder flushInterval(long flushInterval) {
                this.flushInterval = flushInterval;
                return this;
            }

            /**
             * Builds a new bulk processor.
             */
            public Bulk build() {
                return new Bulk(this);
            }
        }

        public Bulk(final Builder builder) {
            this.bulkActions = builder.bulkActions;
            this.bulkSize = builder.bulkSize;
            this.concurrentRequests = builder.concurrentRequests;
            this.flushInterval = builder.flushInterval;
        }

        public int getConcurrentRequests() {
            return concurrentRequests;
        }

        public int getBulkActions() {
            return bulkActions;
        }

        public long getBulkSize() {
            return bulkSize;
        }

        public long getFlushInterval() {
            return flushInterval;
        }

    }

    @SuppressWarnings("unchecked")
    public synchronized static MongoDBRiverDefinition parseSettings(String riverName, String riverIndexName, Map<String, Object> settings) {

        logger.info("Parse river settings for " + riverName);
        // Preconditions.checkNotNull(riverName, "No riverName specified");
        // Preconditions.checkNotNull(riverIndexName, "No riverIndexName specified");
        //  Preconditions.checkNotNull(settings, "No settings specified");

        Builder builder = new Builder();
        builder.riverName(riverName);
        builder.riverIndexName(riverIndexName);

        List<ServerAddress> mongoServers = new ArrayList<ServerAddress>();
        String mongoHost;
        int mongoPort;

        if (settings.containsKey(TYPE)) {
            Map<String, Object> mongoSettings = (Map<String, Object>) settings.get(TYPE);
            if (mongoSettings.containsKey(SERVERS_FIELD)) {
                Object mongoServersSettings = mongoSettings.get(SERVERS_FIELD);
                logger.trace("mongoServersSettings: " + mongoServersSettings);
                boolean array = mongoServersSettings instanceof ArrayList;

                if (array) {
                    ArrayList<Map<String, Object>> feeds = (ArrayList<Map<String, Object>>) mongoServersSettings;
                    for (Map<String, Object> feed : feeds) {
                        mongoHost = Utils.getStringValue(feed.get(HOST_FIELD), null);
                        mongoPort = Utils.getIntValue(feed.get(PORT_FIELD), DEFAULT_DB_PORT);
                        logger.info("Server: " + mongoHost + " - " + mongoPort);
                        try {
                            mongoServers.add(new ServerAddress(mongoHost, mongoPort));
                        } catch (UnknownHostException uhEx) {
                            logger.warn("Cannot add mongo server " + uhEx.getMessage() + " " + mongoHost + ":" + mongoPort);
                        }
                    }
                }
            } else {
                mongoHost = Utils.getStringValue(mongoSettings.get(HOST_FIELD), DEFAULT_DB_HOST);
                mongoPort = Utils.getIntValue(mongoSettings.get(PORT_FIELD), DEFAULT_DB_PORT);
                try {
                    mongoServers.add(new ServerAddress(mongoHost, mongoPort));
                } catch (UnknownHostException uhEx) {
                    logger.warn("Cannot add mongo server " + uhEx.getMessage() + " " + mongoHost + ":" + mongoPort);
                }
            }
            builder.mongoServers(mongoServers);

            MongoClientOptions.Builder mongoClientOptionsBuilder = MongoClientOptions.builder().autoConnectRetry(true)
                    .socketKeepAlive(true);

            // MongoDB options
            if (mongoSettings.containsKey(OPTIONS_FIELD)) {
                Map<String, Object> mongoOptionsSettings = (Map<String, Object>) mongoSettings.get(OPTIONS_FIELD);
                logger.trace("mongoOptionsSettings: " + mongoOptionsSettings);
                builder.mongoSecondaryReadPreference(Utils.getBoolValue(
                        mongoOptionsSettings.get(SECONDARY_READ_PREFERENCE_FIELD), false));
                builder.connectTimeout(Utils.getIntValue(mongoOptionsSettings.get(CONNECTION_TIMEOUT), 0));
                builder.socketTimeout(Utils.getIntValue(mongoOptionsSettings.get(SOCKET_TIMEOUT), 0));
                builder.dropCollection(Utils.getBoolValue(mongoOptionsSettings.get(DROP_COLLECTION_FIELD), false));
                builder.mongoUseSSL(Utils.getBoolValue(mongoOptionsSettings.get(SSL_CONNECTION_FIELD), false));
                builder.mongoSSLVerifyCertificate(Utils.getBoolValue(mongoOptionsSettings.get(SSL_VERIFY_CERT_FIELD), true));
                builder.advancedTransformation(Utils.getBoolValue(mongoOptionsSettings.get(ADVANCED_TRANSFORMATION_FIELD),
                        false));
                builder.skipInitialImport(Utils.getBoolValue(mongoOptionsSettings.get(SKIP_INITIAL_IMPORT_FIELD), false));

                mongoClientOptionsBuilder.connectTimeout(builder.connectTimeout).socketTimeout(builder.socketTimeout);

                if (builder.mongoSecondaryReadPreference) {
                    mongoClientOptionsBuilder.readPreference(ReadPreference.secondaryPreferred());
                }

                if (builder.mongoUseSSL) {
                    mongoClientOptionsBuilder.socketFactory(getSSLSocketFactory());
                }

                if (mongoOptionsSettings.containsKey(PARENT_TYPES_FIELD)) {
                    Set<String> parentTypes = new HashSet<String>();
                    Object parentTypesSettings = mongoOptionsSettings.get(PARENT_TYPES_FIELD);
                    logger.debug("parentTypesSettings: " + parentTypesSettings);
                    boolean array = parentTypesSettings instanceof ArrayList;

                    if (array) {
                        ArrayList<String> fields = (ArrayList<String>) parentTypesSettings;
                        for (String field : fields) {
                            logger.debug("Field: " + field);
                            parentTypes.add(field);
                        }
                    }

                    builder.parentTypes(parentTypes);
                }

                if (mongoOptionsSettings.containsKey(STORE_STATISTICS_FIELD)) {
                    Object storeStatistics = mongoOptionsSettings.get(STORE_STATISTICS_FIELD);
                    boolean object = storeStatistics instanceof Map;
                    if (object) {
                        Map<String, Object> storeStatisticsSettings = (Map<String, Object>) storeStatistics;
                        builder.storeStatistics(true);
                        builder.statisticsIndexName(Utils.getStringValue(storeStatisticsSettings.get(INDEX_OBJECT), riverName
                                + "-stats"));
                        builder.statisticsTypeName(Utils.getStringValue(storeStatisticsSettings.get(TYPE_FIELD), "stats"));
                    } else {
                        builder.storeStatistics(Utils.getBoolValue(storeStatistics, false));
                        if (builder.storeStatistics) {
                            builder.statisticsIndexName(riverName + "-stats");
                            builder.statisticsTypeName("stats");
                        }
                    }
                }
                // builder.storeStatistics(XContentMapValues.nodeBooleanValue(mongoOptionsSettings.get(STORE_STATISTICS_FIELD),
                // false));
                builder.importAllCollections(Utils.getBoolValue(mongoOptionsSettings.get(IMPORT_ALL_COLLECTIONS_FIELD),
                        false));
                builder.disableIndexRefresh(Utils.getBoolValue(mongoOptionsSettings.get(DISABLE_INDEX_REFRESH_FIELD), false));
                builder.includeCollection(Utils.getStringValue(mongoOptionsSettings.get(INCLUDE_COLLECTION_FIELD), ""));

                if (mongoOptionsSettings.containsKey(INCLUDE_FIELDS_FIELD)) {
                    Set<String> includeFields = new HashSet<String>();
                    Object includeFieldsSettings = mongoOptionsSettings.get(INCLUDE_FIELDS_FIELD);
                    logger.debug("includeFieldsSettings: " + includeFieldsSettings);
                    boolean array = includeFieldsSettings instanceof ArrayList;

                    if (array) {
                        ArrayList<String> fields = (ArrayList<String>) includeFieldsSettings;
                        for (String field : fields) {
                            logger.debug("Field: " + field);
                            includeFields.add(field);
                        }
                    }

                    if (!includeFields.contains(MONGODB_ID_FIELD)) {
                        includeFields.add(MONGODB_ID_FIELD);
                    }
                    builder.includeFields(includeFields);
                } else if (mongoOptionsSettings.containsKey(EXCLUDE_FIELDS_FIELD)) {
                    Set<String> excludeFields = new HashSet<String>();
                    Object excludeFieldsSettings = mongoOptionsSettings.get(EXCLUDE_FIELDS_FIELD);
                    logger.debug("excludeFieldsSettings: " + excludeFieldsSettings);
                    boolean array = excludeFieldsSettings instanceof ArrayList;

                    if (array) {
                        ArrayList<String> fields = (ArrayList<String>) excludeFieldsSettings;
                        for (String field : fields) {
                            logger.debug("Field: " + field);
                            excludeFields.add(field);
                        }
                    }

                    builder.excludeFields(excludeFields);
                }

                if (mongoOptionsSettings.containsKey(INITIAL_TIMESTAMP_FIELD)) {
                    BSONTimestamp timeStamp = null;
                    try {
                        Map<String, Object> initalTimestampSettings = (Map<String, Object>) mongoOptionsSettings
                                .get(INITIAL_TIMESTAMP_FIELD);
                        String scriptType = "js";
                        if (initalTimestampSettings.containsKey(INITIAL_TIMESTAMP_SCRIPT_TYPE_FIELD)) {
                            scriptType = initalTimestampSettings.get(INITIAL_TIMESTAMP_SCRIPT_TYPE_FIELD).toString();
                        }
                        if (initalTimestampSettings.containsKey(INITIAL_TIMESTAMP_SCRIPT_FIELD)) {
                            /* FIXME later
                            ExecutableScript scriptExecutable = scriptService.executable(scriptType,
                                    initalTimestampSettings.get(INITIAL_TIMESTAMP_SCRIPT_FIELD).toString(), Maps.newHashMap());
                            Object ctx = scriptExecutable.run();
                            logger.trace("initialTimestamp script returned: {}", ctx);
                            if (ctx != null) {
                                long timestamp = Long.parseLong(ctx.toString());
                                timeStamp = new BSONTimestamp((int) (new Date(timestamp).getTime() / 1000), 1);
                            }
                            */
                        }
                    } catch (Throwable t) {
                        logger.warn("Could set initial timestamp:" + t.getMessage());
                    } finally {
                        builder.initialTimestamp(timeStamp);
                    }
                }
            }
            builder.mongoClientOptions(mongoClientOptionsBuilder.build());

            // Credentials
            if (mongoSettings.containsKey(CREDENTIALS_FIELD)) {
                String dbCredential;
                String mau = "";
                String map = "";
                String mlu = "";
                String mlp = "";
                // String mdu = "";
                // String mdp = "";
                Object mongoCredentialsSettings = mongoSettings.get(CREDENTIALS_FIELD);
                boolean array = mongoCredentialsSettings instanceof ArrayList;

                if (array) {
                    ArrayList<Map<String, Object>> credentials = (ArrayList<Map<String, Object>>) mongoCredentialsSettings;
                    for (Map<String, Object> credential : credentials) {
                        dbCredential = Utils.getStringValue(credential.get(DB_FIELD), null);
                        if (ADMIN_DB_FIELD.equals(dbCredential)) {
                            mau = Utils.getStringValue(credential.get(USER_FIELD), null);
                            map = Utils.getStringValue(credential.get(PASSWORD_FIELD), null);
                        } else if (LOCAL_DB_FIELD.equals(dbCredential)) {
                            mlu = Utils.getStringValue(credential.get(USER_FIELD), null);
                            mlp = Utils.getStringValue(credential.get(PASSWORD_FIELD), null);

                        }
                    }
                }
                builder.mongoAdminUser(mau);
                builder.mongoAdminPassword(map);
                builder.mongoLocalUser(mlu);
                builder.mongoLocalPassword(mlp);
            }

            builder.mongoDb(Utils.getStringValue(mongoSettings.get(DB_FIELD), riverName));
            builder.mongoCollection(Utils.getStringValue(mongoSettings.get(COLLECTION_FIELD), riverName));
            builder.mongoGridFS(Utils.getBoolValue(mongoSettings.get(GRIDFS_FIELD), false));
            if (mongoSettings.containsKey(FILTER_FIELD)) {
                String filter = Utils.getStringValue(mongoSettings.get(FILTER_FIELD), "");
                filter = removePrefix("o.", filter);
                builder.mongoCollectionFilter(convertToBasicDBObject(filter));
                builder.mongoOplogFilter(convertToBasicDBObject(removePrefix("o.", filter)));
            }

            if (mongoSettings.containsKey(SCRIPT_FIELD)) {
                String scriptType = "js";
                builder.script(mongoSettings.get(SCRIPT_FIELD).toString());
                if (mongoSettings.containsKey("scriptType")) {
                    scriptType = mongoSettings.get("scriptType").toString();
                } else if (mongoSettings.containsKey(SCRIPT_TYPE_FIELD)) {
                    scriptType = mongoSettings.get(SCRIPT_TYPE_FIELD).toString();
                }
                builder.scriptType(scriptType);
            }
        } else {
            mongoHost = DEFAULT_DB_HOST;
            mongoPort = DEFAULT_DB_PORT;
            try {
                mongoServers.add(new ServerAddress(mongoHost, mongoPort));
                builder.mongoServers(mongoServers);
            } catch (UnknownHostException e) {
                e.printStackTrace();
            }
            builder.mongoDb(riverName);
            builder.mongoCollection(riverName);
        }

        if (settings.containsKey(INDEX_OBJECT)) {
            Map<String, Object> indexSettings = (Map<String, Object>) settings.get(INDEX_OBJECT);
            builder.indexName(Utils.getStringValue(indexSettings.get(NAME_FIELD), builder.mongoDb));
            builder.typeName(Utils.getStringValue(indexSettings.get(TYPE_FIELD), builder.mongoDb));

            Bulk.Builder bulkBuilder = new Bulk.Builder();
            if (indexSettings.containsKey(BULK_FIELD)) {
                Map<String, Object> bulkSettings = (Map<String, Object>) indexSettings.get(BULK_FIELD);
                int bulkActions = Utils.getIntValue(bulkSettings.get(ACTIONS_FIELD), DEFAULT_BULK_ACTIONS);
                bulkBuilder.bulkActions(bulkActions);
                long size = Utils.getLongValue(bulkSettings.get(SIZE_FIELD), DEFAULT_BULK_SIZE);
                bulkBuilder.bulkSize(size);
                bulkBuilder.concurrentRequests(Utils.getIntValue(bulkSettings.get(CONCURRENT_REQUESTS_FIELD), 1));
//                        EsExecutors.boundedNumberOfProcessors(ImmutableSettings.EMPTY)));
                bulkBuilder.flushInterval(Utils.getLongValue(bulkSettings.get(FLUSH_INTERVAL_FIELD), DEFAULT_FLUSH_INTERVAL));
                builder.throttleSize(Utils.getIntValue(indexSettings.get(THROTTLE_SIZE_FIELD), bulkActions * 5));
            } else {
                int bulkActions = Utils.getIntValue(indexSettings.get(BULK_SIZE_FIELD), DEFAULT_BULK_ACTIONS);
                bulkBuilder.bulkActions(bulkActions);
                bulkBuilder.bulkSize(DEFAULT_BULK_SIZE);
                bulkBuilder.flushInterval(Utils.getLongValue(indexSettings.get(BULK_TIMEOUT_FIELD), DEFAULT_FLUSH_INTERVAL));
                bulkBuilder.concurrentRequests(Utils.getIntValue(indexSettings.get(CONCURRENT_BULK_REQUESTS_FIELD),
                        1));
                //EsExecutors.boundedNumberOfProcessors(ImmutableSettings.EMPTY)));
                builder.throttleSize(Utils.getIntValue(indexSettings.get(THROTTLE_SIZE_FIELD), bulkActions * 5));
            }
            builder.bulk(bulkBuilder.build());
        } else {
            builder.indexName(builder.mongoDb);
            builder.typeName(builder.mongoDb);
            builder.bulk(new Bulk.Builder().build());
        }
        return builder.build();
    }

    private static SocketFactory getSSLSocketFactory() {
        SocketFactory sslSocketFactory;
        try {
            final TrustManager[] trustAllCerts = new TrustManager[]{new X509TrustManager() {

                @Override
                public X509Certificate[] getAcceptedIssuers() {
                    return null;
                }

                @Override
                public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
                }

                @Override
                public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
                }
            }};
            final SSLContext sslContext = SSLContext.getInstance("SSL");
            sslContext.init(null, trustAllCerts, new java.security.SecureRandom());
            // Create an ssl socket factory with our all-trusting manager
            sslSocketFactory = sslContext.getSocketFactory();
            return sslSocketFactory;
        } catch (Exception ex) {
            logger.error("Unable to build ssl socket factory without certificate validation, using default instead.", ex);
        }
        return SSLSocketFactory.getDefault();
    }

    static BasicDBObject convertToBasicDBObject(String object) {
        if (object == null || object.length() == 0) {
            return new BasicDBObject();
        } else {
            return (BasicDBObject) JSON.parse(object);
        }
    }

    static String removePrefix(String prefix, String object) {
        return addRemovePrefix(prefix, object, false);
    }

    static String addPrefix(String prefix, String object) {
        return addRemovePrefix(prefix, object, true);
    }

    static String addRemovePrefix(String prefix, String object, boolean add) {
        if (prefix == null) {
            throw new IllegalArgumentException("prefix");
        }
        if (object == null) {
            throw new NullPointerException("object");
        }
        if (object.length() == 0) {
            return "";
        }
        DBObject bsonObject = (DBObject) JSON.parse(object);

        BasicBSONObject newObject = new BasicBSONObject();
        for (String key : bsonObject.keySet()) {
            if (add) {
                newObject.put(prefix + key, bsonObject.get(key));
            } else {
                if (key.startsWith(prefix)) {
                    newObject.put(key.substring(prefix.length()), bsonObject.get(key));
                } else {
                    newObject.put(key, bsonObject.get(key));
                }
            }
        }
        return newObject.toString();
    }

    private MongoDBRiverDefinition(final Builder builder) {
        // river
        this.riverName = builder.riverName;
        this.riverIndexName = builder.riverIndexName;

        // mongodb.servers
        this.mongoServers.addAll(builder.mongoServers);
        // mongodb
        this.mongoDb = builder.mongoDb;
        this.mongoCollection = builder.mongoCollection;
        this.mongoGridFS = builder.mongoGridFS;
        this.mongoOplogFilter = builder.mongoOplogFilter;
        this.mongoCollectionFilter = builder.mongoCollectionFilter;
        // mongodb.credentials
        this.mongoAdminUser = builder.mongoAdminUser;
        this.mongoAdminPassword = builder.mongoAdminPassword;
        this.mongoLocalUser = builder.mongoLocalUser;
        this.mongoLocalPassword = builder.mongoLocalPassword;

        // mongodb.options
        this.mongoClientOptions = builder.mongoClientOptions;
        this.connectTimeout = builder.connectTimeout;
        this.socketTimeout = builder.socketTimeout;
        this.mongoSecondaryReadPreference = builder.mongoSecondaryReadPreference;
        this.mongoUseSSL = builder.mongoUseSSL;
        this.mongoSSLVerifyCertificate = builder.mongoSSLVerifyCertificate;
        this.dropCollection = builder.dropCollection;
        this.excludeFields = builder.excludeFields;
        this.includeFields = builder.includeFields;
        this.includeCollection = builder.includeCollection;
        this.initialTimestamp = builder.initialTimestamp;
        this.script = builder.script;
        this.scriptType = builder.scriptType;
        this.advancedTransformation = builder.advancedTransformation;
        this.skipInitialImport = builder.skipInitialImport;
        this.parentTypes = builder.parentTypes;
        this.storeStatistics = builder.storeStatistics;
        this.statisticsIndexName = builder.statisticsIndexName;
        this.statisticsTypeName = builder.statisticsTypeName;
        this.importAllCollections = builder.importAllCollections;
        this.disableIndexRefresh = builder.disableIndexRefresh;

        // index
        this.indexName = builder.indexName;
        this.typeName = builder.typeName;
        this.throttleSize = builder.throttleSize;

        // bulk
        this.bulk = builder.bulk;
    }

    public List<ServerAddress> getMongoServers() {
        return mongoServers;
    }

    public String getRiverName() {
        return riverName;
    }

    public String getRiverIndexName() {
        return riverIndexName;
    }

    public String getMongoDb() {
        return mongoDb;
    }

    public String getMongoCollection() {
        return mongoCollection;
    }

    public boolean isMongoGridFS() {
        return mongoGridFS;
    }

    public BasicDBObject getMongoOplogFilter() {
        return mongoOplogFilter;
    }

    public BasicDBObject getMongoCollectionFilter() {
        return mongoCollectionFilter;
    }

    public String getMongoAdminUser() {
        return mongoAdminUser;
    }

    public String getMongoAdminPassword() {
        return mongoAdminPassword;
    }

    public String getMongoLocalUser() {
        return mongoLocalUser;
    }

    public String getMongoLocalPassword() {
        return mongoLocalPassword;
    }

    public MongoClientOptions getMongoClientOptions() {
        return mongoClientOptions;
    }

    public int getConnectTimeout() {
        return connectTimeout;
    }

    public int getSocketTimeout() {
        return socketTimeout;
    }

    public boolean isMongoSecondaryReadPreference() {
        return mongoSecondaryReadPreference;
    }

    public boolean isMongoUseSSL() {
        return mongoUseSSL;
    }

    public boolean isMongoSSLVerifyCertificate() {
        return mongoSSLVerifyCertificate;
    }

    public boolean isDropCollection() {
        return dropCollection;
    }

    public Set<String> getExcludeFields() {
        return excludeFields;
    }

    public Set<String> getIncludeFields() {
        return includeFields;
    }

    public String getIncludeCollection() {
        return includeCollection;
    }

    public BSONTimestamp getInitialTimestamp() {
        return initialTimestamp;
    }

    public String getScript() {
        return script;
    }

    public String getScriptType() {
        return scriptType;
    }

    public boolean isAdvancedTransformation() {
        return advancedTransformation;
    }

    public boolean isSkipInitialImport() {
        return skipInitialImport;
    }

    public Set<String> getParentTypes() {
        return parentTypes;
    }

    public boolean isStoreStatistics() {
        return storeStatistics;
    }

    public String getStatisticsIndexName() {
        return statisticsIndexName;
    }

    public String getStatisticsTypeName() {
        return statisticsTypeName;
    }

    public boolean isImportAllCollections() {
        return importAllCollections;
    }

    public boolean isDisableIndexRefresh() {
        return disableIndexRefresh;
    }

    public String getIndexName() {
        return indexName;
    }

    public String getTypeName() {
        return typeName;
    }

    /*
* Default throttle size is: 5 * bulk.bulkActions
*/
    public int getThrottleSize() {
        return throttleSize;
    }

    public String getMongoOplogNamespace() {
        return getMongoDb() + "." + getMongoCollection();
    }

    public Bulk getBulk() {
        return bulk;
    }
}
