package org.neuinfo.foundry.consumers;

import junit.framework.TestCase;
import org.neuinfo.foundry.common.model.ScicrunchResourceRec;
import org.neuinfo.foundry.common.model.Source;
import org.neuinfo.foundry.consumers.jms.consumers.ScicrunchResourceIngestorConsumer;
import org.neuinfo.foundry.consumers.jms.consumers.plugins.ScicrunchResourceReader;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.*;

/**
 * Created by bozyurt on 4/21/15.
 */
public class ScicrunchIngestorConsumerTest extends TestCase {

    public ScicrunchIngestorConsumerTest(String name) {
        super(name);
    }

    public void testPrepareSource() throws Exception {
        ScicrunchResourceRec rec = new ScicrunchResourceRec();
        rec.setDataSetName("Test Data");
        Source source = ScicrunchResourceIngestorConsumer.prepareSource(rec);
        assertNotNull(source);
        System.out.println(source.toJSON().toString(2));
    }

    public void testParseKeywords() throws Exception {
        List<ScicrunchResourceRec.UserKeyword> userKeywords =
                ScicrunchResourceIngestorConsumer.parseKeywords(
                        "theme: something , some other thing location:Australia");
        for (ScicrunchResourceRec.UserKeyword uk : userKeywords) {
            System.out.println(uk.toString());
        }
    }

    public void testScicrunchIngestorConsumer() throws Exception {
        Map<String, String> map = new HashMap<String, String>(17);
        map.put("Resource Name","Cinergi 1");
        map.put("Description","This is a dummy description with LIDAR, sediments, lakes and calderas.");
        map.put("Keywords","theme: something , some other thing location:Australia");
        map.put("email","iozyurt@ucsd.edu");
        ScicrunchResourceReader mockSR = mock(ScicrunchResourceReader.class);
        when(mockSR.getLastInsertedResourceID()).thenReturn(1L);
        when(mockSR.getResourceData(anyLong())).thenReturn(map);
        when(mockSR.getEmail(anyLong())).thenReturn("iozyurt@ucsd.edu");
        doNothing().when(mockSR).startup();
        doNothing().when(mockSR).shutdown();

        ScicrunchResourceIngestorConsumer consumer = new ScicrunchResourceIngestorConsumer("dummyQueue");
        consumer.setCollectionName("records");
        consumer.setOutStatus("new.1");
        try {
            consumer.setScicrunchResourceReader(mockSR);
            consumer.startup("cinergi-consumers-cfg.xml");

            consumer.handle();


        } finally {
            consumer.shutdown();
        }

    }
}
