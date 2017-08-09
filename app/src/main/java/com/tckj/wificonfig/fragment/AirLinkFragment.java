package com.tckj.wificonfig.fragment;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.net.wifi.ScanResult;
import android.net.wifi.SupplicantState;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import com.tckj.wificonfig.MainActivity;
import com.tckj.wificonfig.R;
import com.tckj.wificonfig.adapter.ScanResultAdapter;
import com.tckj.wificonfig.esptouch.EsptouchTask;
import com.tckj.wificonfig.esptouch.IEsptouchListener;
import com.tckj.wificonfig.esptouch.IEsptouchResult;
import com.tckj.wificonfig.esptouch.IEsptouchTask;
import com.tckj.wificonfig.util.NetUtils;
import com.tckj.wificonfig.util.WifiConnector;
import com.tckj.wificonfig.wifi.interfaces.ConnectionResultListener;

import java.util.List;

/**
 * Created by peng on 2017/8/1.
 */

public class AirLinkFragment extends Fragment implements View.OnClickListener {
    private EditText ssidView, pwdView;
    private List<ScanResult> wifiList;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_airlink, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        ssidView = (EditText) view.findViewById(R.id.ssid);
        pwdView = (EditText) view.findViewById(R.id.pwd);
        view.findViewById(R.id.next).setOnClickListener(this);
        view.findViewById(R.id.switch_net).setOnClickListener(this);
        String ssid = NetUtils.getCurentWifiSSID(getContext());
        ssidView.setText(ssid);
    }

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
                ssidView.setText(wifiList.get(i).SSID);
                pwdView.setText(null);
                alertDialog.dismiss();
                Log.i("wificonfig", wifiList.get(i).toString());
            }
        });
        builder.setView(view);
        alertDialog = builder.create();
        alertDialog.show();
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.next:
                String ssid = ssidView.getText().toString().trim();
                String pwd = pwdView.getText().toString().trim();
                if (TextUtils.isEmpty(ssid) || TextUtils.isEmpty(pwd)) {
                    Toast.makeText(getContext(), "请填写ssid，pwd", Toast.LENGTH_LONG).show();
                    return;
                }
                wifiSwitch(ssid, pwd);
