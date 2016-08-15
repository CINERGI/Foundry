package org.neuinfo.foundry.common.util;

import com.mongodb.*;
import org.bson.types.ObjectId;
import org.json.JSONArray;
import org.json.JSONObject;
import org.neuinfo.foundry.common.config.IMongoConfig;
import org.neuinfo.foundry.common.config.ServerInfo;
import org.neuinfo.foundry.common.ingestion.BaseIngestionService;
import org.neuinfo.foundry.common.ingestion.Configuration;
import org.neuinfo.foundry.common.ingestion.SourceIngestionService;
import org.neuinfo.foundry.common.model.DocWrapper;
import org.neuinfo.foundry.common.model.Source;

import java.io.BufferedWriter;
import java.net.UnknownHostException;
import java.sql.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by bozyurt on 7/26/16.
 */
public class KeywordsEditor extends BaseIngestionService {
    ChangedRecordRegistry registry;
    Map<String, Source> sourceMap;

    public KeywordsEditor() throws Exception {
        registry = new ChangedRecordRegistry();
    }

    public void shutdown() {
        registry.shutdown();
        super.shutdown();
    }

    @Override
    public void start(IMongoConfig conf) throws UnknownHostException {
        super.start(conf);
        SourceIngestionService sis = new SourceIngestionService();
        try {
            sis.start(conf);
            List<Source> sources = sis.getAllSources();
            sourceMap = new HashMap<String, Source>();
            for (Source s : sources) {
                sourceMap.put(s.getResourceID(), s);
            }
        } finally {
            sis.shutdown();
        }

    }

    public List<DocWrapperInfo> findRecordsMatching(Filter filter, int limit) {
        BasicDBObject query = prepQuery(filter);

        BasicDBObject keys = new BasicDBObject("_id", 1)
                .append("primaryKey", 1).append("SourceInfo", 1);
        DB db = mongoClient.getDB(dbName);
        DBCollection records = db.getCollection("records");
        DBCursor cursor;
        if (limit > 0) {
            cursor = records.find(query, keys).limit(limit);
        } else {
            cursor = records.find(query, keys);
        }
        List<DocWrapperInfo> oids = new LinkedList<DocWrapperInfo>();
        try {
            while (cursor.hasNext()) {
                BasicDBObject dbo = (BasicDBObject) cursor.next();
                ObjectId oid = (ObjectId) dbo.get("_id");
                String primaryKey = (String) dbo.get("primaryKey");
                BasicDBObject si = (BasicDBObject) dbo.get("SourceInfo");
                String sourceID = si.getString("SourceID");
                DocWrapperInfo dwi = new DocWrapperInfo(oid, primaryKey, sourceID);
                oids.add(dwi);
            }
            return oids;
        } finally {
            cursor.close();
        }
    }

    public List<String> getPrimaryKeys4MatchingRecords(Filter filter) {
        BasicDBObject query = prepQuery(filter);
        BasicDBObject keys = new BasicDBObject("primaryKey", 1);
        DB db = mongoClient.getDB(dbName);
        DBCollection records = db.getCollection("records");
        DBCursor cursor = records.find(query, keys).limit(10);
        List<String> primaryKeys = new LinkedList<String>();
        try {
            while (cursor.hasNext()) {
                BasicDBObject dbo = (BasicDBObject) cursor.next();
                String primaryKey = (String) dbo.get("primaryKey");
                primaryKeys.add(primaryKey);
            }
            return primaryKeys;
        } finally {
            cursor.close();
        }
    }

    public static class DocWrapperInfo {
        ObjectId oid;
        String primaryKey;
        String sourceID;

        public DocWrapperInfo(ObjectId oid, String primaryKey, String sourceID) {
            this.oid = oid;
            this.primaryKey = primaryKey;
            this.sourceID = sourceID;
        }

        @Override
        public String toString() {
            final StringBuilder sb = new StringBuilder("DocWrapperInfo{");
            sb.append("oid=").append(oid);
            sb.append(", primaryKey='").append(primaryKey).append('\'');
            sb.append(", sourceID='").append(sourceID).append('\'');
            sb.append('}');
            return sb.toString();
        }
    }

    public BasicDBObject prepQuery(Filter filter) {
        BasicDBObject query;
        if (filter.getType() == FilterType.EXACT) {
            query = new BasicDBObject("Data.enhancedKeywords." + filter.getFieldName(), filter.getValue());
        } else if (filter.getType() == FilterType.STARTS_WITH) {
            Pattern p = Pattern.compile("^" + Pattern.quote(filter.getValue()) + ".+");
            query = new BasicDBObject("Data.enhancedKeywords." + filter.getFieldName(), p);
        } else if (filter.getType() == FilterType.REGEX) {
            Pattern p = Pattern.compile(Pattern.quote(filter.getValue()));
            query = new BasicDBObject("Data.enhancedKeywords." + filter.getFieldName(), p);
        } else {
            throw new RuntimeException("Unsupported filter type!");
        }
        return query;
    }

