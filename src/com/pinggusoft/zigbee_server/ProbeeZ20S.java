package com.pinggusoft.zigbee_server;

import java.lang.ref.WeakReference;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import com.pinggusoft.zigbee_server.R;

import android.bluetooth.BluetoothDevice;
import android.os.Handler;
import android.os.Message;
import android.widget.Toast;
import android.text.TextUtils;
import android.util.Log;

public class ProbeeZ20S {
    private static final String TAG = "ProbeeZ20S";
    public static final int IDLE = 0, TAIL_O = 1, TAIL_K = 2, TAIL_CR = 3, TAIL_LF = 4;
    public static final int BT_CON = 1;
    
    private ByteBuffer byteBuf = ByteBuffer.allocate(512);

    private BTConApp        m_App;
    private BTSerialPort    m_BTSerial = null;
    private boolean         m_boolConnected = false;
    private String          m_strConDeviceName = null;
    private Handler         m_hCallback;
    
    private Lock            m_lockAck = new ReentrantLock();
    private Condition       m_condAck = m_lockAck.newCondition();
    private int             m_nCnt = 0;
    private String          m_strAck;
    private BTHandler       mBTHandler = new BTHandler(this);

    ProbeeZ20S(BTConApp app, Handler callback) {
        m_App       = app;
        m_hCallback = callback;
        m_BTSerial  = new BTSerialPort(m_App, mBTHandler);
    }
    
    ProbeeZ20S(BTConApp app, Handler callback, BTSerialPort btSerial) {
        m_App       = app;
        m_hCallback = callback;
        m_BTSerial  = btSerial;
        m_BTSerial.changeHandler(mBTHandler);
    }

    public void pause() {

    }
    
    public void resume() {

    }
    
    public void stop() {
        if (m_BTSerial != null)
            m_BTSerial.stop();     
    }
    
    public void connect(BluetoothDevice device)
    {
        m_BTSerial.connect(device);
    }

    public String writeATCmd(String str, final int time) throws InterruptedException {
        if (!m_boolConnected)
            return null;
        
        String strRet = null;
        
        m_lockAck.lock();
        m_BTSerial.write(str.getBytes());
        try {
            m_nCnt++;
            if (!m_condAck.await(time, TimeUnit.MILLISECONDS)) {
                // time out
                LogUtil.e(str + " => TIMEOUT !!!");
                m_nCnt--;
            } else {
                strRet = m_strAck;
//                LogUtil.e(str + " => " + strRet);
            }
        } finally {
            m_lockAck.unlock();
        }
        return strRet;
    }
    
    private void ack(String str) throws InterruptedException {
        m_lockAck.lock();
        try {
            if (m_nCnt > 0) {
                m_nCnt--;
                m_strAck = str;
                m_condAck.signal();
            }
        } finally {
            m_lockAck.unlock();
        }
    }

    public boolean isConnected() {
        return m_boolConnected;
    }
    
    private int nState = IDLE;
    public void processFrame(byte data[], int size) {
        byte   c;

        for (int i = 0; i < size; i++) {
            c = data[i];

            switch (nState) {
            case IDLE:
                nState = (c == '\r') ? TAIL_CR : IDLE;
                byteBuf.put(c);
                break;
                
            case TAIL_CR:
                byteBuf.put(c);
                if (c == '\n'){
                    int len = byteBuf.position();
                    byte[] line = new byte[len];
                    byteBuf.rewind();
                    byteBuf.get(line, 0, len);
                    String v = new String(line);
//                    LogUtil.e("[TJ] RX:"+ BTSerialPort.byteArrayToHex(line, len));

                    TextUtils.SimpleStringSplitter splitter = new TextUtils.SimpleStringSplitter('\n');
                    splitter.setString(v);
                    while (splitter.hasNext()) {
                        String field = splitter.next();
                        if (field.equals("OK\r") || field.equals("ERROR\r")) {
                            try {
                                ack(v);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                            byteBuf.clear();
                        }
                    }
                }
                nState = IDLE;
                break;

            default:
                byteBuf.put(c);
                break;
            }
        }
    }
    
    static class BTHandler extends Handler {
        private WeakReference<ProbeeZ20S>    mProbee;
        
        BTHandler(ProbeeZ20S msp) {
            mProbee = new WeakReference<ProbeeZ20S>(msp);
        }
        
        @Override
        public void handleMessage(Message msg) {
            final ProbeeZ20S parent = mProbee.get();
            
            switch (msg.what) {
            case BTSerialPort.MESSAGE_STATE_CHANGE:

                switch (msg.arg1) {
                case BTSerialPort.STATE_CONNECTED:
                    parent.m_boolConnected = true;
                    if (parent.m_hCallback != null)
                        parent.m_hCallback.obtainMessage(BT_CON, 0, 0, null).sendToTarget();
                    break;

                case BTSerialPort.STATE_CONNECTING:
                    break;
                    
                case BTSerialPort.STATE_LISTEN:
                case BTSerialPort.STATE_NONE:
                    parent.m_boolConnected = false;
                    break;
                }
                break;
                
            case BTSerialPort.MESSAGE_DEVICE_NAME:
                // save the connected device's name
                parent.m_strConDeviceName = msg.getData().getString(BTSerialPort.DEVICE_NAME);
                Toast.makeText(
                        parent.m_App.getApplicationContext(),
                        parent.m_App.getString(R.string.connected_to) + "\n" + parent.m_strConDeviceName,
                        Toast.LENGTH_SHORT).show();
                break;
                
            case BTSerialPort.MESSAGE_TOAST:
                if (msg.getData().getString(BTSerialPort.TOAST).equals("unable connect")) {
                    Toast.makeText(parent.m_App.getApplicationContext(),
                            parent.m_App.getString(R.string.unable_connect) + "\n" + parent.m_App.m_strBTDevice,
                            Toast.LENGTH_SHORT).show();
                } else if (msg.getData().getString(BTSerialPort.TOAST)
                        .equals("connection lost")) {
                    Toast.makeText(parent.m_App.getApplicationContext(),
                            parent.m_App.getString(R.string.connection_lost) + "\n" + parent.m_App.m_strBTDevice,
                            Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(parent.m_App.getApplicationContext(),
                            msg.getData().getString(BTSerialPort.TOAST), Toast.LENGTH_SHORT)
                            .show();
                }
                break;

            case BTSerialPort.MESSAGE_READ:
//                LogUtil.e("[TJ] RX:"+BTSerialService.byteArrayToHex((byte[])msg.obj, msg.arg1));
                parent.processFrame((byte[])msg.obj, msg.arg1);
                break;
            }            
        }
    }
}
