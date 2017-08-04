package com.tckj.wificonfig.udp;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import java.net.InetAddress;

/**
 * Created by peng on 2017/8/1.
 */

public class UdpManager {
    public static final int PORT = 6666;//udp 发送端和接收端端口一致
    public static final int RECEIVE_MSG = 100;
    public static final int SEND_MSG = 101;
    private UdpSender udpSender;
    private UdpReceiver udpReceiver;
    private SendRunnable sendRunnable = new SendRunnable();
    private UdpCallback udpCallback;
    private Handler handler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case RECEIVE_MSG:
                    if (udpCallback != null) {
                        udpCallback.onReceiveMsg((String) msg.obj);
                    }
                    break;
                case SEND_MSG:
                    if (udpCallback != null) {
                        udpCallback.onSendMsg((String) msg.obj);
                    }
                    break;
            }
        }
    };

    public UdpManager(UdpCallback udpCallback) {
        this.udpCallback = udpCallback;
    }

    public void startSendBroadcast(InetAddress address) {
        if (udpSender == null || !udpSender.isAlive()) {
            udpSender = new UdpSender(handler, address);
            udpSender.start();
        }
        handler.post(sendRunnable);
    }

    public void stopSendBroadcast() {
        handler.removeCallbacks(sendRunnable);
    }

    public void startReceiveUdpMsg() {
        udpReceiver = new UdpReceiver(handler);
        udpReceiver.start();
    }

    public void stopReceiveUdpMsg() {
        if (udpReceiver != null) {
            udpReceiver.quit();
            udpReceiver = null;
        }
    }

    private class SendRunnable implements Runnable {
        @Override
        public void run() {
            if (udpSender != null && udpSender.isAlive()) {
                udpSender.addSendMsg("hello world，你好啊");
            }
            handler.postDelayed(sendRunnable, 3000);
        }
    }

    public void onDestroy() {
        stopReceiveUdpMsg();
        stopSendBroadcast();
        if (udpSender != null) {
            udpSender.quit();
            udpSender = null;
        }
        handler = null;
    }

    public interface UdpCallback {
        void onReceiveMsg(String msg);

        void onSendMsg(String msg);
    }
}
