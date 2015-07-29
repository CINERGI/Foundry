package org.neuinfo.foundry.common.util;

import java.util.List;

/**
 * Created by bozyurt on 7/23/15.
 */
public interface IHierarchyHandler {
    public String getCinergiCategory(String category);

    public String getScigraphCategory(String cinergiCategory);

    public List<String> getSortedCinergiCategories();

    public int getPathLength2Root(String categoryPath);
}
