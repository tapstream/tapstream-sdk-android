package com.tapstream.sdk;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.PopupWindow;


public class PopupWebView {
    final PopupWindow window;
    final WebView wv;

    public static PopupWebView initializeWithActivity(final Activity mainActivity){
        final Context applicationContext = mainActivity.getApplicationContext();
        WebView wv;
        try {
            wv = new WebView(applicationContext);
        }catch(RuntimeException e){
            Logging.log(Logging.ERROR, "RuntimeException thrown creating WebView. This probably" +
                            "means you tried to show a popup on a non-ui thread. Stack trace: %s",
                    Log.getStackTraceString(e));
            throw e;
        }

        return new PopupWebView(new PopupWindow(
                wv,
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT), wv);
    }

    PopupWebView(final PopupWindow window, final WebView wv){
        this.window = window;
        this.wv = wv;
    }

    public void showPopupWithUrl(View parent, String url, WebViewClient webViewClient){
        window.showAtLocation(parent, Gravity.NO_GRAVITY, 0, 0);

        wv.loadUrl(url);
        wv.setBackgroundColor(Color.TRANSPARENT);

        wv.setWebViewClient(webViewClient);
    }
    public void showPopupWithMarkup(View parent, String markup, WebViewClient webViewClient){

        window.showAtLocation(parent, Gravity.NO_GRAVITY, 0, 0);

        wv.loadDataWithBaseURL("https://tapstream.com/", markup, "text/html", null, "https://tapstream.com/");
        wv.setBackgroundColor(Color.TRANSPARENT);

        wv.setWebViewClient(webViewClient);
    }

    public void dismiss(){
        this.window.dismiss();
    }
}
