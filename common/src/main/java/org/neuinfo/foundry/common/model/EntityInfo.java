package org.neuinfo.foundry.common.model;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.LinkedList;
import java.util.List;

/**
* Created by bozyurt on 2/11/15.
*/
public class EntityInfo {
    final String contentLocation;
    final String id;
    final int start;
    final int end;
    final String category;
    List<String> otherCategories = null;

    public EntityInfo(String contentLocation, String id, int start, int end, String category) {
        this.contentLocation = contentLocation;
        this.id = id;
        this.start = start;
        this.end = end;
        this.category = category;
    }

    public boolean hasOtherCategories() {
        return otherCategories != null && !otherCategories.isEmpty();
    }

    public void addOtherCategory(String category) {
        if (otherCategories == null) {
            otherCategories = new LinkedList<String>();
        }
        if (!otherCategories.contains(category)) {
            otherCategories.add(category);
        }
    }

    public List<String> getOtherCategories() {
        return otherCategories;
    }

    public String getContentLocation() {
        return contentLocation;
    }

    public String getId() {
        return id;
    }

    public int getStart() {
        return start;
    }

    public int getEnd() {
        return end;
    }

    public String getCategory() {
        return category;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("EntityInfo{");
        sb.append("contentLocation='").append(contentLocation).append('\'');
        sb.append(", id='").append(id).append('\'');
        sb.append(", start=").append(start);
        sb.append(", end=").append(end);
        sb.append(", category=").append(category);
        sb.append('}');
        return sb.toString();
    }

    public JSONObject toJSON() {
        JSONObject js = new JSONObject();
        js.put("contentLocation", contentLocation);
        js.put("id", id);
        js.put("start", start);
        js.put("end", end);
        js.put("category", category);
        if (hasOtherCategories()) {
            JSONArray jsArr = new JSONArray();
            for(String cat : otherCategories) {
                jsArr.put(cat);
            }
            js.put("otherCategories", jsArr);
        }
        return js;
    }

    public static EntityInfo fromJSON(JSONObject json) {
        String contentLocation = json.getString("contentLocation");
        String id = json.getString("id");
        int start = json.getInt("start");
        int end = json.getInt("end");
        String category = json.getString("category");
        EntityInfo ei = new EntityInfo(contentLocation, id, start, end, category);
        if (json.has("otherCategories")) {
            JSONArray jsArr = json.getJSONArray("otherCategories");
            for(int i = 0; i < jsArr.length(); i++) {
                ei.addOtherCategory(jsArr.getString(i));
            }
        }
        return ei;
    }
}
