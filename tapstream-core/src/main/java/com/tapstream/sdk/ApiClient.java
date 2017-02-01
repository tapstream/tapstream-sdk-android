package com.tapstream.sdk;

import com.tapstream.sdk.landers.LanderApiResponse;
import com.tapstream.sdk.wordofmouth.OfferApiResponse;
import com.tapstream.sdk.wordofmouth.RewardApiResponse;

import java.io.Closeable;

public interface ApiClient extends Closeable {

	/**
	 * Report the given event to Tapstream's analytics platform.
	 *
	 * @param e An Event object to be reported to Tapstream.
	 * @return An ApiFuture wrapping an EventApiResponse, from which the
	 *         HttpResponse can be obtained if necessary.
     */
    ApiFuture<EventApiResponse> fireEvent(Event e);

	/**
	 * Queries Tapstream's timeline lookup API to get a list of historic hits
	 * and events associated with the current user. May be used to implement
	 * deep links. For more information please see
	 * <a href="https://tapstream.com/developer/android/onboarding-links/">Tapstream's documentation</a>
	 * on the subject.
	 *
	 * @return An ApiFuture wrapping a TimelineApiResponse, from which a {@link org.json.JSONObject}
	 * 		can be parsed.
     */
    ApiFuture<TimelineApiResponse> lookupTimeline();

	/**
	 * Queries Tapstream's timeline summary API to get a summary of the current user's timeline.
	 * May be used to more easily implement deferred deep links. For more information please see
	 * <a href="https://tapstream.com/developer/android/onboarding-links/">Tapstream's documentation</a>
	 * on the subject.
	 *
	 * @return An ApiFuture wrapping a TimelineSummaryResponse, which provides a summary of the current
	 * 		user's timeline (in particular the latestDeeplink field).
     */
	ApiFuture<TimelineSummaryResponse> getTimelineSummary();

	/**
	 * Queries the Word of Mouth API to retrieve an offer to be displayed to
	 * the current user, at the given insertion point.
	 *
	 * @param insertionPoint The string id for this insertion point that was
	 *                       configured in Tapstream's dashboard.
	 * @return An ApiFuture wrapping an OfferApiResponse
	 *
	 * <pre>
	 * {@code
	 *     Offer offer = apiClient
	 *         .getWordOfMouthOffer("my-insertion-point")
	 *         .get()
	 *         .getOffer();
	 * }
	 * </pre>
     */
    ApiFuture<OfferApiResponse> getWordOfMouthOffer(final String insertionPoint);

	/**
	 * Queries the Word of Mouth API to retrieve a list of rewards associated
	 * with the current user.
	 *
	 * @return An ApiFuture wrapping a RewardApiResponse from which a
	 *          {@link java.util.List} can be obtained.
	 *
	 * <pre>
	 * {@code
	 *     List<Reward> rewards = apiClient
	 *         .getWordOfMouthRewardList()
	 *         .get()
	 *         .getRewards();
	 * }
	 * </pre>
	 */
	ApiFuture<RewardApiResponse> getWordOfMouthRewardList();

	ApiFuture<LanderApiResponse> getInAppLander();
}
