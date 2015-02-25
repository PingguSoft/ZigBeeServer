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
import android.os.Messenger;
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

import com.pinggusoft.zigbee_server.ServerApp;
import com.pinggusoft.zigbee_server.R;


public class ActivityDeviceConfig extends Activity  implements OnItemClickListener {
    private ServerApp       mApp;
    private String          mStrLocalAddr = null;
    private String          mStrRemoteAddr = null;
    private ArrayAdapter<String> mNewDevicesArrayAdapter;
    private ZigBeeNode      mNode = null;
    private CommonUtils     mCommon = null;
    private ServerServiceUtil   mService = null;
    
    /*
     ***************************************************************************
     * 
     ***************************************************************************
     */

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.config_device);
      
        mApp    = (ServerApp)getApplication();
        mApp.load();
        
        mService = new ServerServiceUtil(getApplicationContext(), new Messenger(new ServiceHandler(this)));
        mCommon  = new CommonUtils(this);
        
        mCommon.createGpioTable();
        mNewDevicesArrayAdapter = new ArrayAdapter<String>(this, R.layout.config_bluetooth_device_name);
        ListView newDevicesListView = (ListView) findViewById(R.id.scanned_devices);
        newDevicesListView.setAdapter(mNewDevicesArrayAdapter);
        newDevicesListView.setOnItemClickListener(this);
        findViewById(R.id.buttonSearch).setEnabled(false);
        findViewById(R.id.buttonWriteNode).setEnabled(false);
        
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                mService.stopRuleChecking();
                mService.asyncGetServerAddr(0);
                onClickScan(findViewById(R.id.buttonSearch));
            }
        }, 500);
    }
    
    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    public synchronized void onResume() {
        super.onResume();
    }

    @Override
    public synchronized void onPause() {
        super.onPause();
        //saveSettings();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mService.startRuleChecking();
        mService.unbind();
        mApp.save();
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
        
        if (!findViewById(R.id.buttonWriteNode).isEnabled())
            findViewById(R.id.buttonWriteNode).setEnabled(true);
    }
    
    
    /*
    ***************************************************************************
    * 
    ***************************************************************************
    */
    public void onClickReadNode(View v) {
        if (mNode == null)
            mNode = new ZigBeeNode(mStrRemoteAddr);
       
        mService.asyncReadInfo(0, mNode);
    }
    
    public void onClickWriteNode(View v) {
        if (mNode == null)
            return;
        
        mNode.setName(((TextView)findViewById(R.id.editNodeName)).getText().toString());
        mNode.setType(((Spinner)findViewById(R.id.spinnerNodeType)).getSelectedItemPosition());
        for (int i = 0; i < ZigBeeNode.GPIO_CNT; i++) {
            int usage = mCommon.getSpinnerUsages()[i].getSelectedItemPosition();
            mNode.setGpioUsage(i, usage);
            mNode.setGpioMode(i, ZigBeeNode.getGpioModeFromUsage(usage));
            mNode.setGpioName(i, mCommon.getEditGpioNames()[i].getText().toString());
        }
        mApp.addNode(mNode, false);
        mService.asyncWriteInfo(0, mNode);
    }

    public void onClickScan(final View v) {
        mNewDevicesArrayAdapter.clear();
        v.setEnabled(false);
        findViewById(R.id.progressBarSearch).setVisibility(View.VISIBLE);
        mService.asyncScan(0);
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
    
    static class ServiceHandler extends Handler {
        private WeakReference<ActivityDeviceConfig> mParent;
        
        ServiceHandler(ActivityDeviceConfig parent) {
            mParent = new WeakReference<ActivityDeviceConfig>(parent);
        }

        @Override
        public void handleMessage(Message msg) {
            final ActivityDeviceConfig parent = mParent.get();
            
            switch (msg.what) {
            case ProbeeZ20S.CB_BT_CON:
                LogUtil.e("CONNECTED !!!");
                break;

            case ServerService.CMD_READ_INFO:
                ZigBeeNode info = parent.mNode;
                parent.mApp.addNode(info, true);

                ((TextView)parent.findViewById(R.id.editNodeAddr)).setText(info.getAddr());
                ((TextView)parent.findViewById(R.id.editNodeName)).setText(info.getName());
                ((Spinner)parent.findViewById(R.id.spinnerNodeType)).setSelection(info.getType());
                for (int i = 0; i < ZigBeeNode.GPIO_CNT; i++) {
                    parent.mCommon.getSpinnerUsages()[i].setSelection(info.getGpioUsage(i));
                    parent.mCommon.getEditGpioNames()[i].setText(info.getGpioName(i));
                }
                break;

            case ServerService.CMD_GET_SERVER_ADDR:
                parent.mStrLocalAddr = (String)msg.obj;
                break;
                
            case ServerService.CMD_SCAN:
                parent.findViewById(R.id.buttonSearch).setEnabled(true);
                parent.findViewById(R.id.progressBarSearch).setVisibility(View.INVISIBLE);
                parent.addDevice((String)msg.obj);
                break;
            }
        }
    }
}
