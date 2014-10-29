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
import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.widget.Toast;
import android.text.TextUtils;
import android.util.Log;

public class ProbeeZ20S {
    public static final String CMD_AT               = "at\n";
    public static final String CMD_RESET            = "atz\n";
    public static final String CMD_GET_NODE_TYPE    = "at+nt?\n";
    public static final String CMD_SET_NODE_TYPE    = "at+nt=%d\n";
    public static final String CMD_GET_NODE_ADDR    = "at+la?\n";
    public static final String CMD_GET_NODE_NAME    = "at+nn?\n";
    public static final String CMD_SET_NODE_NAME    = "at+nn=%s\n";
    public static final String CMD_GET_GPIOS_MODE   = "at+gpio?\n";
    public static final String CMD_SET_GPIOS_MODE   = "at+gpio=%s\n";
    public static final String CMD_GET_GPIOS_VALUE  = "at+dio?\n";
    public static final String CMD_SET_GPIOS_VALUE  = "at+dio=%s\n";
    public static final String CMD_GET_GPIO_VALUE   = "at+dio%d?\n";
    public static final String CMD_SET_GPIO_VALUE   = "at+dio%d=%d\n";
    public static final String CMD_GET_AIS_VALUE    = "at+ai?\n";
    public static final String CMD_GET_AI_VALUE     = "at+ai%d?\n";
    

    public static final String CMD_REMOTE           = "at+rc=%s,%s";
    public static final String CMD_ESCAPE_DATA      = "+++";
    public static final String CMD_SCAN             = "at+ds\n";
    public static final String CMD_GET_ECHO_MODE    = "ats12?\n";
    public static final String CMD_SET_ECHO_MODE    = "ats12=%s\n";
    public static final String CMD_SET_JOIN_TIME    = "at+pj=%d\n";
    public static final String RESP_OK              = "OK\r";
    public static final String RESP_ERROR           = "ERROR\r";
    
    
    public static final int IDLE = 0, TAIL_O = 1, TAIL_K = 2, TAIL_CR = 3, TAIL_LF = 4;
    public static final int BT_CON = 1;
    
    private ByteBuffer byteBuf = ByteBuffer.allocate(512);

    private Context         mContext;
    private BTSerialPort    m_BTSerial = null;
    private boolean         m_boolConnected = false;
    private String          m_strConDeviceName = null;
    private Handler         m_hCallback;
    
    private Lock            m_lockAck = new ReentrantLock();
    private Condition       m_condAck = m_lockAck.newCondition();
    private int             m_nCnt = 0;
    private String          m_strAck;
    private BTHandler       mBTHandler = new BTHandler(this);

    ProbeeZ20S(Context ctx, Handler callback) {
        mContext    = ctx;
        m_hCallback = callback;
        m_BTSerial  = new BTSerialPort(mContext, mBTHandler);
    }
    
    ProbeeZ20S(Context ctx, Handler callback, BTSerialPort btSerial) {
        mContext    = ctx;
        m_hCallback = callback;
        m_BTSerial  = btSerial;
        m_BTSerial.changeHandler(mBTHandler);
    }

    public Handler getCallBack() {
        return m_hCallback;
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

    public String writeATCmd2(String str, final int time) throws InterruptedException {
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
                str = str.replace("\n", " => ");
                LogUtil.d(str + strRet);
            }
        } finally {
            m_lockAck.unlock();
        }
        return strRet;
    }
    
    public String writeATCmd(String strCmd, int nStart, int nEnd, int timeout) {
        String str = null;
        try {
            str = writeATCmd2(strCmd, timeout);
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        
        if (str != null) {
            int pos = str.lastIndexOf("\r");
            if (pos >= 0) {
                if (nStart == -1)
                    nStart = 0;
                if (nEnd == -1 || nEnd > pos)
                    nEnd = pos;
                str = str.substring(nStart, nEnd);
            }
        }
        return str;
    }
    
    public String writeATCmd(String strCmd, int timeout) {
        String str = null;
        try {
            str = writeATCmd2(strCmd, timeout);
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        
        if (str != null) {
            int pos = str.lastIndexOf("\r");
            if (pos >= 0) {
                str = str.substring(0, pos);
            }
        }
        return str;
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
    
    private void nack() throws InterruptedException {
        m_lockAck.lock();
        m_strAck = null;
        try {
            if (m_nCnt > 0) {
                m_nCnt--;
                m_strAck = null;
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
                        if (field.equals(RESP_OK)) {
                            int pos = v.lastIndexOf(RESP_OK);
                            if (pos >= 0) {
                                String strResp = v.substring(0, pos);
                                try {
                                    ack(strResp);
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                                byteBuf.clear();
                            }
                        }
                        else if (field.equals(RESP_ERROR)) {
                            int pos = v.lastIndexOf(RESP_ERROR);
                            if (pos >= 0) {
                                //String strResp = v.substring(0, pos);
                                try {
                                    nack();
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                                byteBuf.clear();
                            }
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
                        parent.mContext.getApplicationContext(),
                        parent.mContext.getString(R.string.connected_to) + "\n" + parent.m_strConDeviceName,
                        Toast.LENGTH_SHORT).show();
                break;
                
            case BTSerialPort.MESSAGE_TOAST:
                if (msg.getData().getString(BTSerialPort.TOAST).equals("unable connect")) {
                    Toast.makeText(parent.mContext.getApplicationContext(),
                            parent.mContext.getString(R.string.unable_connect), // + "\n" + parent.mContext.m_strBTDevice,
                            Toast.LENGTH_SHORT).show();
                } else if (msg.getData().getString(BTSerialPort.TOAST)
                        .equals("connection lost")) {
                    Toast.makeText(parent.mContext.getApplicationContext(),
                            parent.mContext.getString(R.string.connection_lost), // + "\n" + parent.mContext.m_strBTDevice,
                            Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(parent.mContext.getApplicationContext(),
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
    
    public static String byteArrayToHex(byte[] a, int size) {
        StringBuilder sb = new StringBuilder(a.length * 3);
        int c = 0;
        for(byte b: a) {
            if (++c > size)
                   break;
           sb.append(String.format("%02x ", b & 0xff));
        }
        sb.append(" => ");
        c = 0;
        for(byte b: a) {
            if (++c > size)
                   break;
           sb.append(String.format("%c", b & 0xff));
        }
        return sb.toString();
     }
}
