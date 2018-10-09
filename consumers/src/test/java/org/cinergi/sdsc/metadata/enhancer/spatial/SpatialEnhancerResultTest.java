package org.cinergi.sdsc.metadata.enhancer.spatial;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

public class SpatialEnhancerResultTest  {
    static  StanfordNEDLocationFinder finder;

    @BeforeAll
    static void setUp() throws Exception {

        finder = new StanfordNEDLocationFinder();
        finder.startup();
    }

//    @AfterAll;
//    static void tearDown() throws Exception {
//
//    }

    @ParameterizedTest
    @ValueSource(strings = { "spatial_moorea_50578229e4b01ad7e027fc0f.xml",
            "spatial_moorea_crescynt_58.xml",
            "spatial_world_issue_usecaseDS_6.xml" })
    public void testSpatialEnhancerResultFile(String filename){

    }
}
