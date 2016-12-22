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
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.util.EntityUtils;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;
import org.neuinfo.foundry.common.util.LRUCache;

import java.math.BigDecimal;
import java.net.URI;
import java.util.*;

/**
 * Created by bozyurt on 10/17/16.
 */
public class ArcGISGeocoder {

    private final static Logger log = Logger.getLogger("ArcGISGeocoder");
    static LRUCache<String, Map<String, LatLngBounds>> locationCache = new LRUCache<String, Map<String, LatLngBounds>>(5000);

    public static Map<String, LatLngBounds> getBounds(String location) throws Exception {
        if (locationCache.containsKey(location)) {
            return locationCache.get(location);
        }
        Map<String, LatLngBounds> map = getBoundsForLocation(location);
        locationCache.put(location, map);
        return map;
    }

    static Map<String, LatLngBounds> getBoundsForLocation(String location) throws Exception {
        HttpClient client = new DefaultHttpClient();
        URIBuilder builder = new URIBuilder();

        builder.setHost("geocode.arcgis.com").setPath("/arcgis/rest/services/World/GeocodeServer/findAddressCandidates").setScheme("http");
        builder.setParameter("forStorage", "false").setParameter("SingleLine", location);
        builder.setParameter("f","pjson");
        URI uri = builder.build();
        System.out.println("getBoundsForLocation uri:" + uri);
        HttpGet httpGet = new HttpGet(uri);
        httpGet.addHeader("Accept", "application/json");
        HttpParams params = client.getParams();
        int TIMEOUT = 20000; // 20 secs
        HttpConnectionParams.setConnectionTimeout(params, TIMEOUT);
        HttpConnectionParams.setSoTimeout(params, TIMEOUT);

        try {
            HttpResponse response = client.execute(httpGet);
            log.info(response.getStatusLine());
            HttpEntity entity = response.getEntity();
            if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK && entity != null) {
                 String jsonContent = EntityUtils.toString(entity);
                JSONObject json = new JSONObject(jsonContent);
                if (log.isDebugEnabled()) {
                    log.debug(json.toString(2));
                }
                JSONArray candidates = json.getJSONArray("candidates");
                Map<String, LatLngBounds> boundMap = new LinkedHashMap<String, LatLngBounds>(11);
                for(int i = 0; i  < candidates.length(); i++) {
                    JSONObject candidate = candidates.getJSONObject(i);
                    String address = candidate.getString("address");
                    JSONObject extent = candidate.getJSONObject("extent");
                    LatLng southWest = new LatLng(new BigDecimal(extent.getDouble("ymin")), new BigDecimal(extent.getDouble("xmin")));
                    LatLng northEast = new LatLng(new BigDecimal(extent.getDouble("ymax")), new BigDecimal(extent.getDouble("xmax")));
                    LatLngBounds bounds = new LatLngBounds(southWest, northEast);
                    boundMap.put(address, bounds);
                }
                log.info(boundMap);
                return boundMap;
            }
        } finally {
            if (httpGet != null) {
                httpGet.releaseConnection();
            }
        }
        return null;
    }

    public static void main(String[] args) throws Exception{
        Map<String, LatLngBounds> boundsMap = ArcGISGeocoder.getBounds("mount shasta");
        System.out.println(boundsMap);
    }

}
