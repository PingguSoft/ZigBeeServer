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

import com.pinggusoft.zigbee_server.ZigBeeServerApp;
import com.pinggusoft.zigbee_server.R;


public class ActivityDeviceConfig extends Activity  implements OnItemClickListener {
    private final static int    SCAN_DONE     = ZigBeeNode.CB_LAST;
  
    private ZigBeeServerApp  mApp;
    private String          mStrLocalAddr = null;
    private String          mStrRemoteAddr = null;
    private ProbeeHandler   mProbeeCallback = new ProbeeHandler(this);
    private ArrayAdapter<String> mNewDevicesArrayAdapter;
    private ZigBeeNode      mNode = null;
    private CommonUtils     mCommon = null;
    
    /*
     ***************************************************************************
     * 
     ***************************************************************************
     */

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.config_device);
      
        mApp    = (ZigBeeServerApp)getApplication();
        mCommon = new CommonUtils(this, mProbeeCallback);
       
        mApp.updateNode(mCommon.getProbee());
        
        mCommon.createGpioTable();
        mNewDevicesArrayAdapter = new ArrayAdapter<String>(this, R.layout.config_bluetooth_device_name);
        ListView newDevicesListView = (ListView) findViewById(R.id.scanned_devices);
        newDevicesListView.setAdapter(mNewDevicesArrayAdapter);
        newDevicesListView.setOnItemClickListener(this);
        findViewById(R.id.buttonSearch).setEnabled(false);
        findViewById(R.id.buttonWriteNode).setEnabled(false);
        
        mCommon.connect();
    }
    
    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    public synchronized void onResume() {
        super.onResume();
        
        if (ZigBeeServerApp.isAboveICS()) {
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
        mCommon.stop();
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
            mNode = new ZigBeeNode(mCommon.getProbee(), mStrRemoteAddr);
       
        mNode.asyncReadInfo();
    }
    
    public void onClickWriteNode(View v) {
        if (mNode == null)
            return;
        
        mNode.setName(((TextView)findViewById(R.id.editNodeName)).getText().toString());
        mNode.setType(((Spinner)findViewById(R.id.spinnerNodeType)).getSelectedItemPosition());
        for (int i = 0; i < ZigBeeNode.GPIO_CNT; i++) {
            int mode = mCommon.getSpinnerUsages()[i].getSelectedItemPosition();
            mNode.setGpioMode(i, mode);
            mNode.setGpioName(i, mCommon.getEditGpioNames()[i].getText().toString());
        }
        mApp.addNode(mNode, false);
        mNode.asyncWriteInfo();
    }

    public void onClickScan(final View v) {
        if (!mCommon.getProbee().isConnected())
            return;

        mNewDevicesArrayAdapter.clear();
        v.setEnabled(false);
        findViewById(R.id.progressBarSearch).setVisibility(View.VISIBLE);
        
        new Thread() {
            @Override
            public void run() {
                String str = null;
                
                str = mCommon.getProbee().writeATCmd(ProbeeZ20S.CMD_AT, 500);
                str = mCommon.getProbee().writeATCmd(ProbeeZ20S.CMD_AT, 500);
                
                if (str == null) {    /// data mode
                    for (int i = 0; i < 5; i++) {
                        str = mCommon.getProbee().writeATCmd(ProbeeZ20S.CMD_ESCAPE_DATA, 1000);
                        if (str != null)
                            break;
                    }
                }

                str = mCommon.getProbee().writeATCmd(ProbeeZ20S.CMD_GET_ECHO_MODE, 0, 1, 500);
                LogUtil.e("echo=" + str);
                if (!str.startsWith("0")) {
                    str = mCommon.getProbee().writeATCmd(String.format(ProbeeZ20S.CMD_SET_ECHO_MODE, "0"), 500);
                    str = mCommon.getProbee().writeATCmd(ProbeeZ20S.CMD_RESET, 500);
                }

                mStrLocalAddr = mCommon.getProbee().writeATCmd(ProbeeZ20S.CMD_GET_NODE_ADDR, 0, 16, 500);
                
                str = mCommon.getProbee().writeATCmd(String.format(ProbeeZ20S.CMD_SET_JOIN_TIME, 10), 500);
                str = mCommon.getProbee().writeATCmd(ProbeeZ20S.CMD_SCAN, 5000);
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
                parent.onClickScan(parent.findViewById(R.id.buttonSearch));
                break;

            case ZigBeeNode.CB_READ_INFO_DONE:
                ZigBeeNode info = parent.mNode;
                parent.mApp.addNode(info, true);

                ((TextView)parent.findViewById(R.id.editNodeAddr)).setText(info.getAddr());
                ((TextView)parent.findViewById(R.id.editNodeName)).setText(info.getName());
                ((Spinner)parent.findViewById(R.id.spinnerNodeType)).setSelection(info.getType());
                for (int i = 0; i < ZigBeeNode.GPIO_CNT; i++) {
                    parent.mCommon.getSpinnerUsages()[i].setSelection(info.getGpioMode(i));
                    parent.mCommon.getEditGpioNames()[i].setText(info.getGpioName(i));
                }
                break;

            case SCAN_DONE:
                parent.findViewById(R.id.buttonSearch).setEnabled(true);
                parent.findViewById(R.id.progressBarSearch).setVisibility(View.INVISIBLE);
                parent.addDevice((String)msg.obj);
                break;
            }
        }
    }
}
