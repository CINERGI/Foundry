
package org.cinergi.sdsc.metadata.enhancer.spatial;

import com.google.code.geocoder.Geocoder;
import com.google.code.geocoder.GeocoderRequestBuilder;
import com.google.code.geocoder.model.*;
import org.apache.log4j.Logger;
import org.neuinfo.foundry.common.util.LRUCache;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;


public class GoogleGeocoder {
    private final static Logger log = Logger.getLogger("GoogleGeocoder");
    static LRUCache<String, Map<String, LatLngBounds>>  locationCache = new LRUCache<String, Map<String, LatLngBounds>>(5000);

    static public Map<String, LatLngBounds> getBounds(String location) throws Exception {
        if (locationCache.containsKey(location)) {
             locationCache.get(location);
        }
        final Geocoder geocoder = new Geocoder();
        Map<String, LatLngBounds> result = new LinkedHashMap<String, LatLngBounds>(11);
        GeocoderRequest geocoderRequest = new GeocoderRequestBuilder().setAddress(location).setLanguage("en").getGeocoderRequest();
        GeocodeResponse geocoderResponse = geocoder.geocode(geocoderRequest);
        if (geocoderResponse != null) {
            GeocoderStatus status = geocoderResponse.getStatus();

            if (status == GeocoderStatus.ERROR) {
                log.info("Google Geocoder Response: ERROR");
            } else if (status == GeocoderStatus.INVALID_REQUEST) {
                log.info("Google Geocoder Response: INVALID_REQUEST");
            } else if (status == GeocoderStatus.OK) {

            } else if (status == GeocoderStatus.OVER_QUERY_LIMIT) {
                log.info("Google Geocoder Response: OVER_QUERY_LIMIT");
            } else if (status == GeocoderStatus.REQUEST_DENIED) {
                log.info("Google Geocoder Response: REQUEST_DENIED");
            } else if (status == GeocoderStatus.UNKNOWN_ERROR) {
                log.info("Google Geocoder Response: UNKNOWN_ERROR");
            } else if (status == GeocoderStatus.ZERO_RESULTS) {
                log.info("Google Geocoder Response: ZERO_RESULTS");
            }

            for (GeocoderResult geocoderResult : geocoderResponse.getResults()) {
                LatLngBounds bounds = geocoderResult.getGeometry().getViewport();
                result.put(location,bounds);
                break; // first one for this coder
            }
        }
        locationCache.put(location, result);
        return result;

    }

}