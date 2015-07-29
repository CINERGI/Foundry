package org.neuinfo.foundry.river;

import com.mongodb.*;
import com.mongodb.gridfs.GridFS;
import com.mongodb.gridfs.GridFSDBFile;
import org.apache.log4j.Logger;
import org.bson.BasicBSONObject;
import org.bson.types.BSONTimestamp;
import org.bson.types.ObjectId;
import org.neuinfo.foundry.river.util.MongoDBHelper;
import org.neuinfo.foundry.common.util.Utils;

import java.util.HashSet;
import java.util.Set;

/**
 * Created by bozyurt on 4/5/14.
 */
public class OplogProcessor {
    static Logger logger = Logger.getLogger(OplogProcessor.class);
    private final String gridfsOplogNamespace;
    private final String cmdOplogNamespace;
    private final DB db;
    private final MongoDBRiverDefinition definition;
    private final Mongo mongo;
    private Context context;
    private Set<String> oplogOperations = new HashSet<String>(17);

    public OplogProcessor(MongoDBRiverDefinition definition, Context context, DB db) {
        this.definition = definition;
        this.context = context;
        this.db = db;
        this.mongo = db.getMongo();
        this.gridfsOplogNamespace = definition.getMongoOplogNamespace() + MongoDBRiver.GRIDFS_FILES_SUFFIX;
        this.cmdOplogNamespace = definition.getMongoDb() + "." + MongoDBRiver.OPLOG_NAMESPACE_COMMAND;
        oplogOperations.add(MongoDBRiver.OPLOG_DELETE_OPERATION);
        oplogOperations.add(MongoDBRiver.OPLOG_UPDATE_OPERATION);
        oplogOperations.add(MongoDBRiver.OPLOG_INSERT_OPERATION);
        oplogOperations.add(MongoDBRiver.OPLOG_COMMAND_OPERATION);

    }

    public void processEntry(final DBObject entry, final BSONTimestamp startTimestamp) throws InterruptedException {
        if (!isValidEntry(entry, startTimestamp)) {
            return;
        }
        Operation operation = Operation.fromString(entry.get(MongoDBRiver.OPLOG_OPERATION).toString());
        String namespace = entry.get(MongoDBRiver.OPLOG_NAMESPACE).toString();
        String collection = null;
        BSONTimestamp oplogTimestamp = (BSONTimestamp) entry.get(MongoDBRiver.OPLOG_TIMESTAMP);
        DBObject object = (DBObject) entry.get(MongoDBRiver.OPLOG_OBJECT);
        if (definition.isImportAllCollections()) {
            if (namespace.startsWith(definition.getMongoDb()) && !namespace.equals(cmdOplogNamespace)) {
                collection = getCollectionFromNamespace(namespace);
            }
        } else {
            collection = definition.getMongoCollection();
        }

        if (namespace.equals(cmdOplogNamespace)) {
            if (object.containsField(MongoDBRiver.OPLOG_DROP_COMMAND_OPERATION)) {
                operation = Operation.DROP_COLLECTION;
                if (definition.isImportAllCollections()) {
                    collection = object.get(MongoDBRiver.OPLOG_DROP_COMMAND_OPERATION).toString();
                    if (collection.startsWith("tmp.mr.")) {
                        return;
                    }
                }
            }
            if (object.containsField(MongoDBRiver.OPLOG_DROP_DATABASE_COMMAND_OPERATION)) {
                operation = Operation.DROP_DATABASE;
            }
        }

        logger.trace(String.format("namespace: %s - operation: %s", namespace, operation));
        if (namespace.equals(MongoDBRiver.OPLOG_ADMIN_COMMAND)) {
            if (operation == Operation.COMMAND) {
                processAdminCommandOplogEntry(entry, startTimestamp);
                return;
            }
        }

        if (logger.isTraceEnabled()) {
            logger.trace("MongoDB object deserialized: " + object.toString());
        }

        if (logger.isDebugEnabled()) {
            logger.debug("collection: " + collection);
            logger.debug(String.format("oplog entry - namespace [%s], operation [%s]", namespace, operation));
            logger.debug("oplog processing item " + entry);
        }
        String objectId = getObjectIdFromOplogEntry(entry);
        if (definition.isMongoGridFS() && namespace.endsWith(MongoDBRiver.GRIDFS_FILES_SUFFIX)
                && (operation == Operation.INSERT || operation == Operation.UPDATE)) {
            if (objectId == null) {
                throw new NullPointerException(MongoDBRiver.MONGODB_ID_FIELD);
            }
            GridFS grid = new GridFS(mongo.getDB(definition.getMongoDb()), collection);
            GridFSDBFile file = grid.findOne(new ObjectId(objectId));
            if (file != null) {
                logger.info(String.format("Caught file: %s - %s", file.getId(), file.getFilename()));
                object = file;
            } else {
                logger.warn("Cannot find file from id: " + objectId);
            }
        }
        if (object instanceof GridFSDBFile) {
            if (objectId == null) {
                throw new NullPointerException(MongoDBRiver.MONGODB_ID_FIELD);
            }
            logger.debug("Add attachment: " + objectId);
            addToStream(operation, oplogTimestamp,
                    MongoDBHelper.applyFieldFilter(object, definition.getIncludeFields(),
                            definition.getExcludeFields()), collection
            );
        } else {
            if (operation == Operation.UPDATE) {
                DBObject update = (DBObject) entry.get(MongoDBRiver.OPLOG_UPDATE);
                logger.debug("Updated item: " + update);
                addQueryToStream(operation, oplogTimestamp, update, collection);
            } else {
                addToStream(operation, oplogTimestamp,
                        MongoDBHelper.applyFieldFilter(object, definition.getIncludeFields(),
                                definition.getExcludeFields()), collection
                );
            }
        }

    }

