package com.tapstream.sdk;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

import android.os.Build;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.google.android.gms.ads.identifier.AdvertisingIdClient;
import com.google.android.gms.common.GooglePlayServicesNotAvailableException;

import org.junit.Assume;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class TestAdvertisingId extends BaseAndroidTest {

    @Test
    public void testFetcher() throws Exception {
        Assume.assumeTrue(Build.VERSION.SDK_INT >= 17);
        try {
            String expectedId = AdvertisingIdClient.getAdvertisingIdInfo(app.getApplicationContext()).getId();
            AdvertisingIdFetcher fetcher = new AdvertisingIdFetcher(app);
            AdvertisingID id = fetcher.call();
            assertThat(id, notNullValue());
            assertThat(id.getId(), is(expectedId));
            assertThat(id.isLimitAdTracking(), is(false));
        } catch (GooglePlayServicesNotAvailableException ignored) {
        }
    }
}
