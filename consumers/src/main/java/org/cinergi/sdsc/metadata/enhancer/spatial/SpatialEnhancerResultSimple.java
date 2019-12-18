
package org.cinergi.sdsc.metadata.enhancer.spatial;

import bsh.commands.dir;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.code.geocoder.model.LatLng;
import com.google.code.geocoder.model.LatLngBounds;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.text.StringEscapeUtils;
import org.apache.log4j.Logger;
import org.neuinfo.foundry.common.util.Utils;
import ucar.unidata.geoloc.LatLonPoint;
import ucar.unidata.geoloc.LatLonPointImpl;
import ucar.unidata.geoloc.LatLonRect;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.Unmarshaller;
import java.io.File;
import java.math.BigDecimal;
import java.util.*;

public class SpatialEnhancerResultSimple {
    StanfordNEDLocationFinder stanfordNEDLocationFinder;

    @JsonProperty("text")
    private String text;

    @JsonProperty("derived_place_from_text")
    private List<String> extractedPlaces = new ArrayList<String>();

    @JsonProperty("derived_bounding_boxes_from_derived_place")
    private Map<String, LatLngBounds> extractedPlace2Bounds = new HashMap<String, LatLngBounds>();

    // logger
    private static Logger log = Logger.getLogger("SpatialEnhancerResultSimple");


    // get text description from  metadata, in this case, it is just a string, so no extraction
    public void getTextDescription(String metadata) throws Exception {
        this.text = metadata;
    }


    public SpatialEnhancerResultSimple(String textInput, StanfordNEDLocationFinder finder) {
        this.stanfordNEDLocationFinder = finder;
        try {

            log.info("Text from WebService");
            if (StringUtils.isNotBlank(textInput)) {
                getTextDescription(textInput);
                enhance();
            } else {
                log.info("Empty String: ");
            }
        } catch (Exception exception) {
            exception.printStackTrace();
            log.info("Unknown Metadata, failed to parse");

        }

    }


    private void enhance() throws Exception {

        // show text
        log.info("-----------------------------------");
        log.info("Found text: " + this.text);

        log.info("-----------------------------------");

                log.info("Using text to find bounding boxes (NER) ...");
                this.extractedPlaces.addAll(this.stanfordNEDLocationFinder.getLocationsFromText(this.text));
                //this.extractedPlaces.addAll(SpatialTextLocationFinder.getLocationsFromText(this.text));
        if (this.extractedPlaces.isEmpty()) {
            log.info("Found no place from the text. ");
        } else {
            getBounds(this.extractedPlaces, this.extractedPlace2Bounds, this.text);
        }

    }


    private static void getBounds(List<String> locations, Map<String, LatLngBounds> extractedPlace2Bounds, String text) throws Exception {

        for (String location : locations) {
            log.info("Found a location: " + location);
            // orig
            // List<LatLngBounds> bounds = GoogleGeocoder.getBounds(location);
            // List<LatLngBounds> bounds = DataScienceToolkitGeocoder.getBounds(location);
            // List<LatLngBounds> bounds = TwoFishesGeocoder.getBounds(location);
            Map<String, LatLngBounds> boundsMap = ArcGISGeocoder.getBounds(location);
            if (boundsMap == null){
                boundsMap = GoogleGeocoder.getBounds(location);
            }
            if (boundsMap != null) {
                if (boundsMap.size() == 1) {
                    for (LatLngBounds bound : boundsMap.values()) {
                        log.info("     Bounding box: " + bound);
                        extractedPlace2Bounds.put(location, bound);
                    }
                } else if (boundsMap.size() > 1) {
                    String mostLikelyCandidateLocation = findTheMostLikelyCandidate(location, boundsMap, text);
                    if (mostLikelyCandidateLocation != null) {
                        LatLngBounds bound = boundsMap.get(mostLikelyCandidateLocation);
                        log.info("\t(Most Likely) Bounding Box: " + mostLikelyCandidateLocation + " - " + bound);
                        extractedPlace2Bounds.put(mostLikelyCandidateLocation, bound);
                    }
                    // log.info("     Found multiple bounding boxes. Ignore the location.");
                }
            } else {
                log.info("Found no bounds for:" + location);
            }
        }
    }


