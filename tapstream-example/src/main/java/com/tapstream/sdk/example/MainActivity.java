package com.tapstream.sdk.example;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
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
import com.tapstream.sdk.wordofmouth.Offer;
import com.tapstream.sdk.wordofmouth.OfferApiResponse;
import com.tapstream.sdk.wordofmouth.Reward;
import com.tapstream.sdk.wordofmouth.RewardApiResponse;
import com.tapstream.sdk.wordofmouth.WordOfMouth;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;
import java.util.Locale;


public class MainActivity extends AppCompatActivity {

    private TextView statusView;

    private abstract class LoggingCallback<T> implements Callback<T> {
        @Override
        public void error(Throwable reason) {
            String msg = "Failure: " + reason.getMessage();
            runOnUiThread(new TextUpdater(statusView, msg));
        }
    }

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
        runOnUiThread(new TextUpdater(statusView, "Working!"));

        responseFuture.setCallback(new LoggingCallback<EventApiResponse>() {
            @Override
            public void success(EventApiResponse result) {
                String msg = "Success: " + result.getHttpResponse().getStatus();
                runOnUiThread(new TextUpdater(statusView, msg));
            }

            @Override
            public void error(Throwable reason) {
                String msg = "Failure: " + reason.toString();
                runOnUiThread(new TextUpdater(statusView, msg));
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
                    runOnUiThread(new TextUpdater(statusView, "timeline was empty"));
                } else {
                    try {
                        JSONObject timelineJson = result.parse();
                        int numHits = timelineJson.getJSONArray("hits").length();
                        int numEvents = timelineJson.getJSONArray("events").length();
                        String msg = String.format(Locale.US, "Timeline: %d hits, %d events (%dms)",
                                numHits, numEvents, timeDelta);
                        runOnUiThread(new TextUpdater(statusView, msg));
                    } catch (JSONException e){
                        runOnUiThread(new TextUpdater(statusView, "Failed to parse timeline response"));
                    }
                }
            }

            @Override
            public void error(Throwable reason) {
                runOnUiThread(new TextUpdater(statusView, "error getting timeline"));
            }
        });
    }


    public void onClickLookupOffer(View view){
        statusView.setText("Working!");
        String insertionPoint = "test123";
        ApiFuture<OfferApiResponse> resp = Tapstream.getInstance().getWordOfMouthOffer(insertionPoint);
        final Activity mainActivity = this;

        resp.setCallback(new LoggingCallback<OfferApiResponse>() {
            @Override
            public void success(OfferApiResponse result) {
                final Offer o = result.getOffer();
                // Bounce back to UI thread to show WOM offer.
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        statusView.setText("Success. Displaying offer.");
                        WordOfMouth wom = Tapstream.getInstance().getWordOfMouth();
                        View parent = findViewById(R.id.main_layout);
                        wom.showOffer(mainActivity, parent, o);
                    }
                });
            }
        });
    }

    public void onClickLookupRewards(View view) {
        statusView.setText("Working!");
        ApiFuture<RewardApiResponse> resp = Tapstream.getInstance().getWordOfMouthRewardList();
        resp.setCallback(new LoggingCallback<RewardApiResponse>() {
            @Override
            public void success(RewardApiResponse result) {
                WordOfMouth wom = Tapstream.getInstance().getWordOfMouth();
                List<Reward> rewards = result.getRewards();
                StringBuilder sb = new StringBuilder("Success: ")
                    .append(rewards.size())
                    .append(" rewards found.\n");
                for(Reward r: rewards) {
                    sb.append("Reward(sku=")
                        .append(r.getRewardSku())
                        .append(", installs=")
                        .append(r.getInstallCount())
                        .append(", consumed=")
                        .append(wom.isConsumed(r))
                        .append(")\n");
                    if(wom.isConsumed(r)){
                        wom.consumeReward(r);
                        sb.append("You get a reward! (")
                            .append(r.getRewardSku())
                            .append(")\n");
                    }
                }

                runOnUiThread(new TextUpdater(statusView, sb.toString()));
            }
        });

    }

    public void onClickTestIAP(View view){
        Intent intent = new Intent(this, PurchaseActivity.class);
        startActivity(intent);
    }

    public void onClickLookupTimeline(View view){
        lookupTimeline();
    }

    public void onClickFireEventWithParams(View view){
        // Event with custom params
        Event e = new Event("custom-event", false);
        e.setCustomParameter("score", 15000);
        e.setCustomParameter("skill", "easy");
        handleApiResponse(Tapstream.getInstance().fireEvent(e));
    }

    public void onClickFirePurchaseEvent(View view){
        // Purchase event with a price
        handleApiResponse(Tapstream.getInstance().fireEvent(new Event("3da541559918a", "com.myapp.coinpack100", 1, 299, "USD")));
    }

    public void onClickFirePurchaseEventNoPrice(View view){
        // Purchase event with no price
        handleApiResponse(Tapstream.getInstance().fireEvent(new Event("3da541559918a", "com.myapp.coinpack100", 1)));
    }

    public void onClearStateClicked(View view){
        clearState();
        statusView.setText("State cleared");
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        statusView = (TextView)findViewById(R.id.textStatus);

        Config config = new Config("sdktest", "YGP2pezGTI6ec48uti4o1w");
        config.setGlobalEventParameter("user_id", "92429d82a41e");

        Tapstream.create(getApplication(), config);
        lookupTimeline();
    }

}
