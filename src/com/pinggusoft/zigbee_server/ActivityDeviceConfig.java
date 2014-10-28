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
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.AdapterView.OnItemClickListener;

import com.pinggusoft.zigbee_server.BTConApp;
import com.pinggusoft.zigbee_server.R;


public class ActivityDeviceConfig extends Activity  implements OnItemClickListener {
    private final static int    SCAN_DONE     = ZigBeeNode.CB_LAST;
  
    private BTConApp      mApp;
    private ProbeeZ20S    mProbee = null;
    private String        mStrLocalAddr = null;
    private String        mStrRemoteAddr = null;
    private ProbeeHandler mProbeeCallback = new ProbeeHandler(this);
//    private Spinner       mSpinnerMode[];
    private Spinner       mSpinnerUsage[];
    private ArrayAdapter<String> mNewDevicesArrayAdapter;
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
        setContentView(R.layout.config_device);
      
        mApp     = (BTConApp)getApplication();
        mProbee  = new ProbeeZ20S(mApp, mProbeeCallback);
//        mSpinnerMode  = new Spinner[ZigBeeNode.GPIO_CNT];
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

//            mSpinnerMode[i] = (Spinner)row.findViewById(R.id.spinnerGpioMode);
            
            mSpinnerUsage[i] = (Spinner)row.findViewById(R.id.spinnerGpioUsage);
            mSpinnerUsage[i].setAdapter(new SpinnerAdapter(this, R.layout.config_zigbee_spin_row, getResources().getStringArray(R.array.zigbee_gpio_usage)));
            tbl.addView(row);
        }
        tbl.requestLayout();
        
        mNewDevicesArrayAdapter = new ArrayAdapter<String>(this, R.layout.config_bluetooth_device_name);
        ListView newDevicesListView = (ListView) findViewById(R.id.scanned_devices);
        newDevicesListView.setAdapter(mNewDevicesArrayAdapter);
        newDevicesListView.setOnItemClickListener(this);
        findViewById(R.id.button_search).setEnabled(false);
        
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
    }

    @Override
    public synchronized void onPause() {
        super.onPause();
        //saveSettings();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mProbee != null)
            mProbee.stop();
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        LogUtil.e("Activitu result" + requestCode + ", result code" + resultCode);

        String info = null;
        
        if (data != null)
            info = data.getAction();

        LogUtil.e(requestCode + " : " + info);
    }
    
    public void onItemClick(AdapterView<?> av, View v, int arg2, long arg3) {
        String info = ((TextView) v).getText().toString();
        int nPos = info.lastIndexOf("\n");
        mStrRemoteAddr = info.substring(nPos + 1);
        LogUtil.d("SEL:" + mStrRemoteAddr);
        onClickReadNode(null);
    }
    
    
    /*
    ***************************************************************************
    * 
    ***************************************************************************
    */
    public void onClickReadNode(View v) {
        if (mNode == null)
            mNode = new ZigBeeNode(mProbee, mStrRemoteAddr);
        
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

    public void onClickScan(final View v) {
        if (!mProbee.isConnected())
            return;

        mNewDevicesArrayAdapter.clear();
        v.setEnabled(false);
        findViewById(R.id.progressBarSearch).setVisibility(View.VISIBLE);
        
        new Thread() {
            @Override
            public void run() {
                String str = null;
                
                str = mProbee.writeATCmd(ProbeeZ20S.CMD_AT, 500);
                str = mProbee.writeATCmd(ProbeeZ20S.CMD_AT, 500);
                
                if (str == null) {    /// data mode
                    for (int i = 0; i < 5; i++) {
                        str = mProbee.writeATCmd(ProbeeZ20S.CMD_ESCAPE_DATA, 1000);
                        if (str != null)
                            break;
                    }
                }

                str = mProbee.writeATCmd(ProbeeZ20S.CMD_GET_ECHO_MODE, 0, 1, 500);
                LogUtil.e("echo=" + str);
                if (!str.startsWith("0")) {
                    str = mProbee.writeATCmd(String.format(ProbeeZ20S.CMD_SET_ECHO_MODE, "0"), 500);
                    str = mProbee.writeATCmd(ProbeeZ20S.CMD_RESET, 500);
                }

                mStrLocalAddr = mProbee.writeATCmd(ProbeeZ20S.CMD_GET_NODE_ADDR, 0, 16, 500);
                
                str = mProbee.writeATCmd(String.format(ProbeeZ20S.CMD_SET_JOIN_TIME, 10), 500);
                str = mProbee.writeATCmd(ProbeeZ20S.CMD_SCAN, 5000);
                mProbeeCallback.obtainMessage(SCAN_DONE, 0, 0, str).sendToTarget();
            }
        }.start();

        return;
    }
    
    private void addDevice(String str) {
        if (str == null)
            return;
        
        TextUtils.SimpleStringSplitter splitter = new TextUtils.SimpleStringSplitter('\n');
        splitter.setString(str);
        while (splitter.hasNext()) {
            String field = splitter.next();
            if (field.startsWith("ZC*") || field.startsWith("ZED")) {
                String strAddr = field.substring(5, 21);
                String strName = field.substring(41);
                LogUtil.i(strName + " - " + strAddr);

                if (!strAddr.equals(mStrLocalAddr)) {
                    mNewDevicesArrayAdapter.add(strName + "\n" + strAddr);
                }
            }
        }
    }
    
    static class ProbeeHandler extends Handler {
        private WeakReference<ActivityDeviceConfig> mParent;
        
        ProbeeHandler(ActivityDeviceConfig activityOption) {
            mParent = new WeakReference<ActivityDeviceConfig>(activityOption);
        }

        @Override
        public void handleMessage(Message msg) {
            final ActivityDeviceConfig parent = mParent.get();
            
            switch (msg.what) {
            case ProbeeZ20S.BT_CON:
                LogUtil.e("CONNECTED !!!");
                parent.onClickScan(parent.findViewById(R.id.button_search));
                break;

            case ZigBeeNode.CB_READ_DONE:
                ZigBeeNode info = parent.mNode;

                ((TextView)parent.findViewById(R.id.editNodeAddr)).setText(info.getAddr());
                ((TextView)parent.findViewById(R.id.editNodeName)).setText(info.getName());
                ((Spinner)parent.findViewById(R.id.spinnerNodeType)).setSelection(info.getType());
                for (int i = 0; i < ZigBeeNode.GPIO_CNT; i++) {
                    parent.mSpinnerUsage[i].setSelection(info.getGpioMode(i));
                }
                break;

            case SCAN_DONE:
                parent.findViewById(R.id.button_search).setEnabled(true);
                parent.findViewById(R.id.progressBarSearch).setVisibility(View.INVISIBLE);
                parent.addDevice((String)msg.obj);
                break;
            }
        }
    }
}
