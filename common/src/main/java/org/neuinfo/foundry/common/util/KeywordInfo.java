package org.neuinfo.foundry.common.util;

import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import org.json.JSONObject;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Created by bozyurt on 1/28/16.
 */
public class KeywordInfo {
    String id;
    String term;
    String category;
    String hierarchyPath;
    CinergiXMLUtils.KeywordType type;
    Date lastChangedDate;
    DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.ENGLISH);


    public KeywordInfo(String id, String term, String category, String hierarchyPath) {
        this(id, term, category, hierarchyPath, CinergiXMLUtils.KeywordType.Keyword);
    }

    public KeywordInfo(String id, String term, String category, String hierarchyPath, CinergiXMLUtils.KeywordType type) {
        this.term = term;
        this.category = category;
        this.hierarchyPath = hierarchyPath;
        this.type = type;
        this.id = id;
    }

    public CinergiXMLUtils.KeywordType getType() {
        return type;
    }

    public String getTerm() {
        return term;
    }

    public String getCategory() {
        return category;
    }

    public String getHierarchyPath() {
        return hierarchyPath;
    }

    public String getId() {
        return id;
    }

    public Date getLastChangedDate() {
        return lastChangedDate;
    }

    public void setLastChangedDate(Date lastChangedDate) {
        this.lastChangedDate = lastChangedDate;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        KeywordInfo that = (KeywordInfo) o;

        if (!id.equals(that.id)) return false;
        return term.equals(that.term);

    }

    @Override
    public int hashCode() {
        int result = id.hashCode();
        result = 31 * result + term.hashCode();
        return result;
    }

    public JSONObject toJSON() {
        JSONObject json = new JSONObject();
        json.put("id", id);
        json.put("term", term);
        json.put("category", category);
        json.put("hierarchyPath", hierarchyPath);
        json.put("type", type.toString());
        json.put("lastChangedDate", df.format(lastChangedDate));
        return json;
    }

    public static KeywordInfo fromDBOject(DBObject kiDBO) {
        String id = (String) kiDBO.get("id");
        String term = (String) kiDBO.get("term");
        String category = (String) kiDBO.get("category");
        String hierarchyPath = (String) kiDBO.get("hierarchyPath");
        String type = (String) kiDBO.get("type");
        CinergiXMLUtils.KeywordType kt = CinergiXMLUtils.KeywordType.Keyword;
        if (type != null && type.equalsIgnoreCase("organization")) {
            kt = CinergiXMLUtils.KeywordType.Organization;
        }
        Date lastChangedDate = ((BasicDBObject) kiDBO).getDate("lastChangedDate");
        KeywordInfo ki = new KeywordInfo(id, term, category, hierarchyPath, kt);
        ki.setLastChangedDate(lastChangedDate);
        return ki;
    }

}
