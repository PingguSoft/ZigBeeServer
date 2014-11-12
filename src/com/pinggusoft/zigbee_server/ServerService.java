package com.pinggusoft.zigbee_server;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import com.pinggusoft.httpserver.HTTPServer;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationCompat.Builder;
import android.text.TextUtils;
import android.widget.Spinner;
import android.widget.TextView;

public class ServerService extends Service {
    public static final String ACTION_ENABLE_SERVICE    = "ENABLE_SERVICE"; 
    public static final String ACTION_DISABLE_SERVICE   = "DISABLE_SERVICE";
    
    private static final int    NOTIFICATION_STARTED_ID = 1;
    
    public static final int     RPT_DIO_CHANGED         = ProbeeZ20S.CB_REPORT;
    public static final int     CMD_EVALUATE            = ProbeeZ20S.CB_END + 0;
    public static final int     CMD_START_HTTP_SERVER   = ProbeeZ20S.CB_END + 10;
    public static final int     CMD_STOP_HTTP_SERVER    = ProbeeZ20S.CB_END + 11;
    public static final int     CMD_BT_DEVICE_CHANGED   = ProbeeZ20S.CB_END + 12;
    public static final int     CMD_SERVER_PORT_CHANGED = ProbeeZ20S.CB_END + 13;
    public static final int     CMD_REGISTER_CLIENT     = ProbeeZ20S.CB_END + 14;
    public static final int     CMD_UNREGISTER_CLIENT   = ProbeeZ20S.CB_END + 15;
    public static final int     CMD_READ_INFO           = ProbeeZ20S.CB_END + 16;
    public static final int     CMD_WRITE_INFO          = ProbeeZ20S.CB_END + 17;
    public static final int     CMD_READ_GPIO           = ProbeeZ20S.CB_END + 18;
    public static final int     CMD_WRITE_GPIO          = ProbeeZ20S.CB_END + 19;
    public static final int     CMD_READ_ANALOG         = ProbeeZ20S.CB_END + 20;
    public static final int     CMD_SCAN                = ProbeeZ20S.CB_END + 21;
    public static final int     CMD_GET_SERVER_ADDR     = ProbeeZ20S.CB_END + 22;
    
    private ArrayList<Messenger>    mClients   = new ArrayList<Messenger>();
    private final Messenger         mMessenger = new Messenger(new IncomingHandler());
    private MessageManager          mMessageManager;
    private ProbeeZ20S              mProbee = null;
    private HTTPServer              mHTTPServer = null;
    private NotificationManager     mNotifyManager = null;

    /*
     ******************************************************************************************************************
     * 
     ******************************************************************************************************************
     */
    @Override
    public void onCreate() {
        super.onCreate();

        LogUtil.d("SERVICE CREATED !!");
        
        mNotifyManager  = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
        mMessageManager = new MessageManager();
        new Thread(mMessageManager).start();

        mProbee = new ProbeeZ20S(getApplicationContext(), new ProbeeHandler(this));
        String strAddr = ((ServerApp)getApplication()).getBTAddr(); 
        if (strAddr != null) {
            BluetoothDevice device =  BluetoothAdapter.getDefaultAdapter().getRemoteDevice(strAddr);
            mProbee.connect(device);
        }
        mHTTPServer = new HTTPServer(getApplicationContext(), mNotifyManager);
        mHTTPServer.startThread();
        
        showNotification();
    }

    private void showNotification() {
        final NotificationCompat.Builder builder = new Builder(this);
        
        builder.setSmallIcon(R.drawable.icon);
        builder.setContentTitle(getString(R.string.app_name));
        builder.setContentText(getString(R.string.main_service_started));

        final Intent notificationIntent = new Intent(this, ActivityMain.class);
        final PendingIntent pi = PendingIntent.getActivity(this, 0, notificationIntent, 0);
        builder.setContentIntent(pi);
        final Notification notification = builder.build();
        
        mNotifyManager.notify(NOTIFICATION_STARTED_ID, notification);
        
        //startForeground(1, notification);
    }
    
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        LogUtil.d("onStartCommand !!");
        showNotification();
        
