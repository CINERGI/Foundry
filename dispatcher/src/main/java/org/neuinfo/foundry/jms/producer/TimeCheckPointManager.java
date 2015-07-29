package org.neuinfo.foundry.jms.producer;

import org.bson.types.BSONTimestamp;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.input.SAXBuilder;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;
import org.neuinfo.foundry.common.util.Utils;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;

/**
 * Created by bozyurt on 5/2/14.
 */
public class TimeCheckPointManager {
    private File xmlFile;
    private Element rootEl;
    private BSONTimestamp lastHandledTS;
    private static TimeCheckPointManager instance;

    private TimeCheckPointManager(File xmlFile) throws Exception {
        this.xmlFile = xmlFile;
        if (xmlFile.isFile()) {
            SAXBuilder builder = new SAXBuilder();
            Document doc = builder.build(xmlFile);
            rootEl = doc.getRootElement();
            if (rootEl.getChild("cp-time") != null) {
                Element child = rootEl.getChild("cp-time");
                int time = Integer.parseInt(child.getAttributeValue("time"));
                int incr = Integer.parseInt(child.getAttributeValue("incr"));
                this.lastHandledTS = new BSONTimestamp(time, incr);
            }
        }
    }

    public synchronized static TimeCheckPointManager getInstance(File xmlFile) throws Exception {
        if (instance == null) {
            instance = new TimeCheckPointManager(xmlFile);
        }
        return instance;
    }

    public synchronized  static TimeCheckPointManager getInstance() {
        if (instance == null) {
            throw new RuntimeException("TimeCheckPointManager is not properly initialized!");
        }
        return instance;
    }

    public synchronized BSONTimestamp getLastCheckPointTime() {
        return this.lastHandledTS;
    }

    public synchronized void setCheckPointTime(BSONTimestamp ts) {
        this.lastHandledTS = ts;
    }

    public synchronized void checkpoint() throws Exception {
        if (this.lastHandledTS == null) {
            // nothing to checkpoint
            return;
        }
        XMLOutputter xmlOutputter = new XMLOutputter(Format.getPrettyFormat());

        Document doc = new Document();
        Element rootEl = new Element("checkpoint");
        doc.setRootElement(rootEl);
        Element el = new Element("cp-time");
        rootEl.addContent(el);
        el.setAttribute("time", String.valueOf(this.lastHandledTS.getTime()));
        el.setAttribute("incr", String.valueOf(this.lastHandledTS.getInc()));

        BufferedWriter out = null;
        try {
            out = new BufferedWriter(new FileWriter(xmlFile));
            xmlOutputter.output(rootEl, out);
        } finally {
            Utils.close(out);
        }
    }
}