    public void prep4Reprocessing(ObjectId oid) {
        BasicDBObject query = new BasicDBObject("_id", oid);
        DB db = mongoClient.getDB(dbName);
        DBCollection records = db.getCollection("records");
        final DBCursor cursor = records.find(query);
        BasicDBObject docWrapper = null;
        try {
            if (cursor.hasNext()) {
                docWrapper = (BasicDBObject) cursor.next();
            }
        } finally {
            cursor.close();
        }
        if (docWrapper != null) {
            BasicDBObject proc = (BasicDBObject) docWrapper.get("Processing");
            proc.put("status", "new.1");
            docWrapper.remove("History");
            docWrapper.remove("Data");
        }
    }

    public void createIncludeList(List<DocWrapperInfo> dwiList, String includeListFile) throws Exception {
        BufferedWriter in = null;
        try {
            in = Utils.newUTF8CharSetWriter(includeListFile);
            for(DocWrapperInfo dwi : dwiList) {
                BasicDBObject docWrapper = getDoc(dwi);
                String primaryKey = docWrapper.getString("primaryKey");
                String url = registry.getURL(primaryKey);
                if (url != null) {
                    in.write(url);
                    in.newLine();
                }
            }
        } finally {
            Utils.close(in);
        }
    }

    private BasicDBObject getDoc(DocWrapperInfo dwi) {
        BasicDBObject query = new BasicDBObject("_id", dwi.oid);
        DB db = mongoClient.getDB(dbName);
        DBCollection records = db.getCollection("records");
        final DBCursor cursor = records.find(query);
        BasicDBObject docWrapper = null;
        try {
            if (cursor.hasNext()) {
                docWrapper = (BasicDBObject) cursor.next();
            }
        } finally {
            cursor.close();
        }
        return docWrapper;
    }

    public void showEnhancedKeywords(DocWrapperInfo dwi, Filter filter, List<UpdateInfo> uiList, DeleteInfo di, boolean changeDB) throws SQLException {
        BasicDBObject docWrapper = getDoc(dwi);
        DB db = mongoClient.getDB(dbName);
        BasicDBObject query = new BasicDBObject("_id", dwi.oid);
        DBCollection records = db.getCollection("records");

        BasicDBObject dbo = (BasicDBObject) docWrapper.get("Data");
        JSONObject json = JSONUtils.toJSON(dbo, false);
        if (json.has("enhancedKeywords")) {
            JSONArray jsArr = json.getJSONArray("enhancedKeywords");
            if (filter == null) {
                System.out.println(jsArr.toString(2));
            } else {
                boolean updated = false;
                JSONObject foundJS = null;
                List<JSONObject> toBeDeleted = new ArrayList<JSONObject>(2);
                for (int i = 0; i < jsArr.length(); i++) {
                    JSONObject js = jsArr.getJSONObject(i);
                    if (js.has(filter.getFieldName())) {
                        String value = js.getString(filter.getFieldName());
                        if (filter.getType() == FilterType.EXACT) {
                            if (value.equals(filter.getValue())) {
                                foundJS = js;
                            }
                        } else if (filter.getType() == FilterType.STARTS_WITH) {
                            if (value.startsWith(filter.getValue())) {
                                foundJS = js;
                            }
                        } else if (filter.getType() == FilterType.REGEX) {
                            if (value.matches(filter.getValue())) {
                                foundJS = js;
                            }
                        }
                    }
                    if (foundJS != null) {

                        boolean delete = false;
                        if (uiList != null) {
                            for (UpdateInfo ui : uiList) {
                                updated |= ui.update(foundJS);
                            }
                        }
                        if (di != null) {
                            delete = di.isDeleteable(foundJS);
                            toBeDeleted.add(foundJS);
                            updated |= true;
                        }
                        if (delete) {
                            System.out.println("**** TO BE DELETED ****");
                        }
                        System.out.println(foundJS.toString(2));

                        foundJS = null;
                    }
                }
                if (!toBeDeleted.isEmpty()) {
                    Set<JSONObject> set = new HashSet<JSONObject>(toBeDeleted);
                    JSONArray jsArr2 = new JSONArray();
                    for (int i = 0; i < jsArr.length(); i++) {
                        JSONObject js = jsArr.getJSONObject(i);
                        if (!set.contains(js)) {
                            jsArr2.put(js);
                        }
                    }
                    json.put("enhancedKeywords", jsArr2);
                    dbo.put("enhancedKeywords", JSONUtils.encode(jsArr2, true));
                } else if (updated) {
                    dbo.put("enhancedKeywords", JSONUtils.encode(jsArr, true));
                }

                if (updated) {
                    System.out.println("updating record");
                    if (changeDB) {
                        registry.addDWI(dwi);
                        records.update(query, docWrapper, false, false, WriteConcern.SAFE);
                    }
                }

            }
        }
    }

    public enum FilterType {
        EXACT, STARTS_WITH, REGEX
    }

    public static class Filter {
        String fieldName;
        String value;
        FilterType type = FilterType.EXACT;