    private void addQueryToStream(Operation operation, BSONTimestamp oplogTimestamp, DBObject update, String collection) {
        // TODO Auto-generated method stub

    }

    private void addToStream(Operation operation, BSONTimestamp oplogTimestamp, DBObject applyFieldFilter, String collection) {
        // TODO Auto-generated method stub

    }

    private void processAdminCommandOplogEntry(final DBObject entry,
                                               final BSONTimestamp startTimestamp) throws InterruptedException {
        logger.debug(String.format("processAdminCommandOplogEntry - [%s]", entry));
        DBObject object = (DBObject) entry.get(MongoDBRiver.OPLOG_OBJECT);
        if (definition.isImportAllCollections()) {
            if (object.containsField(MongoDBRiver.OPLOG_RENAME_COLLECTION_COMMAND_OPERATION) && object.containsField(MongoDBRiver.OPLOG_TO)) {
                String to = object.get(MongoDBRiver.OPLOG_TO).toString();
                if (to.startsWith(definition.getMongoDb())) {
                    String newCollection = getCollectionFromNamespace(to);
                    DBCollection coll = db.getCollection(newCollection);
                    doInitialImport(coll);
                }
            }
        }
    }

    private void doInitialImport(DBCollection coll) {
        // TODO Auto-generated method stub

    }

    private String getCollectionFromNamespace(String namespace) {
        if (namespace.startsWith(definition.getMongoDb()) && Utils.numOfCharsIn(namespace, '.') == 1) {
            return namespace.substring(definition.getMongoDb().length() + 1);
        }
        logger.info(String.format("Cannot get collection from namespace [%s]", namespace));
        return null;
    }

    private boolean isValidEntry(final DBObject entry, final BSONTimestamp startTimestamp) {
        String namespace = (String) entry.get(MongoDBRiver.OPLOG_NAMESPACE);
        // Initial support for sharded collection -
        // https://jira.mongodb.org/browse/SERVER-4333
        // Not interested in operation from migration or sharding
        if (entry.containsField(MongoDBRiver.OPLOG_FROM_MIGRATE) &&
                ((BasicBSONObject) entry).getBoolean(MongoDBRiver.OPLOG_FROM_MIGRATE)) {
            logger.trace(String.format("From migration or sharding operation. Can be ignored. {%s}", entry));
            return false;
        }
        // Not interested by chunks - skip all
        if (namespace.endsWith(MongoDBRiver.GRIDFS_CHUNKS_SUFFIX)) {
            return false;
        }

        if (startTimestamp != null) {
            BSONTimestamp oplogTimestamp = (BSONTimestamp) entry.get(MongoDBRiver.OPLOG_TIMESTAMP);
            if (oplogTimestamp.compareTo(startTimestamp) < 0) {
                return false;
            }
        }

        boolean validNamespace = false;
        if (definition.isMongoGridFS()) {
            validNamespace = gridfsOplogNamespace.equals(namespace);
        } else {
            if (definition.isImportAllCollections()) {
                // Skip temp entry generated by map / reduce
                if (namespace.startsWith(definition.getMongoDb()) &&
                        !namespace.startsWith(definition.getMongoDb() + ".tmp.mr")) {
                    validNamespace = true;
                }
            } else {
                if (definition.getMongoOplogNamespace().equals(namespace)) {
                    validNamespace = true;
                }
            }
            if (cmdOplogNamespace.equals(namespace)) {
                validNamespace = true;
            }

            if (MongoDBRiver.OPLOG_ADMIN_COMMAND.equals(namespace)) {
                validNamespace = true;
            }
        }
        if (!validNamespace) {
            return false;
        }
        String operation = (String) entry.get(MongoDBRiver.OPLOG_OPERATION);
        if (!oplogOperations.contains(operation)) {
            return false;
        }

        // TODO: implement a better solution
        if (definition.getMongoOplogFilter() != null) {
            DBObject object = (DBObject) entry.get(MongoDBRiver.OPLOG_OBJECT);
            BasicDBObject filter = definition.getMongoOplogFilter();
            if (!filterMatch(filter, object)) {
                return false;
            }
        }
        return true;
    }

    private boolean filterMatch(DBObject filter, DBObject object) {
        for (String key : filter.keySet()) {
            if (!object.containsField(key)) {
                return false;
            }
            if (!filter.get(key).equals(object.get(key))) {
                return false;
            }
        }
        return true;
    }

    /*
     * Extract "_id" from "o" if it fails try to extract from "o2"
     */
    private String getObjectIdFromOplogEntry(DBObject entry) {
        if (entry.containsField(MongoDBRiver.OPLOG_OBJECT)) {
            DBObject object = (DBObject) entry.get(MongoDBRiver.OPLOG_OBJECT);
            if (object.containsField(MongoDBRiver.MONGODB_ID_FIELD)) {
                return object.get(MongoDBRiver.MONGODB_ID_FIELD).toString();
            }
        }
        if (entry.containsField(MongoDBRiver.OPLOG_UPDATE)) {
            DBObject object = (DBObject) entry.get(MongoDBRiver.OPLOG_UPDATE);
            if (object.containsField(MongoDBRiver.MONGODB_ID_FIELD)) {
                return object.get(MongoDBRiver.MONGODB_ID_FIELD).toString();
            }
        }
        logger.trace(String.format("Oplog entry %s", entry));
        return null;
    }
}
