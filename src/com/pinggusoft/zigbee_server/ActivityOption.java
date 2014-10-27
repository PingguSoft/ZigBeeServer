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
import android.widget.SeekBar;
import android.widget.TextView;
import com.pinggusoft.zigbee_server.BTConApp;
import com.pinggusoft.zigbee_server.R;
import com.pinggusoft.zigbee_server.ActivityZigBeeConfig.ProbeeHandler;


public class ActivityOption extends Activity {
    private final static int    GET_NODE_INFO = 2;
    private final static int    SCAN_DONE     = 3;

    
    final static int GPIO_CNT = 17;
    final static int mPinNo[] = new int[] { 3, 4, 5, 6, 7, 8, 9, 10, 11, 32, 31, 30, 29, 28, 27, 24, 23 };
    
    private BTConApp      mApp;
    private ProbeeZ20S    mProbee = null;
    private String        mStrLocalAddr = null;
    private String        mStrNodeName = null;
    private String        mStrNodeType = null;
    private ProbeeHandler mProbeeCallback = new ProbeeHandler(this);
    private TextView      mTextBTAddr;
    
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.config_option);
      
        mApp    = (BTConApp)getApplication();
        mProbee = new ProbeeZ20S(mApp, mProbeeCallback);
        mTextBTAddr = (TextView) findViewById(R.id.textViewBTAddr);

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
        
        if (mApp.m_strBTDevice != null && mApp.m_strBTDevice.length() >= 17) {
            String strBTMac = mApp.m_strBTDevice.substring(mApp.m_strBTDevice.length() - 17);
            BluetoothDevice device =  BluetoothAdapter.getDefaultAdapter().getRemoteDevice(strBTMac);
            mProbee.connect(device);
        }
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
        
        mTextBTAddr.setText(mApp.m_strBTDevice);
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
            if (mTextBTAddr != null && info != null) {
                mTextBTAddr.setText(info);
                mApp.m_strBTDevice = info;
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
    
    public String writeATCmd(String strCmd, int nStart, int nEnd, int timeout) {
        String str = null;
        try {
            str = mProbee.writeATCmd(strCmd, timeout);
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        
        if (str != null) {
            int pos = str.lastIndexOf("\r");
            if (pos >= 0) {
                if (nStart == -1)
                    nStart = 0;
                if (nEnd == -1 || nEnd > pos)
                    nEnd = pos;
                str = str.substring(nStart, nEnd);
            }
        }
        return str;
    }
    
    public String writeATCmd(String strCmd, int timeout) {
        String str = null;
        try {
            str = mProbee.writeATCmd(strCmd, timeout);
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        
        if (str != null) {
            int pos = str.lastIndexOf("\r");
            if (pos >= 0) {
                str = str.substring(0, pos);
            }
        }
        return str;
    }
    
    public void onClickReadNode(View v) {
        if (!mProbee.isConnected())
            return;
        
        new Thread() {
            @Override
            public void run() {
                String str = null;
                str = writeATCmd(ProbeeZ20S.CMD_AT, 300);
                str = writeATCmd(ProbeeZ20S.CMD_AT, 300);
                
                if (str == null) {    /// data mode
                    for (int i = 0; i < 5; i++) {
                        str = writeATCmd(ProbeeZ20S.CMD_ESCAPE_DATA, 1000);
                        if (str != null && str.contains(ProbeeZ20S.RESP_OK))
                            break;
                    }
                }

                str = writeATCmd(ProbeeZ20S.CMD_GET_ECHO_MODE, 0, 1, 300);
                LogUtil.e("echo=" + str);
                if (str.startsWith("1")) {
                    str = writeATCmd(ProbeeZ20S.CMD_SET_ECHO_OFF, 300);
                    str = writeATCmd(ProbeeZ20S.CMD_RESET, 300);
                }
                mStrLocalAddr = writeATCmd(ProbeeZ20S.CMD_GET_NODE_ADDR, 0, 16, 300);
                mStrNodeName  = writeATCmd(ProbeeZ20S.CMD_GET_NODE_NAME, 300);
                mStrNodeType  = writeATCmd(ProbeeZ20S.CMD_GET_NODE_TYPE, 300);
                
                mProbeeCallback.obtainMessage(GET_NODE_INFO, 0, 0, null).sendToTarget();
            }
        }.start();
    }
    
    public void onClickWriteNode(View v) {
        Intent intent = new Intent(this, ActivityZigBeeConfig.class);
        startActivityForResult(intent, GET_ZIGBEE_ADDR);
    }
    

    public void saveSettings() {
        mApp.saveSettings();
    }

    
    static class ProbeeHandler extends Handler {
        private WeakReference<ActivityOption>    mParent;
        
        ProbeeHandler(ActivityOption activityOption) {
            mParent = new WeakReference<ActivityOption>(activityOption);
        }

        @Override
        public void handleMessage(Message msg) {
            final ActivityOption parent = mParent.get();
            
            switch (msg.what) {
            case ProbeeZ20S.BT_CON:
                LogUtil.e("CONNECTED !!!");
                parent.onClickReadNode(parent.findViewById(R.id.buttonReadNode));
                break;

            case GET_NODE_INFO:
                ((TextView)parent.findViewById(R.id.editNodeAddr)).setText(parent.mStrLocalAddr);
                ((TextView)parent.findViewById(R.id.editNodeName)).setText(parent.mStrNodeName);
                Spinner spin = (Spinner)parent.findViewById(R.id.spinnerNodeType);
                spin.setSelection(Integer.valueOf(parent.mStrNodeType));
                
                break;
                
            case SCAN_DONE:
//                parent.findViewById(R.id.button_search).setEnabled(true);
//                parent.findViewById(R.id.progressBarSearch).setVisibility(View.INVISIBLE);
                //parent.addDevice((String)msg.obj);
                break;
            }
        }
    }
}
