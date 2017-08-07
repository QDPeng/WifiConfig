package com.tckj.wificonfig.fragment;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.tckj.wificonfig.R;
import com.tckj.wificonfig.udp.UdpManager;
import com.tckj.wificonfig.util.InetAddressUtils;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;

/**
 * Created by peng on 2017/8/1.
 */

public class CommunicateFragment extends Fragment implements View.OnClickListener {
    private TextView sendTextView, recvTextView;
    private Button sendBtn, recvBtn;
    private UdpManager udpManager;

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_communicate, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        sendTextView = view.findViewById(R.id.send_text);
        recvTextView = view.findViewById(R.id.receive_text);
        sendTextView.setMovementMethod(ScrollingMovementMethod.getInstance());
        recvTextView.setMovementMethod(ScrollingMovementMethod.getInstance());
        sendBtn = view.findViewById(R.id.send_btn);
        sendBtn.setOnClickListener(this);
        recvBtn = view.findViewById(R.id.receive_btn);
        recvBtn.setOnClickListener(this);
        udpManager = new UdpManager(new UdpManager.UdpCallback() {
            @Override
            public void onReceiveMsg(String msg) {
                recvTextView.append(msg + "\n");
                int offset = recvTextView.getLineCount() * recvTextView.getLineHeight();
                if (offset > recvTextView.getHeight()) {
                    recvTextView.setText(null);
                }
            }

            @Override
            public void onSendMsg(String msg) {
                sendTextView.append(msg + "\n");
                int offset = sendTextView.getLineCount() * sendTextView.getLineHeight();
                if (offset > sendTextView.getHeight()) {
                    sendTextView.setText(null);
                }
            }
        });
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.send_btn:
                if (sendBtn.isSelected()) {
                    udpManager.stopSendBroadcast();
                    sendBtn.setSelected(false);
                    sendBtn.setText("开始发送");
                } else {
                    try {
                        sendBtn.setSelected(true);
                        udpManager.startSendBroadcast(InetAddressUtils.getBroadcastAddress(getContext()));
                        sendBtn.setText("停止发送");
                    } catch (UnknownHostException e) {
                        Log.d("wificonfig", e.toString());
                    }
                }
                new Thread(){
                    @Override
                    public void run() {
                        send();
                    }
                }.start();

                break;
            case R.id.receive_btn:
                if (recvBtn.isSelected()) {
                    udpManager.stopReceiveUdpMsg();
                    recvBtn.setSelected(false);
                    recvBtn.setText("开始接收");
                } else {
                    recvBtn.setSelected(true);
                    udpManager.startReceiveUdpMsg();
                    recvBtn.setText("停止接收");
                }
                break;
        }
    }

    private void send() {
        try {
            Socket socket = new Socket(InetAddress.getByName("192.168.4.1"), 5000);
            PrintWriter write = new PrintWriter(socket.getOutputStream());
            write.write("hello world");
            write.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onDestroy() {
        if (udpManager != null) {
            udpManager.onDestroy();
        }
        super.onDestroy();
    }
}
