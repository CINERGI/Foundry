package org.neuinfo.foundry.common.util;

import java.util.LinkedList;
import java.util.List;

/**
 * Created by bozyurt on 5/13/15.
 */
public class Node {
    List<Node> children = new LinkedList<Node>();
    String category;
    String mapTo;
    Node parent;

    public Node(String category, Node parent) {
        this(category, parent, null);
    }

    public Node(String category, Node parent, String mapTo) {
        this.category = category;
        this.parent = parent;
        this.mapTo = mapTo;
    }

    public void addChild(Node child) {
        if (!children.contains(child)) {
            children.add(child);
        }
    }

    public boolean hasChildren() {
        return !children.isEmpty();
    }

    public List<Node> getChildren() {
        return children;
    }

    public String getCategory() {
        return category;
    }

    public String getMapTo() {
        return mapTo;
    }

    public Node getParent() {
        return parent;
    }
}