    static String findTheMostLikelyCandidate(String location, Map<String, LatLngBounds> boundsMap, String text) {
        if (location.length() < 4) {
            return null;
        }
        int idx = text.indexOf(location);
        if (idx != -1) {
            int len = text.length();
            int startOffset = Math.max(idx - 50, 0);
            int endOffset = Math.min(idx + location.length() + 50, len);
            String window = text.substring(startOffset, endOffset);
            int maxLen = -1;
            String longestMatch = null;
            for (String address : boundsMap.keySet()) {
                log.info("address:" + address + " window:" + window);
                int matchLen = Utils.findLongestContiguousMatchLength(window, address);
                if (matchLen > 0) {
                    if (maxLen < matchLen) {
                        longestMatch = address;
                        maxLen = matchLen;
                    }
                }
            }
            if (longestMatch != null) {
                System.out.println("longestMatch:" + longestMatch);
                return longestMatch;
            }
        }
        return null;
    }


    private static boolean contains(List<LatLngBounds> bounds, LatLngBounds bound) {
        boolean result = false;
        log.info("     Place bounding box: " + bound);
        for (LatLngBounds tmp : bounds) {
            log.info("     Metadata bounding box: " + tmp);
            if (intersect(bound, tmp)) {
                result = true;
                break;
            }
        }
        return result;
    }


    private static LatLonRect getLatLonRect(LatLngBounds bb) {
        LatLonPoint left = new LatLonPointImpl(bb.getSouthwest().getLat().doubleValue(),
                bb.getSouthwest().getLng().doubleValue());
        LatLonPoint right = new LatLonPointImpl(bb.getNortheast().getLat().doubleValue(),
                bb.getNortheast().getLng().doubleValue());
        ;
        return new LatLonRect(left, right);
    }


    private static boolean intersect(LatLngBounds bb1, LatLngBounds bb2) {
        LatLonRect rect1 = getLatLonRect(bb1);
        LatLonRect rect2 = getLatLonRect(bb2);
        LatLonRect rect = rect1.intersect(rect2);
        return rect != null;
    }


