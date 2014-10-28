package com.pinggusoft.zigbee_server;

import java.lang.ref.WeakReference;
import android.app.ActionBar;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Spinner;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import com.pinggusoft.zigbee_server.BTConApp;
import com.pinggusoft.zigbee_server.R;


public class ActivityServerConfig extends Activity {
    private BTConApp      mApp;
    private ProbeeZ20S    mProbee = null;
    private ProbeeHandler mProbeeCallback = new ProbeeHandler(this);
    private TextView      mTextBTAddr;
    private Spinner       mSpinner[];
    private ZigBeeNode  mNode = null;
    
    /*
    ***************************************************************************
    * 
    ***************************************************************************
    */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.config_server);
      
        mApp    = (BTConApp)getApplication();
        mProbee = new ProbeeZ20S(mApp, mProbeeCallback);
        mTextBTAddr = (TextView) findViewById(R.id.textViewBTAddr);
        
        mSpinner = new Spinner[ZigBeeNode.GPIO_CNT];
        TableLayout tbl = (TableLayout)findViewById(R.id.container_zigbee_gpio);
        for (int i = 0; i < ZigBeeNode.GPIO_CNT; i++) {
            TableRow row = (TableRow)LayoutInflater.from(this).inflate(R.layout.config_zigbee_gpio_row, null);
            TextView tvNo = (TextView)row.findViewById(R.id.text_zigbee_gpio_no);
            if (tvNo != null)
                tvNo.setText(String.format("%d", i));
            
            TextView tvPIN = (TextView)row.findViewById(R.id.text_zigbee_gpio_pin);
            if (tvPIN != null)
                tvPIN.setText(String.format("%d", ZigBeeNode.getPinNo(i)));

            mSpinner[i] = (Spinner)row.findViewById(R.id.spinnerGpioMode);
            tbl.addView(row);
        }
        tbl.requestLayout();
        
        findViewById(R.id.buttonReadNode).setEnabled(false);
        if (mApp.m_strBTDevice != null && mApp.m_strBTDevice.length() >= 17) {
            String strBTMac = mApp.m_strBTDevice.substring(mApp.m_strBTDevice.length() - 17);
            BluetoothDevice device =  BluetoothAdapter.getDefaultAdapter().getRemoteDevice(strBTMac);
            mProbee.connect(device);
        }
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
        
        mTextBTAddr.setText(mApp.m_strBTDevice);
    }
    
    @Override
    public synchronized void onPause() {
        super.onPause();
        mApp.saveSettings();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mProbee != null)
            mProbee.stop();
    }

    // Definition of the one requestCode we use for receiving results.
    static final private int GET_BT_ADDR = 0;
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        LogUtil.e("Activitu result" + requestCode + ", result code" + resultCode);

        String info = null;
        
        if (data != null)
            info = data.getAction();
            
        if (requestCode == GET_BT_ADDR) {
            if (mTextBTAddr != null && info != null) {
                mTextBTAddr.setText(info);
                mApp.m_strBTDevice = info;
            }
        }
        LogUtil.e(requestCode + " : " + info);
    }
   
    
    /*
    ***************************************************************************
    * 
    ***************************************************************************
    */
    public void onClickBluetooth(View v) {
        Intent intent = new Intent(this, ActivityBluetoothConfig.class);
        startActivityForResult(intent, GET_BT_ADDR);
    }
    
    public void onClickReadNode(View v) {
        if (mNode == null)
            mNode = new ZigBeeNode(mProbee);
        mNode.readInfo();
    }
    
    public void onClickWriteNode(View v) {
        if (mNode == null)
            return;
        
        mNode.setName(((TextView)findViewById(R.id.editNodeName)).getText().toString());
        mNode.setType(((Spinner)findViewById(R.id.spinnerNodeType)).getSelectedItemPosition());
        for (int i = 0; i < ZigBeeNode.GPIO_CNT; i++) {
            int mode = mSpinner[i].getSelectedItemPosition();
            mNode.setGpioMode(i, mode);
        }
        mNode.writeInfo();
    }
    
    static class ProbeeHandler extends Handler {
        private WeakReference<ActivityServerConfig>    mParent;
        
        ProbeeHandler(ActivityServerConfig activityOption) {
            mParent = new WeakReference<ActivityServerConfig>(activityOption);
        }

        @Override
        public void handleMessage(Message msg) {
            final ActivityServerConfig parent = mParent.get();
            
            switch (msg.what) {
            case ProbeeZ20S.BT_CON:
                LogUtil.e("CONNECTED !!!");
                parent.onClickReadNode(parent.findViewById(R.id.buttonReadNode));
                break;

            case ZigBeeNode.CB_READ_DONE:
                ZigBeeNode info = parent.mNode;

                ((TextView)parent.findViewById(R.id.editNodeAddr)).setText(info.getAddr());
                ((TextView)parent.findViewById(R.id.editNodeName)).setText(info.getName());
                ((Spinner)parent.findViewById(R.id.spinnerNodeType)).setSelection(info.getType());
                for (int i = 0; i < ZigBeeNode.GPIO_CNT; i++) {
                    parent.mSpinner[i].setSelection(info.getGpioMode(i));
                }
                parent.findViewById(R.id.buttonReadNode).setEnabled(true);
                break;
            }
        }
    }
}
