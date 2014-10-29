package com.pinggusoft.zigbee_server;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.text.TextUtils;

public class ZigBeeServerCmd {
    private MessageMan  mMessageManager;
    private ProbeeZ20S  mProbee = null;
    
    public ZigBeeServerCmd(ProbeeZ20S probee) {
        mProbee = probee;
        mMessageManager = new MessageMan();
        new Thread(mMessageManager).start();
    }
    
    private class MessageMan implements Runnable {
        private Handler messageHandler;
        private final BlockingQueue<Message> messageQueue = new LinkedBlockingQueue<Message>();
        private Boolean isMessagePending = Boolean.valueOf(false);

        public int getRemainedMsg() {
            return messageQueue.size();
        }

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
                    Message msg = messageQueue.poll();
                    if (msg == null)
                        return;
                    
                    switch(msg.what) {
                    case MSG_KEY_READ_ANALOG:
                        readAnalog(msg.arg1, (ZigBeeNode)msg.obj);
                        break;
                    }
                }
            });
        }
        
        public boolean offer(final Message msg) {
            final boolean success = messageQueue.offer(msg);
            if (success) {
                consumeAsync();
            }
            return success;
        }
    }
    
    private void readAnalog(int id, ZigBeeNode node) {
        if (!mProbee.isConnected())
            return;
        
        String strCmd = ProbeeZ20S.CMD_GET_AIS_VALUE;

        if (node.isRemote())
            strCmd = new String(String.format(ProbeeZ20S.CMD_REMOTE, node.getAddr(), strCmd));

        String strRes = null;
        do {
            strRes = mProbee.writeATCmd(strCmd, 500);
        } while (strRes == null);
        node.setGpioAnalogs(strRes);

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
            node.setGpioAnalog(i, val);
            i++;
        }
        //mProbee.getCallBack().obtainMessage(CB_READ_AI_DONE, id, 0, ZigBeeNode.this).sendToTarget();
    }
    
    
    public final static int MSG_KEY_READ_ANALOG = 1;
    
    public void asyncReadAnalog(int id, ZigBeeNode node) {
        Message newmsg = Message.obtain(null, MSG_KEY_READ_ANALOG, id, 0, node);
        mMessageManager.offer(newmsg);
    }
}
