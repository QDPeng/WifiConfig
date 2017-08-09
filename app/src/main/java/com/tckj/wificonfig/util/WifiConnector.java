package com.tckj.wificonfig.util;

import android.content.Context;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.text.TextUtils;
import android.util.Log;

import java.util.List;

/**
 * Created by peng on 2017/8/1.
 */

public class WifiConnector {
    private static final String TAG = "wificonfig";
    private WifiManager wifiManager;
    private Context context;

    public WifiConnector(Context context) {
        this.context = context;
        wifiManager = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
    }

    /**
     * 定义几种加密方式，一种是WEP，一种是WPA，还有没有密码的情况
     */
    public enum WifiType {
        WIFICIPHER_WEP, WIFICIPHER_WPA, WIFICIPHER_NOPASS, WIFICIPHER_INVALID
    }

    /**
     * 提供一个外部接口，传入要连接的无线网
     *
     * @param ssid
     * @param password
     */
    public void connect(String ssid, String password) {
        WifiType type = getCipherType(ssid);
        Thread thread = new Thread(new ConnectRunnable(ssid, password, type));
        thread.start();
    }

    /***
     * 查看以前是否也配置过这个网络
     * @param SSID
     * @return
     */
    private WifiConfiguration isExsits(String SSID) {
        List<WifiConfiguration> existingConfigs = wifiManager
                .getConfiguredNetworks();
        for (WifiConfiguration existingConfig : existingConfigs) {
            if (existingConfig.SSID.equals("\"" + SSID + "\"")) {
                return existingConfig;
            }
        }
        return null;
    }

    private void removeCurrentNetwork(Context context) {
        String ssid = NetUtils.getCurentWifiSSID(context);
        if (TextUtils.isEmpty(ssid)) return;
        WifiConfiguration config = isExsits(ssid);
        if (config != null) {
            wifiManager.removeNetwork(config.networkId);
        }
    }

