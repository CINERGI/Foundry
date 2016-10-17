
package org.cinergi.sdsc.metadata.enhancer.spatial;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.code.geocoder.model.LatLng;
import com.google.code.geocoder.model.LatLngBounds;
import org.apache.log4j.Logger;
import org.cinergi.alternative.metadata.Metadata;
import org.cinergi.sdsc.metadata.AlternativeMetadata;
import org.cinergi.sdsc.metadata.ISO19139Metadata;
import org.isotc211._2005.gmi.MIMetadataType;
import org.isotc211.iso19139.d_2007_04_17.gmd.EXGeographicBoundingBoxType;
import org.isotc211.iso19139.d_2007_04_17.gmd.MDMetadataType;
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

public class SpatialEnhancerResult {
    StanfordNEDLocationFinder stanfordNEDLocationFinder;

    // get from the metadata
    @JsonProperty("bounding_boxes")
    private List<LatLngBounds> bounds;

    @JsonProperty("place_keywords")
    private List<String> places;

    @JsonProperty("text")
    private String text;

    // calculate from the bounds, places and text
    @JsonProperty("invalid_place_keywords")
    private List<String> invalidPlaces = new ArrayList<String>();

    @JsonProperty("derived_bounding_boxes_from_places")
    private Map<String, LatLngBounds> place2Bounds = new HashMap<String, LatLngBounds>();

    @JsonProperty("derived_place_from_text")
    private List<String> extractedPlaces = new ArrayList<String>();

    @JsonProperty("derived_bounding_boxes_from_derived_place")
    private Map<String, LatLngBounds> extractedPlace2Bounds = new HashMap<String, LatLngBounds>();

    // logger
    private static Logger log = Logger.getLogger("SpatialEnhancerResult");


    // get bounding boxes from iso metadata
    private void getBoundingBoxes(MDMetadataType metadata) throws Exception {
        this.bounds = new ArrayList<LatLngBounds>();
        List<EXGeographicBoundingBoxType> boxes = ISO19139Metadata.getBoundingBox(metadata);
        for (EXGeographicBoundingBoxType box : boxes) {
            BigDecimal east = box.getEastBoundLongitude().getDecimal();
            BigDecimal west = box.getWestBoundLongitude().getDecimal();
            BigDecimal north = box.getNorthBoundLatitude().getDecimal();
            BigDecimal south = box.getSouthBoundLatitude().getDecimal();
            LatLng southwest = new LatLng(south, west);
            LatLng northeast = new LatLng(north, east);
            LatLngBounds bound = new LatLngBounds(southwest, northeast);
            this.bounds.add(bound);
        }
    }


    // get bounding boxes from alternative metadata
    private void getBoundingBoxes(Metadata metadata) throws Exception {
        this.bounds = new ArrayList<LatLngBounds>();
        Metadata.Idinfo.Spdom.Bounding box = AlternativeMetadata.getBoundingBox(metadata);
        BigDecimal east = new BigDecimal(box.getEastbc());
        BigDecimal west = new BigDecimal(box.getWestbc());
        BigDecimal north = new BigDecimal(box.getNorthbc());
        BigDecimal south = new BigDecimal(box.getSouthbc());
        LatLng southwest = new LatLng(south, west);
        LatLng northeast = new LatLng(north, east);
        LatLngBounds bound = new LatLngBounds(southwest, northeast);
        this.bounds.add(bound);
    }


    // get text description from iso metadata
    public void getTextDescription(MDMetadataType metadata) throws Exception {
        this.text = ISO19139Metadata.getTextDescription(metadata);
    }


    // get text description from alternative metadata
    public void getTextDescription(Metadata metadata) throws Exception {
        this.text = AlternativeMetadata.getTextDescription(metadata);
    }


    // get places from iso metadata
    public void getPlaces(MDMetadataType metadata) throws Exception {
        this.places = new ArrayList<String>();
        places.addAll(ISO19139Metadata.getPlaces(metadata));
    }


