package com.pinggusoft.zigbee_server;


import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Locale;

import com.pinggusoft.billing.util.IabHelper;
import com.pinggusoft.billing.util.IabResult;
import com.pinggusoft.billing.util.Inventory;
import com.pinggusoft.billing.util.Purchase;
import com.pinggusoft.zigbee_server.R;
import com.pinggusoft.zigbee_server.ActivityServerConfig.ServiceHandler;
import com.pinggusoft.httpserver.RPCServer;
import com.pinggusoft.listitem.EntryAdapter;
import com.pinggusoft.listitem.EntryItem;
import com.pinggusoft.listitem.EntrySelItem;
import com.pinggusoft.listitem.Item;
import com.pinggusoft.listitem.SectionItem;

import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Messenger;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.webkit.WebView;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
    
public class ActivityMain extends Activity {
    private static final int ID_SERVER_SETTING   = 0x00;
    private static final int ID_DEVICE_SETTING   = 0x01;
    private static final int ID_RULE_SETTING     = 0x02;
    private static final int ID_TEST             = 0x03;
    private static final int ID_PURCHASE         = 0x05;
    private static final int ID_NOTICE           = 0x06;
    private static final int ID_QUIT             = 0x07;
    
    private static final int RULE_REQUEST_CODE     = 1002;
    
    private ServerApp  app;
    
    // Local Bluetooth adapter
    private BluetoothAdapter    mBluetoothAdapter = null;
    private static boolean      mBluetoothEnabled = false;
    private ArrayList<Item>     items = new ArrayList<Item>();
    private ListView            mListView = null;
    private ServerServiceUtil   mService = null;
   
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        LogUtil.initialize(this);
        
        app =  (ServerApp)getApplication();
        setContentView(R.layout.main_list_view);

        app.load();
        
        mService = new ServerServiceUtil(getApplicationContext(), new Messenger(new ServiceHandler(this)));
        
        mListView = (ListView)findViewById(R.id.listView);
        
        items.add(new SectionItem(getString(R.string.main_btcon_config_section)));
        items.add(new EntryItem(R.drawable.icon_server_48, getString(R.string.main_server_config), 
                getString(R.string.main_server_config_desc), ID_SERVER_SETTING));
        items.add(new EntryItem(R.drawable.icon_remote_device_48, getString(R.string.main_device_config), 
                getString(R.string.main_device_config_desc), ID_DEVICE_SETTING));
        items.add(new EntryItem(R.drawable.icon_rule_48, getString(R.string.main_rule_config), 
                getString(R.string.main_rule_config_desc), ID_RULE_SETTING));

        items.add(new SectionItem(getString(R.string.main_etc_section)));
        items.add(new EntryItem(R.drawable.icon_test_48, "TEST", 
                getString(R.string.main_device_config_desc), ID_TEST));
//        items.add(new EntryItem(R.drawable.icon_multiwii, "CLIENT", 
//                getString(R.string.main_device_config_desc), ID_CLIENT));
        
        items.add(new EntryItem(R.drawable.icon_notice, getString(R.string.main_notice), 
                getString(R.string.main_notice_desc), ID_NOTICE));
        items.add(new EntryItem(R.drawable.icon_quit, getString(R.string.main_quit), 
                getString(R.string.main_quit_desc), ID_QUIT));
        
        EntryAdapter adapter = new EntryAdapter(this, items, R.layout.list_item_entry_main);
        
