package com.pinggusoft.zigbee_server;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import android.app.Application;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Build;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.telephony.TelephonyManager;
import android.util.Log;

public class BTConApp extends Application {
    private final static String TAG = "BTConApp";
    
    private final static String KEY_BATTCELL           = "KEY_BATTCELL";
    private final static String KEY_BTDEVICE           = "KEY_BTDEVICE";
    private final static String KEY_RX_RADIO_OPTION    = "KEY_RX_RADIO_OPTION";
    private final static String KEY_MODE               = "KEY_MODE";
    private final static String KEY_ZIGBEE_DEVICE      = "KEY_ZIGBEE_DEVICE";
    private final static String KEY_TRIM_THR           = "KEY_TRIM_THR";
    private final static String KEY_TRIM_ROLL          = "KEY_TRIM_ROLL";
    private final static String KEY_TRIM_PITCH         = "KEY_TRIM_PITCH";
    private final static String KEY_TRIM_YAW           = "KEY_TRIM_YAW";
    private final static String KEY_SENSOR_SENSITIVITY = "KEY_SENSOR_SENSITIVITY";
    private final static String KEY_INSTALL_TIME       = "KEY_INSTALL_TIME";
    private final static String KEY_INSTALL_VER        = "KEY_INSTALL_VER";
    private final static String KEY_UBLOX_ID           = "KEY_UBLOX_ID";
    private final static String KEY_UBLOX_PASSWORD     = "KEY_UBLOX_PASSWORD";

    public final static int     RADIO_DEFAULT_BT = 0;
    public final static int     RADIO_ZIGBEE     = 1;
    public final static int     RADIO_BT10       = 2;
    
    public final static int     MODE_CONTROLLER        = 0;
    public final static int     MODE_STATION           = 1;
    
    public int      m_nBattCell;
    public String   m_strBTDevice = null;
    public String   m_strBTAddr = null;
    public int      m_nRXRadioOption = 0;
    public int      m_nMode = 0;
    public String   m_strZigBeeDevice = null;
    public int      m_nTrimThrottle = 0;
    public int      m_nTrimRoll  = -1;
    public int      m_nTrimPitch = -1;
    public int      m_nTrimYaw   = -1;
    public int      m_nSensitivity   = 0;
    private long    m_lFirstInstallTime;
    private String  m_strVer;
    private SharedPreferences m_spBTCon;
    private Editor  m_editorBTCon;
    public String	m_strUbloxID;
    public String	m_strUbloxPassword;
    
    @Override
    public void onCreate() {
        super.onCreate();
       
        LogUtil.initialize(this);
        m_spBTCon = PreferenceManager.getDefaultSharedPreferences(this);
        m_editorBTCon = m_spBTCon.edit();
        readSettings();
        if (m_strBTDevice != null)
            m_strBTAddr = m_strBTDevice.substring(m_strBTDevice.length() - 17);
    }
    
    private String getTimeString(long lTime) {
        Date date = new Date(lTime);
        Locale locale = Locale.getDefault(); 
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy MMM d, EEE. aa h:mm:ss", locale);
        
        return sdf.format(date);
    }
    
    public void readSettings() {
        m_nBattCell       = m_spBTCon.getInt(KEY_BATTCELL, 0);
        m_strBTDevice     = m_spBTCon.getString(KEY_BTDEVICE, null);
        m_nRXRadioOption  = m_spBTCon.getInt(KEY_RX_RADIO_OPTION, 0);
        m_strZigBeeDevice = m_spBTCon.getString(KEY_ZIGBEE_DEVICE, null);
        m_nTrimThrottle   = m_spBTCon.getInt(KEY_TRIM_THR, 0);
        m_nTrimRoll       = m_spBTCon.getInt(KEY_TRIM_ROLL, -1);
        m_nTrimPitch      = m_spBTCon.getInt(KEY_TRIM_PITCH, -1);
        m_nTrimYaw        = m_spBTCon.getInt(KEY_TRIM_YAW, -1);
        m_nSensitivity    = m_spBTCon.getInt(KEY_SENSOR_SENSITIVITY, 5);
        m_nMode           = m_spBTCon.getInt(KEY_MODE, MODE_CONTROLLER);
        m_strUbloxID      = m_spBTCon.getString(KEY_UBLOX_ID, null);
        m_strUbloxPassword= m_spBTCon.getString(KEY_UBLOX_PASSWORD, null);

        getInstalledTime();
        
        m_strVer = m_spBTCon.getString(KEY_INSTALL_VER, null);
    }
    
