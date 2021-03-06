package org.neuinfo.foundry.common.model;

import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import com.mongodb.util.JSON;
import org.json.JSONArray;
import org.json.JSONObject;
import org.neuinfo.foundry.common.provenance.ProvenanceRec;
import org.neuinfo.foundry.common.util.JSONUtils;
import org.neuinfo.foundry.common.util.Utils;
import org.openprovenance.prov.json.Converter;
import org.openprovenance.prov.xml.ProvFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by bozyurt on 5/27/14.
 */
public class Source {
    private String resourceID;
    private String name;
    private String description;
    private String dataSource;
    private JSONObject schema;
    private JSONObject provenance;
    private JSONObject primaryKey;
    private JSONObject ingestConfiguration;
    private JSONObject contentSpecification;
    private List<String> workflowSteps;
    private List<BatchInfo> batchInfos;


    private PrimaryKeyDef primaryKeyDef;

    private Source(Builder builder) {
        this.resourceID = builder.resourceID;
        this.name = builder.name;
        this.dataSource = builder.dataSource;
        this.description = builder.description;
        this.schema = builder.schema;
        this.provenance = builder.provenance;
        this.primaryKey = builder.primaryKey;
        this.batchInfos = builder.batchInfos;
        this.contentSpecification = builder.contentSpecification;
        this.ingestConfiguration = builder.ingestConfiguration;
        this.workflowSteps = builder.workflowSteps;
        this.primaryKeyDef = builder.primaryKeyDef;
    }


    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public PrimaryKeyDef getPrimaryKeyDef() {
        return primaryKeyDef;
    }

    public JSONObject getSchema() {
        return schema;
    }

    public JSONObject getProvenance() {
        return provenance;
    }

    public List<BatchInfo> getBatchInfos() {
        return batchInfos;
    }

    public String getResourceID() {
        return resourceID;
    }

    public String getDataSource() {
        return dataSource;
    }

    public JSONObject getIngestConfiguration() {
        return ingestConfiguration;
    }

    public JSONObject getContentSpecification() {
        return contentSpecification;
    }

    public List<String> getWorkflowSteps() {
        return workflowSteps;
    }

    public void setResourceID(String resourceID) {
        this.resourceID = resourceID;
    }

    public void setDataSource(String dataSource) {
        this.dataSource = dataSource;
    }

    public JSONObject toJSON() {
        JSONObject js = new JSONObject();
        JSONObject siJSON = new JSONObject();
        js.put("sourceInformation", siJSON);
        siJSON.put("resourceID", resourceID);
        siJSON.put("name", name);
        siJSON.put("dataSource", dataSource);
        JSONUtils.add2JSON(siJSON, "description", description);
        if (schema != null) {
            js.put("schema", schema);
        } else {
            js.put("schema", "");
        }
        if (provenance != null) {
            js.put("provenance", provenance);
        } else {
            js.put("provenance", "");
        }
        js.put("ingestConfiguration", this.ingestConfiguration);
        js.put("contentSpecification", this.contentSpecification);

        js.put("originalRecordIdentifierSpec", this.primaryKeyDef.toJSON());

        JSONArray jsArr = new JSONArray();
        js.put("documentProcessing", jsArr);
        for (String wfStep : workflowSteps) {
            jsArr.put(wfStep);
        }

        // js.put("primaryKey", primaryKey);
        jsArr = new JSONArray();
        js.put("batchInfos", jsArr);
        if (batchInfos != null) {
            for (BatchInfo bi : batchInfos) {
                jsArr.put(bi.toJSON());
            }
        }
        return js;
    }


    public static Source fromDBObject(DBObject sourceDBO) {
        DBObject siDBO = (DBObject) sourceDBO.get("sourceInformation");
        String resourceID = (String) siDBO.get("resourceID");
        String name = (String) siDBO.get("name");
        String dataSource = (String) siDBO.get("dataSource");
        String description = (String) siDBO.get("description");
        BasicDBObject o = (BasicDBObject) sourceDBO.get("ingestConfiguration");
        JSONObject icJSON = JSONUtils.toJSON(o, false);
        o = (BasicDBObject) sourceDBO.get("contentSpecification");
        JSONObject csJSON = JSONUtils.toJSON(o, false);


        o = (BasicDBObject) sourceDBO.get("originalRecordIdentifierSpec");
        JSONObject js = JSONUtils.toJSON(o, false);
        PrimaryKeyDef pkDef = PrimaryKeyDef.fromJSON(js);
        BasicDBList dpArr = (BasicDBList) sourceDBO.get("documentProcessing");
        List<String> workflowSteps = new ArrayList<String>(dpArr.size());
        for (int i = 0; i < dpArr.size(); i++) {
            workflowSteps.add((String) dpArr.get(i));
        }

        Builder builder = new Builder(resourceID, name);
        builder.dataSource(dataSource);
        builder.contentSpecification(csJSON).ingestConfiguration(icJSON).workflowSteps(workflowSteps);

        Object schemaObj = sourceDBO.get("schema");

        if (schemaObj != null && schemaObj instanceof  BasicDBObject) {
            BasicDBObject schemaDbo = (BasicDBObject) schemaObj;
            JSONObject schemaJSON = JSONUtils.toJSON(schemaDbo, false);
            //FIXME only $schema and $ref
            if (schemaJSON.has("_$schema")) {
                final Object schema = schemaJSON.remove("_$schema");
                schemaJSON.put("$schema", schema);
            }
            builder.schema(schemaJSON);
        }


        //TODO batchInfos, provenance


        final Source source = builder.description(description).primaryKey(pkDef).build();
        return source;
    }


