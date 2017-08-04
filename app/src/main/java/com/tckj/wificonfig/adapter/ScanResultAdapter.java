package com.tckj.wificonfig.adapter;

import android.content.Context;
import android.graphics.Color;
import android.net.wifi.ScanResult;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.List;

/**
 * Created by peng on 2017/8/1.
 */

public class ScanResultAdapter extends BaseAdapter {
    private List<ScanResult> wifiList;
    private Context context;

    public ScanResultAdapter(List<ScanResult> wifiList, Context context) {
        this.wifiList = wifiList;
        this.context = context;
    }

    @Override
    public int getCount() {
        return wifiList.size();
    }

    @Override
    public Object getItem(int i) {
        return wifiList.get(i);
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {
        TextView textView = new TextView(context);
        textView.setTextColor(Color.parseColor("#333333"));
        textView.setTextSize(16);
        textView.setPadding(30, 20, 20, 20);
        String ssid = wifiList.get(i).SSID;
        textView.setText(ssid);
        return textView;
    }
}
