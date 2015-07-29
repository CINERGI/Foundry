package org.neuinfo.foundry.ingestor;

import org.jdom2.Element;
import org.json.JSONObject;

import java.sql.*;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by bozyurt on 10/24/14.
 */
public class NIFJDBCIngestor extends IngestorSupport {
    Connection con;
    IngestionConfig ic;

    public NIFJDBCIngestor(IngestionConfig ic, String collectionName) throws SQLException {
        super(collectionName);
        con = DriverManager.getConnection(ic.jdbcUrl, ic.user, ic.pwd);
    }


    public void shutdown() {
        if (con != null) {
            try {
                con.close();
            } catch (SQLException e) {
                // ignore
            }
        }
    }

    public void ingest() throws Exception {
        DatabaseMetaData metaData = con.getMetaData();
        PreparedStatement pst = null;
        List<ColumnMD> cmdList;
        try {
            pst = con.prepareStatement("select * from " + ic.tableName);
            ResultSetMetaData md = pst.getMetaData();

            int cc = md.getColumnCount();
            cmdList = new ArrayList<ColumnMD>(cc);
            for (int i = 1; i <= cc; i++) {
                String colName = md.getColumnName(i);
                String colType = md.getColumnTypeName(i);
                cmdList.add(new ColumnMD(colName, colType));
            }
            ResultSet rs = pst.executeQuery();
            while (rs.next()) {
                JSONObject payload = new JSONObject();
                if (ic.colNameMap.isEmpty()) {
                    for (int i = 1; i <= cc; i++) {
                        String value = rs.getObject(i).toString();
                        ColumnMD colMD = cmdList.get(i - 1);
                        payload.put(colMD.getName(), value);
                    }
                } else {
                    for (int i = 1; i <= cc; i++) {
                        ColumnMD colMD = cmdList.get(i - 1);
                        if (ic.colNameMap.containsKey(colMD.getName())) {
                            String fieldName = ic.colNameMap.get(colMD.getName());
                            String value = rs.getObject(i).toString();
                            payload.put(fieldName, value);
                        }
                    }
                }
                JSONObject docWrapper = super.prepareDocument(payload);
                System.out.println("docWrapper:" + docWrapper);

            }
            rs.close();

        } finally {
            if (pst != null) {
                try {
                    pst.close();
                } catch (SQLException x) {
                    // ignore
                }
            }
        }

    }

    public static class ColumnMD {
        final String name;
        final String type;

        public ColumnMD(String name, String type) {
            this.name = name;
            this.type = type;
        }

        public String getName() {
            return name;
        }

        public String getType() {
            return type;
        }
    }

    public static class IngestionConfig {
        String jdbcUrl;
        String user;
        String pwd;
        String tableName;
        Map<String, String> colNameMap = new LinkedHashMap<String, String>(31);

        public static IngestionConfig fromXml(Element el) {
            IngestionConfig ic = new IngestionConfig();
            ic.user = el.getAttributeValue("user");
            ic.pwd = el.getAttributeValue("pwd");
            ic.jdbcUrl = el.getAttributeValue("jdbcUrl");
            ic.tableName = el.getAttributeValue("tableName");
            List<Element> colEls = el.getChildren("col");
            for (Element colEl : colEls) {
                String name = colEl.getAttributeValue("name");
                String label = name;
                if (colEl.getAttribute("label") != null) {
                    label = colEl.getAttributeValue("label");
                }
                ic.colNameMap.put(name, label);
            }


            return ic;
        }

    }


}
