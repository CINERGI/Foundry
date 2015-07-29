package org.neuinfo.foundry.common.util;

import org.jdom2.Element;

import java.io.File;

/**
 * Created by bozyurt on 4/7/15.
 */
public class ISOXMLFixerDriver {

    public static void usage() {
        System.err.println("Usage: ISOXMLFixerDriver <iso-xml-dir>");
        System.exit(1);
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 1) {
            usage();
        }
        File isoXmlDir = new File(args[0]);
        if (!isoXmlDir.isDirectory()) {
            usage();
        }
        for (File f : isoXmlDir.listFiles()) {
            if (f.isFile() && f.getName().endsWith(".xml")) {
                Utils.copyFile(f.getAbsolutePath(), f.getAbsolutePath() + ".bak");
                Element docEl = Utils.loadXML(f.getAbsolutePath());
                docEl = ISOXMLFixer.fixAnchorProblem(docEl);
                Utils.saveXML(docEl, f.getAbsolutePath());
            }
        }

    }
}
