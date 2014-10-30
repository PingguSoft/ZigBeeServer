package com.pinggusoft.zigbee_server;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;

public class ClientSoc {
    private Thread  mThreadRX = null;
    private Boolean mBoolRun = Boolean.valueOf(true);
    private Socket  mSocket;
    private BufferedReader mReader;
    private BufferedWriter mWriter;
    private MessageManager mMessageManager;
    
    /*
     ******************************************************************************************************************
     * RX Thread
     ******************************************************************************************************************
     */
    private Runnable mRunnableRX = new Runnable() {
        @Override
        public void run() {
            LogUtil.d("C : RunnableRX...");

            try {
                InetAddress serverAddr = InetAddress.getByName("127.0.0.1");
                mSocket = new Socket(serverAddr, 4444);

                try {
                    mReader = new BufferedReader(
                        new InputStreamReader(mSocket.getInputStream()));
                    
                    mWriter = new BufferedWriter(
                        new OutputStreamWriter(mSocket.getOutputStream()));
                } catch (Exception e) {
                    LogUtil.e("C : Error");
                }
            } catch (IOException e) {
                LogUtil.e("C : Error");
            }
            
            while (mBoolRun) {
                try {
                    mReader.read();
                } catch (Exception e) {
                    LogUtil.e("C : Error");
                }
            }
        }
    };
    
    /*
     ******************************************************************************************************************
     * MessageManager for TX
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
    
    public ClientSoc() {
        mThreadRX = new Thread(mRunnableRX);
        mThreadRX.start();

        mMessageManager = new MessageManager();
        new Thread(mMessageManager).start();
    }

    public void stop() {
        mBoolRun = Boolean.valueOf(false);
    }
}
