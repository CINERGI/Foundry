package org.neuinfo.foundry.common.util;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.StreamTokenizer;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 * <p>
 * Xpath like processor for JSON based on the syntax from <code>http://goessner.net/articles/JsonPath/</code>
 * </p>
 * Created by bozyurt on 5/22/14.
 */
public class JSONPathProcessor {


    public List<Object> find(String jsonPathExpr, JSONObject json) throws Exception {
        Parser parser = new Parser(jsonPathExpr);

        final Path path = parser.parse();
        List<Object> list = new ArrayList<Object>(5);
        find(path.start, list, json);
        return list;
    }

    private void findMatching(Node node, Object parent, List<Object> list) {
        if (parent instanceof JSONObject) {
            JSONObject p = (JSONObject) parent;
            if (p.has(node.name)) {
                list.add(p.get(node.name));
            }
            final Iterator<String> keys = p.keys();
            while (keys.hasNext()) {
                String key = keys.next();
                Object child = p.get(key);
                if (child instanceof JSONObject || child instanceof JSONArray) {
                    findMatching(node, child, list);
                }
            }
        } else if (parent instanceof JSONArray) {
            JSONArray jsArr = (JSONArray) parent;
            for (int i = 0; i < jsArr.length(); i++) {
                Object child = jsArr.get(i);
                if (child instanceof JSONObject || child instanceof JSONArray) {
                    findMatching(node, child, list);
                }
            }
        }

    }

    private void find(Node node, List<Object> list, Object parent) {
        if (node.type == Node.FROM_ROOT || node.type == Node.INNER_NODE) {
            if (parent instanceof JSONObject) {
                JSONObject p = (JSONObject) parent;
                if (!p.has(node.name)) {
                    return;
                }
                if (node.includeAll || node.idx >= 0) {
                    if (node.idx >= 0) {
                        final Object o = p.getJSONArray(node.name).get(node.idx);
                        if (node.next == null) {
                            list.add(o);
                        } else {
                            find(node.next, list, o);
                        }
                    } else if (node.includeAll) {
                        final JSONArray jsArr = p.getJSONArray(node.name);
                        if (node.next == null) {
                            list.add(jsArr);
                        } else {
                            for (int i = 0; i < jsArr.length(); i++) {
                                find(node.next, list, jsArr.get(i));
                            }
                        }
                    }
                } else {
                    Object js = p.get(node.name);
                    if (node.next == null) {
                        list.add(js);
                    } else {
                        find(node.next, list, js);
                    }
                }
            }
        } else if (node.type == Node.FROM_ANYWHERE) {
            List<Object> jsNodes = new LinkedList<Object>();
            findMatching(node, parent, jsNodes);

            if (!jsNodes.isEmpty()) {
                if (node.next == null) {
                    for (Object jsNode : jsNodes) {
                        list.add(jsNode);
                    }
                }
                for (Object jsNode : jsNodes) {
                    x(node.next, jsNode, list);
                }
            }
        }
    }

    private void x(Node node, Object jsNode, List<Object> list) {
        if (jsNode instanceof JSONObject) {
            JSONObject p = (JSONObject) jsNode;
            if (node.includeAll || node.idx >= 0) {
                if (node.idx >= 0) {
                    final Object o = p.getJSONArray(node.name).get(node.idx);
                    if (o instanceof JSONObject) {
                        JSONObject js = (JSONObject) o;
                        if (node.next == null) {
                            list.add(js);
                        } else {
                            find(node.next, list, js);
                        }
                    } else if (jsNode instanceof JSONArray) {
                        //TODO
                    }
                }
            } else {
                if (p.has(node.name)) {
                    Object js = p.get(node.name);
                    if (node.next == null) {
                        list.add(js);
                    } else {
                        find(node.next, list, js);
                    }
                }
            }
        } else if (jsNode instanceof JSONArray) {
            JSONArray jsArr = (JSONArray) jsNode;
            if (node.idx >= 0) {
                final Object o = jsArr.get(node.idx);
                if (node.next == null) {
                    list.add(o);
                } else {
                    find(node.next, list, o);
                }
            } else if (node.includeAll) {
                if (node.next == null) {
                    list.add(jsArr);
                } else {
                    find(node.next, list, jsArr);
                }
            }
        } else {
            list.add(jsNode);
        }
    }


    public static class Parser {
        StreamTokenizer stok;

