package org.neuinfo.foundry.consumers.jms.consumers.plugins;

import com.esri.geoportal.commons.utils.SimpleCredentials;
import com.mongodb.DBObject;
import org.apache.log4j.Logger;
;
import org.jdom2.Element;
import org.neuinfo.foundry.common.util.ISOXMLGenerator2;
import org.neuinfo.foundry.common.util.Utils;
import org.neuinfo.foundry.consumers.plugin.IPlugin;
import org.neuinfo.foundry.consumers.plugin.Result;

import java.net.URL;
import java.util.Map;
import com.esri.geoportal.commons.gpt.client.*;


/**
 * Created by valentine.
 */
public class Geoportal2Exporter implements IPlugin {
    private String gptURI;
    private URL gptURL;
    private String user;
    private String password;
    private String elasticsearchIndex;
    private SimpleCredentials cred;

    private final static Logger logger = Logger.getLogger(Geoportal2Exporter.class);

    private Client gptClient;
    private ISOXMLGenerator2 xmlGenerator;

    @Override
    public void initialize(Map<String, String> options) throws Exception {
        this.gptURI = options.get("gptURI");
        this.user = options.get("user");
        this.password = options.get("password");
        this.elasticsearchIndex = options.get("elasticsearchIndex");

        this.cred = new SimpleCredentials(this.user,this.password);
       this.gptURL = new URL(this.gptURI);
        this.gptClient = new Client(this.gptURL,this.cred, this.elasticsearchIndex);
this.xmlGenerator = new ISOXMLGenerator2();

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
            PublishRequest pubRequest = new PublishRequest();
            pubRequest.src_source_name_s = srcName + " (P)";
          //  pubRequest.src_lastupdate_dt = new DateTime.Today().toString();
            pubRequest.src_source_type_s = "Enhanced";
            pubRequest.src_uri_s = primaryKey;

            publishEnhancedXmlFile(primaryKey, docEl, pubRequest, status);
            return new Result(docWrapper, Result.Status.OK_WITH_CHANGE);
        } catch (Throwable t) {
            t.printStackTrace();
            Result r = new Result(docWrapper, Result.Status.ERROR);
            r.setErrMessage(t.getMessage());
            return r;
        }
    }


    private void publishEnhancedXmlFile(String fileIdentifier, Element docEl, PublishRequest pubRequest,
                                        String status) throws Exception {
        String xml = Utils.xmlAsString(docEl);
        PublishResponse response =  this.gptClient.publish(pubRequest, fileIdentifier,
              xml, null, true  );
        logger.info("publish enhancedXmlFile to " + this.gptURI);
    }


    @Override
    public String getPluginName() {
        return "Geoportal2Exporter";
    }
}