        return START_STICKY;
    }
    
    @Override
    public void onDestroy() {
        super.onDestroy();

        if (mHTTPServer != null)
            mHTTPServer.stopThread();
        
        if (mProbee != null)
            mProbee.stop();
        
        mNotifyManager.cancel(NOTIFICATION_STARTED_ID);
        mNotifyManager = null;
        
        LogUtil.d("SERVICE DESTROYED !!");
    }

    @Override
    public IBinder onBind(Intent intent) {
        LogUtil.d("onBind : %d", mClients.size());
        return mMessenger.getBinder();
    }
    
    @Override
    public boolean onUnbind(Intent intent) {
        LogUtil.d("onUnbind : %d", mClients.size());
        return true;
    }
    
    
    /*
     ******************************************************************************************************************
     * 
     ******************************************************************************************************************
     */
    private class IncomingHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
            
            case CMD_START_HTTP_SERVER:
                if (mHTTPServer != null)
                    mHTTPServer.startThread();
                LogUtil.d("CMD_START_HTTP_SERVER !!");
                break;
            
            case CMD_STOP_HTTP_SERVER:
                if (mHTTPServer != null)
                    mHTTPServer.stopThread();
                LogUtil.d("CMD_STOP_HTTP_SERVER !!");
                break;
                
            case CMD_REGISTER_CLIENT:
                mClients.add(msg.replyTo);
                LogUtil.d("Register : %d", mClients.size());
                break;
                
            case CMD_UNREGISTER_CLIENT:
                mClients.remove(msg.replyTo);
                LogUtil.d("Unregister : %d", mClients.size());
                break;
                
            case CMD_BT_DEVICE_CHANGED:
                if (mProbee != null)
                    mProbee.stop();
                
                String strAddr = (String)msg.obj; 
                if (strAddr != null) {
                    BluetoothDevice device =  BluetoothAdapter.getDefaultAdapter().getRemoteDevice(strAddr);
                    mProbee.connect(device);
                }
                break;
                
            case CMD_SERVER_PORT_CHANGED:
                if (mHTTPServer != null) {
                    mHTTPServer.stopThread();
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    mHTTPServer = new HTTPServer(getApplicationContext(), mNotifyManager);
                    mHTTPServer.startThread();
                }
                break;

            default:
                mMessageManager.offer(Message.obtain(msg));
                break;
            }
        }
    }
    
    private void sendMessageToClient(int what, int arg1, int arg2, Object obj) {
        for (int i = mClients.size() - 1; i >= 0; i--) {
            try {
                mClients.get(i).send(Message.obtain(null, what, arg1, arg2, obj));
            } catch (RemoteException e) {
                // The client is dead. Remove it from the list; 
                // we are going through the list from back to front so this is safe to do inside the loop.
                mClients.remove(i);
            }
        }
    }
    
    private void sendMessageToClient(Message msg) {
        for (int i = mClients.size() - 1; i >= 0; i--) {
            try {
                mClients.get(i).send(Message.obtain(msg));
            } catch (RemoteException e) {
                // The client is dead. Remove it from the list; 
                // we are going through the list from back to front so this is safe to do inside the loop.
                mClients.remove(i);
            }
        }
    }
    
    
    /*
     ******************************************************************************************************************
     * MessageManager
     ******************************************************************************************************************
     */
    private class MessageManager implements Runnable {
        private Handler messageHandler;
        private final BlockingQueue<Message> messageQueue = new LinkedBlockingQueue<Message>();

        @Override
        public void run() {
            Looper.prepare();
            messageHandler = new Handler() {
                @Override
                public void handleMessage(Message msg) {
                    LogUtil.w("Please post() your blocking runnables to Mr Manager, " +
                            "don't use sendMessage()");
                }
            };
            Looper.loop();
        }
        
        private void consumeAsync() {
            messageHandler.post(new Runnable() {
                @Override
                public void run() {
                    Message msg;
                    
                    do {
                        msg = messageQueue.poll();
                        if (msg == null)
                            break;
                        
                        switch(msg.what) {
                        case CMD_READ_INFO:
                            asyncReadInfo(msg.arg1, (ZigBeeNode)msg.obj);
                            break;

                        case CMD_WRITE_INFO:
                            asyncWriteInfo(msg.arg1, (ZigBeeNode)msg.obj);
                            break;
                            
                        case CMD_READ_GPIO:
                            asyncReadGpio(msg.arg1, (ZigBeeNode)msg.obj);
                            break;
                            
                        case CMD_WRITE_GPIO:
                            asyncWriteGpio(msg.arg1, msg.arg2, (ZigBeeNode)msg.obj);
                            break;
                            
                        case CMD_READ_ANALOG:
                            asyncReadAnalog(msg.arg1, (ZigBeeNode)msg.obj);
                            break;
                            
                        case CMD_SCAN:
                            asyncScan(msg.arg1);
                            break;
                            
                        case CMD_GET_SERVER_ADDR:
                            asyncGetServerAddr(msg.arg1);
                            break;
                        }
                    } while (msg != null);
                }
            });
        }
        
        public boolean offer(final Message msg) {
            final boolean success = messageQueue.offer(msg);
            if (success) {
                consumeAsync();
            } else {
                LogUtil.d("Error offerring !!! ");
            }
            return success;
        }
    }
    
    
    /*
     ******************************************************************************************************************
     * Service Implementation
     ******************************************************************************************************************
     */
    private void asyncScan(int id) {
        if (!mProbee.isConnected())
            return;
        
        String str;

        str = mProbee.writeATCmd(ProbeeZ20S.CMD_AT, 500);
        str = mProbee.writeATCmd(ProbeeZ20S.CMD_AT, 500);

        str = mProbee.writeATCmd(ProbeeZ20S.CMD_GET_ECHO_MODE, 0, 1, 500);
        if (!str.startsWith("0")) {
            str = mProbee.writeATCmd(String.format(ProbeeZ20S.CMD_SET_ECHO_MODE, "0"), 500);
            str = mProbee.writeATCmd(ProbeeZ20S.CMD_RESET, 500);
        }

        str = mProbee.writeATCmd(String.format(ProbeeZ20S.CMD_SET_JOIN_TIME, 10), 500);
        str = mProbee.writeATCmd(ProbeeZ20S.CMD_SCAN, 5000);
        sendMessageToClient(CMD_SCAN, id, 0, str);
    }
    
    private void asyncReadInfo(int id, ZigBeeNode node) {
        if (!mProbee.isConnected())
            return;
        
        String strCmds[] = new String[4];
        strCmds[0] = ProbeeZ20S.CMD_GET_NODE_NAME;
        strCmds[1] = ProbeeZ20S.CMD_GET_NODE_TYPE;
        strCmds[2] = ProbeeZ20S.CMD_GET_GPIOS_MODE;
        strCmds[3] = ProbeeZ20S.CMD_GET_NODE_ADDR;

        if (node.isRemote()) {
            for (int i = 0; i < strCmds.length; i++) {
                strCmds[i] = String.format(ProbeeZ20S.CMD_REMOTE, node.getAddr(), strCmds[i]); 
            }
        }

        node.setName(mProbee.writeATCmd(strCmds[0], 500));
        String str = mProbee.writeATCmd(strCmds[1], 500);
        if (str != null)
            node.setType(Integer.valueOf(str));
        node.setGpioMode(mProbee.writeATCmd(strCmds[2], 500));
        if (!node.isRemote()) {
            node.setAddr(mProbee.writeATCmd(strCmds[3], 0, 16, 500));
        }
        sendMessageToClient(CMD_READ_INFO, id, 0, node);
    }
    
    private void asyncWriteInfo(int id, ZigBeeNode node) {
        if (!mProbee.isConnected())
            return;

        String strCmds[] = new String[7];
        strCmds[0] = String.format(ProbeeZ20S.CMD_SET_NODE_NAME, node.getName());
        strCmds[1] = String.format(ProbeeZ20S.CMD_SET_NODE_TYPE, node.getType());
        strCmds[2] = String.format(ProbeeZ20S.CMD_SET_GPIOS_MODE, node.getGpioMode());
        

    
        String      strCmd = null;
        int         nCtr = 3;
        ZigBeeNode  nodeServer = null;
        
        if (node.isRemote()) {
            nodeServer = ((ServerApp)getApplication()).getLocalNode();
            if (nodeServer != null) {
                strCmds[3] = String.format(ProbeeZ20S.CMD_SET_DEST, nodeServer.getAddr());
                strCmds[4] = String.format(ProbeeZ20S.CMD_SET_ATS43, 0);
                strCmds[5] = String.format(ProbeeZ20S.CMD_SET_ATS44, 1);
                strCmds[6] = ProbeeZ20S.CMD_RESET;
                nCtr = strCmds.length;
            }
        }
        
        for (int i = 0; i < nCtr; i++) {
            if (node.isRemote())
                strCmd = new String(String.format(ProbeeZ20S.CMD_REMOTE, node.getAddr(), strCmds[i]));
            else
                strCmd = strCmds[i];
            
            mProbee.writeATCmd(strCmd, 500);
        }
        // send join command to local node (coordinator)
        mProbee.writeATCmd(String.format(ProbeeZ20S.CMD_SET_JOIN_TIME, 10), 500);
        sendMessageToClient(CMD_WRITE_INFO, id, 0, node);
    }
    
    private void asyncReadGpio(int id, ZigBeeNode node) {
        if (!mProbee.isConnected())
            return;

        String  strCmd = null;
        int     gpio = ZigBeeNode.getGpioFromID(id);
        
        if (gpio > node.getMaxGPIO())
            strCmd = ProbeeZ20S.CMD_GET_GPIOS_VALUE;
        else
            strCmd = String.format(ProbeeZ20S.CMD_GET_GPIO_VALUE, gpio);

        if (node.isRemote())
            strCmd = new String(String.format(ProbeeZ20S.CMD_REMOTE, node.getAddr(), strCmd));
        
        String strRes = mProbee.writeATCmd(strCmd, 500);
        if (strRes != null) {
            if (gpio > node.getMaxGPIO())
                node.setGpioValue(strRes);
            else 
                node.setGpioValue(gpio, Integer.valueOf(strRes));
        }
        sendMessageToClient(CMD_READ_GPIO, id, 0, node);
    }
    
    private void asyncWriteGpio(int id, int value, ZigBeeNode node) {
        if (!mProbee.isConnected())
            return;

        String  strCmd = null;
        int     gpio = ZigBeeNode.getGpioFromID(id);
        
        if (gpio > node.getMaxGPIO())
            strCmd = String.format(ProbeeZ20S.CMD_SET_GPIOS_VALUE, node.getGpioValue());
        else {
            node.setGpioValue(gpio, value);
            strCmd = String.format(ProbeeZ20S.CMD_SET_GPIO_VALUE, gpio, value);
        }

        if (node.isRemote())
            strCmd = new String(String.format(ProbeeZ20S.CMD_REMOTE, node.getAddr(), strCmd));
        
        String strRes = mProbee.writeATCmd(strCmd, 500);
        sendMessageToClient(CMD_WRITE_GPIO, id, 0, node);
    }

    private void asyncReadAnalog(int id, ZigBeeNode node) {
        if (!mProbee.isConnected())
            return;

        String  strCmd = ProbeeZ20S.CMD_GET_AIS_VALUE;
        int     gpio = ZigBeeNode.getGpioFromID(id);

        if (node.isRemote())
            strCmd = new String(String.format(ProbeeZ20S.CMD_REMOTE, node.getAddr(), strCmd));

        String strRes = null;
        strRes = mProbee.writeATCmd(strCmd, 500);
        if (strRes != null) {
            node.setGpioAnalog(strRes);
    
            TextUtils.SimpleStringSplitter splitter = new TextUtils.SimpleStringSplitter(',');
            splitter.setString(strRes);
            int i = 0;
            while (splitter.hasNext()) {
                String strHex = splitter.next();
                
                int val = 0;
                try {
                    val = Integer.parseInt(strHex, 16);
                }  catch(NumberFormatException nfe) {
                    val = 0;
                }
//                LogUtil.d("ANALOG" + i + ":"+ val);
                node.setGpioAnalog(9 + i, val);
                i++;
            }
            sendMessageToClient(CMD_READ_ANALOG, id, 0, node);
        }
    }
    
    private void asyncGetServerAddr(int id) {
        if (!mProbee.isConnected())
            return;

        String str = mProbee.writeATCmd(ProbeeZ20S.CMD_GET_NODE_ADDR, 0, 16, 500);
        sendMessageToClient(CMD_GET_SERVER_ADDR, id, 0, str);
    }
    
    /*
     ******************************************************************************************************************
     * ProbeeHandler 
     ******************************************************************************************************************
     */
    static class ProbeeHandler extends Handler {
        private WeakReference<ServerService>    mParent;
        
        ProbeeHandler(ServerService parent) {
            mParent = new WeakReference<ServerService>(parent);
        }

        @Override
        public void handleMessage(Message msg) {
            final ServerService parent = mParent.get();
            if (msg.what == RPT_DIO_CHANGED) {
                String strMsg  = (String)msg.obj;
                
                int nPos = strMsg.indexOf("|");
                String strAddr = strMsg.substring(2, nPos);
                String strDIO  = strMsg.substring(nPos + 1, nPos + 1 + ZigBeeNode.GPIO_CNT);
                nPos = strMsg.lastIndexOf("|");
                String strAnalog = strMsg.substring(nPos + 1);
                
                LogUtil.d("ADDR:%s, DIO:%s, ANALOG:%s", strAddr, strDIO, strAnalog);
                ServerApp app = (ServerApp)parent.getApplicationContext();
                ZigBeeNode node = app.getNode(strAddr);
                node.setGpioValue(strDIO);
                node.setGpioAnalog(strAnalog);
             // ++000195000000735A|100101000*0000001|1826,****,****,****,****,****
             //
            } else if (msg.what >= CMD_START_HTTP_SERVER) {
                parent.sendMessageToClient(msg);
            }
        }
    }
}