    public static Source fromJSON(JSONObject json) {
        JSONObject siJS = json.getJSONObject("sourceInformation");
        String resourceID = siJS.getString("resourceID");
        String name = siJS.getString("name");
        String dataSource = siJS.getString("dataSource");
        String description = null;
        if (siJS.has("description")) {
            description = siJS.getString("description");
        }
        JSONObject icJSON = json.getJSONObject("ingestConfiguration");
        JSONObject csJSON = json.getJSONObject("contentSpecification");

        JSONObject js = json.getJSONObject("originalRecordIdentifierSpec");
        PrimaryKeyDef pkDef = PrimaryKeyDef.fromJSON(js);

        JSONArray dpArr = json.getJSONArray("documentProcessing");
        List<String> workflowSteps = new ArrayList<String>(dpArr.length());
        for (int i = 0; i < dpArr.length(); i++) {
            workflowSteps.add(dpArr.getString(i));
        }

        Builder builder = new Builder(resourceID, name);
        builder.dataSource(dataSource);
        builder.contentSpecification(csJSON).ingestConfiguration(icJSON).workflowSteps(workflowSteps);

        //TODO batchInfos, provenance

        final Source source = builder.description(description).primaryKey(pkDef).build();
        return source;
    }

    public static class Builder {
        private String resourceID;
        private String name;
        private String dataSource;
        private String description;
        private JSONObject schema;
        private JSONObject provenance;
        private JSONObject primaryKey;
        private JSONObject ingestConfiguration;
        private JSONObject contentSpecification;
        private List<String> workflowSteps;
        private PrimaryKeyDef primaryKeyDef;
        private List<BatchInfo> batchInfos = new LinkedList<BatchInfo>();

        public Builder(String resourceID, String name) {
            this.resourceID = resourceID;
            this.name = name;
        }

        public Builder dataSource(String dataSource) {
            this.dataSource = dataSource;
            return this;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public Builder ingestConfiguration(JSONObject ingestConfiguration) {
            this.ingestConfiguration = ingestConfiguration;
            return this;
        }

        public Builder contentSpecification(JSONObject contentSpecification) {
            this.contentSpecification = contentSpecification;
            return this;
        }

        public Builder workflowSteps(List<String> workflowSteps) {
            this.workflowSteps = workflowSteps;
            return this;
        }

        public Builder schemaFromFile(String schemaFile) throws IOException {
            String jsonStr = Utils.loadAsString(schemaFile);
            JSONObject js = new JSONObject(jsonStr);
            this.schema = js;
            return this;
        }

        public Builder schema(JSONObject schema) {
            this.schema = schema;
            return this;
        }

        public Builder provenance(ProvenanceRec provRec) throws IOException {
            org.openprovenance.prov.model.ProvFactory pFactory = new ProvFactory();
            final Converter convert = new Converter(pFactory);
            final String jsStr = convert.getString(provRec.getDoc());
            JSONObject js = new JSONObject(jsStr);
            this.provenance = js;
            return this;
        }

        public Builder primaryKey(PrimaryKeyDef pkDef) {
            this.primaryKeyDef = pkDef;
            return this;
        }

        public Builder primaryKey2(String... keyJsonPaths) {
            List<String> keys = new ArrayList<String>(3);
            for (String keyJsonPath : keyJsonPaths) {
                keys.add(keyJsonPath);
            }
            JSONObject js = new JSONObject();
            JSONArray jsArr = new JSONArray();
            for (String key : keys) {
                jsArr.put(key);
            }
            js.put("key", jsArr);

            this.primaryKey = js;
            return this;
        }

        public Builder batchInfos(BatchInfo... batchInfos) {
            for (BatchInfo bi : batchInfos) {
                this.batchInfos.add(bi);
            }
            return this;
        }

        public Source build() {
            return new Source(this);
        }
    }
}
