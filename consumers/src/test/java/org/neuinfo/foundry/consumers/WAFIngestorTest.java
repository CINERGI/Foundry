package org.neuinfo.foundry.consumers;

import junit.framework.TestCase;
import org.neuinfo.foundry.consumers.jms.consumers.ingestors.WAFIngestor;
import org.neuinfo.foundry.consumers.plugin.Ingestor;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by bozyurt on 11/28/15.
 */
public class WAFIngestorTest extends TestCase {
    public WAFIngestorTest(String name) {
        super(name);
    }

    public void testWAFSubdirs() throws Exception {
        Map<String, String> options = new HashMap<String, String>(3);
        options.put("ingestURL", "http://maxim.ucsd.edu/waf/data.gov_all");
        Ingestor ingestor = new WAFIngestor();
        ingestor.initialize(options);
        ingestor.startup();
    }
}
