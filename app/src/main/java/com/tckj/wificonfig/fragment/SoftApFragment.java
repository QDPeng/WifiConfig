package com.tckj.wificonfig.fragment;

import android.app.ProgressDialog;
import android.net.wifi.ScanResult;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import com.tckj.wificonfig.R;
import com.tckj.wificonfig.adapter.ScanResultAdapter;
import com.tckj.wificonfig.util.NetUtils;
import com.tckj.wificonfig.util.WifiConnector;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.util.List;

/**
 * Created by peng on 2017/8/1.
 */

public class SoftApFragment extends Fragment implements View.OnClickListener {
    private EditText ap_SSIDView, ap_PWDView, ssidView, pwdView;
    private View apLayout, sendLayout;

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_softap, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        apLayout = view.findViewById(R.id.connect_layout);
        sendLayout = view.findViewById(R.id.send_layout);
        ap_SSIDView = view.findViewById(R.id.ap_ssid);
        ap_PWDView = view.findViewById(R.id.ap_pwd);
        ssidView = view.findViewById(R.id.ssid);
        pwdView = view.findViewById(R.id.pwd);

        view.findViewById(R.id.switch_net).setOnClickListener(this);
        view.findViewById(R.id.ap_connect).setOnClickListener(this);
        view.findViewById(R.id.connect).setOnClickListener(this);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.switch_net:
                showDialog();
                break;
            case R.id.ap_connect:
                String ap_ssid = ap_SSIDView.getText().toString().trim();
                String ap_pwd = ap_PWDView.getText().toString().trim();
                connectWifi(ap_ssid, ap_pwd);
                break;
            case R.id.connect:
                String ssid = ssidView.getText().toString().trim();
                String pwd = pwdView.getText().toString().trim();
                //use tcp send ssid pwd
                showProgressDialog("正在AP连接……");
                new Thread() {
                    @Override
                    public void run() {
                        try {
                            tcpSend();
                        } catch (IOException e) {
                            Log.i("wificonfig", "error:" + e.toString());
                            closeSocket();
                        }
                    }
                }.start();
                break;
        }
    }

    private ProgressDialog progressDialog;

    private void showProgressDialog(String info) {
        if (progressDialog == null) {
            progressDialog = new ProgressDialog(getContext());
            progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        }
        progressDialog.setMessage(info);
        progressDialog.show();
    }


    //wifi scan list
    private List<ScanResult> wifiList;
    private AlertDialog alertDialog;

    private void showDialog() {
        wifiList = NetUtils.getCurrentWifiScanResult(getActivity());
        if (wifiList == null) {
            Toast.makeText(getContext(), "没有wifi网络", Toast.LENGTH_LONG).show();
            return;
        }
        if (alertDialog != null && alertDialog.isShowing()) return;

        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        View view = View.inflate(getContext(), R.layout.dialog_scanresult, null);
        ListView listView = view.findViewById(R.id.scan_list);
        ScanResultAdapter adapter = new ScanResultAdapter(wifiList, getActivity());
        listView.setAdapter(adapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                ap_SSIDView.setText(wifiList.get(i).SSID);
                alertDialog.dismiss();
                Log.i("wificonfig", wifiList.get(i).toString());
            }
        });
        builder.setView(view);
        alertDialog = builder.create();
        alertDialog.show();
    }

    //wifi connect
    private WifiConnector wifiConnector;

    private void connectWifi(String ssid, String pwd) {
        showProgressDialog("正在连接网络……");
        if (wifiConnector == null) {
            wifiConnector = new WifiConnector(getContext());
            wifiConnector.setConnectCallback(new WifiConnector.ConnectCallback() {
                @Override
                public void onConnectResult(final boolean isConnect) {
                    if (isConnect) {
                        while (true) {
                            if (NetUtils.isWifiConnected(getActivity())) {
                                break;
                            }
                            try {
                                Thread.sleep(500);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                    Log.i("wificonfig", "thread name:"+ Thread.currentThread().getName());
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Log.i("wificonfig", "thread name2222:"+ Thread.currentThread().getName());
                            if (progressDialog != null) progressDialog.dismiss();
                            if (isConnect) {
                                Toast.makeText(getContext(), "连接成功", Toast.LENGTH_LONG).show();
                            } else {

                                Toast.makeText(getContext(), "连接失败", Toast.LENGTH_LONG).show();
                            }

                        }
                    });

                }
            });
        }
        wifiConnector.connect(ssid, pwd);
    }

    //tcp send
    private Socket socket;
    private Handler handler = new Handler(Looper.getMainLooper());
    private TimerRunnable timerRunnable;

    private void tcpSend() throws IOException {
        if (socket != null && !socket.isClosed()) {
            socket.close();
        }
        socket = new Socket(InetAddress.getByName("192.168.4.1"), 5000);
        PrintWriter printWriter = new PrintWriter(socket.getOutputStream());
        printWriter.write("hello world");
        printWriter.flush();
        BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        String s;
        if (timerRunnable == null) timerRunnable = new TimerRunnable();

        handler.removeCallbacks(timerRunnable);
        handler.postDelayed(timerRunnable, 20000);
        Log.i("wificonfig", "tcpSend waiting read data========");
        while (socket != null && !socket.isClosed() && (s = in.readLine()) != null) {
            Log.i("wificonfig", "Reveived: " + s);
        }
        Log.i("wificonfig", "tcpSend finish========");
        closeSocket();

    }

    private void closeSocket() {
        if (progressDialog != null) progressDialog.dismiss();
        try {
            if (timerRunnable != null) {
                handler.removeCallbacks(timerRunnable);
            }
            if (socket != null && !socket.isClosed()) {
                socket.close();
                socket = null;
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private class TimerRunnable implements Runnable {
        @Override
        public void run() {
            closeSocket();
        }
    }


}