    public void getPlaces(Metadata metadata) throws Exception {
        this.places = new ArrayList<String>();
        places.addAll(AlternativeMetadata.getPlaces(metadata));
    }


    public SpatialEnhancerResult(File file, StanfordNEDLocationFinder finder) {
        this.stanfordNEDLocationFinder = finder;
        try {
            // parse it as ISO metadata
            JAXBContext jc = JAXBContext.newInstance(MIMetadataType.class);
            Unmarshaller u = jc.createUnmarshaller();
            JAXBElement object = (JAXBElement) u.unmarshal(file);
            log.info("ISO 19135 Metadata");
            if (object.getValue() instanceof MDMetadataType) {
                MDMetadataType metadata = (MDMetadataType) object.getValue();
                getBoundingBoxes(metadata);
                getTextDescription(metadata);
                getPlaces(metadata);
                enhance();
            } else {
                log.info("Unhandled Type: " + object.getValue().getClass());
            }
        } catch (Exception exception) {
            exception.printStackTrace();
            try {
                JAXBContext jc = JAXBContext.newInstance(Metadata.class);
                Unmarshaller u = jc.createUnmarshaller();
                Metadata metadata = (Metadata) u.unmarshal(file);
                log.info("Unknown Metadata, but parsed");
                getBoundingBoxes(metadata);
                getTextDescription(metadata);
                getPlaces(metadata);
                enhance();
            } catch (Exception exception1) {
                log.info("Unknown Metadata, failed to parse");
            }
        }

    }


