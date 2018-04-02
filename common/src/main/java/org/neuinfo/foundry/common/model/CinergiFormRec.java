package org.neuinfo.foundry.common.model;

import org.apache.commons.lang.StringEscapeUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.LinkedList;
import java.util.List;

/**
 * Created by bozyurt on 8/12/15.
 */
public class CinergiFormRec {
    private String resourceType;
    private String resourceTitle;
    private String abstractText;
    private String resourceURL;
    private String individualName;
    private String contactEmail;
    private String definingCitation;
    private String resourceContributor;
    private String alternateTitle;
    private List<String> geoscienceSubdomains = new LinkedList<String>();
    private List<String> equipments = new LinkedList<String>();
    private List<String> methods = new LinkedList<String>();
    private List<String> earthProcesses = new LinkedList<String>();
    private List<String> describedFeatures = new LinkedList<String>();
    private List<String> otherTags = new LinkedList<String>();
    private List<String> placeNames = new LinkedList<String>();
    private TemporalExtent temporalExtent;
    private List<SpatialExtent> spatialExtents = new LinkedList<SpatialExtent>();
    private String fileFormat;
    private String lineage;
    private GeologicAge geologicAge;

    public String getFileFormat() {
        return fileFormat;
    }

    public void setFileFormat(String fileFormat) {
        this.fileFormat = fileFormat;
    }

    public String getLineage() {
        return lineage;
    }

    public void setLineage(String lineage) {
        this.lineage = lineage;
    }

    public GeologicAge getGeologicAge() {
        return geologicAge;
    }

    public void setGeologicAge(GeologicAge geologicAge) {
        this.geologicAge = geologicAge;
    }

    public String getResourceType() {
        return resourceType;
    }

    public void setResourceType(String resourceType) {
        this.resourceType = resourceType;
    }

    public String getResourceTitle() {
        return resourceTitle;
    }

    public void setResourceTitle(String resourceTitle) {
        this.resourceTitle = resourceTitle;
    }

    public String getAbstractText() {
        return abstractText;
    }

    public void setAbstractText(String abstractText) {
        this.abstractText = abstractText;
    }

    public String getResourceURL() {
        return resourceURL;
    }

    public void setResourceURL(String resourceURL) {
        this.resourceURL = resourceURL;
    }

    public String getIndividualName() {
        return individualName;
    }

    public void setIndividualName(String individualName) {
        this.individualName = individualName;
    }

    public String getContactEmail() {
        return contactEmail;
    }

    public void setContactEmail(String contactEmail) {
        this.contactEmail = contactEmail;
    }

    public String getDefiningCitation() {
        return definingCitation;
    }

    public void setDefiningCitation(String definingCitation) {
        this.definingCitation = definingCitation;
    }

    public String getResourceContributor() {
        return resourceContributor;
    }

    public void setResourceContributor(String resourceContributor) {
        this.resourceContributor = resourceContributor;
    }

    public String getAlternateTitle() {
        return alternateTitle;
    }

    public void setAlternateTitle(String alternateTitle) {
        this.alternateTitle = alternateTitle;
    }

    public List<String> getGeoscienceSubdomains() {
        return geoscienceSubdomains;
    }

    public CinergiFormRec addGeoscienceSubdomain(String geoscienceSubdomain) {
        if (!geoscienceSubdomains.contains(geoscienceSubdomain)) {
            geoscienceSubdomains.add(geoscienceSubdomain);
        }
        return this;
    }

    public List<String> getEquipments() {
        return equipments;
    }

    public CinergiFormRec addEquipment(String equipment) {
        if (!equipments.contains(equipment)) {
            equipments.add(equipment);
        }
        return this;
    }

    public List<String> getMethods() {
        return methods;
    }

    public CinergiFormRec addMethod(String method) {
        if (!methods.contains(method)) {
            methods.add(method);
        }
        return this;
    }

    public List<String> getEarthProcesses() {
        return earthProcesses;
    }

    public CinergiFormRec addEarthProcess(String earthProcess) {
        if (!earthProcesses.contains(earthProcess)) {
            earthProcesses.add(earthProcess);
        }
        return this;
    }

    public List<String> getDescribedFeatures() {
        return describedFeatures;
    }

    public CinergiFormRec addDescribedFeature(String describedFeature) {
        if (!describedFeatures.contains(describedFeature)) {
            describedFeatures.add(describedFeature);
        }
        return this;
    }

    public List<String> getOtherTags() {
        return otherTags;
    }

    public CinergiFormRec addOtherTag(String otherTag) {
        if (!otherTags.contains(otherTag)) {
            otherTags.add(otherTag);
        }
        return this;
    }

    public List<String> getPlaceNames() {
        return placeNames;
    }

