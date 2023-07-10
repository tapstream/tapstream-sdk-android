package com.tapstream.sdk;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;

import androidx.test.platform.app.InstrumentationRegistry;

import com.tapstream.sdk.http.HttpResponse;

import org.junit.Before;

import java.io.InputStream;

abstract public class BaseAndroidTest {

    protected Application app;

    @Before
    final public void setupApp() {
        final Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        app = (Application) context.getApplicationContext();
        clearState();
    }

    public void clearPrefs(String key) {
        SharedPreferences prefs = app.getApplicationContext().getSharedPreferences(key, 0);
        SharedPreferences.Editor editor = prefs.edit();
        editor.clear();
        editor.apply();
    }

    public void clearState() {
        clearPrefs(AndroidPlatform.FIRED_EVENTS_KEY);
        clearPrefs(AndroidPlatform.UUID_KEY);
        clearPrefs(AndroidPlatform.WOM_REWARDS_KEY);
        clearPrefs(AndroidPlatform.IN_APP_LANDERS_KEY);
    }

    public HttpResponse jsonResponse(int resId) throws Exception {
        InputStream is = app.getResources().openRawResource(resId);
        byte[] body = Utils.readFully(is);
        return new HttpResponse(200, "", body);
    }
}
