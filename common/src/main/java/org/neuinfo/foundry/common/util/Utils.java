package org.neuinfo.foundry.common.util;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.input.SAXBuilder;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;

import java.io.*;
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Created by bozyurt on 4/4/14.
 */
public class Utils {

    public static int numOfCharsIn(String s, char c) {
        int count = 0, len = s.length();
        for (int i = 0; i < len; i++) {
            if (s.charAt(i) == c) {
                count++;
            }
        }
        return count;
    }

    public static Properties loadProperties(String propsFilename)
            throws IOException {
        InputStream is = Utils.class.getClassLoader().getResourceAsStream(
                propsFilename);
        if (is == null) {
            throw new IOException(
                    "Cannot find properties file in the classpath:"
                            + propsFilename
            );
        }
        Properties props = new Properties();
        props.load(is);

        return props;
    }

    public static String getStringValue(Object o, String defaultVal) {
        if (o == null) {
            return defaultVal;
        }
        return o.toString();
    }

    public static int getIntValue(Object o, int defaultVal) {
        if (o == null) {
            return defaultVal;
        }
        return Integer.parseInt(o.toString());
    }

    public static boolean getBoolValue(Object o, boolean defaultVal) {
        if (o == null) {
            return defaultVal;
        }
        return Boolean.parseBoolean(o.toString());
    }

    public static long getLongValue(Object o, long defaultVal) {
        if (o == null) {
            return defaultVal;
        }
        return Long.parseLong(o.toString());
    }

    public static void close(Reader in) {
        try {
            in.close();
        } catch (Exception x) {
            // ignore
        }
    }

    public static void close(Writer out) {
        try {
            out.close();
        } catch (Exception x) {
            // ignore
        }
    }

    public static void close(InputStream in) {
        try {
            in.close();
        } catch (Exception x) {
            // ignore
        }
    }

    public static void close(OutputStream os) {
        try {
            os.close();
        } catch (Exception x) {
            // no op
        }
    }

    public static BufferedReader newUTF8CharSetReader(String filename)
            throws IOException {
        return new BufferedReader(new InputStreamReader(new FileInputStream(
                filename), Charset.forName("UTF-8")));
    }

    public static BufferedWriter newUTF8CharSetWriter(String filename) throws IOException {
        return new BufferedWriter(new OutputStreamWriter(new FileOutputStream(filename),
                Charset.forName("UTF-8")));
    }

    public static void saveXML(Element rootElem, String filename) throws Exception {
        BufferedWriter out = null;
        try {
            out = newUTF8CharSetWriter(filename);
            XMLOutputter xout = new XMLOutputter(Format.getPrettyFormat());
            xout.output(rootElem, out);
        } finally {
            close(out);
        }
    }

    public static String xmlAsString(Element rootElem) throws Exception {
        XMLOutputter xout = new XMLOutputter(Format.getPrettyFormat());
        StringWriter sw = new StringWriter(15000);
        xout.output(rootElem, sw);
        return sw.toString();
    }

    public static Element loadXML(String xmlFile) throws Exception {
        SAXBuilder builder = new SAXBuilder();
        BufferedReader in = null;
        Element root = null;
        try {
            in = newUTF8CharSetReader(xmlFile);
            Document doc = builder.build(in);
            root = doc.getRootElement();
        } finally {
            close(in);
        }
        return root;
    }

    public static Element readXML(String xmlContent) throws Exception {
        SAXBuilder builder = new SAXBuilder();
        Document docEl = builder.build(new StringReader(xmlContent));
        return docEl.getRootElement();
    }

    public static String loadAsStringFromClasspath(String textFile) throws IOException {
        StringBuilder buf = new StringBuilder(4096);
        BufferedReader in = null;
        try {
            in = new BufferedReader(new InputStreamReader(Utils.class.getClassLoader().getResourceAsStream(textFile)));
            String line;
            while ((line = in.readLine()) != null) {
                buf.append(line).append('\n');
            }
        } finally {
            close(in);
        }
        return buf.toString().trim();
    }

