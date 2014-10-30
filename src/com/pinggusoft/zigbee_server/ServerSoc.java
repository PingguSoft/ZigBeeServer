package com.pinggusoft.zigbee_server;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.lang.ref.WeakReference;
import java.net.ServerSocket;
import java.net.Socket;

import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.os.Messenger;

import com.pinggusoft.zigbee_server.ActivityServerConfig.ProbeeHandler;

public class ServerSoc {
    public static final int     SERVERPORT = 4444;
    private Context             mCtx;
    private ServerThread        mThread;
   
    private class ServerThread extends Thread {
        private ServerSocket        serverSocket = null;
        private ServerServiceUtil   mService = new ServerServiceUtil(mCtx, new Messenger(new ProbeeHandler(this)));
        private Boolean             mBoolRun = Boolean.valueOf(true);
        
        @Override
        public void run() {
            try {
                LogUtil.d("S : Connecting...");
                serverSocket = new ServerSocket(SERVERPORT);
                
                while (mBoolRun) {
                    Socket client = serverSocket.accept();
                   
                    try {
                        BufferedReader in = new BufferedReader(
                            new InputStreamReader(client.getInputStream()));
                        BufferedWriter out = new BufferedWriter(
                            new OutputStreamWriter(client.getOutputStream()));

                    } catch (Exception e) {
                        LogUtil.e("S : Error");
                        e.printStackTrace();
                    } finally {
                        client.close();
                        LogUtil.d("S : Done");
                    }
                }
                
                serverSocket.close();
            } catch (Exception e) {
                LogUtil.d("S : Error");
                e.printStackTrace();
            }
        }
        
        public void stopThread() {
            mBoolRun = Boolean.valueOf(false);
            mService.unbind();
        }
        
        private class ProbeeHandler extends Handler {
            private WeakReference<ServerThread>    mParent;
            
            ProbeeHandler(ServerThread parent) {
                mParent = new WeakReference<ServerThread>(parent);
            }

            @Override
            public void handleMessage(Message msg) {
                final ServerThread parent = mParent.get();
                
                switch (msg.what) {
                
                }
            }
        }
    }
    
    public ServerSoc(Context ctx) {
        mCtx     = ctx;
        mThread  = new ServerThread();
        mThread.start();
    }
    
    public void stop() {
        mThread.stopThread();
    }
}
