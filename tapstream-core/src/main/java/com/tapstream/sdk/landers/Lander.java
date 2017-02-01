package com.tapstream.sdk.landers;

import com.tapstream.sdk.DelegatedJSONObject;

import org.json.JSONObject;


public class Lander extends DelegatedJSONObject {
    public static Lander fromApiResponse(JSONObject resp){
        return new Lander(resp);
    }
    private Lander(JSONObject delegate){
        super(delegate);
    }

    public int getId(){
        return getOrDefault("id", -1);
    }

    public String getMarkup(){
        return getOrDefault("markup", "");
    }

    public String getUrl(){
        return getOrDefault("url", null);
    }
}