        public Filter(String fieldName, String value, FilterType type) {
            this.fieldName = fieldName;
            this.value = value;
            this.type = type;
        }

        public Filter(String fieldName, String value) {
            this(fieldName, value, FilterType.EXACT);
        }

        public String getFieldName() {
            return fieldName;
        }

        public String getValue() {
            return value;
        }

        public FilterType getType() {
            return type;
        }
    }//;

    public interface IChange {
        public String change(String newValue, String outPattern, String oldValue);
    }

    public static class DeleteInfo {
        String deleteFieldName;
        String deletePattern;
        boolean exact = false;

        public DeleteInfo(String deleteFieldName, String deletePattern, boolean exact) {
            this.deleteFieldName = deleteFieldName;
            this.deletePattern = deletePattern;
            this.exact = exact;
        }

        public boolean isDeleteable(JSONObject json) {
            if (json.has(deleteFieldName)) {
                String value = json.getString(deleteFieldName);
                if (value == null) {
                    return false;
                }
                if (exact) {
                    return value.equals(deletePattern);
                } else {
                    Pattern p = Pattern.compile(deletePattern);
                    Matcher m = p.matcher(value);
                    return m.find();
                }
            }
            return false;
        }

        public String getDeleteFieldName() {
            return deleteFieldName;
        }

        public String getDeletePattern() {
            return deletePattern;
        }

        public boolean isExact() {
            return exact;
        }
    }

    public static class UpdateInfo {
        String inFieldName;
        String inPattern;
        String outPattern;
        boolean exactOut = false;
        String updateFieldName;
        IChange changer;

        public UpdateInfo(String inFieldName, String inPattern, String outPattern, boolean exactOut,
                          String updateFieldName, IChange changer) {
            this.inFieldName = inFieldName;
            this.inPattern = inPattern;
            this.outPattern = outPattern;
            this.exactOut = exactOut;
            this.updateFieldName = updateFieldName;
            this.changer = changer;
        }

        public boolean update(JSONObject json) {
            if (exactOut) {
                Assertion.assertNotNull(outPattern);
                if (json.has(updateFieldName)) {
                    json.put(updateFieldName, outPattern);
                    return true;
                }
            } else {
                String newValue = null;
                if (inFieldName != null && json.has(inFieldName)) {
                    String inValue = json.getString(inFieldName);
                    if (inPattern != null) {
                        Pattern p = Pattern.compile(inPattern);
                        Matcher matcher = p.matcher(inValue);
                        if (matcher.find()) {
                            newValue = matcher.group(1);
                        }
                    }
                }
                String oldValue = json.getString(updateFieldName);
                String updatedValue = changer.change(newValue, outPattern, oldValue);
                if (!oldValue.equals(updatedValue)) {
                    json.put(updateFieldName, updatedValue);
                    return true;
                }
            }
            return false;
        }
    }//;


    public static void main(String[] args) throws Exception {
        //Configuration conf = new Configuration("cinergiStage");
        Configuration conf = new Configuration("discotest");
        ServerInfo si = new ServerInfo("localhost", 27017);
        si = new ServerInfo("132.249.238.128", 27017);
        conf.addServer(si);
        KeywordsEditor ke = new KeywordsEditor();

        IChange changer = new IChange() {
            @Override
            public String change(String newValue, String outPattern, String oldValue) {
                if (outPattern != null && outPattern.indexOf("${0}") != -1) {
                    if (newValue == null) {
                        return oldValue;
                    }
                    return outPattern.replaceAll("\\$\\{0\\}", newValue);
                }
                return newValue;
            }
        };
        UpdateInfo ui = new UpdateInfo("hierarchyPath", "> ([^>]+)$",
                "Eon > Era > Period > Epoch > ${0}", false, "hierarchyPath", changer);

        UpdateInfo ui2 = new UpdateInfo("term", "(.+ou$)", "${0}s", false, "term", changer);
        DeleteInfo di = new DeleteInfo("category", "(Property|Activity) .+", false);
        try {
            ke.start(conf);

            Filter filter = new Filter("term", "Elevation");
            //filter = new Filter("term", "Paleozoic");
            // filter = new Filter("term", "Paleo", FilterType.STARTS_WITH);
            filter = new Filter("category", "Geologic Time", FilterType.STARTS_WITH);
            // filter = new Filter("term", ".+si$", FilterType.REGEX);
            // filter = new Filter("term", "Creta", FilterType.STARTS_WITH);

            List<DocWrapperInfo> dwiList = ke.findRecordsMatching(filter, -1);
            int count = 0;
            for (DocWrapperInfo dwi : dwiList) {
                System.out.println(dwi);
                //ke.showEnhancedKeywords(oid, filter, Arrays.asList(ui2), null);
                // ke.showEnhancedKeywords(dwi, filter, null, di, true);
                ke.showEnhancedKeywords(dwi, filter, null, null, false);

                count++;
                System.out.println("processed " + count + " out of " + dwiList.size());
            }
            ke.createIncludeList(dwiList, "/tmp/geo_time_include_list.txt");
        } finally {
            ke.shutdown();
        }

    }

}
