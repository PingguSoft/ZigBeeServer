package com.pinggusoft.zigbee_server;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;

public class ZigBeeNode {
    public  final static int GPIO_CNT  = 17;
    
    public  final static int CB_REPORT_DONE     = ProbeeZ20S.CB_REPORT;
    public  final static int CB_READ_INFO_DONE  = ProbeeZ20S.CB_END + 0;
    public  final static int CB_WRITE_INFO_DONE = ProbeeZ20S.CB_END + 1;
    public  final static int CB_READ_GPIO_DONE  = ProbeeZ20S.CB_END + 2;
    public  final static int CB_WRITE_GPIO_DONE = ProbeeZ20S.CB_END + 3;
    public  final static int CB_READ_AI_DONE    = ProbeeZ20S.CB_END + 4;
    public  final static int CB_LAST            = ProbeeZ20S.CB_END + 5;
    
    public  final static int GPIO_MODE_DISABLED = 0;
    public  final static int GPIO_MODE_DIN      = 1;
    public  final static int GPIO_MODE_DOUT_LO  = 2;
    public  final static int GPIO_MODE_DOUT_HI  = 3;
    public  final static int GPIO_MODE_AIN      = 4;
    
    private final static int    GPIO_PINS[]         = { 3, 4, 5, 6, 7, 8, 9, 10, 11, 32, 31, 30, 29, 28, 27, 24, 23 };
    private final static String GPIO_MODE_LABEL[]   = { "0 - disabled", "1 - DIN", "2 - DOUT (low)", "3 - DOUT (high)", "4 - Analog In", "5 - Reserved" };
    
    private boolean     mBoolRemote  = false;
    private int         mIntNodeType = 0;
    private String      mStrNodeAddr = null;
    private String      mStrNodeName = null;
    private String      mStrGpioMode = null;
    private String      mStrGpioValue = null;
    private String      mStrAIValue   = null;
    private String      mStrGpioName[] = new String[GPIO_CNT];
    private ProbeeZ20S  mProbee = null;
    private final Lock  mMutex = new ReentrantLock(true);
    private int         mIntAnalog[] = new int[6];
    
    public ZigBeeNode(ProbeeZ20S probee, String addr, boolean remote, int type, String name, String mode) {
        mStrNodeAddr = addr;
        mBoolRemote  = remote;
        mIntNodeType = type;
        mStrNodeName = name;
        mStrGpioMode = mode;
        mProbee      = probee;
    }
    
    public ZigBeeNode(ProbeeZ20S probee, String addr) {
        mStrNodeAddr = addr;
        mBoolRemote  = true;
        mIntNodeType = 0;
        mStrNodeName = null;
        mStrGpioMode = null;
        mProbee      = probee;
    }
    
    public ZigBeeNode(ProbeeZ20S probee) {
        mStrNodeAddr = null;
        mBoolRemote  = false;
        mIntNodeType = 0;
        mStrNodeName = null;
        mStrGpioMode = null;
        mProbee      = probee;
    }
    
    public ZigBeeNode() {
        mStrNodeAddr = null;
        mBoolRemote  = false;
        mIntNodeType = 0;
        mStrNodeName = null;
        mStrGpioMode = null;
        mProbee      = null;        
    }
    
    public void setProbeeHandle(ProbeeZ20S probee) {
        mProbee = probee;
    }

    public static boolean isNumeric(String str)  
    {  
        try {  
            double d = Double.parseDouble(str);  
        } catch(NumberFormatException nfe) {  
            return false;
        }  
        return true;
    }
    
    public boolean isRemote() {
        return mBoolRemote;
    }
    
    public int getGpioMode(int gpio) {
        int mode = 0;
        
        if (gpio >= GPIO_CNT)
            return 0;
        
        String strVal = mStrGpioMode.substring(gpio, gpio + 1);
        if (isNumeric(strVal))
            mode = Integer.valueOf(strVal);
        
        return mode;
    }
    
    public void setGpioMode(int gpio, int mode) {
        if (gpio >= GPIO_CNT)
            return;

        if (mStrGpioMode == null)
            mStrGpioMode = "00000000000000000";
        
        StringBuilder builder = new StringBuilder(mStrGpioMode);
        builder.setCharAt(gpio, (char)('0' + mode));
        mStrGpioMode = builder.toString();
    }
    
