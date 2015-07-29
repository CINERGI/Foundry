package org.neuinfo.foundry.river.util;

import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import com.mongodb.gridfs.GridFSDBFile;
import com.mongodb.gridfs.GridFSFile;
import org.apache.commons.codec.binary.Base64;
import org.joda.time.DateTimeZone;
import org.joda.time.format.ISODateTimeFormat;
import org.json.JSONObject;
import org.neuinfo.foundry.common.util.Utils;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;
/*
* Licensed to Elastic Search and Shay Banon under one
* or more contributor license agreements. See the NOTICE file
* distributed with this work for additional information
* regarding copyright ownership. Elastic Search licenses this
* file to you under the Apache License, Version 2.0 (the
* "License"); you may not use this file except in compliance
* with the License. You may obtain a copy of the License at
*
* http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing,
* software distributed under the License is distributed on an
* "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
* KIND, either express or implied. See the License for the
* specific language governing permissions and limitations
* under the License.
*/

/**
 * Created by bozyurt on 4/4/14.
 */
public class MongoDBHelper {

    public static JSONObject serialize(GridFSDBFile file) throws Exception {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();

        int nRead;
        byte[] data = new byte[1024];

        InputStream stream = null;

        try {
            stream = file.getInputStream();
            while ((nRead = stream.read(data, 0, data.length)) != -1) {
                buffer.write(data, 0, nRead);
            }

            buffer.flush();
        } finally {
            if (stream != null) {
                stream.close();
            }
        }
        String encodedContent = Base64.encodeBase64String(buffer.toByteArray());

        // Probably not necessary...
        buffer.close();

        JSONObject json = new JSONObject();
        JSONObject contentJS = new JSONObject();

        contentJS.put("content_type", file.getContentType());
        contentJS.put("title", file.getFilename());
        contentJS.put("content", encodedContent);
        json.put("content", contentJS);
        //builder.endObject();
        json.put("filename", file.getFilename());
        json.put("contentType", file.getContentType());
        json.put("md5", file.getMD5());
        json.put("length", file.getLength());
        json.put("chunkSize", file.getChunkSize());
        json.put("uploadDate", file.getUploadDate());
        JSONObject mdJS = new JSONObject();
        json.put("metadata", mdJS);
        // builder.startObject("metadata");
        DBObject metadata = file.getMetaData();
        if (metadata != null) {
            for (String key : metadata.keySet()) {
                mdJS.put(key, metadata.get(key));
            }
        }

        return json;
    }

    public static DBObject applyExcludeFields(DBObject bsonObject, Set<String> excludeFields) {
        if (excludeFields == null) {
            return bsonObject;
        }

        DBObject filteredObject = bsonObject;
        for (String field : excludeFields) {
            if (field.contains(".")) {
                String rootObject = field.substring(0, field.indexOf("."));
                String childObject = field.substring(field.indexOf(".") + 1);
                if (filteredObject.containsField(rootObject)) {
                    Object object = filteredObject.get(rootObject);
                    if (object instanceof DBObject) {
                        DBObject object2 = (DBObject) object;
                        applyExcludeFields(object2, new HashSet<String>(Arrays.asList(childObject)));
                    }
                }
            } else {
                if (filteredObject.containsField(field)) {
                    filteredObject.removeField(field);
                }
            }
        }
        return filteredObject;
    }


    private static Set<String> getChildItems(String parent, final Set<String> fields) {
        Set<String> children = new HashSet<String>();
        for (String field : fields) {
            if (field.startsWith(parent + ".")) {
                children.add(field.substring((parent + ".").length()));
            } else if (field.startsWith(parent)) {
                children.add(field);
            }
        }
        return children;
    }

    public static DBObject applyIncludeFields(DBObject bsonObject, final Set<String> includeFields) {
        if (includeFields == null) {
            return bsonObject;
        }

        DBObject filteredObject = new BasicDBObject();

        for (String field : bsonObject.keySet()) {
            if (includeFields.contains(field)) {
                filteredObject.put(field, bsonObject.get(field));
            }
        }

        for (String field : includeFields) {
            if (field.contains(".")) {
                String rootObject = field.substring(0, field.indexOf("."));
                Object object = bsonObject.get(rootObject);
                if (object instanceof DBObject) {
                    DBObject object2 = (DBObject) object;
                    System.out.println(getChildItems(rootObject, includeFields));
                    object2 = applyIncludeFields(object2, getChildItems(rootObject, includeFields));

                    filteredObject.put(rootObject, object2);
                }
            }
        }
        return filteredObject;
    }

    public static String getRiverVersion() {
        String version = "Undefined";
        try {
            Properties props = Utils.loadProperties("es-build.properties");
            //String properties = Streams.copyToStringFromClasspath("/org/elasticsearch/river/mongodb/es-build.properties");
            String ver = props.getProperty("version", "undefined");
            String hash = props.getProperty("hash", "undefined");
            if (!"undefined".equals(hash)) {
                hash = hash.substring(0, 7);
            }
            String timestamp = "undefined";
            String gitTimestampRaw = props.getProperty("timestamp");
            if (gitTimestampRaw != null) {
               timestamp = ISODateTimeFormat.dateTimeNoMillis().withZone(DateTimeZone.UTC).print(Long.parseLong(gitTimestampRaw));
            }
            version = String.format("version[%s] - hash[%s] - time[%s]", ver, hash, timestamp);
        } catch (Exception ex) {
        }
        return version;

    }

    public static DBObject applyFieldFilter(DBObject object, final Set<String> includeFields, final Set<String> excludeFields) {
        if (object instanceof GridFSFile) {
            GridFSFile file = (GridFSFile) object;
            DBObject metadata = file.getMetaData();
            if (metadata != null) {
                file.setMetaData(applyFieldFilter(metadata, includeFields, excludeFields));
            }
        } else {
            object = MongoDBHelper.applyExcludeFields(object, excludeFields);
            object = MongoDBHelper.applyIncludeFields(object, includeFields);
        }
        return object;
    }
}
