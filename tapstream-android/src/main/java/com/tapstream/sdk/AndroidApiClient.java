package com.tapstream.sdk;

import android.app.Activity;
import android.view.View;

import com.tapstream.sdk.landers.ILanderDelegate;
import com.tapstream.sdk.landers.Lander;
import com.tapstream.sdk.wordofmouth.WordOfMouth;

public interface AndroidApiClient extends ApiClient {
    /**
     * @return A WordOfMouth object used for working with Tapstream's feature
     *         of the same name.
     *
     * @see WordOfMouth
     */

    class ServiceNotEnabled extends RuntimeException {
        public ServiceNotEnabled(String message){ super(message); }
    }

    WordOfMouth getWordOfMouth() throws ServiceNotEnabled;

    void showLanderIfUnseen(Activity mainActivity, View parent, Lander lander) throws ServiceNotEnabled;
    void showLanderIfUnseen(Activity mainActivity, View parent, Lander lander, ILanderDelegate delegate) throws ServiceNotEnabled;
}