    public int getGpioValue(int gpio) {
        int value = 0;

        if (gpio >= GPIO_CNT || mStrGpioValue == null || mStrGpioValue.length() < GPIO_CNT)
            return 0;

        String strVal = mStrGpioValue.substring(gpio, gpio + 1);
        if (isNumeric(strVal))
            value = Integer.valueOf(strVal);

        return value;
    }
    
    public String getGpioValues() {
        return mStrGpioValue;
    }
    
    public void setGpioValue(int gpio, int value) {
        if (gpio >= GPIO_CNT)
            return;

        if (mStrGpioValue == null)
            mStrGpioValue = "00000000000000000";
        
        StringBuilder builder = new StringBuilder(mStrGpioMode);
        builder.setCharAt(gpio, (char)('0' + value));
        mStrGpioValue = builder.toString();
    }
    
    public int getGpioAnalog(int gpio) {
        if (9 <= gpio && gpio <= 14) {
            gpio -= 9;
            return mIntAnalog[gpio];
        }
        return 0;
    }
    
    public void setGpioAnalog(int gpio, int value) {
        if (9 <= gpio && gpio <= 14) {
            gpio -= 9;
            mIntAnalog[gpio] = value;
        }
    }
    
    public String getGpioAnalogs() {
        return mStrAIValue;
    }
    
    public void setGpioAnalogs(String str) {
        mStrAIValue = str;
    }
    
    public int getMaxGPIO() {
        return GPIO_CNT;
    }
    
    public static int getPinNo(int gpio) {
        return GPIO_PINS[gpio];
    }
    
    public static String getGpioModeLabel(int mode) {
        return GPIO_MODE_LABEL[mode];
    }
    
    public static String[] getGpioModeLabels() {
        return GPIO_MODE_LABEL;
    }
    
    public void setGpioName(int gpio, String name) {
        if (gpio >= GPIO_CNT)
            return;
        
        mStrGpioName[gpio] = name;
    }
    
    public String getGpioName(int gpio) {
        if (gpio >= GPIO_CNT)
            return null;
        
        return mStrGpioName[gpio];
    }
    
    public String getAddr() {
        return mStrNodeAddr;
    }
    
    public int getType() {
        return mIntNodeType;
    }
    
    public void setType(int type) {
        mIntNodeType = type;
    }
    
    public String getName() {
        return mStrNodeName;
    }
    
    public void setName(String name) {
        mStrNodeName = name;
    }
    
    private void changeATMode() {
/*        
        String str = null;
        str = mProbee.writeATCmd(ProbeeZ20S.CMD_AT, 500);
        str = mProbee.writeATCmd(ProbeeZ20S.CMD_AT, 500);
        
        if (str == null) {    /// data mode
            for (int i = 0; i < 5; i++) {
                str = mProbee.writeATCmd(ProbeeZ20S.CMD_ESCAPE_DATA, 1000);
                if (str != null && str.contains(ProbeeZ20S.RESP_OK))
                    break;
            }
        }

        str = mProbee.writeATCmd(ProbeeZ20S.CMD_GET_ECHO_MODE, 0, 1, 500);
        if (str != null && !str.startsWith("0")) {
            str = mProbee.writeATCmd(String.format(ProbeeZ20S.CMD_SET_ECHO_MODE, "0"), 500);
            str = mProbee.writeATCmd(ProbeeZ20S.CMD_RESET, 500);
        }
*/        
    }

    
    public void asyncReadInfo() {
        if (!mProbee.isConnected())
            return;
        
        new Thread() {
            @Override
            public void run() {
                String strCmds[] = new String[4];

                mMutex.lock();
                changeATMode();
                strCmds[0] = new String(ProbeeZ20S.CMD_GET_NODE_NAME);
                strCmds[1] = new String(ProbeeZ20S.CMD_GET_NODE_TYPE);
                strCmds[2] = new String(ProbeeZ20S.CMD_GET_GPIOS_MODE);
                strCmds[3] = new String(ProbeeZ20S.CMD_GET_NODE_ADDR);

                if (mBoolRemote) {
                    for (int i = 0; i < strCmds.length; i++) {
                        strCmds[i] = new String(String.format(ProbeeZ20S.CMD_REMOTE, mStrNodeAddr, strCmds[i])); 
                    }
                }

                mStrNodeName = mProbee.writeATCmd(strCmds[0], 500);
                String str = mProbee.writeATCmd(strCmds[1], 500);
                if (str != null)
                    mIntNodeType = Integer.valueOf(str);
                mStrGpioMode = mProbee.writeATCmd(strCmds[2], 500);
                if (!mBoolRemote) {
                    mStrNodeAddr = mProbee.writeATCmd(strCmds[3], 0, 16, 500);
                }
                mProbee.getCallBack().obtainMessage(CB_READ_INFO_DONE, 0, 0, ZigBeeNode.this).sendToTarget();
                mMutex.unlock();
            }
        }.start();
    }
    
