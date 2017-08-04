package com.tckj.wificonfig;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.tckj.wificonfig.fragment.AirLinkFragment;
import com.tckj.wificonfig.fragment.CommunicateFragment;
import com.tckj.wificonfig.fragment.SoftApFragment;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        showFragment(2);
    }

    public void showFragment(int index) {
        Fragment fragment = null;
        switch (index) {
            case 0:
                fragment = new AirLinkFragment();
                break;
            case 1:
                fragment = new CommunicateFragment();
                break;
            case 2:
                fragment = new SoftApFragment();
                break;
        }
        if (fragment != null) {
            getSupportFragmentManager().beginTransaction().replace(R.id.main_container, fragment).commit();
        }
    }

    @Override
    public void onClick(View view) {

    }
}
