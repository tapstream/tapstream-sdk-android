package com.tapstream.sdk;

import com.tapstream.sdk.http.HttpResponse;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by adam on 2016-10-13.
 */

public class TimelineSummaryResponse implements ApiResponse {
    private final HttpResponse httpResponse;
    private final String latestDeeplink;
    private final List<String> deeplinks;
    private final List<String> campaigns;
    private final Map<String, String> hitParams;
    private final Map<String, String> eventParams;

    private static List<String> jsonArrayToStringList(JSONArray arr) {
        List<String> strs = new ArrayList<String>(arr.length());
        if(arr != null) {
            for (int ii = 0; ii < arr.length(); ii++) {
                strs.add(arr.getString(ii));
            }
        }
        return strs;
    }

    private static Map<String, String> jsonObjectToStringMap(JSONObject obj){
        Map<String, String> strs = new HashMap<String, String>(obj.length());
        if(obj != null) {
            for (Object k : obj.keySet()) {
                strs.put((String) k, obj.getString((String) k));
            }
        }
        return strs;
    }


    static TimelineSummaryResponse createSummaryResponse(HttpResponse response) {
        String rawResponse = response.getBodyAsString();
        try {
            if (rawResponse != null) {
                JSONObject asJson = new JSONObject(rawResponse);
                String latestDeeplink = asJson.getString("latest_deeplink");
                List<String> deeplinks = jsonArrayToStringList(asJson.getJSONArray("deeplinks"));
                List<String> campaigns = jsonArrayToStringList(asJson.getJSONArray("campaigns"));
                Map<String, String> hitParams = jsonObjectToStringMap(asJson.getJSONObject("hit_params"));
                Map<String, String> eventParams = jsonObjectToStringMap(asJson.getJSONObject("event_params"));
                return new TimelineSummaryResponse(response, latestDeeplink, deeplinks, campaigns, hitParams, eventParams);
            }
        }catch(JSONException e){
            Logging.log(Logging.WARN, "JSON decode error from timeline summary: (%s)", e.getMessage());
        }
        return new TimelineSummaryResponse(response);
    }

    public TimelineSummaryResponse(HttpResponse httpResponse){
        this.httpResponse = httpResponse;
        this.latestDeeplink = null;
        this.deeplinks = null;
        this.campaigns = null;
        this.hitParams = null;
        this.eventParams = null;
    }

    public TimelineSummaryResponse(HttpResponse httpResponse, String latestDeeplink, List<String> deeplinks, List<String> campaigns, Map<String, String> hitParams, Map<String, String> eventParams) {
        this.httpResponse = httpResponse;
        this.latestDeeplink = latestDeeplink;
        this.deeplinks = deeplinks;
        this.campaigns = campaigns;
        this.hitParams = hitParams;
        this.eventParams = eventParams;
    }

    @Override
    public HttpResponse getHttpResponse() {
        return httpResponse;
    }

    public boolean isEmpty() {
        return (eventParams == null || eventParams.isEmpty())
                && (campaigns == null || campaigns.isEmpty());  // Sufficient
    }
    public String getLatestDeeplink() {
        return latestDeeplink;
    }

    public List<String> getDeeplinks() {
        return deeplinks;
    }

    public List<String> getCampaigns() {
        return campaigns;
    }

    public Map<String, String> getHitParams() {
        return hitParams;
    }

    public Map<String, String> getEventParams() {
        return eventParams;
    }
}
