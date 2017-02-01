package com.tapstream.sdk;

import android.app.Activity;
import android.app.Application;
import android.app.Service;
import android.view.View;

import com.tapstream.sdk.landers.ILanderDelegate;
import com.tapstream.sdk.landers.InAppLanderImpl;
import com.tapstream.sdk.landers.Lander;
import com.tapstream.sdk.landers.LanderApiResponse;
import com.tapstream.sdk.wordofmouth.OfferApiResponse;
import com.tapstream.sdk.wordofmouth.RewardApiResponse;
import com.tapstream.sdk.wordofmouth.WordOfMouth;
import com.tapstream.sdk.wordofmouth.WordOfMouthImpl;

import java.io.IOException;

public class Tapstream implements AndroidApiClient {
	private static Tapstream instance;
	private WordOfMouth wom;
	private InAppLanderImpl landerImpl;
	private ApiClient client;


	static {
		if (!Logging.isConfigured()) {
			Logging.setLogger(new AndroidLogger());
		}
	}

	public interface ClientBuilder {
		ApiClient build(Platform platform, Config config);
	}

	private static ClientBuilder clientBuilder;

	public static class DefaultClientBuilder implements ClientBuilder {
		@Override
		public ApiClient build(Platform platform, Config config) {
			HttpApiClient client = new HttpApiClient(platform, config);
			client.start();
			return client;
		}
	}

	synchronized public static void setClientBuilder(ClientBuilder clientBuilder){
		Tapstream.clientBuilder = clientBuilder;
	}

	synchronized public static void create(Application app, Config config) {
		if (instance == null) {
			ClientBuilder builder = clientBuilder == null ? new DefaultClientBuilder() : clientBuilder;
			final Platform platform = new AndroidPlatform(app);
			WordOfMouth wom = null;
			InAppLanderImpl ial = null;

			if(config.getUseWordOfMouth()) {
				wom = WordOfMouthImpl.getInstance(platform);
			}

			if(config.getUseInAppLanders()) {
				ial = InAppLanderImpl.getInstance(platform);
			}

			instance = new Tapstream(builder.build(platform, config), wom, ial);

		} else {
			Logging.log(Logging.WARN, "Tapstream Warning: Tapstream already instantiated, it cannot be re-created.");
		}
	}

	synchronized public static Tapstream getInstance() {
		if (instance == null) {
			throw new RuntimeException("You must first call Tapstream.create");
		}
		return instance;
	}

	Tapstream(ApiClient client, WordOfMouth wom, InAppLanderImpl landerImpl){
		this.client = client;
		this.wom = wom;
		this.landerImpl = landerImpl;
	}

	@Override
	public void close() throws IOException {
		instance.close();
	}

	@Override
	public ApiFuture<EventApiResponse> fireEvent(Event e) {
		return client.fireEvent(e);
	}

	@Override
	public ApiFuture<TimelineApiResponse> lookupTimeline() {
		return client.lookupTimeline();
	}

	@Override
	public ApiFuture<TimelineSummaryResponse> getTimelineSummary() {
		return client.getTimelineSummary();
	}

	@Override
	public ApiFuture<OfferApiResponse> getWordOfMouthOffer(String insertionPoint) {
		return client.getWordOfMouthOffer(insertionPoint);
	}

	@Override
	public ApiFuture<RewardApiResponse> getWordOfMouthRewardList() {
		return client.getWordOfMouthRewardList();
	}


	@Override
	public WordOfMouth getWordOfMouth() throws ServiceNotEnabled {
		if(wom == null){
			throw new ServiceNotEnabled(
				"To use Word of Mouth features, " +
				"ensure that the useWordOfMouth setting " +
				"in your configuration is enabled."
			);
		}
		return wom;
	}


	@Override
	public ApiFuture<LanderApiResponse> getInAppLander() {
		return client.getInAppLander();
	}

	@Override
	public void showLanderIfUnseen(final Activity mainActivity, View view, Lander lander) throws ServiceNotEnabled{

		ILanderDelegate noopDelegate = new ILanderDelegate() {
			@Override
			public void showedLander(Lander lander) {}

			@Override
			public void dismissedLander() {}

			@Override
			public void submittedLander() {}
		};

		showLanderIfUnseen(mainActivity, view, lander, noopDelegate);
	}

	@Override
	public void showLanderIfUnseen(final Activity mainActivity,
						   final View parent,
						   final Lander lander,
						   final ILanderDelegate delegate) throws ServiceNotEnabled {
		if(landerImpl == null){
			throw new ServiceNotEnabled(
					"To use In-App Lander features, " +
							"ensure that the getUseInAppLanders setting " +
							"in your configuration is enabled."
			);
		}

		if(landerImpl.shouldShowLander(lander)) {
			landerImpl.showLander(mainActivity, parent, lander, delegate);
		}
	}
}