    private void getInstalledTime() {
        long lInstallTimeSP = m_spBTCon.getLong(KEY_INSTALL_TIME, -1);
        
        ByteBuffer  byteBuf = ByteBuffer.allocate(8);
        byteBuf.order(ByteOrder.LITTLE_ENDIAN);
        File file = new File(Environment.getExternalStorageDirectory().getPath() + "/Android/data/", ".noctb");
        if (file.exists()) {
            LogUtil.e("file exists");
            try {
                FileInputStream os = new FileInputStream(file);
                os.read(byteBuf.array());
                if (os != null)
                    os.close();
                m_lFirstInstallTime = byteBuf.getLong();
                LogUtil.e("installed time file : " + getTimeString(m_lFirstInstallTime));
            } catch (IOException e) {
                e.printStackTrace();
            }

            LogUtil.e("SP:" + lInstallTimeSP + ", FILE:" + m_lFirstInstallTime);
            
            if (lInstallTimeSP == -1) {
                // app is un-installed, use time of .noctb
                m_editorBTCon.putLong(KEY_INSTALL_TIME, m_lFirstInstallTime);
                m_editorBTCon.commit();
                LogUtil.e("app in un-installed, use file :" + getTimeString(m_lFirstInstallTime));
            } else if (lInstallTimeSP != m_lFirstInstallTime) {
                // different time, use min time
                m_lFirstInstallTime = Math.min(m_lFirstInstallTime, lInstallTimeSP);
                LogUtil.e("different time use min time:" + getTimeString(m_lFirstInstallTime));
            }
        } else {
            LogUtil.e("file doesn't exist");

            File fileDir = new File(Environment.getExternalStorageDirectory().getPath() + "/Android/data/");
            fileDir.mkdirs();

            if (lInstallTimeSP == -1) {
                // real first install
                m_lFirstInstallTime = System.currentTimeMillis();
                m_editorBTCon.putLong(KEY_INSTALL_TIME,  m_lFirstInstallTime);
                m_editorBTCon.commit();
                LogUtil.e("real first install!!:" + getTimeString(m_lFirstInstallTime));
            } else {
                // .noctb is removed
                m_lFirstInstallTime = lInstallTimeSP;
                LogUtil.e("file is removed!!, Use SP:" + getTimeString(m_lFirstInstallTime));
            }
            byteBuf.putLong(m_lFirstInstallTime);

            try {
                FileOutputStream os = new FileOutputStream(file);
                os.write(byteBuf.array());
                if (os != null)
                    os.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void saveSettings() {
        m_editorBTCon.putInt(KEY_BATTCELL, m_nBattCell);
        m_editorBTCon.putString(KEY_BTDEVICE, m_strBTDevice);
        m_editorBTCon.putInt(KEY_RX_RADIO_OPTION, m_nRXRadioOption);
        m_editorBTCon.putString(KEY_ZIGBEE_DEVICE, m_strZigBeeDevice);
        m_editorBTCon.putInt(KEY_TRIM_THR, m_nTrimThrottle);
        m_editorBTCon.putInt(KEY_TRIM_ROLL, m_nTrimRoll);
        m_editorBTCon.putInt(KEY_TRIM_PITCH, m_nTrimPitch);
        m_editorBTCon.putInt(KEY_TRIM_YAW, m_nTrimYaw);
        m_editorBTCon.putInt(KEY_SENSOR_SENSITIVITY, m_nSensitivity);
        m_editorBTCon.putInt(KEY_MODE, m_nMode);
        m_editorBTCon.putString(KEY_UBLOX_ID, m_strUbloxID);
        m_editorBTCon.putString(KEY_UBLOX_PASSWORD, m_strUbloxPassword);
        m_editorBTCon.commit();
        
        if (m_strBTDevice != null)
            m_strBTAddr = m_strBTDevice.substring(m_strBTDevice.length() - 17);
    }

    @Override
    public void onTerminate() {
        super.onTerminate();
    }
    
    public String getBTAddr() {
        return m_strBTAddr;
    }
    
    public boolean IsExpired() {
        getInstalledTime();
        
        long diff  = System.currentTimeMillis() - m_lFirstInstallTime;
        long hours = diff / (1000 * 60 * 60);   

        LogUtil.e(String.format("inst:%d,  diff:%d, hour:%d", m_lFirstInstallTime, diff, hours));
        if (hours > 24)
            return true;
        
        return false;
    }

    public void testExpire() {
        m_lFirstInstallTime -= (1000 * 60 * 60 * 24);
        m_editorBTCon.putLong(KEY_INSTALL_TIME,  m_lFirstInstallTime);
        m_editorBTCon.commit();
    }
    
    public String getInstVer() {
        return m_strVer;
    }
    
    public int getMode() {
        return m_nMode;
    }
    
    public void setInstVer(String strVer) {
        m_strVer = strVer;
        m_editorBTCon.putString(KEY_INSTALL_VER, m_strVer);
        m_editorBTCon.commit();
    }
    
    public String getPackageVer() {
        String strVer = null;
        try {
            strVer = getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
        } catch (NameNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        
        return strVer;
    }
    
    public static boolean isAboveICS() {
       return android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH;
    }
    
    public boolean isAuthorized() {
        TelephonyManager telephonyManager = (TelephonyManager) getSystemService(TELEPHONY_SERVICE);
        String imei = telephonyManager.getDeviceId();
        
        if (imei != null && imei.equals("358240055059836"))
            return true;
        
        return false;
    }
}
