package org.neuinfo.foundry.river;

import com.mongodb.DBObject;
import com.mongodb.gridfs.GridFSDBFile;
import org.bson.types.BSONTimestamp;

/**
* Created by bozyurt on 4/24/14.
*/
public class QueueEntry {

    private final DBObject data;
    private final Operation operation;
    private final BSONTimestamp oplogTimestamp;
    private final String collection;

    public QueueEntry(DBObject data, String collection) {
        this(null, Operation.INSERT, data, collection);
    }

    public QueueEntry(BSONTimestamp oplogTimestamp, Operation oplogOperation, DBObject data, String collection) {
        this.data = data;
        this.operation = oplogOperation;
        this.oplogTimestamp = oplogTimestamp;
        this.collection = collection;
    }

    public boolean isOplogEntry() {
        return oplogTimestamp != null;
    }

    public boolean isAttachment() {
        return (data instanceof GridFSDBFile);
    }

    public DBObject getData() {
        return data;
    }

    public Operation getOperation() {
        return operation;
    }

    public BSONTimestamp getOplogTimestamp() {
        return oplogTimestamp;
    }

    public String getCollection() {
        return collection;
    }
}