    /**
     * 创建wifi连接配置
     *
     * @param SSID
     * @param Password
     * @param Type
     * @return
     */
    private WifiConfiguration createWifiConfiguration(String SSID, String Password, WifiType Type) {
        WifiConfiguration config = new WifiConfiguration();
        config.allowedAuthAlgorithms.clear();
        config.allowedGroupCiphers.clear();
        config.allowedKeyManagement.clear();
        config.allowedPairwiseCiphers.clear();
        config.allowedProtocols.clear();
        config.SSID = "\"" + SSID + "\"";
        // config.SSID = SSID;
        // nopass
        if (Type == WifiType.WIFICIPHER_NOPASS) {
            // config.wepKeys[0] = "";
            config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
            // config.wepTxKeyIndex = 0;
        }
        // wep
        if (Type == WifiType.WIFICIPHER_WEP) {
            if (!TextUtils.isEmpty(Password)) {
                if (isHexWepKey(Password)) {
                    config.wepKeys[0] = Password;
                } else {
                    config.wepKeys[0] = "\"" + Password + "\"";
                }
            }
            config.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.OPEN);
            config.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.SHARED);
            config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
            config.wepTxKeyIndex = 0;
        }
        // wpa
        if (Type == WifiType.WIFICIPHER_WPA) {
            config.preSharedKey = "\"" + Password + "\"";
            config.hiddenSSID = true;
            config.allowedAuthAlgorithms
                    .set(WifiConfiguration.AuthAlgorithm.OPEN);
            config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);
            config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);
            config.allowedPairwiseCiphers
                    .set(WifiConfiguration.PairwiseCipher.TKIP);
            // 此处需要修改否则不能自动重联
            // config.allowedProtocols.set(WifiConfiguration.Protocol.WPA);
            config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);
            config.allowedPairwiseCiphers
                    .set(WifiConfiguration.PairwiseCipher.CCMP);
            config.status = WifiConfiguration.Status.ENABLED;

        }
        return config;
    }

    // 打开wifi功能
    private boolean openWifi() {
        boolean bRet = true;
        if (!wifiManager.isWifiEnabled()) {
            bRet = wifiManager.setWifiEnabled(true);
        }
        return bRet;
    }

    // 关闭WIFI
    private void closeWifi() {
        if (wifiManager.isWifiEnabled()) {
            wifiManager.setWifiEnabled(false);
        }
    }

    private class ConnectRunnable implements Runnable {
        private String ssid;

        private String password;

        private WifiType type;

        public ConnectRunnable(String ssid, String password, WifiType type) {
            this.ssid = ssid;
            this.password = password;
            this.type = type;
        }

        @Override
        public void run() {
            // 打开wifi
            openWifi();
            // 开启wifi功能需要一段时间(我在手机上测试一般需要1-3秒左右)，所以要等到wifi
            // 状态变成WIFI_STATE_ENABLED的时候才能执行下面的语句
            while (wifiManager.getWifiState() == WifiManager.WIFI_STATE_ENABLING) {
                try {
                    // 为了避免程序一直while循环，让它睡个100毫秒检测……
                    Thread.sleep(100);

                } catch (InterruptedException ie) {
                    ie.printStackTrace();
                }
            }

            WifiConfiguration tempConfig = isExsits(ssid);

            if (tempConfig != null) {
                wifiManager.removeNetwork(tempConfig.networkId);
//                boolean enabled = wifiManager.enableNetwork(tempConfig.networkId, true);
//                if (connectCallback != null) {
//                    connectCallback.onConnectResult(enabled);
//                }
            }
            WifiConfiguration wifiConfig = createWifiConfiguration(ssid, password, type);
            if (wifiConfig == null) {
                return;
            }
            int netID = wifiManager.addNetwork(wifiConfig);
            removeCurrentNetwork(context);
            Log.d(TAG, "netID=" + netID);
            boolean enabled = wifiManager.enableNetwork(netID, true);
            if (connectCallback != null) {
                connectCallback.onConnectResult(enabled);
            }
            Log.d(TAG, "enableNetwork status enable=" + enabled);
            boolean connected = wifiManager.reconnect();
            Log.d(TAG, "enableNetwork connected=" + connected);


        }
    }

    private static boolean isHexWepKey(String wepKey) {
        final int len = wepKey.length();

        // WEP-40, WEP-104, and some vendors using 256-bit WEP (WEP-232?)
        if (len != 10 && len != 26 && len != 58) {
            return false;
        }

        return isHex(wepKey);
    }

    private static boolean isHex(String key) {
        for (int i = key.length() - 1; i >= 0; i--) {
            final char c = key.charAt(i);
            if (!(c >= '0' && c <= '9' || c >= 'A' && c <= 'F' || c >= 'a'
                    && c <= 'f')) {
                return false;
            }
        }

        return true;
    }


    /**
     * 获取wifi ssid的加密方式
     *
     * @param ssid
     * @return
     */
    private WifiType getCipherType(String ssid) {
        // WifiManager wifiManager = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        List<ScanResult> list = wifiManager.getScanResults();
        for (ScanResult scResult : list) {
            if (!TextUtils.isEmpty(scResult.SSID) && scResult.SSID.equals(ssid)) {
                String capabilities = scResult.capabilities;
                if (!TextUtils.isEmpty(capabilities)) {
                    if (capabilities.contains("WPA")
                            || capabilities.contains("wpa")) {
                        return WifiType.WIFICIPHER_WPA;
                    } else if (capabilities.contains("WEP")
                            || capabilities.contains("wep")) {
                        return WifiType.WIFICIPHER_WEP;
                    } else {
                        return WifiType.WIFICIPHER_NOPASS;
                    }
                }
            }
        }
        return WifiType.WIFICIPHER_INVALID;
    }

    private ConnectCallback connectCallback;

    public void setConnectCallback(ConnectCallback connectCallback) {
        this.connectCallback = connectCallback;
    }

    public interface ConnectCallback {
        void onConnectResult(boolean isConnect);
    }
}
