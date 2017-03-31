package com.tapstream.sdk;

import com.google.common.io.Resources;
import com.tapstream.sdk.errors.EventAlreadyFiredException;
import com.tapstream.sdk.errors.RetriesExhaustedException;
import com.tapstream.sdk.errors.UnrecoverableApiException;
import com.tapstream.sdk.http.HttpClient;
import com.tapstream.sdk.http.HttpRequest;
import com.tapstream.sdk.http.HttpResponse;
import com.tapstream.sdk.http.RequestBuilders;
import com.tapstream.sdk.wordofmouth.Offer;
import com.tapstream.sdk.wordofmouth.OfferApiResponse;
import com.tapstream.sdk.wordofmouth.Reward;
import com.tapstream.sdk.wordofmouth.RewardApiResponse;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.io.IOException;
import java.net.URL;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static com.tapstream.sdk.matchers.RequestURLMatcher.urlEq;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;



public class TestHttpApiClient {

    Platform platform;
    HttpClient httpClient;

    Config config;
    HttpApiClient apiClient;
    ScheduledExecutorService executor;

    static HttpResponse jsonResponse(String jsonResourcePath) throws IOException {
        URL resourceURL = Resources.getResource(jsonResourcePath);
        return new HttpResponse(200, "", Resources.toByteArray(resourceURL));
    }

    @Before
    public void setup() throws Exception {
        platform = mock(Platform.class);
        httpClient = mock(HttpClient.class);
        Set<String> alreadyFired = new HashSet<String>();
        alreadyFired.add("alreadyFired");
        when(platform.loadFiredEvents()).thenReturn(alreadyFired);
        config = new Config("accountName", "theSecret");
        config.setFireAutomaticInstallEvent(false);
        config.setFireAutomaticOpenEvent(false);
        executor = Executors.newSingleThreadScheduledExecutor(new DaemonThreadFactory());
        apiClient = new HttpApiClient(platform, config, httpClient, executor);
    }

    @After
    public void tearDown() throws Exception {
        executor.shutdownNow();
        executor.awaitTermination(1, TimeUnit.SECONDS);
    }

    @Test
    public void testTimelineLookup() throws Exception {
        String session = UUID.randomUUID().toString();
        when(platform.loadSessionId()).thenReturn(session);
        apiClient.start();
        HttpRequest request = RequestBuilders.timelineLookupRequestBuilder("theSecret", session).build();
        when(httpClient.sendRequest(request)).thenReturn(new HttpResponse(200, "OK"));
        TimelineApiResponse response = apiClient.lookupTimeline().get();
        assertThat(response.getHttpResponse().getStatus(), is(200));
        assertThat(response.isEmpty(), is(true));
    }

    @Test
    public void testGetTimelineSummary() throws Exception {
        String session = UUID.randomUUID().toString();
        when(platform.loadSessionId()).thenReturn(session);
        apiClient.start();
        HttpRequest request = RequestBuilders.timelineSummaryRequestBuilder("theSecret", session).build();
        when(httpClient.sendRequest(request)).thenReturn(new HttpResponse(200, "OK"));
        TimelineSummaryResponse response = apiClient.getTimelineSummary().get();
        assertThat(response.getHttpResponse().getStatus(), is(200));
        assertThat(response.isEmpty(), is(true));
    }

    @Test
    public void testAutoOpenEvent() throws Exception {
        when(httpClient.sendRequest(any(HttpRequest.class))).thenReturn(new HttpResponse(200, "OK"));
        when(platform.getAppName()).thenReturn("testapp");
        config.setFireAutomaticOpenEvent(true);

        apiClient.start();

        ArgumentCaptor<HttpRequest> requestCaptor = ArgumentCaptor.forClass(HttpRequest.class);
        Thread.sleep(200);
        verify(httpClient).sendRequest(requestCaptor.capture());
        assertThat(requestCaptor.getValue().getURL(), is(new URL("https://api.tapstream.com/accountName/event/android-testapp-open")));
    }

