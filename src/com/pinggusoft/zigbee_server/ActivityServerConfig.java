package com.pinggusoft.zigbee_server;

import java.lang.ref.WeakReference;
import android.app.ActionBar;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
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
//    private Spinner       mSpinner[];
    private Spinner       mSpinnerUsage[];
    private ZigBeeNode  mNode = null;
    
    /*
    ***************************************************************************
    * 
    ***************************************************************************
    */
    private class SpinnerAdapter extends ArrayAdapter<String>{
        int    nResImages[] = { R.drawable.type_cross_32,
                                R.drawable.type_status_32,
                                R.drawable.type_light_32,
                                R.drawable.type_switch_32, 
                                R.drawable.type_adc_32,
                                R.drawable.type_reserved_32 };
        String strUsages[] = null;
        
        public SpinnerAdapter(Context context, int textViewResourceId, String[] objects) {
            super(context, textViewResourceId, objects);
            strUsages = getResources().getStringArray(R.array.zigbee_gpio_usage);
        }
 
        @Override
        public View getDropDownView(int position, View convertView, ViewGroup parent) {
            return getCustomView(position, convertView, parent);
        }
 
        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            return getCustomView(position, convertView, parent);
        }
 
        public View getCustomView(int position, View convertView, ViewGroup parent) {
 
            LayoutInflater inflater=getLayoutInflater();
            View row=inflater.inflate(R.layout.config_zigbee_spin_row, parent, false);
            TextView label=(TextView)row.findViewById(R.id.textMode);
            label.setText(strUsages[position]);
 
            ImageView icon=(ImageView)row.findViewById(R.id.imageMode);
            icon.setImageResource(nResImages[position]);
 
            return row;
        }
    };
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.config_server);
      
        mApp    = (BTConApp)getApplication();
        mProbee = new ProbeeZ20S(mApp, mProbeeCallback);
        mTextBTAddr = (TextView) findViewById(R.id.textViewBTAddr);
        
//        mSpinner = new Spinner[ZigBeeNode.GPIO_CNT];
        mSpinnerUsage = new Spinner[ZigBeeNode.GPIO_CNT];
        TableLayout tbl = (TableLayout)findViewById(R.id.container_zigbee_gpio);
        for (int i = 0; i < ZigBeeNode.GPIO_CNT; i++) {
            TableRow row = (TableRow)LayoutInflater.from(this).inflate(R.layout.config_zigbee_gpio_row, null);
            TextView tvNo = (TextView)row.findViewById(R.id.text_zigbee_gpio_no);
            if (tvNo != null)
                tvNo.setText(String.format("%d", i));
            
            TextView tvPIN = (TextView)row.findViewById(R.id.text_zigbee_gpio_pin);
            if (tvPIN != null)
                tvPIN.setText(String.format("%d", ZigBeeNode.getPinNo(i)));

//            mSpinner[i] = (Spinner)row.findViewById(R.id.spinnerGpioMode);
            mSpinnerUsage[i] = (Spinner)row.findViewById(R.id.spinnerGpioUsage);
            mSpinnerUsage[i].setAdapter(new SpinnerAdapter(this, R.layout.config_zigbee_spin_row, getResources().getStringArray(R.array.zigbee_gpio_usage)));
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
            int mode = mSpinnerUsage[i].getSelectedItemPosition();
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
                    parent.mSpinnerUsage[i].setSelection(info.getGpioMode(i));
                }
                parent.findViewById(R.id.buttonReadNode).setEnabled(true);
                break;
            }
        }
    }
}
