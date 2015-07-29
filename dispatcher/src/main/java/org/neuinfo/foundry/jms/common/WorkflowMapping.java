package org.neuinfo.foundry.jms.common;

import org.jdom2.Element;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by bozyurt on 10/30/14.
 */
public class WorkflowMapping {
    String workflowName;
    List<String> steps;
    String ingestorOutStatus;
    /** if the document is updated, some steps such as doc id generation can be skipped */
    String updateOutStatus;

    public WorkflowMapping(String workflowName) {
        this.workflowName = workflowName;
    }

    public String getWorkflowName() {
        return workflowName;
    }

    public List<String> getSteps() {
        return steps;
    }

    public String getIngestorOutStatus() {
        return ingestorOutStatus;
    }

    public String getUpdateOutStatus() {
        return updateOutStatus;
    }

    public static WorkflowMapping fromXml(Element elem) throws Exception {
        String name = elem.getAttributeValue("name");
        WorkflowMapping wm = new WorkflowMapping(name);
        List<Element> children = elem.getChildren("step");
        wm.steps = new ArrayList<String>(children.size());
        for (Element el : children) {
            wm.steps.add(el.getTextTrim());

        }
        wm.ingestorOutStatus = elem.getAttributeValue("ingestorOutStatus");
        wm.updateOutStatus = elem.getAttributeValue("updateOutStatus");
        return wm;
    }


}
