package com.tckj.wificonfig.udp;

import android.os.Handler;
import android.os.Message;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;

/**
 * Created by peng on 2017/8/1.
 */

public class UdpSender extends Thread {
    private Handler handler;
    private DatagramSocket udpSocket;
    private InetAddress broadcastAddress;
    private final ArrayList<String> msgList = new ArrayList<>();
    private volatile boolean isStopped;

    public UdpSender(Handler handler, InetAddress broadcastAddress) {
        this.broadcastAddress = broadcastAddress;
        this.handler = handler;
        this.setPriority(MAX_PRIORITY);
        init();
    }

    private void init() {
        try {

            udpSocket = new DatagramSocket(null);
            udpSocket.setReuseAddress(true);
            udpSocket.bind(new InetSocketAddress(5000/*UdpManager.PORT*/));
        } catch (SocketException e) {
            e.printStackTrace();
            if (udpSocket != null) {
                udpSocket.close();
                udpSocket = null;
            }
        }
    }

    public void addSendMsg(String msg) {
        this.msgList.add(msg);
        synchronized (msgList) {
            msgList.notify();
        }
    }

    @Override
    public void run() {
        while (!isStopped) {
            while (!msgList.isEmpty()) {
                String msg = msgList.remove(0);

                try {
                    InetAddress address = InetAddress.getByName("192.168.4.1");//广播地址
                    sendUdpData(msg, address);
                } catch (UnknownHostException e) {
                    e.printStackTrace();
                }

            }
            try {
                synchronized (msgList) {
                    msgList.wait();
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

        }
        release();
    }

    private synchronized void sendUdpData(String sendStr, InetAddress sendTo) {
        try {
            byte[] sendBuffer = sendStr.getBytes("UTF-8");
            DatagramPacket sendPacket = new DatagramPacket(sendBuffer, sendBuffer.length, sendTo, UdpManager.PORT);
            if (udpSocket != null) {
                udpSocket.send(sendPacket);
                sendHandlerMsg(UdpManager.SEND_MSG, sendStr + ",to:" + sendTo.getHostAddress());
            }
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
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
        handler = null;
        broadcastAddress = null;
    }
}
