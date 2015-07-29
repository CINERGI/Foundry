package org.neuinfo.foundry.common.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by bozyurt on 4/20/15.
 */
public class ScicrunchResourceRec {
    String resourceUrl;
    String  dataSetName;
    String description;
    String email;
    List<UserKeyword> keywords = new ArrayList<UserKeyword>(5);

    public void addKeyword(UserKeyword keyword) {
        keywords.add(keyword);
    }

    public String getResourceUrl() {
        return resourceUrl;
    }

    public void setResourceUrl(String resourceUrl) {
        this.resourceUrl = resourceUrl;
    }

    public String getDataSetName() {
        return dataSetName;
    }

    public void setDataSetName(String dataSetName) {
        this.dataSetName = dataSetName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public List<UserKeyword> getKeywords() {
        return keywords;
    }

    public static class UserKeyword {
        String term;
        String category;

        public UserKeyword(String term, String category) {
            this.term = term;
            this.category = category;
        }

        public String getTerm() {
            return term;
        }

        public String getCategory() {
            return category;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            UserKeyword that = (UserKeyword) o;

            if (!term.equals(that.term)) return false;
            return category.equals(that.category);

        }

        @Override
        public int hashCode() {
            int result = term.hashCode();
            result = 31 * result + category.hashCode();
            return result;
        }

        @Override
        public String toString() {
            final StringBuilder sb = new StringBuilder("UserKeyword{");
            sb.append("term='").append(term).append('\'');
            sb.append(", category='").append(category).append('\'');
            sb.append('}');
            return sb.toString();
        }
    }
}
