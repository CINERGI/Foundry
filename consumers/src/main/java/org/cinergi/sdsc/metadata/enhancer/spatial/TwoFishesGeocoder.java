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
import org.neuinfo.foundry.common.util.Utils;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.net.URI;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created by bozyurt on 10/5/16.
 */
public class TwoFishesGeocoder {
    private final static Logger log = Logger.getLogger("TwoFishesGeocoder");
    static LRUCache<String, List<LatLngBounds>> locationCache = new LRUCache<String, List<LatLngBounds>>(5000);

    public static List<LatLngBounds> getBounds(String location) throws Exception {
        if (locationCache.containsKey(location)) {
            return locationCache.get(location);
        }
        List<LatLngBounds> boundsList = getBoundsForLocation(location);
        locationCache.put(location, boundsList);
        return boundsList;

    }


    static List<LatLngBounds> getBoundsForLocation(String location) throws Exception {
        HttpClient client = new DefaultHttpClient();
        //URI uri = new URI( "http://localhost:8081//search/geocode/?query=" + URLEncoder.encode(location, "UTF-8"));


        URL url = new URL("http://132.249.238.123:8081//search/geocode/?query=" + URLEncoder.encode(location, "UTF-8"));
        System.out.println("getBoundsForLocation uri:" + url);

        BufferedReader in = null;
        StringBuilder sb = new StringBuilder(4096);
        try {
            in = new BufferedReader(
                    new InputStreamReader(url.openStream()));

            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                sb.append(inputLine);
            }
        } finally {
            Utils.close(in);
        }

        JSONObject json = new JSONObject(sb.toString());
        log.info(json.toString(2));
        if (!json.has("interpretations")) {
            return Collections.emptyList();
        }
        JSONArray results = json.getJSONArray("interpretations");
        List<LatLngBounds> boundsList = new ArrayList<LatLngBounds>(results.length());
        for (int i = 0; i < results.length(); i++) {
            JSONObject resultJS = results.getJSONObject(i);
            JSONObject feature = resultJS.getJSONObject("feature");
            if (feature.has("geometry")) {
                JSONObject geometry = feature.getJSONObject("geometry");
                if (geometry.has("bounds")) {
                    JSONObject bjs = geometry.getJSONObject("bounds");
                    JSONObject swJS = bjs.getJSONObject("sw");
                    JSONObject neJS = bjs.getJSONObject("ne");
                    LatLng southWest = new LatLng(new BigDecimal(swJS.getDouble("lat")), new BigDecimal(swJS.getDouble("lng")));
                    LatLng northEast = new LatLng(new BigDecimal(neJS.getDouble("lat")), new BigDecimal(neJS.getDouble("lng")));
                    LatLngBounds bounds = new LatLngBounds(southWest, northEast);
                    boundsList.add(bounds);
                } else if (geometry.has("center")) {
                    JSONObject center = geometry.getJSONObject("center");
                    LatLng southWest = new LatLng(new BigDecimal(center.getDouble("lat")), new BigDecimal(center.getDouble("lng")));
                    LatLng northEast = new LatLng(new BigDecimal(center.getDouble("lat")), new BigDecimal(center.getDouble("lng")));
                    LatLngBounds bounds = new LatLngBounds(southWest, northEast);
                    boundsList.add(bounds);
                }

            }
        }
        return boundsList;

    }

    public static void main(String[] args) throws Exception {
        List<LatLngBounds> bounds = getBounds("mount shasta");

        System.out.println("--------------------------");
        System.out.println(bounds);

    }
}
