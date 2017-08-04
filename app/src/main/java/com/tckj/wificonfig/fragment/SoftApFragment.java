package com.tckj.wificonfig.fragment;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.net.wifi.ScanResult;
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
                String bssid = NetUtils.getWifiConnectedBssid(getActivity());
                if (TextUtils.isEmpty(bssid)) {
                    Toast.makeText(getActivity(), "wifi 未连接", Toast.LENGTH_LONG).show();
                    return;
                }
                new EsptouchAsyncTask().execute(ssid, bssid, pwd, String.valueOf(1));
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

                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {

                            if (isConnect) {
                                if (progressDialog != null)
                                    progressDialog.setMessage("连接成功,开始检查网络……");
                                Toast.makeText(getContext(), "连接成功", Toast.LENGTH_LONG).show();
                                ping();
                            } else {
                                if (progressDialog != null) progressDialog.dismiss();
                                Toast.makeText(getContext(), "连接失败", Toast.LENGTH_LONG).show();
                            }

                        }
                    });

                }
            });
        }
        wifiConnector.connect(ssid, pwd);
    }

    private void ping() {
        new Thread() {
            @Override
            public void run() {
                while (true) {
                    if (!NetUtils.isWifiConnected(getContext())) {
                        try {
                            Thread.sleep(300);
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
                        if (progressDialog != null) progressDialog.dismiss();
                        final boolean pingOk = NetUtils.isInternetReachable();
                        Toast.makeText(getContext(), "ping:" + pingOk, Toast.LENGTH_LONG).show();
                    }
                });
            }
        }.start();

    }

    //esptouch send
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
            mProgressDialog.setMessage("正在配网，请稍后……");
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
                mEsptouchTask = new EsptouchTask(apSsid, apBssid, apPassword, false, getContext());
                mEsptouchTask.setEsptouchListener(myListener);
            }
            return mEsptouchTask.executeForResults(taskResultCount);
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

}
