package org.neuinfo.foundry.common.util;

import org.apache.commons.lang.StringUtils;
import org.jdom2.Attribute;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.input.SAXBuilder;

import java.io.InputStream;
import java.util.*;

/**
 * Created by bozyurt on 4/28/15.
 */
public class CategoryHierarchyHandler implements IHierarchyHandler {
    Node root;
    Map<String, Node> nodeMap = new HashMap<String, Node>();
    Map<String, String> scigraph2CinergiCategoryMap = new HashMap<String, String>();
    Map<String, String> cinergi2ScigraphCategoryMap = new HashMap<String, String>();
    List<String> sortedCinergiCategories;
    private static CategoryHierarchyHandler instance = null;

    public static synchronized CategoryHierarchyHandler getInstance() {
        if (instance == null) {
            instance = new CategoryHierarchyHandler();
        }
        return instance;
    }

    private CategoryHierarchyHandler() {
        root = new Node("Thing", null);
        SAXBuilder builder = new SAXBuilder();
        InputStream in = null;
        try {
            in = CategoryHierarchyHandler.class.getClassLoader().getResourceAsStream("steve_categories.xml");
            Document doc = builder.build(in);
            Element docRoot = doc.getRootElement();
            List<Element> level1Elems = docRoot.getChildren("level1");
            for (Element l1El : level1Elems) {
                String category = l1El.getTextTrim();
                Attribute mapTo = l1El.getAttribute("mapTo");

                Node child = new Node(category, root);
                root.addChild(child);
                String categoryPath = prepCategoryPath(child);
                nodeMap.put(categoryPath, child);
                if (mapTo != null) {
                    prepMapping(mapTo.getValue(), categoryPath);
                } else {
                    prepMapping(category, categoryPath);
                }
                List<Element> level2Elems = l1El.getChildren("level2");
                if (level2Elems != null) {
                    for (Element l2El : level2Elems) {
                        String childCategory = l2El.getTextTrim();
                        Node n = new Node(childCategory, child);
                        child.addChild(n);
                        categoryPath = prepCategoryPath(n);
                        nodeMap.put(categoryPath, n);
                        mapTo = l2El.getAttribute("mapTo");
                        if (mapTo != null) {
                            prepMapping(mapTo.getValue(), categoryPath);
                        } else {
                            prepMapping(childCategory, categoryPath);
                        }
                    }
                }
            }
        } catch (Exception x) {
            x.printStackTrace();
        } finally {
            Utils.close(in);
        }
    }

    void prepMapping(String scigraphCategories, String categoryPath) {
        String[] scigraphCategoryArr = scigraphCategories.split(",");
        for(String scigraphCategory : scigraphCategoryArr) {
            this.cinergi2ScigraphCategoryMap.put(categoryPath, scigraphCategory);
            this.scigraph2CinergiCategoryMap.put(scigraphCategory, categoryPath);
        }
    }

    String prepCategoryPath(Node node) {
        List<String> path = new ArrayList<String>(5);
        while (node.parent != null) {
            if (node.category.equals("Thing")) {
                break;
            }
            path.add(node.category);
            node = node.parent;
        }
        Collections.reverse(path);
        String categoryPath = StringUtils.join(path," > ");
        return categoryPath;
    }


    public String getCinergiCategory(String category) {
        return this.scigraph2CinergiCategoryMap.get(category);
    }

    public String getScigraphCategory(String cinergiCategory) {
        return this.cinergi2ScigraphCategoryMap.get(cinergiCategory);
    }

    public List<String> getSortedCinergiCategories() {
        if (sortedCinergiCategories == null) {
            sortedCinergiCategories = new ArrayList<String>(this.cinergi2ScigraphCategoryMap.size());
            for(String category : cinergi2ScigraphCategoryMap.keySet()) {
                sortedCinergiCategories.add(category);
            }
            Collections.sort(sortedCinergiCategories);
        }
        return sortedCinergiCategories;
    }

    public int getPathLength2Root(String categoryPath) {
        if (categoryPath == null) {
            return -1;
        }
        Node node = nodeMap.get(categoryPath);
        if (node == null) {
            return -1;
        }
        int len = 0;
        while (node.parent != null) {
            len++;
            node = node.parent;
        }
        return len;
    }

    public static void main(String[] args) {
        CategoryHierarchyHandler instance = CategoryHierarchyHandler.getInstance();

        System.out.println(instance.getCinergiCategory("atmospheric process"));
        System.out.println(instance.getCinergiCategory("Location"));
        System.out.println(instance.getCinergiCategory("Water body"));
        System.out.println(instance.getCinergiCategory("Material entity"));

        System.out.println(instance.getSortedCinergiCategories());
    }

}
