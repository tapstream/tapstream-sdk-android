package com.tapstream.sdk;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

import android.content.Intent;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.tapstream.sdk.landers.Lander;
import com.tapstream.sdk.wordofmouth.Reward;

import org.hamcrest.Matchers;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.InputStream;
import java.util.Set;


@RunWith(AndroidJUnit4.class)
public class TestAndroidPlatform extends BaseAndroidTest {

    AndroidPlatform platform;

    @Before
    public void setup() throws Exception {
        platform = new AndroidPlatform(app);
    }

    @Test
    public void testLoadSessionId() throws Exception {
        assertThat(app.getSharedPreferences(AndroidPlatform.UUID_KEY, 0).getString("uuid", null), is(nullValue()));
        String sessionId = platform.loadSessionId();
        assertThat(sessionId, notNullValue());
        assertThat(app.getSharedPreferences(AndroidPlatform.UUID_KEY, 0).getString("uuid", null), is(sessionId));
        assertThat(platform.loadSessionId(), is(sessionId));
        assertThat(app.getSharedPreferences(AndroidPlatform.UUID_KEY, 0).getString("uuid", null), is(sessionId));
    }

    @Test
    public void testPersistFiredEvents() throws Exception {
        Set<String> firedEvents = platform.loadFiredEvents();
        assertThat(firedEvents, is(Matchers.empty()));
        firedEvents.add("eventName");
        platform.saveFiredEvents((firedEvents));
        assertThat(platform.loadFiredEvents(), is(firedEvents));
    }

    @Test
    public void testGetAppName() throws Exception {
        assertThat(platform.getAppName(), is("tapstream-android"));
    }

    @Test
    public void testGetPackageName() throws Exception {
        assertThat(platform.getPackageName(), is("com.tapstream.sdk.test"));
    }

    @Test
    public void testGetReferrer() throws Exception {
        String expected = "theReferrer";
        ReferrerReceiver r = new ReferrerReceiver();
        String url = "intent://scan/#Intent;scheme=zxing;package=com.google.zxing.client.android;S.referrer=" + expected + ";end" + expected;
        Intent intent = Intent.parseUri(url, Intent.URI_INTENT_SCHEME);
        r.onReceive(app, intent);
        String actual = platform.getReferrer();
        assertThat(actual, is(expected));
    }

    @Test
    public void testGetCountForReward() throws Exception {
        JSONObject rewardDelegate = new JSONObject("{\"offer_id\": 2}");
        Reward r = Reward.fromApiResponse(rewardDelegate);
        platform.consumeReward(r);
        assertThat(platform.getCountForReward(r), is(1));
    }

    @Test
    public void testConsumeReward() throws Exception {
        JSONObject rewardDelegate = new JSONObject("{\"offer_id\": 1}");
        Reward r = Reward.fromApiResponse(rewardDelegate);

        assertThat(platform.getCountForReward(r), is(0));

        platform.consumeReward(r);
        platform.consumeReward(r);
        platform.consumeReward(r);

        assertThat(platform.getCountForReward(r), is(3));
    }

    @Test
    public void testGetAdIdFetcher() throws Exception {
        Object fetcher = platform.getAdIdFetcher();
        assertThat(fetcher, allOf(notNullValue(), instanceOf(AdvertisingIdFetcher.class)));
    }

    @Test
    public void testGetActivityEventSource() throws Exception {
        Object aes = platform.getActivityEventSource();
        assertThat(aes, is(notNullValue()));
        assertThat(aes, instanceOf(ActivityCallbacks.class));
    }

    @Test
    public void testLanderLogic() throws Exception {
        InputStream is = app.getResources().openRawResource(com.tapstream.sdk.test.R.raw.lander_with_markup);
        byte[] body = Utils.readFully(is);
        Lander lander = Lander.fromApiResponse(new JSONObject(new String(body)));
        assertThat(platform.hasShown(lander), is(false));
        platform.registerLanderShown(lander);
        assertThat(platform.hasShown(lander), is(true));
    }
}