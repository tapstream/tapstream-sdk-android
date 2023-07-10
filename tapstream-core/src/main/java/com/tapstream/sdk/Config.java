package com.tapstream.sdk;

public class Config {

    private final String accountName;
    private final String developerSecret;

    public Config(String accountName, String developerSecret) {
        this.accountName = accountName;
        this.developerSecret = developerSecret;
    }

    private Retry.Strategy dataCollectionRetryStrategy = Retry.DEFAULT_DATA_COLLECTION_STRATEGY;
    private Retry.Strategy userFacingRequestRetryStrategy = Retry.DEFAULT_USER_FACING_RETRY_STRATEGY;

    // Set these if you want to override the names of the automatic events sent by the sdk
    private String installEventName = null;
    private String openEventName = null;

    // Unset these if you want to disable the sending of the automatic events
    private boolean fireAutomaticInstallEvent = true;
    private boolean fireAutomaticOpenEvent = true;

    // Unset this if you want to disable automatic collection of Android Advertising Id
    private boolean collectAdvertisingId = false;

    // Unset this to save a little memory by not using the WordOfMouth feature.
    private boolean useWordOfMouth = false;

    // Unset this to save a bit more memory by not using the InAppLanders feature.
    private boolean useInAppLanders = false;

    // These parameters will be automatically attached to all events fired by the sdk.
    private final Event.Params globalEventParams = new Event.Params();

    private boolean activityListenerBindsLate = false;

    public String getAccountName() {
        return accountName;
    }

    public String getDeveloperSecret() {
        return developerSecret;
    }

    public void setGlobalEventParameter(String name, Object value) {
        globalEventParams.put(name, value.toString());
    }

    public Event.Params getGlobalEventParams() {
        return globalEventParams;
    }
	
    public String getInstallEventName() {
        return installEventName;
    }

    public void setInstallEventName(String name) {
        installEventName = name;
    }

    public String getOpenEventName() {
        return openEventName;
    }

    public void setOpenEventName(String name) {
        openEventName = name;
    }

    public boolean getFireAutomaticInstallEvent() {
        return fireAutomaticInstallEvent;
    }

    public void setFireAutomaticInstallEvent(boolean fire) {
        fireAutomaticInstallEvent = fire;
    }

    public boolean getFireAutomaticOpenEvent() {
        return fireAutomaticOpenEvent;
    }

    public void setFireAutomaticOpenEvent(boolean fire) {
        fireAutomaticOpenEvent = fire;
    }

    public boolean getCollectAdvertisingId() {
        return collectAdvertisingId;
    }

    public void setCollectAdvertisingId(boolean collect) {
        collectAdvertisingId = collect;
    }

    public boolean getUseWordOfMouth() {
        return useWordOfMouth;
    }

    public void setUseWordOfMouth(boolean v) {
        useWordOfMouth = v;
    }

    public boolean getUseInAppLanders() {
        return useInAppLanders;
    }

    public void setUseInAppLanders(boolean useInAppLanders) {
        this.useInAppLanders = useInAppLanders;
    }

    public Retry.Strategy getDataCollectionRetryStrategy() {
        return dataCollectionRetryStrategy;
    }

    public void setDataCollectionRetryStrategy(Retry.Strategy dataCollectionRetryStrategy) {
        this.dataCollectionRetryStrategy = dataCollectionRetryStrategy;
    }

    public Retry.Strategy getUserFacingRequestRetryStrategy() {
        return userFacingRequestRetryStrategy;
    }

    public void setUserFacingRequestRetryStrategy(Retry.Strategy userFacingRequestRetryStrategy) {
        this.userFacingRequestRetryStrategy = userFacingRequestRetryStrategy;
    }

    public boolean getActivityListenerBindsLate() {
        return activityListenerBindsLate;
    }

    public void setActivityListenerBindsLate(boolean activityListenerBindsLate) {
        this.activityListenerBindsLate = activityListenerBindsLate;
    }
}
