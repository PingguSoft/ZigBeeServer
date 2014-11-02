package com.pinggusoft.zigbee_server;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;

public class ServerServiceUtil {
    private Messenger       mService = null;
    private boolean         mIsBound = false;
    private Context         mCtx;
    private Messenger       mMessenger;
    private Object          mLock = new Object();
    
    public ServerServiceUtil(Context ctx, Messenger messenger) {
        mCtx = ctx;
        mMessenger = messenger;
        bind();
    }

    private ServiceConnection mConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            mService = new Messenger(service);
            LogUtil.i("Attached !!!");
            try {
                Message msg = Message.obtain(null, ServerService.CMD_REGISTER_CLIENT);
                msg.replyTo = mMessenger;
                mService.send(msg);
            } catch (RemoteException e) {
                // In this case the service has crashed before we could even do anything with it
            }
            synchronized (mLock) {
                mLock.notifyAll();
            }
        }

        public void onServiceDisconnected(ComponentName className) {
            // This is called when the connection with the service has been unexpectedly disconnected - process crashed.
            mService = null;
            LogUtil.i("Disconnected !!!");
        }
    };
    
    private void bind() {
        mCtx.bindService(new Intent(mCtx, ServerService.class), mConnection, Context.BIND_AUTO_CREATE);
        mIsBound = true;
        LogUtil.i("Binding !!!");
    }
    
    public void unbind() {
        if (mIsBound) {
            if (mService != null) {
                try {
                    Message msg = Message.obtain(null, ServerService.CMD_UNREGISTER_CLIENT);
                    msg.replyTo = mMessenger;
                    mService.send(msg);
                } catch (RemoteException e) {
                }
            }
            mCtx.unbindService(mConnection);
            mIsBound = false;
            LogUtil.i("Unbinding !!!");
        }
    }
    
    public void sendMessageToService(int what, int arg1, int arg2, Object obj) {
        if (mIsBound) {
            if (mService == null) {
                synchronized (mLock) {
                    try {
                        mLock.wait();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
            
            if (mService != null) {
                try {
                    Message msg = Message.obtain(null, what, arg1, arg2, obj);
                    msg.replyTo = mMessenger;
                    mService.send(msg);
                } catch (RemoteException e) {
                }
            } else {
                LogUtil.e("service is not attached !!!");
            }
        } else {
            LogUtil.e("not bound !!!");
        }
    }
    
    
    /*
     ******************************************************************************************************************
     * Server Service Utils 
     ******************************************************************************************************************
     */
    public void asyncChangeBTAddr(String addr) {
        sendMessageToService(ServerService.CMD_BT_DEVICE_CHANGED, 0, 0, addr);
    }
    
    public void asyncChangeServerPort(int port) {
        sendMessageToService(ServerService.CMD_SERVER_PORT_CHANGED, port, 0, null);
    }    
    
    public void asyncReadInfo(int id, ZigBeeNode node) {
        sendMessageToService(ServerService.CMD_READ_INFO, id, 0, node);
    }
    
    public void asyncWriteInfo(int id, ZigBeeNode node) {
        sendMessageToService(ServerService.CMD_WRITE_INFO, id, 0, node);
    }
    
    public void asyncReadGpio(int id, ZigBeeNode node) {
        sendMessageToService(ServerService.CMD_READ_GPIO, id, 0, node);
    }
    
    public void asyncWriteGpio(int id, int value, ZigBeeNode node) {
        sendMessageToService(ServerService.CMD_WRITE_GPIO, id, value, node);
    }
    
    public void asyncReadAnalog(int id, ZigBeeNode node) {
        sendMessageToService(ServerService.CMD_READ_ANALOG, id, 0, node);
    }
    
    public void asyncScan(int id) {
        sendMessageToService(ServerService.CMD_SCAN, id, 0, null);
    }
    
    public void asyncGetServerAddr(int id) {
        sendMessageToService(ServerService.CMD_GET_SERVER_ADDR, id, 0, null);
    }
}
