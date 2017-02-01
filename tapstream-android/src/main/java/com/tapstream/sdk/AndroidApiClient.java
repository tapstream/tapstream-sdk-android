package com.tapstream.sdk;

import android.app.Activity;
import android.view.View;

import com.tapstream.sdk.landers.ILanderDelegate;
import com.tapstream.sdk.landers.Lander;
import com.tapstream.sdk.wordofmouth.WordOfMouth;

public interface AndroidApiClient extends ApiClient {

    class ServiceNotEnabled extends RuntimeException {
        public ServiceNotEnabled(String message){ super(message); }
    }
    /**
     * @return A WordOfMouth object used for working with Tapstream's feature
     *         of the same name.
     *
     * @see WordOfMouth
     */

    WordOfMouth getWordOfMouth() throws ServiceNotEnabled;

    /**
     * @param mainActivity Your app's main activity
     * @param parent The parent view you want to show a popup over
     * @param lander The Lander whose content you want to display (if it hasn't already)
     * @throws ServiceNotEnabled
     */
    void showLanderIfUnseen(Activity mainActivity, View parent, Lander lander) throws ServiceNotEnabled;
    void showLanderIfUnseen(Activity mainActivity, View parent, Lander lander, ILanderDelegate delegate) throws ServiceNotEnabled;
}
