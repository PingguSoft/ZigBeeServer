package com.pinggusoft.zigbee_server;

import java.io.FileInputStream;
import java.io.FileOutputStream;
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
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import com.pinggusoft.zigbee_server.ZigBeeServerApp;
import com.pinggusoft.zigbee_server.R;

public class ActivityServerConfig extends Activity {
    private ZigBeeServerApp        mApp;
    private ProbeeHandler   mProbeeCallback = new ProbeeHandler(this);
    private TextView        mTextBTAddr;
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
        setContentView(R.layout.config_server);
      
        mApp        = (ZigBeeServerApp)getApplication();
        mTextBTAddr = (TextView) findViewById(R.id.textViewBTAddr);
        
        mCommon = new CommonUtils(this, mProbeeCallback);
        mApp.updateNode(mCommon.getProbee());
        
        mCommon.createGpioTable();
        findViewById(R.id.buttonReadNode).setEnabled(false);
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
        mCommon.stop();
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
            mNode = new ZigBeeNode(mCommon.getProbee());
        
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
        
        byte[] data = mNode.serialize();
        mNode.deserialize(data);
        
        mApp.addNode(mNode, false);
        mNode.asyncWriteInfo();
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
                
                if (!parent.findViewById(R.id.buttonReadNode).isEnabled())
                    parent.findViewById(R.id.buttonReadNode).setEnabled(true);
                
                if (!parent.findViewById(R.id.buttonWriteNode).isEnabled())
                    parent.findViewById(R.id.buttonWriteNode).setEnabled(true);
                
                break;
            }
        }
    }
}