    @Test
    public void testAutoOpenEventWithLateBinding() throws Exception {
        ActivityEventSource.ActivityListener listener = mock(ActivityEventSource.ActivityListener.class);
        ActivityEventSource aes = new ActivityEventSource();
        aes.setListener(listener);

        when(httpClient.sendRequest(any(HttpRequest.class))).thenReturn(new HttpResponse(200, "OK"));
        when(platform.getAppName()).thenReturn("testapp");
        when(platform.getActivityEventSource()).thenReturn(aes);

        config.setFireAutomaticOpenEvent(true);
        config.setActivityListenerBindsLate(true);

        apiClient.start();

        ArgumentCaptor<HttpRequest> requestCaptor = ArgumentCaptor.forClass(HttpRequest.class);
        Thread.sleep(200);
        verify(httpClient).sendRequest(requestCaptor.capture());
        assertThat(requestCaptor.getValue().getURL(), is(new URL("https://api.tapstream.com/accountName/event/android-testapp-open")));
    }

    @Test
    public void testAutoInstallEvent() throws Exception {
        when(httpClient.sendRequest(any(HttpRequest.class))).thenReturn(new HttpResponse(200, "OK"));
        when(platform.getAppName()).thenReturn("testapp");
        config.setFireAutomaticInstallEvent(true);

        apiClient.start();

        ArgumentCaptor<HttpRequest> requestCaptor = ArgumentCaptor.forClass(HttpRequest.class);
        Thread.sleep(200);
        verify(httpClient).sendRequest(requestCaptor.capture());
        assertThat(requestCaptor.getValue().getURL(), is(new URL("https://api.tapstream.com/accountName/event/android-testapp-install")));
    }

    @Test
    public void testEventInteractionWithClient() throws Exception {
        String appName = "appName";
        when(platform.getAppName()).thenReturn(appName);
        when(httpClient.sendRequest(any(HttpRequest.class))).thenReturn(new HttpResponse(200, "OK"));
        apiClient.start();
        Event event = spy(new Event("eventName", false));
        apiClient.fireEvent(event).get();
        //verify(event).prepare(appName);
        //verify(event).buildPostBody(apiClient.getCommonEventParams(), config.getGlobalEventParams());
        verify(httpClient).sendRequest(any(HttpRequest.class));
    }

    @Test
    public void testFireEventMultipleTimes() throws Exception {
        when(httpClient.sendRequest(any(HttpRequest.class))).thenReturn(new HttpResponse(200, "OK"));
        apiClient.start();
        Event event = new Event("eventName", false);
        ApiFuture<EventApiResponse> firstResponse = apiClient.fireEvent(event);
        ApiFuture<EventApiResponse> secondResponse = apiClient.fireEvent(event);
        assertThat(firstResponse.get().getHttpResponse().getStatus(), is(200));
        assertThat(secondResponse.get().getHttpResponse().getStatus(), is(200));
        verify(httpClient, times(2)).sendRequest(any(HttpRequest.class));
    }

    @Test(timeout=1000)
    public void testFireEventWithRetries() throws Exception{
        apiClient.start();
        config.setDataCollectionRetryStrategy(new Retry.FixedDelay(100, 5, 1000));
        when(httpClient.sendRequest(any(HttpRequest.class)))
                .thenReturn(new HttpResponse(500, "ERROR"))
                .thenReturn(new HttpResponse(500, "ERROR"))
                .thenReturn(new HttpResponse(200, "OK"));
        Event event = new Event("eventName", false);
        ApiFuture<EventApiResponse> firstResponse = apiClient.fireEvent(event);
        assertThat(firstResponse.get().getHttpResponse().getStatus(), is(200));
        verify(httpClient, times(3)).sendRequest((HttpRequest) anyObject());
    }

    @Test(timeout=1000)
    public void testFireEventWithRetriesExhausted() throws Exception{
        apiClient.start();

        config.setDataCollectionRetryStrategy(new Retry.FixedDelay(100, 1, 1000));
        when(httpClient.sendRequest(any(HttpRequest.class)))
                .thenReturn(new HttpResponse(500, "ERROR"));

        Event event = new Event("eventName", false);
        ApiFuture<EventApiResponse> firstResponse = apiClient.fireEvent(event);

        try {
            assertThat(firstResponse.get().getHttpResponse().getStatus(), is(200));
            fail("Expected retries exhausted exception");
        } catch (ExecutionException e){
            assertThat(e.getCause(), is(instanceOf(RetriesExhaustedException.class)));
        }

        verify(httpClient, times(1)).sendRequest((HttpRequest) anyObject());
    }