        public Parser(String jsonPathStr) {
            stok = new StreamTokenizer(new StringReader(jsonPathStr));
            stok.resetSyntax();
            stok.wordChars('a', 'z');
            stok.wordChars('A', 'Z');
            stok.wordChars('_', '_');
            stok.wordChars('0', '9');
            stok.whitespaceChars(' ', ' ');
            stok.whitespaceChars('\t', '\t');
            stok.ordinaryChar('.');
            stok.ordinaryChar('$');
            stok.ordinaryChar('*');
            stok.ordinaryChar('[');
            stok.ordinaryChar(']');
            stok.quoteChar('\'');
        }

        public Path parse() throws Exception {
            int tokCode;
            Path path = null;
            while ((tokCode = stok.nextToken()) != StreamTokenizer.TT_EOF) {
                if (tokCode == '$') {
                    tokCode = stok.nextToken();
                    if (tokCode == '.') {
                        int tc2 = stok.nextToken();
                        if (tc2 == StreamTokenizer.TT_WORD || tc2 == '\'') {
                            String name = stok.sval;
                            int tc3 = stok.nextToken();
                            Node node;
                            if (tc3 == '[') {
                                node = handleIndex(name);
                                node.type = Node.FROM_ROOT;
                            } else {
                                stok.pushBack();
                                node = new Node(name, Node.FROM_ROOT);
                            }
                            if (path == null) {
                                path = new Path(node);
                            } else {
                                path.add(node);
                            }
                        } else if (tc2 == '.') {
                            tc2 = stok.nextToken();
                            checkIfWordOrString(tc2);
                            String name = stok.sval;
                            path = prepPath(path, name);
                        } else {
                            throw new Exception("Invalid syntax after $.!");
                        }
                    } else if (tokCode == '[') {
                        int tc2 = stok.nextToken();
                        if (tc2 == '\'') {
                            String name = stok.sval;
                            Node node;
                            if (path == null) {
                                node = new Node(name, Node.FROM_ROOT);
                                path = new Path(node);
                            } else {
                                node = new Node(name, Node.INNER_NODE);
                                path.add(node);
                            }
                            int tc3 = stok.nextToken();
                            checkIfValid(name, tc3, ']');
                        } else {
                            //TODO
                        }
                    } else {
                        throw new Exception("Syntax error '.' or '[' expected after root $!");
                    }
                } else if (tokCode == '.') {
                    int tc2 = stok.nextToken();
                    if (tc2 == StreamTokenizer.TT_WORD || tc2 == '\'') {
                        String name = stok.sval;
                        int tc3 = stok.nextToken();
                        Node node;
                        if (tc3 == '[') {
                            node = handleIndex(name);
                        } else {
                            stok.pushBack();
                            node = new Node(name, Node.INNER_NODE);
                        }
                        path.add(node);
                    } else if (tc2 == '.') {
                        tc2 = stok.nextToken();
                        checkIfWordOrString(tc2);
                        String name = stok.sval;
                        path = prepPath(path, name);
                    }
                } else if (tokCode == '[') {
                    int tc2 = stok.nextToken();
                    if (tc2 == '\'') {
                        String name = stok.sval;
                        Node node = new Node(name, Node.INNER_NODE);
                        path.add(node);
                        int tc3 = stok.nextToken();
                        checkIfValid(name, tc3, ']');

                    }

                }
            }
            return path;
        }

        private void checkIfWordOrString(int tc) throws Exception {
            if (tc != StreamTokenizer.TT_WORD && tc != '\'') {
                throw new Exception("A word of string is expected but found " + (char) tc);
            }
        }

        private Path prepPath(Path path, String name) throws Exception {
            int tc3 = stok.nextToken();
            Node node;
            if (tc3 == '[') {
                node = handleIndex(name);
                node.type = Node.FROM_ANYWHERE;
            } else {
                stok.pushBack();
                node = new Node(name, Node.FROM_ANYWHERE);
            }
            if (path == null) {
                path = new Path(node);
            } else {
                path.add(node);
            }
            return path;
        }

        private void checkIfValid(String name, int tc, int expected) throws Exception {
            if (tc == StreamTokenizer.TT_EOF) {
                throw new Exception("Unexpected end of expression after " + name);
            }
            if (tc != expected) {
                throw new Exception("Expected '" + (char) expected + " after " + name);
            }
        }

