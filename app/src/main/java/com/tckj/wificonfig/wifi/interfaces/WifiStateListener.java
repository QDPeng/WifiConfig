package com.tckj.wificonfig.wifi.interfaces;


public interface WifiStateListener{

    void onStateChange(int wifiState);

    void onWifiEnabled();

    void onWifiEnabling();

    void onWifiDisabling();

    void onWifiDisabled();

}