    @Test
    public void testFireEventWithHardFail() throws Exception{
        apiClient.start();

        config.setDataCollectionRetryStrategy(new Retry.FixedDelay(100, 5, 1000));
        when(httpClient.sendRequest(any(HttpRequest.class)))
                .thenReturn(new HttpResponse(404, "ERROR"))
                .thenReturn(new HttpResponse(200, "OK"));

        Event event = new Event("eventName", false);
        ApiFuture<EventApiResponse> firstResponse = apiClient.fireEvent(event);

        try {
            assertThat(firstResponse.get().getHttpResponse().getStatus(), is(200));
            fail("Expected retries exhausted exception");
        } catch (ExecutionException e){
            assertThat(e.getCause(), is(instanceOf(UnrecoverableApiException.class)));
        }

        verify(httpClient, times(1)).sendRequest((HttpRequest) anyObject());
    }


    @Test(expected = EventAlreadyFiredException.class)
    public void testOneTimeOnlyEvents() throws Throwable {
        apiClient.start();
        when(httpClient.sendRequest(any(HttpRequest.class))).thenReturn(new HttpResponse(200, "OK"));
        Event event = new Event("eventName", true);
        ApiFuture<EventApiResponse> firstResponse = apiClient.fireEvent(event);
        ApiFuture<EventApiResponse> secondResponse = apiClient.fireEvent(event);
        assertThat(firstResponse.get().getHttpResponse().getStatus(), is(200));

        try {
            secondResponse.get();
        } catch (ExecutionException e) {
            throw e.getCause();
        }
    }


    @Test(expected = EventAlreadyFiredException.class)
    public void testPersistedOneTimeOnlyEvents() throws Throwable{
        apiClient.start();
        when(httpClient.sendRequest(any(HttpRequest.class))).thenReturn(new HttpResponse(200, "OK"));
        Event event = new Event("alreadyFired", true);
        ApiFuture<EventApiResponse> response = apiClient.fireEvent(event);

        try {
            response.get();
        } catch (ExecutionException e) {
            throw e.getCause();
        }

    }

    @Test
    public void testBuildCommonParams() throws Exception {
        String expectedReferrer = "referrerValue";
        when(platform.getReferrer()).thenReturn(expectedReferrer);

        String expectedGAID = "GAIDValue";
        boolean expectedLimitAdTracking = true;

        final AdvertisingID expectedAdvertisingId = new AdvertisingID(expectedGAID, expectedLimitAdTracking);
        when(platform.getAdIdFetcher()).thenReturn(new Callable<AdvertisingID>(){

            @Override
            public AdvertisingID call() throws Exception {
                return expectedAdvertisingId;
            }
        });
        String expectedSessionId = UUID.randomUUID().toString();
        when(platform.loadSessionId()).thenReturn(expectedSessionId);

        String expectedVendor = "vendorValue";
        when(platform.getManufacturer()).thenReturn(expectedVendor);

        String expectedModel = "modelValue";
        when(platform.getModel()).thenReturn(expectedModel);

        String expectedOs = "osValue";
        when(platform.getOs()).thenReturn(expectedOs);

        String expectedResolution = "123x456";
        when(platform.getResolution()).thenReturn(expectedResolution);

        String expectedLocale = "EN_US";
        when(platform.getLocale()).thenReturn(expectedLocale);

        String expectedAppName = "theAppName";
        when(platform.getAppName()).thenReturn(expectedAppName);

        String expectedAppVersion = "1.2.3";
        when(platform.getAppVersion()).thenReturn(expectedAppVersion);

        String expectedPackageName = "com.test";
        when(platform.getPackageName()).thenReturn(expectedPackageName);

        String expectedOdin1 = "odin1Value";
        config.setOdin1(expectedOdin1);

        String expectedOpenUdid = "openUdidValue";
        config.setOpenUdid(expectedOpenUdid);

        String expectedWifiMac = "wifiMacValue";
        config.setWifiMac(expectedWifiMac);

        String expectedTZOffset = Long.toString(TimeZone.getDefault().getOffset((new Date()).getTime()) / 1000);

        Map<String, String> commonParams = apiClient.buildCommonEventParams().toMap();
        assertThat(commonParams.remove("secret"), is("theSecret"));
        assertThat(commonParams.remove("sdkversion"), is(HttpApiClient.VERSION));
        assertThat(commonParams.remove("hardware-odin1"), is(expectedOdin1));
        assertThat(commonParams.remove("hardware-open-udid"), is(expectedOpenUdid));
        assertThat(commonParams.remove("hardware-wifi-mac"), is(expectedWifiMac));
        assertThat(commonParams.remove("uuid"), is(expectedSessionId));
        assertThat(commonParams.remove("platform"), is("Android"));
        assertThat(commonParams.remove("vendor"), is(expectedVendor));
        assertThat(commonParams.remove("model"), is(expectedModel));
        assertThat(commonParams.remove("os"), is(expectedOs));
        assertThat(commonParams.remove("resolution"), is(expectedResolution));
        assertThat(commonParams.remove("locale"), is(expectedLocale));
        assertThat(commonParams.remove("app-name"), is(expectedAppName));
        assertThat(commonParams.remove("app-version"), is(expectedAppVersion));
        assertThat(commonParams.remove("package-name"), is(expectedPackageName));
        assertThat(commonParams.remove("gmtoffset"), is(expectedTZOffset));
        assertThat(commonParams.remove("hardware-android-advertising-id"), is(expectedGAID));
        assertThat(commonParams.remove("android-limit-ad-tracking"), is(Boolean.toString(expectedLimitAdTracking)));
        assertThat(commonParams.remove("android-referrer"), is(expectedReferrer));
        assertThat(commonParams.isEmpty(), is(true));
    }