        mListView.setAdapter(adapter);
        mListView.setDivider( null ); 
        mListView.setDividerHeight(0);
        mListView.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> l, View v, int position, long id) {
                if(items.get(position).getMode() != Item.MODE_SECTION) {
                    EntryAdapter fia = (EntryAdapter) l.getAdapter();
                    EntryItem item = (EntryItem)fia.getItem(position);
                    EntrySelItem it = null;
                    
                    if (item.getMode() == Item.MODE_ITEM_SEL) {
                        it = (EntrySelItem)item;
                    }

                    switch (item.id) {
                    case ID_SERVER_SETTING:
                        onClickServerConfig(null);
                        break;
                        
                    case ID_DEVICE_SETTING:
                        onClickDeviceConfig(null);
                        break;
                        
                    case ID_RULE_SETTING:
                        onClickRuleConfig(null);
                        break;
                        
                    case ID_NOTICE:
                        onClickNotice(null);
                        break;
                        
                    case ID_QUIT:
                        doQuit();
                        break;
                        
                    case ID_TEST:
                        break;
                    }
                }
            }

        });
        
        // Get local Bluetooth adapter
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        // If the adapter is null, then Bluetooth is not supported
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth is not available",
                    Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
    }
    
    
    public void doQuit() {
        final DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which){
                case DialogInterface.BUTTON_POSITIVE:
                    if (mService != null) {
                        mService.stopHTTP();
                        mService.unbind();
                    }
                    finish();
                    break;

                case DialogInterface.BUTTON_NEGATIVE:
                    //No button clicked
                    break;
                }
            }
        };

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(getString(R.string.dlg_quit_msg));
        builder.setPositiveButton(getString(R.string.dlg_yes), dialogClickListener);
        builder.setNegativeButton(getString(R.string.dlg_no), dialogClickListener);
        
        AlertDialog dialog = builder.show();
        TextView v = (TextView)dialog.findViewById(android.R.id.message);
        if(v != null) 
            v.setGravity(Gravity.CENTER);
        dialog.show();
    }
    
    
    @Override
    public void onStart() {
        super.onStart();
        LogUtil.e("onStart");
        
        if (!mBluetoothAdapter.isEnabled()) {
            mBluetoothEnabled = false;
            mBluetoothAdapter.enable();
        } else {
            mBluetoothEnabled = true;
        }
        
        String strVer = app.getInstVer();
        String strPackVer = app.getPackageVer();
        if (strVer == null || !strVer.equals(strPackVer)) {
            app.setInstVer(strPackVer);
            onClickNotice(null);
        }
    }

    @Override
    public synchronized void onResume() {
        super.onResume();
        LogUtil.e("onResume");
    }
    
    @Override
    public synchronized void onPause() {
        super.onPause();
        LogUtil.e("onPause");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        LogUtil.e("onDestroy " + mBluetoothEnabled);
        if (mBluetoothEnabled == false) {
            mBluetoothAdapter.disable();
            LogUtil.e("BT disable !!!!");
        }
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        switch(requestCode) {
            case RULE_REQUEST_CODE:
                if (resultCode == RESULT_OK) {
                    mService.startRuleChecking();
                }
                break;
        }
    }

    public void onClickRuleConfig(View v) {
        Intent intent = new Intent(this, ActivityRuleConfig.class);
        startActivityForResult(intent, RULE_REQUEST_CODE);
    }
    
    public void onClickServerConfig(View v) {
        Intent intent = new Intent(this, ActivityServerConfig.class);
        startActivity(intent);
    }
    
    public void onClickDeviceConfig(View v) {
        Intent intent = new Intent(this, ActivityDeviceConfig.class);
        startActivity(intent);
    }
    
    private Dialog mDialog = null;
    public void onClickNotice(View v) {
        mDialog = new Dialog(this);
        mDialog.setTitle(R.string.main_notice);
        mDialog.setContentView(R.layout.main_notice);
        mDialog.setCancelable(false);

        final WebView view = (WebView)mDialog.findViewById(R.id.webView);
        
        String strURL;
        if (Locale.getDefault().getLanguage().equals("ko"))
            strURL = "file:///android_res/raw/notice_ko.html";
        else
            strURL = "file:///android_res/raw/notice_en.html";
        view.loadUrl(strURL);
        view.setBackgroundColor(0x00000000);
        
        final Button btnOK = (Button)mDialog.findViewById(R.id.buttonOK);
        btnOK.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                mDialog.dismiss();
            }
        });

        mDialog.show();
    }

    void alert(int nResID) {
        String strMsg = getResources().getString(nResID);
        Toast t = Toast.makeText(this, strMsg, Toast.LENGTH_SHORT);
        TextView v = (TextView)t.getView().findViewById(android.R.id.message);
        if(v != null) 
            v.setGravity(Gravity.CENTER);
        t.show();
    }
    
    void alert(String message) {
        Toast t = Toast.makeText(this, message, Toast.LENGTH_SHORT);
        TextView v = (TextView)t.getView().findViewById(android.R.id.message);
        if(v != null) 
            v.setGravity(Gravity.CENTER);
        t.show();
    }
    
    public int findItemPosbyId(int id) {
        int pos = 0;

        for (Item i : items) {
            if (i.getMode() != Item.MODE_SECTION) {
                EntryItem ei = (EntryItem)i;
                if (ei.id == id)
                  return pos;  
            }
            pos++;
        }
        
        return -1;
    }
    
    void setEnableItemById(int id, boolean en) {
        int pos = findItemPosbyId(id);
        if (pos > 0) {
            EntryItem ei = (EntryItem)items.get(pos);
            ei.setEnabled(en);
            items.set(pos, ei);
        }
    }
    
    void removeItemById(int id) {
        int pos = findItemPosbyId(id);
        if (pos > 0) {
            items.remove(pos);
        }
    }
    
    boolean mBoolRemoved = false;
    
    void updateButtons(boolean boolPurchased) {
        boolean boolEnable = false;

        if(app.isAuthorized()) {
            alert(R.string.main_authorized);
            boolPurchased = true;
        }
        if (!boolPurchased && app.IsExpired()) {
            alert(R.string.main_free_timeout);
            boolEnable = false;
        } else {
            if (app.getBTDevice() == null || app.getBTDevice().length() == 0) {
                boolEnable = false;
            } else {
                boolEnable = true;
            }
        }
        
        setEnableItemById(ID_DEVICE_SETTING, boolEnable);
        if (mListView != null) {
            EntryAdapter adapter = (EntryAdapter)mListView.getAdapter();
            if (adapter != null)
                adapter.notifyDataSetChanged();
        }
    }
    
    static class ServiceHandler extends Handler {
        private WeakReference<ActivityMain>    mParent;
        
        ServiceHandler(ActivityMain parent) {
            mParent = new WeakReference<ActivityMain>(parent);
        }

        @Override
        public void handleMessage(Message msg) {
            final ActivityMain parent = mParent.get();
            
            switch (msg.what) {
            
            }
        }
    }
    
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode) {
        case KeyEvent.KEYCODE_BACK:
            alert(R.string.toast_quit);
            moveTaskToBack(true);
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }
}