    public CinergiFormRec addPlaceName(String placeName) {
        if (!placeNames.contains(placeName)) {
            placeNames.add(placeName);
        }
        return this;
    }

    public void setTemporalExtent(TemporalExtent temporalExtent) {
        this.temporalExtent = temporalExtent;
    }

    public TemporalExtent getTemporalExtent() {
        return temporalExtent;
    }

    public List<SpatialExtent> getSpatialExtents() {
        return spatialExtents;
    }

    public CinergiFormRec addSpatialExtent(SpatialExtent spatialExtent) {
        if (!spatialExtents.contains(spatialExtent)) {
            spatialExtents.add(spatialExtent);
        }
        return this;
    }

    public JSONObject toJSON() {
        JSONObject json = new JSONObject();
        json.put("resourceType", resourceType);
        json.put("resourceTitle", resourceTitle);
        json.put("abstractText", abstractText);
        json.put("resourceURL", resourceURL);
        json.put("individualName", individualName);
        json.put("contactEmail", contactEmail);
        json.put("definingCitation", definingCitation);
        json.put("resourceContributor", resourceContributor);
        if (alternateTitle != null) {
            json.put("alternateTitle", alternateTitle);
        }
        prepJsonArray(geoscienceSubdomains, json, "geoscienceSubdomains");
        prepJsonArray(equipments, json, "equipments");
        prepJsonArray(methods, json, "methods");
        prepJsonArray(earthProcesses, json, "earthProcesses");
        prepJsonArray(describedFeatures, json, "describedFeatures");
        prepJsonArray(otherTags, json, "otherTags");
        prepJsonArray(placeNames, json, "placeNames");
        if (temporalExtent != null) {
            json.put("temporalExtent", temporalExtent.toJSON());
        }
        if (!spatialExtents.isEmpty()) {
            JSONArray jsArr = new JSONArray();
            for (SpatialExtent se : spatialExtents) {
                jsArr.put(se.toJSON());
            }
            json.put("spatialExtents", jsArr);
        }
        if (fileFormat != null) {
            json.put("fileFormat", fileFormat);
        }
        if (lineage != null) {
            json.put("lineage", lineage);
        }
        if (geologicAge != null) {
            json.put("geologicAge", geologicAge.toJSON());
        }
        return json;
    }

    static void prepJsonArray(List<String> list, JSONObject json, String name) {
        if (!list.isEmpty()) {
            JSONArray jsArr = new JSONArray();
            for (String e : list) {
                jsArr.put(e);
            }
            json.put(name, jsArr);
        }
    }

    public static CinergiFormRec fromJSON(JSONObject json) {
        CinergiFormRec cfr = new CinergiFormRec();
        cfr.resourceType = json.getString("resourceType");
        cfr.resourceTitle = json.getString("resourceTitle");
        cfr.abstractText = json.getString("AbstractText");
        cfr.resourceURL = json.getString("resourceURL");
        cfr.individualName = json.getString("individualName");
        cfr.contactEmail = json.getString("contactEmail");
        cfr.definingCitation = json.getString("definingCitation");
        cfr.resourceContributor = json.getString("resourceContributor");
        if (json.has("alternateTitle")) {
            cfr.alternateTitle = json.getString("alternateTitle");
        }
        prepList(cfr.geoscienceSubdomains, json, "geoscienceSubdomains");
        if (json.has("equipment")) {
            prepList(cfr.equipments, json, "equipment", ",");
        } else if (json.has("equimpment")) { // Uncorrected typo in the form
            prepList(cfr.equipments, json, "equimpment", ",");
        }
        prepList(cfr.methods, json, "methods", ",");
        prepList(cfr.earthProcesses, json, "earthProcesses");
        prepList(cfr.describedFeatures, json, "describedFeatures");
        prepList(cfr.otherTags, json, "otherTags");
        prepList(cfr.placeNames, json, "placeNames");
        if (json.has("temporalExtent") && !isEmptyObject(json.getJSONObject("temporalExtent"))) {
            cfr.temporalExtent = TemporalExtent.fromJSON(json.getJSONObject("temporalExtent"));
        }
        if (json.has("spatialExtents")) {
            JSONArray jsArr = json.getJSONArray("spatialExtents");
            for (int i = 0; i < jsArr.length(); i++) {
                SpatialExtent se = SpatialExtent.fromJSON(jsArr.getJSONObject(i));
                cfr.addSpatialExtent(se);
            }
        }
        if (json.has("Extent") && !isEmptyObject(json.getJSONObject("Extent"))) {
            SpatialExtent se = SpatialExtent.fromJSON(json.getJSONObject("Extent"));
            cfr.addSpatialExtent(se);
        }
        if (json.has("fileFormat")) {
            cfr.setFileFormat(json.getString("fileFormat"));
        }
        if (json.has("lineage")) {
            cfr.setLineage(json.getString("lineage"));
        }
        if (json.has("geologicAge") && !isEmptyObject(json.getJSONObject("geologicAge"))) {
            cfr.setGeologicAge(GeologicAge.fromJSON(json.getJSONObject("geologicAge")));
        }
        return cfr;
    }