    @Test
    public void testGetWordOfMouthOffer() throws Exception {
        String bundle = platform.getPackageName();
        URL expectedURL = RequestBuilders
                .wordOfMouthOfferRequestBuilder(config.getDeveloperSecret(), "wom", bundle)
                .build().getURL();

        when(httpClient.sendRequest(urlEq(expectedURL))).thenReturn(jsonResponse("offer.json"));


        apiClient.start();
        ApiFuture<OfferApiResponse> futureOffer = apiClient.getWordOfMouthOffer("wom");
        Offer offer = futureOffer.get().getOffer();
        verify(httpClient, times(1)).sendRequest(urlEq(expectedURL));

        assertThat(offer, notNullValue());
        assertThat(offer.getMessage(), is("This is the message"));
    }

    @Test
    public void testGetWordOfMouthRewardList() throws Exception{
        when(platform.loadSessionId()).thenReturn("my-uuid");
        URL expectedURL = RequestBuilders
                .wordOfMouthRewardRequestBuilder(config.getDeveloperSecret(), "my-uuid")
                .build().getURL();
        when(httpClient.sendRequest(urlEq(expectedURL))).thenReturn(jsonResponse("rewards.json"));

        apiClient.start();
        final ApiFuture<RewardApiResponse> futureRewards = apiClient.getWordOfMouthRewardList();

        List<Reward> rewards = futureRewards.get().getRewards();
        verify(httpClient, times(1)).sendRequest(urlEq(expectedURL));
        assertThat(rewards, notNullValue());
        assertThat(rewards.size(), is(1));
        assertThat(rewards.get(0).getRewardSku(), is("my reward sku"));
    }


    @Test
    public void testRewardAutoConsumption() throws Exception{

        String session = platform.loadSessionId();
        URL expectedURL = RequestBuilders
                .wordOfMouthRewardRequestBuilder(config.getDeveloperSecret(), session)
                .build().getURL();

        when(httpClient.sendRequest(urlEq(expectedURL))).thenReturn(jsonResponse("rewards.json"));

        List<Reward> rewards = apiClient.getWordOfMouthRewardList().get().getRewards();
        assertThat(rewards.size(), is(1));

        when(platform.isConsumed((Reward) any())).thenReturn(true);

        rewards = apiClient.getWordOfMouthRewardList().get().getRewards();
        assertThat(rewards.size(), is(0));
    }

    @Test
    public void testLanderAPIResponse() throws Exception{

        String session = platform.loadSessionId();
        URL expectedURL = RequestBuilders
                .inAppLanderRequestBuilder(config.getDeveloperSecret(), session)
                .build().getURL();

        when(httpClient.sendRequest(urlEq(expectedURL))).thenReturn(new HttpResponse(404, "Not found"));

        try {
            HttpResponse resp = apiClient.getInAppLander().get().getHttpResponse();
        }catch(Exception e){
            Logging.log(Logging.INFO, e.getLocalizedMessage());
        }

    }

}