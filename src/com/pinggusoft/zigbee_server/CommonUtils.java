package com.pinggusoft.zigbee_server;


import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.lang.ref.WeakReference;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.Vector;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

public class CommonUtils {
    private Spinner             mSpinnerUsages[];
    private EditText            mEditGpioNames[];
    private Activity            mActivity;
        
    /*
     ***************************************************************************
     * 
     ***************************************************************************
     */
     public class SpinnerAdapter extends ArrayAdapter<String>{
         int        mIntResImages[] = { R.drawable.type_cross_32,
                                        R.drawable.type_status_32,
                                        R.drawable.type_light_32,
                                        R.drawable.type_switch_32, 
                                        R.drawable.type_adc_32,
                                        R.drawable.type_reserved_32 };
         String     mStrUsages[] = null;
         int        mIntResId;
         
         public SpinnerAdapter(Context context, int textViewResourceId, String[] objects) {
             super(context, textViewResourceId, objects);
             mIntResId = textViewResourceId;
             mStrUsages = objects;
         }
  
         @Override
         public View getDropDownView(int position, View convertView, ViewGroup parent) {
             return getCustomView(position, convertView, parent);
         }
  
         @Override
         public View getView(int position, View convertView, ViewGroup parent) {
             return getCustomView(position, convertView, parent);
         }
  
         public View getCustomView(int position, View convertView, ViewGroup parent) {
  
             LayoutInflater inflater = mActivity.getLayoutInflater();
             View row=inflater.inflate(mIntResId, parent, false);
             
             TextView label = (TextView)row.findViewById(R.id.textMode);
             label.setText(mStrUsages[position]);
  
             ImageView icon = (ImageView)row.findViewById(R.id.imageMode);
             icon.setImageResource(mIntResImages[position]);
  
             return row;
         }
     };
     
     public CommonUtils(Activity activity) {
         mActivity      = activity;
         mSpinnerUsages = new Spinner[ZigBeeNode.GPIO_CNT];
         mEditGpioNames = new EditText[ZigBeeNode.GPIO_CNT];
     }
     
     public Spinner[] getSpinnerUsages() {
         return mSpinnerUsages;
     }
     
     public EditText[] getEditGpioNames() {
         return mEditGpioNames;
     }
     
     public void createGpioTable() {
         TableLayout tbl = (TableLayout)mActivity.findViewById(R.id.container_zigbee_gpio);
         for (int i = 0; i < ZigBeeNode.GPIO_CNT; i++) {
             TableRow row = (TableRow)LayoutInflater.from(mActivity).inflate(R.layout.config_zigbee_gpio_row, null);
             TextView tvNo = (TextView)row.findViewById(R.id.text_zigbee_gpio_no);
             if (tvNo != null) {
                 tvNo.setText(String.format("%d", i));
                 if (9 <= i && i <= 14)
                     tvNo.setBackgroundColor(mActivity.getResources().getColor(R.color.green));
             }
             
             TextView tvPIN = (TextView)row.findViewById(R.id.text_zigbee_gpio_pin);
             if (tvPIN != null) {
                 tvPIN.setText(String.format("%d", ZigBeeNode.getPinNo(i)));
                 if (9 <= i && i <= 14)
                     tvPIN.setBackgroundColor(mActivity.getResources().getColor(R.color.green));
             }

             mSpinnerUsages[i] = (Spinner)row.findViewById(R.id.spinnerGpioUsage);
             mSpinnerUsages[i].setAdapter(new SpinnerAdapter(mActivity, R.layout.config_zigbee_spin_row, mActivity.getResources().getStringArray(R.array.zigbee_gpio_usage)));
             
             mEditGpioNames[i] = (EditText)row.findViewById(R.id.editGpioName);
             tbl.addView(row);
         }
         tbl.requestLayout();
     }
}
