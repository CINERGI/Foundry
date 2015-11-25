package org.neuinfo.foundry.consumers.common;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.jdom2.Element;
import org.json.JSONObject;
import org.neuinfo.foundry.common.util.XML2JSONConverter;

import java.io.File;
import java.net.URI;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.TimeZone;

/**
 * Created by bozyurt on 11/20/14.
 */
public class ConsumerUtils {
    public static List<File> getXMLFiles(List<String> paths) {
        List<File> xmlFiles = new LinkedList<File>();
        for (String pathStr : paths) {
            File dir = new File(pathStr);
            if (!dir.isDirectory()) {
                continue;
            }
            File[] files = dir.listFiles();
            for (File f : files) {
                if (f.isFile() && f.getName().endsWith(".xml")) {
                    xmlFiles.add(f);
                }
            }
        }
        return xmlFiles;
    }
    public static File prepareInputFile(String format, JSONObject originalDoc, String objectId) throws Exception {
        File dir = new File("/tmp/consumers");
        if (!dir.isDirectory()) {
            dir.mkdirs();
        }

        File inputFile;
        if (format.equals("xml")) {
            inputFile = new File(dir, objectId + ".xml");
            XML2JSONConverter converter = new XML2JSONConverter();
            // System.out.println(originalDoc.toString(2));
            Element docEl = converter.toXML(originalDoc);
            org.neuinfo.foundry.common.util.Utils.saveXML(docEl, inputFile.getAbsolutePath());
        } else {
            inputFile = new File(dir, objectId + ".json");
        }

        return inputFile;
    }

    public static File prepareOutFile(String format, String objectId) {
        File dir = new File("/tmp/consumers");
        if (!dir.isDirectory()) {
            dir.mkdirs();
        }
        File outputFile;
        if (format.equals("xml")) {
            outputFile = new File(dir, objectId + ".xml");
        } else {
            outputFile = new File(dir, objectId + ".json");
        }
        return outputFile;
    }

    public static boolean send2ElasticSearch(String jsonDocStr, String docId, String indexPath, String serverURL) throws Exception {
        HttpClient client = new DefaultHttpClient();

        URIBuilder builder = new URIBuilder(serverURL);
        // "http://localhost:9200/");
        indexPath = ensureIndexPathStartsWithSlash(indexPath);
        builder.setPath(indexPath + "/" + docId);  //"/nif/cinergi/" + docId);
        URI uri = builder.build();
        System.out.println("uri:" + uri);
        HttpPut httpPut = new HttpPut(uri);
        boolean ok = false;
        try {
            httpPut.addHeader("Accept", "application/json");
            httpPut.addHeader("Content-Type", "application/json");
            StringEntity entity = new StringEntity(jsonDocStr, "UTF-8");
            httpPut.setEntity(entity);
            final HttpResponse response = client.execute(httpPut);
            int statusCode = response.getStatusLine().getStatusCode();
            if (statusCode == 200 || statusCode == 201) {
                ok = true;
            } else {
                System.out.println(response.getStatusLine());
            }

        } finally {
            if (httpPut != null) {
                httpPut.releaseConnection();
            }
        }
        return ok;
    }


    public static String ensureIndexPathStartsWithSlash(String indexPath) {
        if (!indexPath.startsWith("/")) {
            indexPath = "/" + indexPath;
        }
        return indexPath;
    }


    public static String getTimeInProvenanceFormat() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX");
        sdf.setTimeZone(TimeZone.getDefault());
        return sdf.format(new Date());
    }

    public static String getTimeInProvenanceFormat(Date date) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX");
        sdf.setTimeZone(TimeZone.getDefault());
        return sdf.format(date);
    }

    public static void main(String[] args) {
        System.out.println(getTimeInProvenanceFormat());
    }
}