        private Node handleIndex(String name) throws Exception {
            final int tc = stok.nextToken();
            if (tc == '*') {
                int tc2 = stok.nextToken();
                checkIfValid(name, tc2, ']');
                Node node = new Node(name, Node.INNER_NODE);
                node.includeAll = true;
                return node;
            } else if (tc == StreamTokenizer.TT_WORD) {
                int index = Integer.parseInt(stok.sval);
                Node node = new Node(name, Node.INNER_NODE);
                node.idx = index;
                int tc2 = stok.nextToken();
                checkIfValid(name, tc2, ']');
                return node;
            }
            return null;
        }
    }

    public static class Node {
        Node next;
        Node prev;
        String name;
        int idx = -1;
        int type = FROM_ROOT;
        boolean includeAll;
        public final static int FROM_ROOT = 1;
        public final static int FROM_ANYWHERE = 2;
        public final static int INNER_NODE = 3;

        public Node(String name, int type) {
            this.name = name;
            this.type = type;
        }

        @Override
        public String toString() {
            final StringBuilder sb = new StringBuilder("Node{");
            sb.append("name='").append(name).append('\'');
            sb.append(", type=").append(type);
            if (idx >= 0) {
                sb.append(", idx=").append(idx);
            }
            sb.append('}');
            return sb.toString();
        }
    }

    public static class Path {
        Node start;
        Node current;

        public Path(Node start) {
            this.start = start;
            this.current = this.start;
        }

        public void add(Node node) {
            this.current.next = node;
            node.prev = this.current;
            this.current = node;
        }

        @Override
        public String toString() {
            final StringBuilder sb = new StringBuilder("Path{");
            Node p = start;
            while (p != null) {
                sb.append("\n\t").append(p);
                p = p.next;
            }
            sb.append('}');
            return sb.toString();
        }

    }


    public static void testParser() throws Exception {

        String jsonPath = "$..author";
        jsonPath = "$.store.book[*].author";
        jsonPath = "$..book[2]";
        jsonPath = "$.store..price";
        jsonPath = "$['store']['book']['title']";
        jsonPath = "$.'$'[0]";
        jsonPath = "$..'$'[0]";
        jsonPath = "$.store..'$'[0]";

        Parser parser = new Parser(jsonPath);

        final Path path = parser.parse();
        System.out.println(path);

        JSONObject json = new JSONObject(TEST);
        JSONPathProcessor processor = new JSONPathProcessor();

        // final List<Object> list = processor.find("$.store.book[2].author", json);
        //final List<Object> list = processor.find("$.store..price", json);
        //final List<Object> list = processor.find("$..book[2]", json);
        final List<Object> list = processor.find("$.store.book[*].author", json);
        for (Object o : list) {
            System.out.println(o);
        }

    }


    static final String TEST = "{ \"store\": {\n" +
            "    \"book\": [ \n" +
            "      { \"category\": \"reference\",\n" +
            "        \"author\": \"Nigel Rees\",\n" +
            "        \"title\": \"Sayings of the Century\",\n" +
            "        \"price\": 8.95\n" +
            "      },\n" +
            "      { \"category\": \"fiction\",\n" +
            "        \"author\": \"Evelyn Waugh\",\n" +
            "        \"title\": \"Sword of Honour\",\n" +
            "        \"price\": 12.99\n" +
            "      },\n" +
            "      { \"category\": \"fiction\",\n" +
            "        \"author\": \"Herman Melville\",\n" +
            "        \"title\": \"Moby Dick\",\n" +
            "        \"isbn\": \"0-553-21311-3\",\n" +
            "        \"price\": 8.99\n" +
            "      },\n" +
            "      { \"category\": \"fiction\",\n" +
            "        \"author\": \"J. R. R. Tolkien\",\n" +
            "        \"title\": \"The Lord of the Rings\",\n" +
            "        \"isbn\": \"0-395-19395-8\",\n" +
            "        \"price\": 22.99\n" +
            "      }\n" +
            "    ],\n" +
            "    \"bicycle\": {\n" +
            "      \"color\": \"red\",\n" +
            "      \"price\": 19.95\n" +
            "    }\n" +
            "  }\n" +
            "}\n" +
            "\n" +
            "XPath \tJSONPath";

    public static void main(String[] args) throws Exception {

        testParser();
    }
}
