package com.tapstream.sdk.landers;

import com.tapstream.sdk.ApiResponse;
import com.tapstream.sdk.http.HttpResponse;


public class LanderApiResponse implements ApiResponse {
    private HttpResponse response;
    private Lander lander;

    public LanderApiResponse(HttpResponse response, Lander lander){
        this.response = response;
        this.lander = lander;
    }

    /**
     * @return The Lander returned by the server.
     */
    public Lander getLander(){
        return lander;
    }

    @Override
    public HttpResponse getHttpResponse() {
        return response;
    }
}