    private void enhance() throws Exception {

        // show bounding boxes
        log.info("-----------------------------------");
        if (bounds.isEmpty()) {
            log.info("Found no bounding box.");
        } else {
            for (LatLngBounds bound : bounds) {
                log.info("Found a bounding box: " + bound);
            }
        }

        // show places
        log.info("-----------------------------------");
        if (places.isEmpty()) {
            log.info("Found no keywords for place.");
        } else {
            for (String place : places) {
                log.info("Found a place keyword: " + place);
            }
        }

        // show text
        log.info("-----------------------------------");
        log.info("Found text: " + this.text);

        log.info("-----------------------------------");
        //bounds.clear();
        //places.clear();
        if (bounds.isEmpty()) {
            if (places.isEmpty()) {
                // using text (abstract or introduction) to find place keywords and bounding boxes
                log.info("Using text to find bounding boxes (NER) ...");

                this.extractedPlaces.addAll(this.stanfordNEDLocationFinder.getLocationsFromText(this.text));
                //this.extractedPlaces.addAll(SpatialTextLocationFinder.getLocationsFromText(this.text));
                if (this.extractedPlaces.isEmpty()) {
                    log.info("Found no place from the text. ");
                } else {
                    getBounds(this.extractedPlaces, this.extractedPlace2Bounds, this.text);
                }
            } else {
                // using place keywords to find bounding boxes
                log.info("Using place keywords to find bounding boxes...");

                for (String place : places) {
                    log.info("     --------------------------------");
                    log.info("     " + place);

                    int pos = place.lastIndexOf(" > ");
                    if (pos != -1) {
                        place = place.substring(pos + 3).trim();
                    }

                    if (place.trim().toLowerCase().equals("northern hemisphere")) {
                        LatLngBounds bound = new LatLngBounds(new LatLng("-90", "-180"), new LatLng("0", "180"));
                        this.place2Bounds.put(place, bound);
                        log.info("     Bounding box: " + bound);
                        continue;
                    }

                    Set<String> locations = new HashSet<String>();
                    locations.add(place);

                    //Set<String> locations = SpatialTextLocationFinder.getLocationsFromText(place);
                    if (locations.size() == 1) {
                        String location = locations.iterator().next();
                        Thread.sleep(200);
                        List<LatLngBounds> boundsList = GoogleGeocoder.getBounds(location);
                        if (boundsList.size() == 1) {
                            LatLngBounds bound = boundsList.get(0);
                            this.place2Bounds.put(place, bound);
                            log.info("     Bounding box: " + bound);
                        } else {
                            log.info("     Found multiple locations, can't decide its bounding box");
                        }
                    } else {
                        log.info("     Found multiple locations, can't decide its bounding box " + locations);
                    }
                }
            }
        } else if (places.isEmpty()) {
            log.warn("WARN: No place keywords for the bounding boxes.");
        } else {
            log.info("Validating the consistence of bounds and places...");


            for (String place : places) {

                log.info("     --------------------------------");
                log.info("     " + place);

                int pos = place.lastIndexOf(" > ");
                if (pos != -1) {
                    place = place.substring(pos + 3).trim();
                }

                //Set<String> locations = SpatialTextLocationFinder.getLocationsFromText(place);

                Set<String> locations = new HashSet<String>();
                locations.add(place);
                boolean valid = false;
                boolean checked = false;
                for (String location : locations) {

                    if (location.trim().toLowerCase().equals("northern hemisphere")) {
                        for (LatLngBounds bound : this.bounds) {
                            checked = true;
                            if (bound.getSouthwest().getLat().doubleValue() > 0 ||
                                    bound.getNortheast().getLat().doubleValue() > 0) {
                                log.info("     Valid");
                                valid = true;
                                break;
                            }
                        }

                        if (!valid && checked) {
                            log.info("     Invalid");
                            this.invalidPlaces.add(place);
                        } else if (valid && !checked) {
                            log.info("     Couldn't validate this place.");
                        }
                        continue;
                    }

                    Thread.sleep(200);
                    List<LatLngBounds> boundsList = GoogleGeocoder.getBounds(location);
                    for (LatLngBounds bound : boundsList) {
                        checked = true;

                        // check if this bounding box is covered by the bounding boxes specified in the metadata
                        if (contains(bounds, bound)) {
                            valid = true;
                            log.info("     Valid");
                            break;
                        }
                    }

                    if (valid) {
                        break;
                    }
                }

                if (!valid && checked) {
                    log.info("     Invalid");
                    this.invalidPlaces.add(place);
                } else if (!valid && !checked) {
                    log.info("     Couldn't validate this place.");
                }

            }
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
        int idx = text.indexOf(location);
        if (idx != -1) {
            int len = text.length();
            int startOffset = Math.max(idx - 50, 0);
            int endOffset = Math.min(idx + location.length() + 50, len);
            String window = text.substring(startOffset, endOffset);
            int maxLen = -1;
            String longestMatch = null;
            for (String address : boundsMap.keySet()) {
                if (Utils.fuzzyContains(window, address)) {
                    if (maxLen < address.length()) {
                        longestMatch = address;
                        maxLen = address.length();
                    }
                }
            }
            if (longestMatch != null) {
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

        //File dir = new File("/Users/kailin/Desktop/metadata/samples/National_Climatic_Data_Center");
        File dir = new File("/scratch/slocal/cycore/tomcat6-beta/webapps/cinergi/WEB-INF/metadata/samples/NCDC_GIS");
        //File dir = new File("/Users/kailin/Desktop/metadata/samples/Critical_Zone_Observatory_Catalog");
        dir = new File("/home/bozyurt/dev/java/Foundry/data/test");
        String[] filenames = dir.list();
        StanfordNEDLocationFinder finder = new StanfordNEDLocationFinder();
        finder.startup();
        for (String filename : filenames) {
            log.info("=============================================================");
            log.info("Processing the file: " + filename);
            File file = new File(dir, filename);
            SpatialEnhancerResult result = new SpatialEnhancerResult(file, finder);
            String resultStr = new ObjectMapper().writeValueAsString(result);
            System.out.println(resultStr);

            break;
        }

    }


}
