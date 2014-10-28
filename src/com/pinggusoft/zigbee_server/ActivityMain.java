package com.pinggusoft.zigbee_server;


import java.util.ArrayList;
import java.util.Locale;

import com.pinggusoft.billing.util.IabHelper;
import com.pinggusoft.billing.util.IabResult;
import com.pinggusoft.billing.util.Inventory;
import com.pinggusoft.billing.util.Purchase;
import com.pinggusoft.zigbee_server.R;
import com.pinggusoft.listitem.EntryAdapter;
import com.pinggusoft.listitem.EntryItem;
import com.pinggusoft.listitem.EntrySelItem;
import com.pinggusoft.listitem.Item;
import com.pinggusoft.listitem.SectionItem;

import android.app.ActionBar;
import android.app.Activity;
import android.app.Dialog;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.webkit.WebView;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;
    
public class ActivityMain extends Activity {
    private final static String TAG = "ActivityMain";
    private static final int RC_REQUEST = 10001;
    private static final String SKU_PRODUCT = "com.pinggusoft.btcon";
    
    private static final int ID_SERVER_SETTING   = 0x00;
    private static final int ID_DEVICE_SETTING   = 0x01;
    private static final int ID_PURCHASE         = 0x05;
    private static final int ID_NOTICE           = 0x06;
    private static final int ID_QUIT             = 0x07;
    
    private BTConApp  app;
    private boolean   mIsPurchased = true;
    private IabHelper mHelper;
    
    // Local Bluetooth adapter
    private BluetoothAdapter mBluetoothAdapter = null;
    private static boolean mBluetoothEnabled = false;
    private ArrayList<Item> items = new ArrayList<Item>();
    private ListView mListView = null;
    private EntryItem mPurchaseItem = null;
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        app =  (BTConApp)getApplication();
        setContentView(R.layout.main_list_view);
        
        mListView = (ListView)findViewById(R.id.listView);
        
        items.add(new SectionItem(getString(R.string.main_btcon_config_section)));
        items.add(new EntryItem(R.drawable.icon_settings, getString(R.string.main_server_config), 
                getString(R.string.main_server_config_desc), ID_SERVER_SETTING));
        items.add(new EntryItem(R.drawable.icon_multiwii, getString(R.string.main_device_config), 
                getString(R.string.main_device_config_desc), ID_DEVICE_SETTING));
        
        items.add(new SectionItem(getString(R.string.main_etc_section)));
        mPurchaseItem = new EntryItem(R.drawable.icon_purchase, getString(R.string.main_purchase), 
                getString(R.string.main_purchase_desc), ID_PURCHASE); 
        items.add(mPurchaseItem);
        items.add(new EntryItem(R.drawable.icon_notice, getString(R.string.main_notice), 
                getString(R.string.main_notice_desc), ID_NOTICE));
        items.add(new EntryItem(R.drawable.icon_quit, getString(R.string.main_quit), 
                getString(R.string.main_quit_desc), ID_QUIT));
        
        EntryAdapter adapter = new EntryAdapter(this, items, R.layout.list_item_entry_main);
        final Context ctx = this; 
        
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
                        
                    case ID_PURCHASE:
                        onClickPurchase(null);
                        break;
                        
                    case ID_NOTICE:
                        onClickNotice(null);
                        break;
                        
                    case ID_QUIT:
                        onClickQuit(null);
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

        String base64EncodedPublicKey = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAowry+1jfMdRZlz2gDrc3gkzwKtNyuFdwm+Pk2y+IyE2D67m17I4ZAq0zlhkJSU2NrSA6Su/3GPXVv412zIk3vveMVS4SwqTBhDVfJQek9YRWPVNOMJZBVA5j+C1T5ekdippU1I6fG/q1+NmdInE5xdk4K1bBNlHYZL40eZ6X2ejf7zi9RIthTPqM7c+Nl52GInbPRT0nlFgC9HUGDIMegiLtYiWSdlTFTUz5/Re8/ieM3bH6KXF289ZbsExZTJXvM6Io44D5Pf41XeSiVhGktvs8Chk0YZQ/h5S/4G+WpQ7TlgXhmsPQ91RVR49sUKLk1rh44urQbJ5kpptd2OLwOQIDAQAB";
        LogUtil.e("Creating IAB helper.");
        mHelper = new IabHelper(this, base64EncodedPublicKey);

        // enable debug logging (for a production application, you should set this to false).
        mHelper.enableDebugLogging(BuildConfig.DEBUG ? true : false);

        // Start setup. This is asynchronous and the specified listener
        // will be called once setup completes.
        LogUtil.e("Starting setup.");
        mHelper.startSetup(new IabHelper.OnIabSetupFinishedListener() {
            public void onIabSetupFinished(IabResult result) {
                LogUtil.e("Setup finished.");

                if (!result.isSuccess()) {
                    // Oh noes, there was a problem.
                    complain(getResources().getString(R.string.main_inapp_setting_fail) + " " + result);
                    mIsPurchased = false;
                    return;
                }

                // Have we been disposed of in the meantime? If so, quit.
                if (mHelper == null) 
                    return;

                // IAB is fully set up. Now, let's get an inventory of stuff we own.
                LogUtil.e("Setup successful. Querying inventory.");
                mHelper.queryInventoryAsync(mGotInventoryListener);
            }
        });
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
        if (BTConApp.isAboveICS()) {
            ActionBar bar = getActionBar();
            bar.setBackgroundDrawable(new ColorDrawable(Color.parseColor("#222222")));
            int titleId = getResources().getIdentifier("action_bar_title", "id", "android");
            TextView abTitle = (TextView) findViewById(titleId);
            abTitle.setTextColor(Color.WHITE);
        }
        
