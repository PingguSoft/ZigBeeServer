package com.pinggusoft.zigbee_server;

import java.lang.ref.WeakReference;
import android.app.ActionBar;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Messenger;
import android.view.View;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import com.pinggusoft.zigbee_server.ServerApp;
import com.pinggusoft.zigbee_server.R;

public class ActivityRuleConfig extends Activity {
    private ServerApp           mApp;
    private ZigBeeNode          mNode = null;
    
    /*
    ***************************************************************************
    * 
    ***************************************************************************
    */

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.config_rule);
        
        mApp        = (ServerApp)getApplication();
        mApp.load();
        
    }

    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    public synchronized void onResume() {
        super.onResume();
        
        if (ServerApp.isAboveICS()) {
            ActionBar bar = getActionBar();
            bar.setBackgroundDrawable(new ColorDrawable(Color.parseColor("#222222")));
            int titleId = getResources().getIdentifier("action_bar_title", "id", "android");
            TextView abTitle = (TextView) findViewById(titleId);
            abTitle.setTextColor(Color.WHITE);
        }
        
//        ((EditText)findViewById(R.id.editServerPort)).setText(String.valueOf(mApp.getServerPort()));
    }
    
    @Override
    public synchronized void onPause() {
        super.onPause();

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    
    /*
    ***************************************************************************
    * 
    ***************************************************************************
    */

}