    public static void main(String[] args) throws Exception {


       List<String> list = new ArrayList<String>();
        list.add("San Diego, California");
        list.add( "The Type 2 Diabetes (T2D) Genetic Exploration by Next-generation sequencing in Ethnic Samples (T2D-GENES) Consortium is a collaborative international effort to identify genes influencing susceptibility to type 2 diabetes in multiple ethnic groups using next generation sequencing. To fulfill this objective, T2D-GENES Consortium undertook two large sequencing studies, called T2D-GENES Projects 1 and 2. Project 1 has carried out whole exome sequencing of 12,940 individuals, 6,504 with T2D and 6,436 non-diabetic controls, equally divided among five continental ancestry groups: Europeans, East Asians, South Asians, Hispanic Americans, and African Americans. The goal of Project 1 is to identify all genetic variants in the complete coding regions of the genomes (i.e., whole exome) by sequencing, including rare variants. Project 2 (i.e., SAMAFS substudy 2) is a pedigree-based study designed to identify low frequency or rare variants influencing susceptibility to T2D, using whole genome sequence information from approximately 600 individuals in 20 Mexican American T2D-enriched pedigrees from San Antonio, Texas, augmented with family-based imputation into approximately 440 additional family members. The major objectives of Project 2 are to identify low frequency or rare variants in and around known common variant signals for T2D, as well as to find novel low frequency or rare variants influencing susceptibility to T2D. Both T2D-GENES Projects 1 and 2 involve the San Antonio Mexican American Family Studies (SAMAFS), which are composed of four San Antonio, Texas-based family studies: the San Antonio Family Heart Study (SAFHS), San Antonio Family Diabetes/Gallbladder Study (SAFDGS), Veterans Administration Genetic Epidemiology Study (VAGES), and Family Investigation of Nephropathy and Diabetes - San Antonio (FIND-SA) Component and its extension called the Extended FIND [E-FIND]. The SAFHS and SAFDGS began in 1991 and have followed participants with extensive clinical phenotyping related to T2D for over 20 years. The VAGES was initiated in 1994 and a large battery of T2D-related phenotypic data has been obtained from its participants. The FIND-SA began in 2000, a part of the multicenter FIND study which was designed to identify genetic determinants of diabetic kidney disease; data from its participants related to T2D were used for this project.  Non-overlapping subsets of SAMAFS participants are part of the T2D-GENES Projects 1 and 2, henceforth referred to as SAMAFS substudies 1 and 2, respectively. The SAMAFS substudies 1 and 2 are part of one of the five awards funded by NIDDK under a cooperative agreement award mechanism, which is governed by the Steering Committee of the T2D-GENES Consortium. Since Project 1 relies on population based subsets of cases and controls, 491 unrelated participants are drawn from the four SAMAFS as part of the T2D-GENES Project 1 Mexican American sample (i.e., SAMAFS substudy 1). The whole exome sequencing was performed at the Broad Institute. For Project 2, 1048 individuals are drawn from two SAMAFS (SAFHS and SAFDGS), representing 20 large families for substudy 2. The substudy 2 strategy is to sequence approximately 600 individuals at an average of 50x coverage across the entire genome, then impute genome wide genotypes for about 440 additional family members. The 600 sequenced individuals are specifically chosen for their value in imputing sequence information into other family members. By studying large pedigrees, we expect to find multiple individuals carrying each genetic variant, even if this variant is very rare in the population at large. Thus, a pedigree-based approach provides an excellent opportunity for identifying rare novel variants influencing risk of T2D and quantitative variation in T2D-related phenotypes. The whole genome sequencing has been done commercially by Complete Genomics, Inc. (CGI). The available sample of 1,048 includes 5 sequenced individuals who do not belong to any of the 20 large pedigrees. The final family data of 1,043 individuals includes whole genome sequence data for 607 individuals. After quality control, 590 sequenced individuals provide data for family based imputation using Merlin linkage analysis software into approximately 440 additional family members for whom chip based genotypes are available to indicate which parental haplotype is transmitted. The complete SAMAFS data including phenotype, genotype, sequence, other T2D-related trait data utilized for Projects 1 and 2 are available. These data can readily be viewed by clicking on the substudy title shown below or in the box: \\\"Substudies\\\", located on the right hand side of this parent or top study page phs000847.v1.p1, titled T2D-GENES Consortium: San Antonio Mexican American Family Studies (SAMAFS).  phs000849 T2D-GENES Project 1: San Antonio Mexican American Family Studies (SAMAFS), Substudy 1: Whole Exome Sequencing  phs000462 T2D-GENES Project 2: San Antonio Mexican American Family Studies (SAMAFS), Substudy 2: Whole Genome Sequencing in Pedigrees");
        list.add("This study funded by the National Cancer Institute (NCI) involves conducting a genome-wide association study of common genetic variants to identify markers of susceptibility to bladder cancer.  This bladder GWAS has led to the discovery of three novel regions in the genome associated with bladder cancer risk. Cases were defined as individuals having histologically confirmed primary carcinoma of the urinary bladder, including carcinoma in situ (ICD-0-2 topography codes C67.0-C67.9 or ICD9 codes 188.1-188.9). Scan data were obtained from two case-control studies carried out in Spain and the United States (specifically, those in the Maine and Vermont components of the New England Bladder Cancer Study) and three prospective cohort studies in Finland and the United States (specifically Alpha-Tocopherol, Beta-Carotene Cancer Prevention Study, Prostate, Lung, Colorectal and Ovarian Cancer Screening Trial, and The American Cancer Society Cancer Prevention Study... (for more see dbGaP study page.)");
        list.add("Humboldt State University is a public university in Arcata, California. It is the northernmost campus of the 23-school California State University system. The main campus, situated hillside at the edge of a coast redwood forest, has commanding views overlooking Arcata, much of Humboldt Bay, and the Pacific Ocean");
        StanfordNEDLocationFinder finder = new StanfordNEDLocationFinder();
        finder.startup();
        for (String inputText : list) {
            log.info("=============================================================");
            log.info("Processing: " + inputText);

            SpatialEnhancerResultSimple result = new SpatialEnhancerResultSimple(inputText, finder);
            String resultStr = new ObjectMapper().writeValueAsString(result);
            System.out.println(resultStr);


        }

    }


}
