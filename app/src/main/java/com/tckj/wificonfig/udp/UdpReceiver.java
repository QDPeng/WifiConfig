package com.tckj.wificonfig.udp;

import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;

import com.tckj.wificonfig.util.InetAddressUtils;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketException;


/**
 * Created by peng on 2017/8/1.
 */

public class UdpReceiver extends Thread {
    private static final int BUFFER_LENGTH = 1024;
    private DatagramSocket udpSocket;
    private DatagramPacket receivePacket;
    private byte[] receiveBuffer = new byte[BUFFER_LENGTH];
    private String[] mLocalIPs;
    private boolean isStopped = false;
    private Handler handler = null;

    public UdpReceiver(Handler handler) {
        this.handler = handler;
        init();
        setPriority(8);
    }

    private void init() {
        mLocalIPs = InetAddressUtils.getLocalAllIP();
        try {
            udpSocket = new DatagramSocket(null);
            udpSocket.setReuseAddress(true);
            udpSocket.bind(new InetSocketAddress(18266/*UdpManager.PORT*/));
        } catch (SocketException e) {
            e.printStackTrace();
            if (udpSocket != null) {
                udpSocket.close();
                isStopped = true;
                return;
            }
        }
        receivePacket = new DatagramPacket(receiveBuffer, BUFFER_LENGTH);
        isStopped = false;
    }

    @Override
    public void run() {
        while (!isStopped) {
            try {
                udpSocket.receive(receivePacket);
            } catch (IOException e) {
                e.printStackTrace();
                isStopped = true;
                break;
            }
            if (udpSocket == null || receivePacket == null) break;
            if (receivePacket.getLength() == 0) {
                continue;
            }
            String strReceive = null;
            try {
                strReceive = new String(receiveBuffer, 0, receivePacket.getLength(), "UTF-8");
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
                continue;
            }
            String ip = receivePacket.getAddress().getHostAddress();
            if (!TextUtils.isEmpty(ip)) {
                if (!isLocal(ip)) {//自己会收到自己的广播消息，进行过滤
                    if (!isStopped) {
                        sendHandlerMsg(UdpManager.RECEIVE_MSG, strReceive + ",ip:" + ip);
                    } else {
                        break;
                    }
                }
            } else {
                isStopped = true;
                break;
            }
            if (receivePacket != null)
                receivePacket.setLength(BUFFER_LENGTH);
        }

        release();
    }

    private void sendHandlerMsg(int what, String msg) {
        Message message = Message.obtain();
        message.what = what;
        message.obj = msg;
        if (handler != null) {
            handler.sendMessage(message);
        }
    }

    public void quit() {
        isStopped = true;
        release();
    }

    private synchronized void release() {
        if (udpSocket != null)
            udpSocket.close();
        if (receivePacket != null)
            receivePacket = null;
        handler = null;
    }

    private boolean isLocal(String ip) {
        for (int i = 0; i < mLocalIPs.length; i++) {
            if (ip.equals(mLocalIPs[i]))
                return true;
        }

        return false;
    }
}
