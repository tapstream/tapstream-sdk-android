package com.tapstream.sdk.wordofmouth;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.PopupWindow;

import com.tapstream.sdk.Logging;
import com.tapstream.sdk.Platform;
import com.tapstream.sdk.PopupWebView;
import com.tapstream.sdk.Tapstream;


/**
 * Date: 15-05-01
 * Time: 1:50 PM
 */
public class WordOfMouthImpl implements WordOfMouth{
    final Platform platform;


    public static WordOfMouth getInstance(Platform platform){
        return new WordOfMouthImpl(platform);
    }

    private WordOfMouthImpl(Platform platform){
        this.platform = platform;
    }

    @Override
    public void consumeReward(Reward reward){
        platform.consumeReward(reward);
    }

    @Override
    public boolean isConsumed(Reward reward){
        return platform.isConsumed(reward);
    }

    public void showOffer(final Activity mainActivity, View parent, final Offer o){
        /**
         * - Build PopupWindow
         * - Make WebView (given Context)
         * - Populate WebView with HTML postBody
         * - Add webview to PopupWindow
         * - Build WebViewClient
         *   - WebViewClient builds intent
         *   - WebViewClient sends intent (given mainActivity)
         */
        final PopupWebView popup = PopupWebView.initializeWithActivity(mainActivity);
        final String uuid = platform.loadSessionId();

        popup.showPopupWithMarkup(parent, o.getMarkup(), new WebViewClient(){
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                if(url.endsWith("accept")){
                    Intent sendIntent = new Intent();
                    sendIntent.setAction(Intent.ACTION_SEND);
                    sendIntent.putExtra(Intent.EXTRA_TEXT, o.prepareMessage(uuid));
                    sendIntent.setType("text/plain");
                    mainActivity.startActivity(sendIntent);
                }
                popup.dismiss();
                return true;
            }
        });
    }
}
