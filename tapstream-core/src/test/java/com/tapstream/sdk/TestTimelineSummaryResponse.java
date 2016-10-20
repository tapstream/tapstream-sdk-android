package com.tapstream.sdk;

import com.tapstream.sdk.http.HttpResponse;

import org.junit.Test;


import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.collection.IsIterableContainingInOrder.contains;


public class TestTimelineSummaryResponse {

    Long timestamp = System.currentTimeMillis();

    String validBody = "{" +
            "\"latest_deeplink\": \"testapp://somepath\"," +
            "\"latest_deeplink_timestamp\": " + timestamp + "," +
            "\"deeplinks\": [\"testapp://someotherpath\",\"testapp://somepath\"]," +
            "\"campaigns\": [\"mycampaign1\", \"mycampaign2\"]," +
            "\"hit_params\": {\"mypar1\": \"myval1\",\"mypar2\":\"myval2\"}," +
            "\"event_params\": {\"myepar1\": \"myeval1\",\"myepar2\":\"myeval2\"}" +
            "}";

    TimelineSummaryResponse legacyEmptyResponse = TimelineSummaryResponse.createSummaryResponse(new HttpResponse(200, "OK", "{}".getBytes()));
    TimelineSummaryResponse emptyStringResponse = TimelineSummaryResponse.createSummaryResponse(new HttpResponse(200, "OK", "".getBytes()));
    TimelineSummaryResponse nullBodyResponse = TimelineSummaryResponse.createSummaryResponse(new HttpResponse(200, "OK", null));
    TimelineSummaryResponse validResponse = TimelineSummaryResponse.createSummaryResponse(new HttpResponse(200, "OK", validBody.getBytes()));

    @Test
    public void testEmptyTimeline() {
        assertThat(legacyEmptyResponse.isEmpty(), is(true));
        assertThat(emptyStringResponse.isEmpty(), is(true));
        assertThat(nullBodyResponse.isEmpty(), is(true));
        assertThat(validResponse.isEmpty(), is(false));
    }

    @Test
    public void testTimelineParsing() {
        assertThat(validResponse.getHitParams().size(), is(2));
        assertThat(validResponse.getHitParams(), hasEntry("mypar1", "myval1"));
        assertThat(validResponse.getHitParams(), hasEntry("mypar2", "myval2"));
        assertThat(validResponse.getEventParams().size(), is(2));
        assertThat(validResponse.getEventParams(), hasEntry("myepar1", "myeval1"));
        assertThat(validResponse.getEventParams(), hasEntry("myepar2", "myeval2"));
        assertThat(validResponse.getCampaigns().size(), is(2));
        assertThat(validResponse.getCampaigns(), contains("mycampaign1", "mycampaign2"));
        assertThat(validResponse.getDeeplinks().size(), is(2));
        assertThat(validResponse.getDeeplinks(), contains("testapp://someotherpath", "testapp://somepath"));
        assertThat(validResponse.getLatestDeeplink(), is("testapp://somepath"));
        assertThat(validResponse.getLatestDeeplinkTimestamp(), is(timestamp));
    }

}