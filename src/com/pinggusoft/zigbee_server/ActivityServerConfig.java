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

public class ActivityServerConfig extends Activity {
    private ServerApp           mApp;
    private TextView            mTextBTAddr;
    private ZigBeeNode          mNode = null;
    private CommonUtils         mCommon = null;
    private ServerServiceUtil   mService = null;
    
    /*
    ***************************************************************************
    * 
    ***************************************************************************
    */

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.config_server);
        
        mApp        = (ServerApp)getApplication();
        mApp.load();
        
        mTextBTAddr = (TextView) findViewById(R.id.textViewBTAddr);
        mService = new ServerServiceUtil(getApplicationContext(), new Messenger(new ServiceHandler(this)));

        mCommon  = new CommonUtils(this);
        mCommon.createGpioTable();
        findViewById(R.id.buttonReadNode).setEnabled(false);

        
        //findViewById(R.id.buttonWriteNode).setEnabled(false);
        mNode = mApp.getNode(0);
        if (mNode != null) {
            for (int i = 0; i < ZigBeeNode.GPIO_CNT; i++) {
                mCommon.getSpinnerUsages()[i].setSelection(mNode.getGpioUsage(i));
                mCommon.getEditGpioNames()[i].setText(mNode.getGpioName(i));
            }
        }

        
        new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    mService.stopRuleChecking();
                    onClickReadNode(findViewById(R.id.buttonReadNode));
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
        
        if (ServerApp.isAboveICS()) {
            ActionBar bar = getActionBar();
            bar.setBackgroundDrawable(new ColorDrawable(Color.parseColor("#222222")));
            int titleId = getResources().getIdentifier("action_bar_title", "id", "android");
            TextView abTitle = (TextView) findViewById(titleId);
            abTitle.setTextColor(Color.WHITE);
        }
        
        mTextBTAddr.setText(mApp.getBTDevice());
        ((EditText)findViewById(R.id.editServerPort)).setText(String.valueOf(mApp.getServerPort()));
    }
    
    @Override
    public synchronized void onPause() {
        super.onPause();

        int nPort = Integer.valueOf(((EditText)findViewById(R.id.editServerPort)).getText().toString());
        int nOldPort = mApp.getServerPort();
        mApp.setServerPort(nPort);
        if (nPort != nOldPort) {
            mService.asyncChangeServerPort(nPort);
        }
        //mApp.saveSettings();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mService.startRuleChecking();
        mService.unbind();
        mApp.save();
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
                mApp.setBTDevice(info);
                mService.asyncChangeBTAddr(mApp.getBTAddr());
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
            mNode = new ZigBeeNode();
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
        
        byte[] data = mNode.serialize();
        mNode.deserialize(data);
        
        mApp.addNode(mNode, false);
        mService.asyncWriteInfo(0, mNode);
    }
    
    static class ServiceHandler extends Handler {
        private WeakReference<ActivityServerConfig>    mParent;
        
        ServiceHandler(ActivityServerConfig parent) {
            mParent = new WeakReference<ActivityServerConfig>(parent);
        }

        @Override
        public void handleMessage(Message msg) {
            final ActivityServerConfig parent = mParent.get();
            
            switch (msg.what) {
            case ProbeeZ20S.CB_BT_CON:
                LogUtil.d("BT CONNECTED !!!");
                parent.onClickReadNode(parent.findViewById(R.id.buttonReadNode));
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
                
                if (!parent.findViewById(R.id.buttonReadNode).isEnabled())
                    parent.findViewById(R.id.buttonReadNode).setEnabled(true);
                
                if (!parent.findViewById(R.id.buttonWriteNode).isEnabled())
                    parent.findViewById(R.id.buttonWriteNode).setEnabled(true);
                
                break;
                
//            case ZigBeeNode.CB_REPORT_DONE:
//                break;
            }
        }
    }
}
