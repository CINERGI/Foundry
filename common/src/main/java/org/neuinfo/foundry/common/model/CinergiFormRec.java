package org.neuinfo.foundry.common.model;

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

    public TemporalExtent getTemporalExtent() {
        return temporalExtent;
    }

    public void setTemporalExtent(TemporalExtent temporalExtent) {
        this.temporalExtent = temporalExtent;
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

    public static CinergiFormRec fromJSON(JSONObject json) {
        CinergiFormRec cfr = new CinergiFormRec();
        cfr.resourceType = json.getString("resourceType");
        cfr.resourceTitle = json.getString("resourceTitle");
        cfr.abstractText = json.getString("abstractText");
        cfr.resourceURL = json.getString("resourceURL");
        cfr.individualName = json.getString("individualName");
        cfr.contactEmail = json.getString("contactEmail");
        cfr.definingCitation = json.getString("definingCitation");
        cfr.resourceContributor = json.getString("resourceContributor");
        if (json.has("alternateTitle")) {
            cfr.alternateTitle = json.getString("alternateTitle");
        }
        prepList(cfr.geoscienceSubdomains, json, "geoscienceSubdomains");
        prepList(cfr.equipments, json, "equipments");
        prepList(cfr.methods, json, "methods");
        prepList(cfr.earthProcesses, json, "earthProcesses");
        prepList(cfr.describedFeatures, json, "describedFeatures");
        prepList(cfr.otherTags, json, "otherTags");
        prepList(cfr.placeNames, json, "placeNames");
        if (json.has("temporalExtent")) {
            cfr.temporalExtent = TemporalExtent.fromJSON( json.getJSONObject("temporalExtent"));
        }
        if (json.has("spatialExtents")) {
            JSONArray jsArr = json.getJSONArray("spatialExtents");
            for(int i = 0; i < jsArr.length(); i++) {
                SpatialExtent se = SpatialExtent.fromJSON( jsArr.getJSONObject(i));
                cfr.addSpatialExtent(se);
            }
        }
        return cfr;
    }

    static void prepList(List<String> list, JSONObject json, String name) {
        if (json.has(name)) {
            JSONArray jsArr = json.getJSONArray(name);
            int len = jsArr.length();
            for(int i = 0; i < len; i++) {
                list.add( jsArr.getString(i));
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
            return new TemporalExtent( json.getString("start"), json.getString("end"));
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
            String wbl = json.getString("westBoundLongitude");
            String ebl = json.getString("eastBoundLongitude");
            String sbl = json.getString("southBoundLatitude");
            String nbl = json.getString("northBoundLatitude");
            return new SpatialExtent(wbl, ebl, sbl, nbl);
        }
    }

}
