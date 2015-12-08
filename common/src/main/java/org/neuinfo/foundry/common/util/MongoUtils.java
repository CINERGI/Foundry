package org.neuinfo.foundry.common.util;

import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import org.neuinfo.foundry.common.model.Source;

/**
 * Created by bozyurt on 11/4/15.
 */
public class MongoUtils {
    public static Source getSource(BasicDBObject query, DBCollection sources) {
        final DBCursor cursor = sources.find(query);

        Source source = null;
        try {
            if (cursor.hasNext()) {
                DBObject dbObject = cursor.next();
                source = Source.fromDBObject(dbObject);
            }
        } finally {
            cursor.close();
        }
        return source;
    }
}
