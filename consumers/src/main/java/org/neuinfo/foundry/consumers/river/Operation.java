package org.neuinfo.foundry.consumers.river;

/**
 * Created by bozyurt on 4/4/14.
 */
public enum Operation {

    INSERT(MongoDBRiverDefinition.OPLOG_INSERT_OPERATION),
    UPDATE(MongoDBRiverDefinition.OPLOG_UPDATE_OPERATION),
    DELETE(MongoDBRiverDefinition.OPLOG_DELETE_OPERATION),
    DROP_COLLECTION(
            "dc"), DROP_DATABASE("dd"),
    COMMAND(MongoDBRiverDefinition.OPLOG_COMMAND_OPERATION), UNKNOWN(null);

    private String value;

    private Operation(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static Operation fromString(String value) {
        if (value != null) {
            for (Operation operation : Operation.values()) {
                if (value.equalsIgnoreCase(operation.getValue())) {
                    return operation;
                }
            }
        }
        return Operation.UNKNOWN;
    }
}