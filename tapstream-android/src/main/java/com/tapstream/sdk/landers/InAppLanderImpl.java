package com.tapstream.sdk.landers;

import android.app.Activity;
import android.view.View;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.tapstream.sdk.Platform;
import com.tapstream.sdk.PopupWebView;



public class InAppLanderImpl {
    final Platform platform;

    public static InAppLanderImpl getInstance(Platform platform){
        return new InAppLanderImpl(platform);
    }
    InAppLanderImpl(Platform platform){
        this.platform = platform;
    }

    public boolean shouldShowLander(Lander lander){
        return !platform.hasShown(lander);
    }

    public void showLander(final Activity mainActivity, View parent, final Lander lander, final ILanderDelegate delegate) {
        final PopupWebView popup = PopupWebView.initializeWithActivity(mainActivity);

        final WebViewClient client = new WebViewClient(){
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                final String url = request.getUrl().toString();
                if(url.endsWith("close") || url.endsWith("close/")) {
                    delegate.dismissedLander();
                    popup.dismiss();
                    return true;
                }

                return false;
            }

            @Override
            public void onPageFinished(WebView view, String url){
                // Make sure this isn't the initial load.
                if(!url.startsWith("https://tapstream.com") && !url.equals(lander.getUrl())){
                    popup.dismiss();
                    delegate.submittedLander();
                }
            }
        };

        if(lander.getUrl() == null) {
            popup.showPopupWithMarkup(parent, lander.getMarkup(), client);
        }else{
            popup.showPopupWithUrl(parent, lander.getUrl(), client);
        }

        platform.registerLanderShown(lander);
        delegate.showedLander(lander);
    }
}