        updateButtons(mIsPurchased);
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
        
        // very important:
        LogUtil.e("Destroying helper.");
        if (mHelper != null) {
            mHelper.dispose();
            mHelper = null;
        }
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

    public void onClickQuit(View v) {
        finish();
    }    
    
    
    //
    // In-App Billing
    //

    public void onClickPurchase(View v) {
        LogUtil.e("Buy button clicked; launching purchase flow for upgrade.");

        /* TODO: for security, generate your payload here for verification. See the comments on
         *        verifyDeveloperPayload() for more info. Since this is a SAMPLE, we just use
         *        an empty string, but on a production app you should carefully generate this. */
        String payload = String.valueOf(System.currentTimeMillis());

        mHelper.launchPurchaseFlow(this, SKU_PRODUCT, RC_REQUEST,
                mPurchaseFinishedListener, payload);
    }
    
    // Listener that's called when we finish querying the items and subscriptions we own
    IabHelper.QueryInventoryFinishedListener mGotInventoryListener = new IabHelper.QueryInventoryFinishedListener() {
        public void onQueryInventoryFinished(IabResult result, Inventory inventory) {
            LogUtil.e("Query inventory finished.");

            // Have we been disposed of in the meantime? If so, quit.
            if (mHelper == null) return;

            // Is it a failure?
            if (result.isFailure()) {
                complain(getResources().getString(R.string.main_inapp_query_fail) + " : " + result);
                return;
            }

            LogUtil.e("Query inventory was successful.");

            /*
             * Check for items we own. Notice that for each purchase, we check
             * the developer payload to see if it's correct! See
             * verifyDeveloperPayload().
             */
            Purchase premiumPurchase = inventory.getPurchase(SKU_PRODUCT);
            mIsPurchased = (premiumPurchase != null && verifyDeveloperPayload(premiumPurchase));
            LogUtil.e("Product is " + (mIsPurchased ? "Purchased" : "not purchased"));
            updateButtons(mIsPurchased);
        }
    };
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        LogUtil.e("onActivityResult(" + requestCode + "," + resultCode + "," + data);
        if (mHelper == null) return;

        // Pass on the activity result to the helper for handling
        if (!mHelper.handleActivityResult(requestCode, resultCode, data)) {
            // not handled, so handle it ourselves (here's where you'd
            // perform any handling of activity results not related to in-app
            // billing...
            super.onActivityResult(requestCode, resultCode, data);
        }
        else {
            LogUtil.e("onActivityResult handled by IABUtil.");
        }
    }

    /** Verifies the developer payload of a purchase. */
    boolean verifyDeveloperPayload(Purchase p) {
        String payload = p.getDeveloperPayload();

        /*
         * TODO: verify that the developer payload of the purchase is correct. It will be
         * the same one that you sent when initiating the purchase.
         *
         * WARNING: Locally generating a random string when starting a purchase and
         * verifying it here might seem like a good approach, but this will fail in the
         * case where the user purchases an item on one device and then uses your app on
         * a different device, because on the other device you will not have access to the
         * random string you originally generated.
         *
         * So a good developer payload has these characteristics:
         *
         * 1. If two different users purchase an item, the payload is different between them,
         *    so that one user's purchase can't be replayed to another user.
         *
         * 2. The payload must be such that you can verify it even when the app wasn't the
         *    one who initiated the purchase flow (so that items purchased by the user on
         *    one device work on other devices owned by the user).
         *
         * Using your own server to store and verify developer payloads across app
         * installations is recommended.
         */
        LogUtil.e("Payload:" + payload);

        return true;
    }

    // Callback for when a purchase is finished
    IabHelper.OnIabPurchaseFinishedListener mPurchaseFinishedListener = new IabHelper.OnIabPurchaseFinishedListener() {
        public void onIabPurchaseFinished(IabResult result, Purchase purchase) {
            LogUtil.e("Purchase finished: " + result + ", purchase: " + purchase);

            // if we were disposed of in the meantime, quit.
            if (mHelper == null) return;

            if (result.isFailure()) {
                complain(getResources().getString(R.string.main_inapp_fail) + " : " + result);
                return;
            }
            if (!verifyDeveloperPayload(purchase)) {
                complain(getResources().getString(R.string.main_inapp_fail) + " : " + getResources().getString(R.string.main_inapp_auth_fail));
                return;
            }

            LogUtil.e("Purchase successful.");
            if (purchase.getSku().equals(SKU_PRODUCT)) {
                alert(R.string.main_inapp_success);
                mIsPurchased = true;
                updateButtons(mIsPurchased);
            }
        }
    };
    
    void complain(String strMsg) {
        LogUtil.e(strMsg);
        alert(strMsg);
    }
    
    void complain(int nResID) {
        String strMsg = getResources().getString(nResID);
        
        LogUtil.e(strMsg);
        alert(strMsg);
    }

    void alert(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
    
    void alert(int nResID) {
        String strMsg = getResources().getString(nResID);
        Toast.makeText(this, strMsg, Toast.LENGTH_SHORT).show();
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

        if (boolPurchased) {
            removeItemById(ID_PURCHASE);
            mBoolRemoved = true;
        } else if (mBoolRemoved) {
            items.add(8, mPurchaseItem);
            mBoolRemoved = false;
        }

        if (!boolPurchased && app.IsExpired()) {
            alert(R.string.main_free_timeout);
            boolEnable = false;
        } else {
            if (app.m_strBTDevice == null || app.m_strBTDevice.length() == 0) {
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
}