//                espTouch(ssid, pwd);
                // connectWifi(ssid, pwd);
                break;
            case R.id.switch_net:
                showDialog();
                break;
        }
    }

    private void espTouch(String ssid, String pwd) {
        String bssid = NetUtils.getWifiConnectedBssid(getActivity());
        new EsptouchAsyncTask().execute(ssid, bssid, pwd, String.valueOf(1));
    }

    private WifiConnector wifiConnector;

    private void connectWifi(String ssid, String pwd) {
        showProgressDialog("正在连接网络……");
        if (wifiConnector == null) {
            wifiConnector = new WifiConnector(getContext());
            wifiConnector.setConnectCallback(new WifiConnector.ConnectCallback() {
                @Override
                public void onConnectResult(final boolean isConnect) {

                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (progressDialog != null) progressDialog.dismiss();
                            if (isConnect) {
                                Toast.makeText(getContext(), "连接成功", Toast.LENGTH_LONG).show();
                                ping();
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

    private ProgressDialog progressDialog;

    private void showProgressDialog(String info) {
        if (progressDialog == null) {
            progressDialog = new ProgressDialog(getContext());
            progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        }
        progressDialog.setMessage(info);
        progressDialog.show();
    }

    private void ping() {
        new Thread() {
            @Override
            public void run() {
                while (true) {
                    if (!NetUtils.isWifiConnected(getContext())) {
                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    } else {
                        break;
                    }
                }

                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        final boolean pingOk = NetUtils.isInternetReachable();
                        Toast.makeText(getContext(), "ping:" + pingOk, Toast.LENGTH_LONG).show();
                        ((MainActivity) getActivity()).showFragment(1);
                    }
                });
            }
        }.start();

    }

    //========================esptouch===========
    private void onEsptoucResultAddedPerform(final IEsptouchResult result) {
        getActivity().runOnUiThread(new Runnable() {

            @Override
            public void run() {
                String text = result.getBssid() + " is connected to the wifi";
                Toast.makeText(getActivity(), text, Toast.LENGTH_LONG).show();
            }

        });
    }

    private IEsptouchListener myListener = new IEsptouchListener() {

        @Override
        public void onEsptouchResultAdded(final IEsptouchResult result) {
            onEsptoucResultAddedPerform(result);
        }
    };
    private ProgressDialog mProgressDialog;
    private IEsptouchTask mEsptouchTask;
    private final Object mLock = new Object();

    private class EsptouchAsyncTask extends AsyncTask<String, Void, List<IEsptouchResult>> {

        // without the lock, if the user tap confirm and cancel quickly enough,
        // the bug will arise. the reason is follows:
        // 0. task is starting created, but not finished
        // 1. the task is cancel for the task hasn't been created, it do nothing
        // 2. task is created
        // 3. Oops, the task should be cancelled, but it is running
        @Override
        protected void onPreExecute() {

            mProgressDialog = new ProgressDialog(getActivity());
            mProgressDialog
                    .setMessage("Esptouch is configuring, please wait for a moment...");
            mProgressDialog.setCanceledOnTouchOutside(false);
            mProgressDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
                @Override
                public void onCancel(DialogInterface dialog) {
                    synchronized (mLock) {

                        if (mEsptouchTask != null) {
                            mEsptouchTask.interrupt();
                        }
                    }
                }
            });
            mProgressDialog.setButton(DialogInterface.BUTTON_POSITIVE,
                    "Waiting...", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                        }
                    });
            mProgressDialog.show();
            mProgressDialog.getButton(DialogInterface.BUTTON_POSITIVE)
                    .setEnabled(false);
        }

        @Override
        protected List<IEsptouchResult> doInBackground(String... params) {
            int taskResultCount = -1;
            synchronized (mLock) {
                // !!!NOTICE
                String apSsid = NetUtils.getWifiConnectedSsidAscii(getContext(), params[0]);
                String apBssid = params[1];
                String apPassword = params[2];
                String taskResultCountStr = params[3];
                taskResultCount = Integer.parseInt(taskResultCountStr);
                mEsptouchTask = new EsptouchTask(apSsid, apBssid, apPassword, true, getContext());
                mEsptouchTask.setEsptouchListener(myListener);
            }
            List<IEsptouchResult> resultList = mEsptouchTask.executeForResults(taskResultCount);
            return resultList;
        }

        @Override
        protected void onPostExecute(List<IEsptouchResult> result) {
            mProgressDialog.getButton(DialogInterface.BUTTON_POSITIVE)
                    .setEnabled(true);
            mProgressDialog.getButton(DialogInterface.BUTTON_POSITIVE).setText(
                    "Confirm");
            IEsptouchResult firstResult = result.get(0);
            // check whether the task is cancelled and no results received
            if (!firstResult.isCancelled()) {
                int count = 0;
                // max results to be displayed, if it is more than maxDisplayCount,
                // just show the count of redundant ones
                final int maxDisplayCount = 5;
                // the task received some results including cancelled while
                // executing before receiving enough results
                if (firstResult.isSuc()) {
                    StringBuilder sb = new StringBuilder();
                    for (IEsptouchResult resultInList : result) {
                        sb.append("Esptouch success, bssid = "
                                + resultInList.getBssid()
                                + ",InetAddress = "
                                + resultInList.getInetAddress()
                                .getHostAddress() + "\n");
                        count++;
                        if (count >= maxDisplayCount) {
                            break;
                        }
                    }
                    if (count < result.size()) {
                        sb.append("\nthere's " + (result.size() - count)
                                + " more result(s) without showing\n");
                    }
                    mProgressDialog.setMessage(sb.toString());
                } else {
                    mProgressDialog.setMessage("Esptouch fail");
                }
            }
        }
    }

    /***========test wifi connector and wifi switch==================***/
    com.tckj.wificonfig.wifi.WifiConnector wifiLinker;

    private void wifiSwitch(String ssid, String pwd) {
        if (wifiLinker == null) {
            wifiLinker = new com.tckj.wificonfig.wifi.WifiConnector(getContext(), ssid, pwd);
            wifiLinker.registerWifiConnectionListener(new ConnectionResultListener() {
                @Override
                public void successfulConnect(String SSID) {
                    Toast.makeText(getContext(), "connect to:" + SSID, Toast.LENGTH_LONG).show();
                }

                @Override
                public void errorConnect(int codeReason) {
                    Toast.makeText(getContext(), "errorConnect:" + codeReason, Toast.LENGTH_LONG).show();
                }

                @Override
                public void onStateChange(SupplicantState supplicantState) {
                    Toast.makeText(getContext(), "onStateChange:" + supplicantState.toString(), Toast.LENGTH_LONG).show();
                }
            });
            wifiLinker.connectToWifi();
        }

    }

    @Override
    public void onDestroy() {
        if (wifiLinker != null) {
            wifiLinker.unregisterWifiConnectionListener();
        }
        super.onDestroy();
    }
}