    public void asyncWriteInfo() {
        if (!mProbee.isConnected())
            return;

        new Thread() {
            @Override
            public void run() {
                String strCmds[] = new String[4];

                mMutex.lock();
                changeATMode();
                strCmds[0] = new String(String.format(ProbeeZ20S.CMD_SET_NODE_NAME, mStrNodeName));
                strCmds[1] = new String(String.format(ProbeeZ20S.CMD_SET_NODE_TYPE, mIntNodeType));
                strCmds[2] = new String(String.format(ProbeeZ20S.CMD_SET_GPIOS_MODE, mStrGpioMode));
                strCmds[3] = new String(ProbeeZ20S.CMD_RESET);

                String strCmd = null;
                int    nCtr = mBoolRemote ? strCmds.length : strCmds.length - 1;
                for (int i = 0; i < nCtr; i++) {
                    if (mBoolRemote)
                        strCmd = new String(String.format(ProbeeZ20S.CMD_REMOTE, mStrNodeAddr, strCmds[i]));
                    else
                        strCmd = strCmds[i];
                    
                    mProbee.writeATCmd(strCmd, 500);
                }
                // send join command to local node (coordinator)
                mProbee.writeATCmd(String.format(ProbeeZ20S.CMD_SET_JOIN_TIME, 10), 500);
                
                mProbee.getCallBack().obtainMessage(CB_WRITE_INFO_DONE, 0, 0, ZigBeeNode.this).sendToTarget();
                mMutex.unlock();
            }
        }.start();
    }
    
    public void asyncReadGpio(final int id, final int gpio) {
        if (!mProbee.isConnected())
            return;

        new Thread() {
            @Override
            public void run() {
                String strCmd = null;
                
                if (gpio < 0)
                    strCmd = ProbeeZ20S.CMD_GET_GPIOS_VALUE;
                else
                    strCmd = String.format(ProbeeZ20S.CMD_GET_GPIO_VALUE, gpio);

                mMutex.lock();
                changeATMode();
                if (mBoolRemote)
                    strCmd = new String(String.format(ProbeeZ20S.CMD_REMOTE, mStrNodeAddr, strCmd));
                
                String strRes = null;
                do {
                    strRes = mProbee.writeATCmd(strCmd, 500);
                } while (strRes == null);
                
                if (gpio < 0)
                    mStrGpioValue = strRes;
                else
                    setGpioValue(gpio, Integer.valueOf(strRes));
                mProbee.getCallBack().obtainMessage(CB_READ_GPIO_DONE, id, 0, ZigBeeNode.this).sendToTarget();
                mMutex.unlock();
            }
        }.start();
    }
    
    public void asyncWriteGpio(final int id, final int gpio, final int value) {
        if (!mProbee.isConnected())
            return;

        new Thread() {
            @Override
            public void run() {
                String strCmd = null;
                
                if (gpio < 0)
                    strCmd = String.format(ProbeeZ20S.CMD_SET_GPIOS_VALUE, mStrGpioValue);
                else {
                    setGpioValue(gpio, value);
                    strCmd = String.format(ProbeeZ20S.CMD_SET_GPIO_VALUE, gpio, value);
                }

                mMutex.lock();
                changeATMode();
                if (mBoolRemote)
                    strCmd = new String(String.format(ProbeeZ20S.CMD_REMOTE, mStrNodeAddr, strCmd));
                
                String strRes = null;
                do {
                    strRes = mProbee.writeATCmd(strCmd, 500);
                } while (strRes == null);
                mProbee.getCallBack().obtainMessage(CB_WRITE_GPIO_DONE, id, 0, ZigBeeNode.this).sendToTarget();
                mMutex.unlock();
            }
        }.start();
    }
    
