package org.neuinfo.foundry.common.util;

import java.sql.*;

/**
 * Created by bozyurt on 7/28/16.
 */
public class ChangedRecordRegistry {
    Connection con;

    public ChangedRecordRegistry() throws Exception {
        Class.forName("org.sqlite.JDBC");
        this.con = DriverManager.getConnection("jdbc:sqlite:/var/data/cinergi/changes.db");
        if (!hasTable("changed_oids")) {
            Statement st = null;
            try {
                st = this.con.createStatement();
                st.executeUpdate("create table changed_oids (oid varchar(50) primary key not null, " +
                        "pk varchar(1000), source_id varchar(50) not null)");
            } finally {
                st.close();
            }
        }
        if (!hasTable("waf_urls")) {
            Statement st = null;
            try {
                st = this.con.createStatement();
                st.executeUpdate("create table waf_urls (pk varchar(1000) primary key not null, " +
                        "waf_url varchar(1000) not null)");
            } finally {
                st.close();
            }
        }
    }

    boolean hasTable(String tableName) throws SQLException {
        PreparedStatement st = null;
        try {
            st = this.con.prepareStatement("select name from sqlite_master where type='table' and name = ?");
            st.setString(1, tableName);
            ResultSet rs = st.executeQuery();
            boolean ok = rs.next();
            rs.close();
            return ok;
        } finally {
            st.close();
        }
    }

    boolean hasOID(String oid) throws SQLException {
        PreparedStatement st = null;
        try {
            st = this.con.prepareStatement("select * from changed_oids where oid = ?");
            st.setString(1, oid);
            ResultSet rs = st.executeQuery();
            boolean ok = rs.next();
            rs.close();
            return ok;
        } finally {
            st.close();
        }
    }

    boolean hasURL(String primaryKey) throws SQLException {
        PreparedStatement st = null;
        try {
            st = this.con.prepareStatement("select * from waf_urls where pk = ?");
            st.setString(1, primaryKey);
            ResultSet rs = st.executeQuery();
            boolean ok = rs.next();
            rs.close();
            return ok;
        } finally {
            st.close();
        }
    }

    public String getURL(String primaryKey) throws SQLException {
        PreparedStatement st = null;
        try {
            st = this.con.prepareStatement("select waf_url from waf_urls where pk = ?");
            st.setString(1, primaryKey);
            ResultSet rs = st.executeQuery();
            String url = null;
            if (rs.next()) {
                url = rs.getString(1);
            }
            rs.close();
            return url;
        } finally {
            st.close();
        }
    }

    public void addURL(String primaryKey, String wafURL) throws SQLException {
        if (hasURL(primaryKey)) {
            return;
        }
        PreparedStatement st = null;
        try {
            st = this.con.prepareStatement("insert into waf_urls (pk, waf_url)  values (?,?)");
            st.setString(1, primaryKey);
            st.setString(2, wafURL);
            st.executeUpdate();
        } finally {
            st.close();
        }
    }

    public synchronized void addDWI(KeywordsEditor.DocWrapperInfo dwi) throws SQLException {
        if (hasOID(dwi.oid.toString())) {
            return;
        }
        PreparedStatement st = null;
        try {
            st = this.con.prepareStatement("insert into changed_oids (oid, pk, source_id)  values (?,?,?)");
            st.setString(1, dwi.oid.toString());
            st.setString(2, dwi.primaryKey);
            st.setString(3, dwi.sourceID);
            st.executeUpdate();
        } finally {
            st.close();
        }
    }

    public void shutdown() {
        if (con != null) {
            try {
                con.close();
            } catch (SQLException e) {
                // no op
            }
        }
    }
}
