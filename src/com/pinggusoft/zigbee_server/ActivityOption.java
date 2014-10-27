package com.pinggusoft.zigbee_server;

import android.app.ActionBar;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.SeekBar;
import android.widget.TextView;
import com.pinggusoft.zigbee_server.BTConApp;
import com.pinggusoft.zigbee_server.R;


public class ActivityOption extends Activity {
    private final static String TAG = "ActivityBTConSettings";

    private BTConApp app;
    private TextView textBTAddr;
    
    final static int GPIO_CNT = 17;
    final static int mPinNo[] = new int[] { 3, 4, 5, 6, 7, 8, 9, 10, 11, 32, 31, 30, 29, 28, 27, 24, 23 };
    
//    private RadioGroup  radioGroupMode;
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.config_option);
      
        app =  (BTConApp)getApplication();
        textBTAddr       = (TextView) findViewById(R.id.textViewBTAddr);

        updateButtonStatus();
        
        TableLayout tbl = (TableLayout)findViewById(R.id.container_zigbee_gpio);
        for (int i = 0; i < GPIO_CNT; i++) {
            TableRow row = (TableRow)LayoutInflater.from(this).inflate(R.layout.config_zigbee_gpio_row, null);
            TextView tvNo = (TextView)row.findViewById(R.id.text_zigbee_gpio_no);
            if (tvNo != null)
                tvNo.setText(String.format("%d", i));
            
            TextView tvPIN = (TextView)row.findViewById(R.id.text_zigbee_gpio_pin);
            if (tvPIN != null)
                tvPIN.setText(String.format("%d", mPinNo[i]));

            tbl.addView(row);
        }
        tbl.requestLayout();
    }
    
    private void updateButtonStatus() {
        int visible = View.VISIBLE;
    }
    
    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    public synchronized void onResume() {
        super.onResume();
        
        if (BTConApp.isAboveICS()) {
            ActionBar bar = getActionBar();
            bar.setBackgroundDrawable(new ColorDrawable(Color.parseColor("#222222")));
            int titleId = getResources().getIdentifier("action_bar_title", "id", "android");
            TextView abTitle = (TextView) findViewById(titleId);
            abTitle.setTextColor(Color.WHITE);
        }
        
        textBTAddr.setText(app.m_strBTDevice);
    }

    @Override
    public synchronized void onPause() {
        super.onPause();
        saveSettings();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    // Definition of the one requestCode we use for receiving results.
    static final private int GET_BT_ADDR = 0;
    static final private int GET_ZIGBEE_ADDR = 1;

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        LogUtil.e("Activitu result" + requestCode + ", result code" + resultCode);

        String info = null;
        
        if (data != null)
            info = data.getAction();
            
        if (requestCode == GET_BT_ADDR) {
            if (textBTAddr != null && info != null) {
                textBTAddr.setText(info);
                app.m_strBTDevice = info;
                updateButtonStatus();
            }
        }
        LogUtil.e(requestCode + " : " + info);
    }
   
    public void onClickBluetooth(View v) {
        Intent intent = new Intent(this, ActivityBluetoothConfig.class);
        startActivityForResult(intent, GET_BT_ADDR);
    }
    
    public void onClickZigBee(View v) {
        Intent intent = new Intent(this, ActivityZigBeeConfig.class);
        startActivityForResult(intent, GET_ZIGBEE_ADDR);
    }

    public void saveSettings() {
        app.saveSettings();
    }

    public void onStartTrackingTouch(SeekBar seekBar) {
        //mTrackingText.setText(getString(R.string.seekbar_tracking_on));
    }

    public void onStopTrackingTouch(SeekBar seekBar) {
        //mTrackingText.setText(getString(R.string.seekbar_tracking_off));
    }    
}
