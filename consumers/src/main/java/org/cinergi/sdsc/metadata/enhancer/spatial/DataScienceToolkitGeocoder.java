package org.cinergi.sdsc.metadata.enhancer.spatial;

import com.google.code.geocoder.model.LatLng;
import com.google.code.geocoder.model.LatLngBounds;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;
import org.neuinfo.foundry.common.util.LRUCache;

import java.math.BigDecimal;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by bozyurt on 10/4/16.
 */
public class DataScienceToolkitGeocoder {
    private final static Logger log = Logger.getLogger("GoogleGeocoder");
    static LRUCache<String, List<LatLngBounds>> locationCache = new LRUCache<String, List<LatLngBounds>>(5000);

    static public List<LatLngBounds> getBounds(String location) throws Exception {
        if (locationCache.containsKey(location)) {
            return locationCache.get(location);
        }
        List<LatLngBounds> boundsList = getBoundsForLocation(location);
        locationCache.put(location, boundsList);
        return boundsList;
    }


    static List<LatLngBounds> getBoundsForLocation(String location) throws Exception {
        HttpClient client = new DefaultHttpClient();
        URIBuilder builder = new URIBuilder();
        builder.setHost("132.249.238.123").setPath("/maps/api/geocode/json").setScheme("http");
        builder.setParameter("sensor", "false").setParameter("address", location);
        URI uri = builder.build();
        System.out.println("getBoundsForLocation uri:" + uri);
        HttpGet httpGet = new HttpGet(uri);
        httpGet.addHeader("Accept", "application/json");
        try {
            HttpResponse response = client.execute(httpGet);
            HttpEntity entity = response.getEntity();
            if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK && entity != null) {
                String jsonContent = EntityUtils.toString(entity);
                JSONObject json = new JSONObject(jsonContent);
                System.out.println(json.toString(2));
                JSONArray results = json.getJSONArray("results");
                List<LatLngBounds> boundsList = new ArrayList<LatLngBounds>(results.length());
                for (int i = 0; i < results.length(); i++) {
                    JSONObject resultJS = results.getJSONObject(i);
                    if (resultJS.has("geometry")) {
                        JSONObject geometry = resultJS.getJSONObject("geometry");
                        if (geometry.has("viewport")) {
                            JSONObject viewport = geometry.getJSONObject("viewport");
                            JSONObject swJS = viewport.getJSONObject("southwest");
                            JSONObject neJS = viewport.getJSONObject("northeast");
                            LatLng southWest = new LatLng(new BigDecimal(swJS.getDouble("lat")), new BigDecimal(swJS.getDouble("lng")));
                            LatLng northEast = new LatLng(new BigDecimal(neJS.getDouble("lat")), new BigDecimal(neJS.getDouble("lng")));
                            LatLngBounds bounds = new LatLngBounds(southWest, northEast);
                            boundsList.add(bounds);
                        }
                    }
                }
               return boundsList;
            }

        } finally {
            if (httpGet != null) {
                httpGet.releaseConnection();
            }
        }
        return null;
    }


    public static void main(String[] args) throws Exception {
        List<LatLngBounds> bounds = getBounds("Istanbul, Turkey");

        System.out.println("--------------------------");
        System.out.println(bounds);
    }
}
