package org.neuinfo.foundry.consumers.jms.consumers.plugins;

import com.mongodb.DBObject;
import org.apache.log4j.Logger;
import org.jdom2.Element;
import org.neuinfo.foundry.common.util.Assertion;
import org.neuinfo.foundry.common.util.ISOXMLGenerator2;
import org.neuinfo.foundry.common.util.LargeDataSetDirectoryAssigner;
import org.neuinfo.foundry.common.util.Utils;
import org.neuinfo.foundry.consumers.plugin.IPlugin;
import org.neuinfo.foundry.consumers.plugin.Result;

import java.io.File;
import java.util.Map;

/**
 * Created by bozyurt on 11/25/14.
 */
public class WAFExporter implements IPlugin {
    private String outDirectory;
    private final static Logger logger = Logger.getLogger(WAFExporter.class);

    @Override
    public void initialize(Map<String, String> options) throws Exception {
        this.outDirectory = options.get("outDirectory");
    }

    @Override
    public Result handle(DBObject docWrapper) {
        try {
            DBObject procDBO = (DBObject) docWrapper.get("Processing");
            String status = procDBO.get("status").toString();
            String primaryKey = docWrapper.get("primaryKey").toString();
            DBObject siDBO = (DBObject) docWrapper.get("SourceInfo");
            String srcName = siDBO.get("Name").toString();

            ISOXMLGenerator2 generator = new ISOXMLGenerator2();
            Element docEl = generator.generate(docWrapper);
            // also enhance existing keywords 12/01/2015
            /* FIXME not enhancing existing keywords
            ExistingKeywordsFacetHandler handler = new ExistingKeywordsFacetHandler(docEl);
            docEl = handler.handle(docWrapper);
            */
            saveEnhancedXmlFile(primaryKey, docEl, srcName, status);
            return new Result(docWrapper, Result.Status.OK_WITH_CHANGE);
        } catch (Throwable t) {
            t.printStackTrace();
            Result r = new Result(docWrapper, Result.Status.ERROR);
            r.setErrMessage(t.getMessage());
            return r;
        }
    }


    private void saveEnhancedXmlFile(String fileIdentifier, Element docEl, String srcName,
                                     String status) throws Exception {
        String sourceDirname = srcName.replaceAll("\\s+", "_");
        File sourceDir = new File(this.outDirectory, sourceDirname);
        if (status.equals("annotated.1")) {
            sourceDir = new File(sourceDir, "annotated");
        }
        if (!sourceDir.isDirectory()) {
            sourceDir.mkdirs();
        }
        sourceDir.setLastModified(System.currentTimeMillis()); // touch directory
        Assertion.assertTrue(sourceDir.isDirectory());
        if ((srcName.equals("Data.gov") || srcName.equals("ScienceBase WAF"))
                && !status.equals("annotated.1")) {
            LargeDataSetDirectoryAssigner assigner =
                    LargeDataSetDirectoryAssigner.getInstance(sourceDir.getAbsolutePath());
            sourceDir = assigner.getNextDirPath();
        }
        fileIdentifier = fileIdentifier.replaceAll("/", "__");
        fileIdentifier = fileIdentifier.replaceAll("\\.+", "_");
        fileIdentifier = fileIdentifier.replaceAll("[:;,\\s\\?]+", "_");
        File enhancedXmlFile = new File(sourceDir, fileIdentifier + ".xml");
        Utils.saveXML(docEl, enhancedXmlFile.getAbsolutePath());
        logger.info("saved enhancedXmlFile to " + enhancedXmlFile);
    }


    @Override
    public String getPluginName() {
        return "WAFExporter";
    }
}
