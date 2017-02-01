package com.tapstream.sdk.landers;


public interface ILanderDelegate {
    void showedLander(Lander lander);
    void dismissedLander();
    void submittedLander();
}
