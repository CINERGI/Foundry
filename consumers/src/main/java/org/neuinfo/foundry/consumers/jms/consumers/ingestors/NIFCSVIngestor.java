package org.neuinfo.foundry.consumers.jms.consumers.ingestors;

import au.com.bytecode.opencsv.CSVReader;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONObject;
import org.neuinfo.foundry.common.util.Assertion;
import org.neuinfo.foundry.common.util.HtmlEntityUtil;
import org.neuinfo.foundry.common.util.Utils;
import org.neuinfo.foundry.consumers.plugin.Ingestor;
import org.neuinfo.foundry.consumers.plugin.Result;

import java.io.*;
import java.net.URI;
import java.util.Map;

/**
 * Created by bozyurt on 11/3/14.
 */
public class NIFCSVIngestor implements Ingestor {
    boolean allowDuplicates = false;
    Map<String, String> optionMap;
    String ingestURL;
    boolean keepMissing;
    int ignoreLines;
    int headerLine;
    String delimiter;
    String textQuote;
    String escapeChar;
    String localeStr;
    CSVReader csvReader;
    String[] currentRow;
    String[] headerCols;
    File csvFile;
    int recordIdx = 0;

    @Override
    public void initialize(Map<String, String> options) throws Exception {
        this.allowDuplicates = options.containsKey("allowDuplicates") ?
                Boolean.parseBoolean(options.get("allowDuplicates")) : false;
        this.optionMap = options;
        this.ingestURL = options.get("ingestURL");
        this.keepMissing = options.containsKey("keepMissing") ?
                Boolean.parseBoolean(options.get("keepMissing")) : false;
        this.ignoreLines = Utils.getIntValue(options.get("ignoreLines"), -1); // one based
        this.headerLine = Utils.getIntValue(options.get("headerLine"), -1); // one based
        this.delimiter = options.get("delimiter");
        this.textQuote = options.get("textQuote");
        this.escapeChar = options.get("escapeCharacter");
        this.localeStr = options.get("locale");
        if (HtmlEntityUtil.isHtmlEntity(this.textQuote)) {
            this.textQuote = HtmlEntityUtil.getChar(this.textQuote).toString();
        }
        if (HtmlEntityUtil.isHtmlEntity(this.escapeChar)) {
            this.escapeChar = HtmlEntityUtil.getChar(this.escapeChar).toString();
        }
        if (HtmlEntityUtil.isHtmlEntity(this.delimiter)) {
            this.delimiter = HtmlEntityUtil.getChar(this.delimiter).toString();
        }
    }

    @Override
    public void startup() throws Exception {
        this.csvFile = getCSVContent();
        Assertion.assertNotNull(csvFile);
        BufferedReader in = Utils.newUTF8CharSetReader(csvFile.getAbsolutePath());
        this.csvReader = new CSVReader(in);
        if (ignoreLines > 0) {
            for (int i = 0; i < ignoreLines; i++) {
                String[] line = csvReader.readNext();
                if (i + 1 == headerLine) {
                    this.headerCols = new String[line.length];
                    for (int j = 0; j < line.length; j++) {
                        this.headerCols[j] = line[j].trim().replaceAll("\\.", "_");
                    }
                }
            }
        }

        Assertion.assertNotNull(this.headerLine);
    }

    /**
     * assumption: <code>hasNext()</code> is called first
     *
     * @return
     */
    @Override
    public Result prepPayload() {
        if (this.currentRow != null) {
            JSONObject json = new JSONObject();
            for (int i = 0; i < this.currentRow.length; i++) {
                String value = this.currentRow[i];
                json.put(headerCols[i], value);
            }
            return new Result(json, Result.Status.OK_WITH_CHANGE);
        }
        return null;
    }

    @Override
    public String getName() {
        return "NIFCSVIngestor";
    }

    @Override
    public int getNumRecords() {
        return -1;
    }

    @Override
    public String getOption(String optionName) {
        return this.optionMap.get(optionName);
    }

    @Override
    public void shutdown() {
        if (csvReader != null) {
            try {
                csvReader.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if (csvFile != null) {
            System.out.println("CSV file:" + csvFile);
            csvFile.delete();
        }
    }

    @Override
    public boolean hasNext() {
        try {
            this.currentRow = this.csvReader.readNext();
            this.recordIdx++;
            // FIXME TEST
            if (this.recordIdx > 17) {
                return false;
            }
            return true;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    File getCSVContent() throws Exception {
        HttpClient client = new DefaultHttpClient();
        URIBuilder builder = new URIBuilder(ingestURL);
        URI uri = builder.build();
        HttpGet httpGet = new HttpGet(uri);
        try {
            HttpResponse response = client.execute(httpGet);
            HttpEntity entity = response.getEntity();
            if (entity != null) {
                File file = File.createTempFile("consumer_csv_", ".csv");
                BufferedWriter out = null;
                BufferedReader in = null;
                try {
                    in = new BufferedReader(new InputStreamReader(entity.getContent()));
                    out = Utils.newUTF8CharSetWriter(file.getAbsolutePath());
                    String line;
                    while ((line = in.readLine()) != null) {
                        out.write(line);
                        out.newLine();
                    }
                } finally {
                    Utils.close(in);
                    Utils.close(out);
                }
                return file;
            }

        } finally {
            if (httpGet != null) {
                httpGet.releaseConnection();
            }
        }
        return null;
    }
}
