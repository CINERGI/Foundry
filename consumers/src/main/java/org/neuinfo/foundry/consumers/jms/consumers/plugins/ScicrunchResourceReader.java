package org.neuinfo.foundry.consumers.jms.consumers.plugins;

import org.neuinfo.foundry.common.util.Assertion;
import org.neuinfo.foundry.common.util.Utils;

import java.sql.*;
import java.util.*;

/**
 * Created by bozyurt on 4/23/15.
 */
public class ScicrunchResourceReader {
    private Connection con;

    public void startup() throws Exception {
        Properties p = Utils.loadProperties("scicrunch.properties");
        Properties props = new Properties();
        if (p != null) {
            props.put("user", p.getProperty("username"));
            props.put("password", p.getProperty("password"));
        }
        con = DriverManager.getConnection("jdbc:mysql://mysql5-1.crbs.ucsd.edu:3306/nif_eelg", props);
    }

    public void shutdown() {
        if (con != null) {
            try {
                con.close();
            } catch (SQLException e) {
                //ignored
            }
        }
    }

    public List<Long> getCinergiResourcesInChronologicalOrder() throws SQLException {
        List<Long> resourceIDList = new LinkedList<Long>();
        PreparedStatement pst = null;
        Long cid = null;
        try {
            pst = con.prepareStatement("select id from communities where name = ?");
            pst.setString(1, "CINERGI");
            ResultSet rs = pst.executeQuery();
            if (rs.next()) {
                cid = rs.getLong(1);
            }
            rs.close();

        } finally {
            close(pst);
        }
        if (cid != null) {
            try {
                pst = con.prepareStatement("select id from resources where cid = ? order by insert_time");
                pst.setLong(1, cid);
                ResultSet rs = pst.executeQuery();
                while (rs.next()) {
                    resourceIDList.add(rs.getLong(1));
                }
                rs.close();
            } finally {
                close(pst);
            }
        }
        return resourceIDList;
    }

    public Long getLastInsertedResourceID() throws SQLException {
        Statement st = null;
        Long latestInsertTime = null;
        try {
            st = con.createStatement();
            ResultSet rs = st.executeQuery("select max(insert_time) from resources where type = 'Resource'");
            if (rs.next()) {
                latestInsertTime = rs.getLong(1);
            }
            rs.close();
            if (latestInsertTime == null) {
                return null;
            }
            rs = st.executeQuery("select id from resources where insert_time = " + latestInsertTime);
            Long latestResourceId = null;
            if (rs.next()) {
                latestResourceId = rs.getLong(1);
            }
            rs.close();
            return latestResourceId;
        } finally {
            close(st);
        }
    }

    public Map<String, String> getResourceData(Long resourceID) throws SQLException {
        PreparedStatement pst = null;
        Map<String, String> map = new HashMap<String, String>();
        try {
            pst = con.prepareStatement("select name, value from resource_columns where rid = ?");
            pst.setLong(1, resourceID);

            ResultSet rs = pst.executeQuery();
            while (rs.next()) {
                String name = rs.getString(1);
                String value = rs.getString(2);
                if (value != null && value.trim().length() > 0) {
                    map.put(name, value.trim());
                }
            }
            rs.close();
            String email = getEmail(resourceID);
            if (!Utils.isEmpty(email)) {
                map.put("email", email);
            }
            return map;
        } finally {
            close(pst);
        }
    }

    public String getEmail(Long lastInsertedResourceID) throws SQLException {
        String email = null;
        PreparedStatement pst = null;
        try {
            pst = con.prepareStatement("select email from resources where id = ?");
            pst.setLong(1, lastInsertedResourceID);
            ResultSet rs = pst.executeQuery();
            if (rs.next()) {
                email = rs.getString(1);
            }
            rs.close();
        } finally {
            close(pst);
        }
        return email;
    }

    public static void close(Statement st) {
        if (st != null) {
            try {
                st.close();
            } catch (SQLException e) {
                // ignore
            }
        }
    }

    public static void main(String[] args) throws Exception {

        ScicrunchResourceReader sr = new ScicrunchResourceReader();
        try {
            sr.startup();
            Long lastInsertedResourceID = sr.getLastInsertedResourceID();
            Assertion.assertNotNull(lastInsertedResourceID);
            Map<String, String> map = sr.getResourceData(lastInsertedResourceID);
            System.out.println(map.toString());

        } finally {
            sr.shutdown();
        }
    }

}