    public static void appendToFile(String filePath, List<String> records) throws IOException {
        BufferedWriter bout = null;
        try {
            bout = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(filePath, true),
                    Charset.forName("UTF-8")));
            for (String rec : records) {
                bout.write(rec);
                bout.newLine();
            }
        } finally {
            close(bout);
        }

    }

    public static List<String> loadList(String listTextFile) throws IOException {
        List<String> list = new LinkedList<String>();
        BufferedReader in = null;
        try {
            in = newUTF8CharSetReader(listTextFile);

            String line;
            while ((line = in.readLine()) != null) {
                list.add(line);
            }
        } finally {
            close(in);
        }
        return list;
    }

    public static String loadAsString(String textFile) throws IOException {
        StringBuilder buf = new StringBuilder((int) new File(textFile).length());
        BufferedReader in = null;
        try {
            in = newUTF8CharSetReader(textFile);

            String line;
            while ((line = in.readLine()) != null) {
                buf.append(line).append('\n');
            }
        } finally {
            close(in);
        }
        return buf.toString().trim();
    }

    public static void saveText(String text, String textFile) throws IOException {
        BufferedWriter out = null;
        try {
            out = newUTF8CharSetWriter(textFile);

            out.write(text);
            out.newLine();
        } finally {
            close(out);
        }
    }

    public static Properties loadPropertiesFromPath(String propsFilePath)
            throws IOException {
        InputStream is = null;
        try {
            is = new BufferedInputStream(new FileInputStream(propsFilePath));
            Properties props = new Properties();
            props.load(is);
            return props;
        } finally {
            close(is);
        }
    }

    public static boolean isEmpty(String s) {
        return s == null || s.trim().isEmpty();
    }

    public static String prepBatchId(Date date) {
        SimpleDateFormat sdf = new SimpleDateFormat("MMddyyyy-HH:mm");
        return sdf.format(date);
    }

    public static String nextVersion(String version) {
        String[] tokens = version.split("\\.");
        String lastToken = tokens[tokens.length - 1];
        int curVersion = getIntValue(lastToken, -1);
        Assertion.assertTrue(curVersion != -1);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < tokens.length - 1; i++) {
            sb.append(tokens[i]).append('.');
        }
        sb.append(curVersion + 1);
        return sb.toString();
    }

    public static void copyFile(String sourceFile, String destFile)
            throws IOException {

        BufferedInputStream bin = null;
        BufferedOutputStream bout = null;
        try {
            bin = new BufferedInputStream(new FileInputStream(sourceFile));
            bout = new BufferedOutputStream(new FileOutputStream(destFile));
            byte[] buffer = new byte[4096];
            int readBytes = 0;
            while ((readBytes = bin.read(buffer)) != -1) {
                bout.write(buffer, 0, readBytes);
            }

        } finally {
            close(bin);
            close(bout);
        }
    }

    public static String getMD5Checksum(String filePath) throws Exception {
        byte[] barr = createMD5Checksum(filePath);
        StringBuilder sb = new StringBuilder(32);
        for (int i = 0; i < barr.length; i++) {
            sb.append(Integer.toString((barr[i] & 0xff) + 0x100, 16).substring(
                    1));
        }
        return sb.toString();
    }

    public static String getMD5ChecksumOfString(String text) throws Exception {
        byte[] barr = createMD5ChecksumOfString(text);
        StringBuilder sb = new StringBuilder(32);
        for (int i = 0; i < barr.length; i++) {
            sb.append(Integer.toString((barr[i] & 0xff) + 0x100, 16).substring(
                    1));
        }
        return sb.toString();
    }

    public static byte[] createMD5ChecksumOfString(String text)
            throws Exception {
        byte[] buffer = text.getBytes("UTF-8");
        MessageDigest md = MessageDigest.getInstance("MD5");
        return md.digest(buffer);
    }

    public static byte[] createMD5Checksum(String filePath) throws Exception {
        BufferedInputStream in = null;
        byte[] buffer = new byte[4096];
        MessageDigest md = MessageDigest.getInstance("MD5");
        try {
            int nr = 0;
            in = new BufferedInputStream(new FileInputStream(filePath));
            while ((nr = in.read(buffer)) > 0) {
                md.update(buffer, 0, nr);
            }
            return md.digest();
        } finally {
            close(in);
        }
    }

    public static String getOntologyID(String id) {
        int idx = id.lastIndexOf('/');
        if (idx != -1) {
            return id.substring(idx + 1);
        }
        return id;
    }

    public static String formatText(String str, int maxLineLen) {
        if (str.length() <= maxLineLen) {
            return str;
        }
        StringBuilder buf = new StringBuilder(str.length() + 10);
        StringTokenizer stok = new StringTokenizer(str);
        int count = 0;
        boolean first = true;
        while (stok.hasMoreTokens()) {
            String tok = stok.nextToken();
            int tokLen = tok.length();
            if (count + tokLen + 1 > maxLineLen) {
                buf.append("\n");
                buf.append(tok);
                count = tokLen;
            } else {
                if (first) {
                    buf.append(tok);
                    first = false;
                    count += tokLen;
                } else {
                    buf.append(' ').append(tok);
                    count += tokLen + 1;
                }
            }
        }
        return buf.toString();
    }

    public static boolean isAllCapital(String phrase) {
        if (!Character.isUpperCase(phrase.charAt(0))) {
            return false;
        }
        char[] carr = phrase.toCharArray();
        for (char c : carr) {
            if (!Character.isUpperCase(c)) {
                return false;
            }
        }
        return true;
    }

    public static boolean fuzzyContains(String text, String containedCandidate) {
        String textLC = text.toLowerCase();
        char[] buf = textLC.toCharArray();
        int len = buf.length;
        char[] candidateBuf = containedCandidate.toLowerCase().toCharArray();
        int clen = candidateBuf.length;
        int cIdx = 0;
        int idx = textLC.indexOf(candidateBuf[0]);
        if (idx == -1) {
            return false;
        }
        boolean inMatch = false;
        while (idx < len && cIdx < containedCandidate.length()) {

            if (candidateBuf[cIdx] == buf[idx]) {
                cIdx++;
                idx++;
                inMatch = true;
                if (cIdx >= clen) {
                    return true;
                }
            } else {
                if (inMatch && canBeIgnored(candidateBuf[cIdx])) {
                    while (canBeIgnored(candidateBuf[cIdx]) && cIdx < len) {
                        cIdx++;
                    }
                    int offset = 0;
                    for (int i = idx + 1; i < len; i++) {
                        if (!canBeIgnored(buf[i]) && buf[i] != candidateBuf[cIdx]) {
                            cIdx = 0;
                            idx = textLC.indexOf(candidateBuf[0], idx + 1);
                            if (idx == -1) {
                                return false;
                            }
                            inMatch = false;
                            break;
                        } else {
                            offset++;
                            if (buf[i] == candidateBuf[cIdx]) {
                                cIdx++;
                                offset++;
                                break;
                            }
                        }
                    }
                    if (offset > 0) {
                        idx += offset;
                    }
                } else if (inMatch && canBeIgnored(buf[idx])) {
                    while (canBeIgnored(buf[idx]) && idx < len) {
                        idx++;
                    }
                    if (buf[idx] == candidateBuf[cIdx]) {
                        cIdx++;
                        idx++;
                    } else {
                        idx = textLC.indexOf(candidateBuf[0], idx + 1);
                        if (idx == -1) {
                            return false;
                        }
                        inMatch = false;
                    }

                } else {
                    cIdx = 0;
                    idx = text.indexOf(candidateBuf[0], idx + 1);
                    if (idx == -1) {
                        return false;
                    }
                    inMatch = false;
                }
            }
        }
        return true;
    }

    static boolean canBeIgnored(char c) {
        return Character.isWhitespace(c) || c == '.' || c == ',' || c == '-';
    }


    public static void main(String[] args) {
        System.out.println(fuzzyContains("Washington, D. C.", "Washington - D.C."));
    }
}