    public static boolean isEmptyObject(JSONObject json) {
        return json == null || json.keySet() == null || json.keySet().isEmpty();
    }

    static void prepList(List<String> list, JSONObject json, String name) {
        prepList(list, json, name, null);
    }

    static void prepList(List<String> list, JSONObject json, String name, String delimiter) {
        if (json.has(name)) {
            Object o = json.get(name);
            if (o instanceof JSONArray) {
                JSONArray jsArr = json.getJSONArray(name);
                int len = jsArr.length();
                for (int i = 0; i < len; i++) {
                    list.add(StringEscapeUtils.escapeHtml(jsArr.getString(i)));
                }
            } else {
                if (delimiter != null) {
                    String[] tokens = o.toString().split(delimiter);
                    for (String token : tokens) {
                        list.add(StringEscapeUtils.escapeXml(token));
                    }
                } else {
                    list.add(StringEscapeUtils.escapeXml(o.toString()));
                }
            }
        }
    }

    public static class TemporalExtent {
        private String start;
        private String end;

        public TemporalExtent(String start, String end) {
            this.start = start;
            this.end = end;
        }

        public String getStart() {
            return start;
        }

        public String getEnd() {
            return end;
        }

        public static TemporalExtent fromJSON(JSONObject json) {
            return new TemporalExtent(json.getString("start"), json.getString("end"));
        }

        public JSONObject toJSON() {
            JSONObject json = new JSONObject();
            json.put("start", start);
            json.put("end", end);
            return json;
        }
    }

    public static class SpatialExtent {
        private String westBoundLongitude;
        private String eastBoundLongitude;
        private String southBoundLatitude;
        private String northBoundLatitude;

        public SpatialExtent(String westBoundLongitude, String eastBoundLongitude,
                             String southBoundLatitude, String northBoundLatitude) {
            this.westBoundLongitude = westBoundLongitude;
            this.eastBoundLongitude = eastBoundLongitude;
            this.southBoundLatitude = southBoundLatitude;
            this.northBoundLatitude = northBoundLatitude;
        }

        public SpatialExtent() {
        }

        public String getWestBoundLongitude() {
            return westBoundLongitude;
        }

        public void setWestBoundLongitude(String westBoundLongitude) {
            this.westBoundLongitude = westBoundLongitude;
        }

        public String getEastBoundLongitude() {
            return eastBoundLongitude;
        }

        public void setEastBoundLongitude(String eastBoundLongitude) {
            this.eastBoundLongitude = eastBoundLongitude;
        }

        public String getSouthBoundLatitude() {
            return southBoundLatitude;
        }

        public void setSouthBoundLatitude(String southBoundLatitude) {
            this.southBoundLatitude = southBoundLatitude;
        }

        public String getNorthBoundLatitude() {
            return northBoundLatitude;
        }

        public void setNorthBoundLatitude(String northBoundLatitude) {
            this.northBoundLatitude = northBoundLatitude;
        }

        public static SpatialExtent fromJSON(JSONObject json) {
            String wbl = json.get("westBoundLongitude").toString();
            String ebl = json.get("eastBoundLongitude").toString();
            String sbl = json.get("southBoundLatitude").toString();
            String nbl = json.get("NorthBoundLatitude").toString();
            return new SpatialExtent(wbl, ebl, sbl, nbl);
        }

        public JSONObject toJSON() {
            JSONObject json = new JSONObject();
            json.put("westBoundLongitude", westBoundLongitude);
            json.put("eastBoundLongitude", eastBoundLongitude);
            json.put("southBoundLatitude", southBoundLatitude);
            json.put("NorthBoundLatitude", northBoundLatitude); // to match form submission JSON from Tom
            return json;
        }
    }

    public static class GeologicAge {
        String older;
        String younger;

        public GeologicAge(String older, String younger) {
            this.older = older;
            this.younger = younger;
        }

        public String getOlder() {
            return older;
        }

        public String getYounger() {
            return younger;
        }

        public static GeologicAge fromJSON(JSONObject json) {
            return new GeologicAge(json.getString("older"), json.getString("younger"));
        }

        public JSONObject toJSON() {
            JSONObject json = new JSONObject();
            json.put("older", older);
            json.put("younger", younger);
            return json;
        }
    }


    public static void main(String[] args) {
        JSONObject json = new JSONObject();
        json.put("field", (new JSONObject()).put("flag",true));
        System.out.println(isEmptyObject(json.getJSONObject("field")));
    }
}