    public void asyncReadAnalog(final int id) {
        if (!mProbee.isConnected())
            return;
        
        new Thread() {
            @Override
            public void run() {
                String strCmd = ProbeeZ20S.CMD_GET_AIS_VALUE;

                mMutex.lock();
                changeATMode();

                if (mBoolRemote)
                    strCmd = new String(String.format(ProbeeZ20S.CMD_REMOTE, mStrNodeAddr, strCmd));

                String strRes = null;
                do {
                    strRes = mProbee.writeATCmd(strCmd, 500);
                } while (strRes == null);
                mStrAIValue = strRes;
                
                TextUtils.SimpleStringSplitter splitter = new TextUtils.SimpleStringSplitter(',');
                splitter.setString(mStrAIValue);
                int i = 0;
                while (splitter.hasNext()) {
                    String strHex = splitter.next();
                    
                    int val = 0;
                    try {
                        val = Integer.parseInt(strHex, 16);
                    }  catch(NumberFormatException nfe) {
                        val = 0;
                    }
                    mIntAnalog[i] = val;
                    //LogUtil.d(String.format("Val:%s [%d]", strHex, mIntAnalog[i]));
                    i++;
                }

                mProbee.getCallBack().obtainMessage(CB_READ_AI_DONE, id, 0, ZigBeeNode.this).sendToTarget();

                mMutex.unlock();
            }
        }.start();
    }

    private byte[] get16Bytes(String str) {
        byte[] bufDst = new byte[16];
        Arrays.fill(bufDst, (byte)0);
        
        if (str != null) {
            byte[] bufStr = str.getBytes();
            int    nLen   = Math.min(16, bufStr.length);
            System.arraycopy(bufStr, 0, bufDst, 0, nLen);
        }
        return bufDst;
    }
    
    public final static String  NODE_SIGNATURE = "NODE";
    public final static int     NODE_SIZE      = 4 + 16 + 1 + 16 + 17 * (1 + 16);
    
    public byte[] serialize() {
        ByteBuffer byteBuf = ByteBuffer.allocate(NODE_SIZE);
        byteBuf.order(ByteOrder.LITTLE_ENDIAN);
        byteBuf.clear();
        byteBuf.put(NODE_SIGNATURE.getBytes());             // signature
        byteBuf.put(get16Bytes(mStrNodeAddr));              // addr
        byteBuf.put((byte)mIntNodeType);                    // type
        byteBuf.put(get16Bytes(mStrNodeName));              // name
        for (int i = 0; i < GPIO_CNT; i++) {
            byteBuf.put((byte)getGpioMode(i));              // gpio mode
            byteBuf.put(get16Bytes(getGpioName(i)));        // gpio name
        }
        LogUtil.d("DONE !!");
        return byteBuf.array();
    }
    
    public int deserialize(byte buf[]) {
        if (buf.length < NODE_SIZE) {
            LogUtil.e("ABNORMAL DATA SIZE !!");
            return -1;
        }

        byte[] bufDst = new byte[16];
        ByteBuffer byteBuf = ByteBuffer.allocate(NODE_SIZE);
        
        byteBuf.order(ByteOrder.LITTLE_ENDIAN);
        byteBuf.put(buf);
        byteBuf.rewind();
        
        Arrays.fill(bufDst, (byte)0);
        byteBuf.get(bufDst, 0, 4);                  // signature
        String strSig = new String(bufDst).trim();
        if (!strSig.equals(NODE_SIGNATURE)) {
            LogUtil.e("SIGNATURE MISMATCHED !! : " + strSig);
            return -1;
        }
        
        byteBuf.get(bufDst, 0, 16);                 // addr
        mStrNodeAddr = new String(bufDst).trim();
        LogUtil.i("ADDR:" + mStrNodeAddr);
        
        mIntNodeType = (int)byteBuf.get();          // type
        if (mIntNodeType != 1)
            mBoolRemote = true;

        byteBuf.get(bufDst, 0, 16);                 // name
        mStrNodeName = new String(bufDst).trim();
        LogUtil.i("NAME:" + mStrNodeName);
        
        for (int i = 0; i < GPIO_CNT; i++) {
            setGpioMode(i, (int)byteBuf.get());     // gpio mode
            byteBuf.get(bufDst, 0, 16);             // gpio name
            setGpioName(i, new String(bufDst).trim());
            LogUtil.i("GPIO" + i + ", MODE:" + getGpioMode(i) + ", NAME:" + getGpioName(i));
        }
        
        return 1;
    }
}
