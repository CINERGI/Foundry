package org.neuinfo.foundry.consumers.common;

import java.util.List;
import java.util.Map;

/**
 * Created by bozyurt on 9/22/17.
 */
public class EditDiffInfo {
    Map<String, List<DiffRecord>> updateDiffMap;
    Map<String, List<DiffRecord>> deleteDiffMap;
    Map<String, List<DiffRecord>> addDiffMap;


    public EditDiffInfo(Map<String, List<DiffRecord>> updateDiffMap, Map<String, List<DiffRecord>> deleteDiffMap,
                        Map<String, List<DiffRecord>> addDiffMap) {
        this.updateDiffMap = updateDiffMap;
        this.deleteDiffMap = deleteDiffMap;
        this.addDiffMap = addDiffMap;
    }

    public Map<String, List<DiffRecord>> getUpdateDiffMap() {
        return updateDiffMap;
    }

    public Map<String, List<DiffRecord>> getDeleteDiffMap() {
        return deleteDiffMap;
    }

    public Map<String, List<DiffRecord>> getAddDiffMap() {
        return addDiffMap;
    }
}
