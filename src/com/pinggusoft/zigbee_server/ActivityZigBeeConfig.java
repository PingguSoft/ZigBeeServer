package com.pinggusoft.zigbee_server;

import java.lang.ref.WeakReference;
import com.pinggusoft.zigbee_server.R;

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
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import android.widget.ListView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;
    
public class ActivityZigBeeConfig extends Activity implements OnItemClickListener  {
    private final static String TAG = "ActivityZigBeeConfig";
    private final static int    SCAN_DONE = 2;
   
    private ArrayAdapter<String> mNewDevicesArrayAdapter;
    private BTConApp      m_App;
    private ProbeeZ20S    m_Probee = null;
    private String        m_strLocalAddr = null;
    private ProbeeHandler mProbeeCallback = new ProbeeHandler(this);
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.config_zigbee);
        m_App    = (BTConApp)getApplication();
        m_Probee = new ProbeeZ20S(m_App, mProbeeCallback);
        
        mNewDevicesArrayAdapter = new ArrayAdapter<String>(this, R.layout.config_bluetooth_device_name);
        ListView newDevicesListView = (ListView) findViewById(R.id.scanned_devices);
        newDevicesListView.setAdapter(mNewDevicesArrayAdapter);
        newDevicesListView.setOnItemClickListener(this);
        
        String strBTMac = m_App.m_strBTDevice.substring(m_App.m_strBTDevice.length() - 17);
        BluetoothDevice device =  BluetoothAdapter.getDefaultAdapter().getRemoteDevice(strBTMac);
        m_Probee.connect(device);

        findViewById(R.id.button_search).setEnabled(false);
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
        
        LogUtil.e("onPause");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        m_Probee.stop();
        LogUtil.e("onDestroy");
    }

    public void onClickScan(final View v) {
        if (!m_Probee.isConnected())
            return;
        
//        zigbee device : 000195000000735B
//        zigbee coord  : 0001950000007359
        
        mNewDevicesArrayAdapter.clear();
        v.setEnabled(false);
        findViewById(R.id.progressBarSearch).setVisibility(View.VISIBLE);
        
        new Thread() {
            @Override
            public void run() {
                String str = null;
                try {
                    str = m_Probee.writeATCmd("\n\n\n", 100);
                    str = m_Probee.writeATCmd("at\n", 300);
                    
                    if (str == null) {    /// data mode
                        for (int i = 0; i < 5; i++) {
                            str = m_Probee.writeATCmd("+++", 1000);
                            if (str != null && str.contains("OK\r\n"))
                                break;
                        }
                    }

                    str = m_Probee.writeATCmd("at+la\n", 300);
                    if (str != null) {
                    	if (str.length() < 16) {
                    		str = m_Probee.writeATCmd("at+la\n", 300);
                    	}
                        m_strLocalAddr = str.substring(0, 16);
                        LogUtil.e("at+la=" + m_strLocalAddr);
                    }
                    
                    str = m_Probee.writeATCmd("at+ds\n", 5000);
                    mProbeeCallback.obtainMessage(SCAN_DONE, 0, 0, str).sendToTarget();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }.start();

        return;
    }
    
    public void onItemClick(AdapterView<?> av, View v, int arg2, long arg3) {
        String info = ((TextView) v).getText().toString();
        setResult(RESULT_OK, (new Intent()).setAction(info));
        finish();
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
                LogUtil.e(strName + " - " + strAddr);

                if (strAddr.equals(m_strLocalAddr)) {
                    mNewDevicesArrayAdapter.add(strName + " - This device\n" + strAddr);
                } else { 
                    mNewDevicesArrayAdapter.add(strName + "\n" + strAddr);
                }
            }
        }
    }
    
    static class ProbeeHandler extends Handler {
        private WeakReference<ActivityZigBeeConfig>    mParent;
        
        ProbeeHandler(ActivityZigBeeConfig parent) {
            mParent = new WeakReference<ActivityZigBeeConfig>(parent);
        }

        @Override
        public void handleMessage(Message msg) {
            final ActivityZigBeeConfig parent = mParent.get();
            
            switch (msg.what) {
            case ProbeeZ20S.BT_CON:
                LogUtil.e("CONNECTED !!!");
                parent.onClickScan(parent.findViewById(R.id.button_search));
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
