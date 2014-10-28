package com.pinggusoft.zigbee_server;

public class ZigBeeNode {
    public  final static int GPIO_CNT  = 17;
    public  final static int CB_READ_DONE  = 2;
    public  final static int CB_WRITE_DONE = 3;
    public  final static int CB_LAST       = 4;
    
    private final static int    GPIO_PINS[]         = { 3, 4, 5, 6, 7, 8, 9, 10, 11, 32, 31, 30, 29, 28, 27, 24, 23 };
    private final static String GPIO_MODE_LABEL[]   = { "0 - disabled", "1 - DIN", "2 - DOUT (low)", "3 - DOUT (high)", "4 - Analog In", "5 - Reserved" };
    
    private boolean       mBoolRemote  = false;
    private int           mIntNodeType = 0;
    private String        mStrNodeAddr = null;
    private String        mStrNodeName = null;
    private String        mStrGpioMode = null;
    private ProbeeZ20S    mProbee = null;
    
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

    public int getGpioMode(int gpio) {
        int mode = 0;
        
        if (gpio >= GPIO_CNT)
            return 0;
        
        String strVal = mStrGpioMode.substring(gpio, gpio + 1);
        mode = Integer.valueOf(strVal);
        
        return mode;
    }
    
    public void setGpioMode(int gpio, int mode) {
        if (gpio >= GPIO_CNT)
            return;
        
        StringBuilder builder = new StringBuilder(mStrGpioMode);
        builder.setCharAt(gpio, (char)('0' + mode));
        
        mStrGpioMode = builder.toString();
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
        if (!str.startsWith("0")) {
            str = mProbee.writeATCmd(String.format(ProbeeZ20S.CMD_SET_ECHO_MODE, "0"), 500);
            str = mProbee.writeATCmd(ProbeeZ20S.CMD_RESET, 500);
        }
    }
    
    public void readInfo() {
        if (!mProbee.isConnected())
            return;
        
        new Thread() {
            @Override
            public void run() {
                String strCmds[] = new String[4];
                
                changeATMode();
                strCmds[0] = new String(ProbeeZ20S.CMD_GET_NODE_NAME);
                strCmds[1] = new String(ProbeeZ20S.CMD_GET_NODE_TYPE);
                strCmds[2] = new String(ProbeeZ20S.CMD_GET_GPIO_MODE);
                strCmds[3] = new String(ProbeeZ20S.CMD_GET_NODE_ADDR);

                if (mBoolRemote) {
                    for (int i = 0; i < strCmds.length; i++) {
                        strCmds[i] = new String(String.format(ProbeeZ20S.CMD_REMOTE, mStrNodeAddr, strCmds[i])); 
                    }
                }

                mStrNodeName = mProbee.writeATCmd(strCmds[0], 500);
                mIntNodeType = Integer.valueOf(mProbee.writeATCmd(strCmds[1], 500));
                mStrGpioMode = mProbee.writeATCmd(strCmds[2], 500);
                if (!mBoolRemote) {
                    mStrNodeAddr = mProbee.writeATCmd(strCmds[3], 0, 16, 500);
                }
                mProbee.getCallBack().obtainMessage(CB_READ_DONE, 0, 0, null).sendToTarget();
            }
        }.start();
    }
    
    public void writeInfo() {
        if (!mProbee.isConnected())
            return;
        
        new Thread() {
            @Override
            public void run() {
                String strCmds[] = new String[4];
                
                changeATMode();
                strCmds[0] = new String(String.format(ProbeeZ20S.CMD_SET_NODE_NAME, mStrNodeName));
                strCmds[1] = new String(String.format(ProbeeZ20S.CMD_SET_NODE_TYPE, mIntNodeType));
                strCmds[2] = new String(String.format(ProbeeZ20S.CMD_SET_GPIO_MODE, mStrGpioMode));
                strCmds[3] = new String(ProbeeZ20S.CMD_RESET);

                String strCmd = null;
                for (int i = 0; i < strCmds.length; i++) {
                    if (mBoolRemote)
                        strCmd = new String(String.format(ProbeeZ20S.CMD_REMOTE, mStrNodeAddr, strCmds[i]));
                    else
                        strCmd = strCmds[i];
                    
                    mProbee.writeATCmd(strCmd, 500);
                }
                
                mProbee.getCallBack().obtainMessage(CB_WRITE_DONE, 0, 0, null).sendToTarget();
            }
        }.start();
    }
}
