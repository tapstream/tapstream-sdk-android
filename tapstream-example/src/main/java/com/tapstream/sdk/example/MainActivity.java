package com.tapstream.sdk.example;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.TextView;

import com.tapstream.sdk.ApiFuture;
import com.tapstream.sdk.Callback;
import com.tapstream.sdk.Config;
import com.tapstream.sdk.Event;
import com.tapstream.sdk.EventApiResponse;
import com.tapstream.sdk.Tapstream;
import com.tapstream.sdk.TimelineApiResponse;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private Handler uiHandler;
    private TextView statusView;

    private class TextUpdater implements Runnable {

        TextView view;
        String text;

        public TextUpdater(TextView view, String text){
            this.view = view;
            this.text = text;
        }

        @Override
        public void run() {
            view.setText(text);
        }
    }

    private void handleApiResponse(ApiFuture<EventApiResponse> responseFuture){
        uiHandler.post(new TextUpdater(statusView, "Working!"));

        responseFuture.setCallback(new Callback<EventApiResponse>() {
            @Override
            public void success(EventApiResponse result) {
                String msg = "Success: " + result.getHttpResponse().getStatus();
                uiHandler.post(new TextUpdater(statusView, msg));
            }

            @Override
            public void error(Throwable reason) {
                String msg = "Failure: " + reason.toString();
                uiHandler.post(new TextUpdater(statusView, msg));
            }
        });
    }

    private void clearPrefs(String key){
        SharedPreferences.Editor editor = getSharedPreferences(key, 0).edit();
        editor.clear();
        editor.apply();
    }

    private void clearState(){
        clearPrefs("TapstreamSDKFiredEvents");
        clearPrefs("TapstreamSDKUUID");
        clearPrefs("TapstreamWOMRewards");
    }

    private void lookupTimeline(){
        statusView.setText("Working!");

        final long startTime = System.currentTimeMillis();
        ApiFuture<TimelineApiResponse> timelineFuture = Tapstream.getInstance().lookupTimeline();
        timelineFuture.setCallback(new Callback<TimelineApiResponse>(){

            @Override
            public void success(TimelineApiResponse result) {
                final long timeDelta = System.currentTimeMillis() - startTime;

                if (result.isEmpty()){
                    uiHandler.post(new TextUpdater(statusView, "timeline was empty"));
                } else {
                    try {
                        JSONObject timelineJson = result.parse();
                        int numHits = timelineJson.getJSONArray("hits").length();
                        int numEvents = timelineJson.getJSONArray("events").length();
                        String msg = String.format(Locale.US, "Timeline: %d hits, %d events (%dms)",
                                numHits, numEvents, timeDelta);
                        uiHandler.post(new TextUpdater(statusView, msg));
                    } catch (JSONException e){
                        uiHandler.post(new TextUpdater(statusView, "Failed to parse timeline response"));
                    }
                }
            }

            @Override
            public void error(Throwable reason) {
                uiHandler.post(new TextUpdater(statusView, "error getting timeline"));
            }
        });
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        uiHandler = new Handler(Looper.getMainLooper());
        statusView = (TextView)findViewById(R.id.textStatus);

        Config config = new Config("sdktest", "YGP2pezGTI6ec48uti4o1w");
        config.addGlobalEventParameter("user_id", "92429d82a41e");

        Tapstream.create(getApplication(), config);

        findViewById(R.id.buttonFireWithCustomParams).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Event with custom params
                Event e = new Event("custom-event", false);
                e.setCustomParameter("score", 15000);
                e.setCustomParameter("skill", "easy");
                handleApiResponse(Tapstream.getInstance().fireEvent(e));
            }
        });

        findViewById(R.id.buttonFirePurchase).setOnClickListener(new View.OnClickListener(){

            @Override
            public void onClick(View v) {
                // Purchase event with a price
                handleApiResponse(Tapstream.getInstance().fireEvent(new Event("3da541559918a", "com.myapp.coinpack100", 1, 299, "USD")));
            }
        });


        findViewById(R.id.buttonFirePurchaseNoPrice).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Purchase event with no price
                handleApiResponse(Tapstream.getInstance().fireEvent(new Event("3da541559918a", "com.myapp.coinpack100", 1)));
            }
        });

        findViewById(R.id.buttonClearState).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                clearState();
                statusView.setText("State cleared");
            }
        });

        findViewById(R.id.buttonLookupTimeline).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                lookupTimeline();
            }
        });

        lookupTimeline();

    }

}
