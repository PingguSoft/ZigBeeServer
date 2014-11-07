package com.pinggusoft.zigbee_server;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.Vector;

import android.app.ActionBar;
import android.app.Activity;
import android.app.Dialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Messenger;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.webkit.WebView;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.TimePicker;

import com.pinggusoft.zigbee_server.ServerApp;
import com.pinggusoft.zigbee_server.R;

public class ActivityRuleConfig extends Activity {
    private final int           USAGE_TIME = 0xff;
    private ServerApp           mApp;
    private ZigBeeNode          mNode = null;
    private Vector <TableLayout> mListPortOutTable = new Vector <TableLayout>();
    private SparseArray <TableRow> mListPortInRow  = new SparseArray <TableRow>();
    private Vector <Integer>     mListInputID      = new Vector <Integer>();
    
    
    /*
    ***************************************************************************
    * 
    ***************************************************************************
    */

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.config_rule);
        
        mApp        = (ServerApp)getApplication();
        mApp.load();
        
        createScreen();
    }
    
    
    private Dialog mDialog = null;
    public void onClickTime(View v) {
        mDialog = new Dialog(this);
        mDialog.setTitle(R.string.config_rule_time);
        mDialog.setContentView(R.layout.config_rule_time);
        
        final Button btnCancel = (Button)mDialog.findViewById(R.id.buttonCancel);
        btnCancel.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                mDialog.dismiss();
            }
        });
        
        final Button btnOK = (Button)mDialog.findViewById(R.id.buttonDone);
        btnOK.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                mDialog.dismiss();
            }
        });

        mDialog.show();
    }
    
    
    private TableRow addRow(final int portRowIdx, final TableLayout tblInput, final ArrayAdapter<String> adapterInput) {
        final TableRow    tblRow        = (TableRow)LayoutInflater.from(this).inflate(R.layout.config_rule_input_row, null);
        final Spinner     spinInput     = (Spinner)tblRow.findViewById(R.id.spinnerGpioInput);
        final Spinner     spinCondition = (Spinner)tblRow.findViewById(R.id.spinnerCondition);
        final Spinner     spinOperator  = (Spinner)tblRow.findViewById(R.id.spinnerOperator);
        final Button      btnCondition  = (Button)tblRow.findViewById(R.id.buttonRangeTime);
        final Button      btnMinus      = (Button)tblRow.findViewById(R.id.buttonMinus);

        spinInput.setAdapter(adapterInput);
        spinInput.setOnItemSelectedListener(new OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent,
                    View view, int position, long id) {
                int ids = mListInputID.get(position);
                int usage = (int)(ids & 0xff);
                if (usage == USAGE_TIME) {
                    spinCondition.setVisibility(View.GONE);
                    btnCondition.setVisibility(View.VISIBLE);
                    btnCondition.setOnClickListener(new OnClickListener() {

                        @Override
                        public void onClick(View v) {
                            // TODO Auto-generated method stub
                            Calendar mcurrentTime = Calendar.getInstance();
                            int hour = mcurrentTime.get(Calendar.HOUR_OF_DAY);
                            int minute = mcurrentTime.get(Calendar.MINUTE);
                            onClickTime(v);
//                            TimePickerDialog mTimePicker;
//                            mTimePicker = new TimePickerDialog(ActivityRuleConfig.this, new TimePickerDialog.OnTimeSetListener() {
//                                @Override
//                                public void onTimeSet(TimePicker timePicker, int selectedHour, int selectedMinute) {
//                                    //eReminderTime.setText( selectedHour + ":" + selectedMinute);
//                                }
//
//                            }, hour, minute, true);//Yes 24 hour time
//                            mTimePicker.setTitle("Select Time");
//                            mTimePicker.show();

                        }
                    });
                    
                } else {
                    spinCondition.setVisibility(View.VISIBLE);
                    btnCondition.setVisibility(View.GONE);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // TODO Auto-generated method stub
            }
        });

        
        btnMinus.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                tblInput.removeViewInLayout(tblRow);
                mListPortInRow.remove(portRowIdx);
                LogUtil.d("ROW removed  :%d Total:%d", portRowIdx, mListPortInRow.size());
                tblInput.requestLayout();
                redrawOperator();
            }
        });
        
        tblInput.addView(tblRow);
        mListPortInRow.put(portRowIdx, tblRow);
        LogUtil.d("ROW added to :%d Total:%d", portRowIdx, mListPortInRow.size());
        redrawOperator();
        
        return tblRow;
    }
    
    private void redrawOperator() {
        TableRow    row;
        Spinner     spinOperator = null;
        
        for (int i = 0; i < mListPortInRow.size(); i++) {
            int key = mListPortInRow.keyAt(i);
            row = mListPortInRow.get(key);
            if (row != null) {
                spinOperator  = (Spinner)row.findViewById(R.id.spinnerOperator);
                if (i == mListPortInRow.size() - 1) {
                    spinOperator.setVisibility(View.INVISIBLE);
                } else {
                    spinOperator.setVisibility(View.VISIBLE);
                }
            }
        }
    }
    
    private void createScreen() {
        LinearLayout layoutContainer = (LinearLayout)findViewById(R.id.container);
        int nCtr = mApp.getNodeCtr();
        
        List<String> listInputs = new ArrayList<String>();
        for (int i = 0; i < nCtr; i++) {
            ZigBeeNode node = mApp.getNode(i);
            for (int j = 0; j < node.getMaxGPIO(); j++) {
                int usage = node.getGpioUsage(j);
                if (ZigBeeNode.TYPE_INPUT_TOUCH <= usage && usage <= ZigBeeNode.TYPE_INPUT_ANALOG) {
                    listInputs.add(node.getGpioName(j));
                    int id = (i << 16) | (j << 8) | usage;
                    mListInputID.add((Integer)(id));
                }
            }
        }
        listInputs.add(getResources().getString(R.string.config_rule_input_time));
        mListInputID.add((Integer)(USAGE_TIME));
        
        LinearLayout.LayoutParams llp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        llp.setMargins(10, 10, 10, 60);

        final ArrayAdapter<String> adapterInput = new ArrayAdapter<String>(this, R.layout.config_spinner, listInputs);

        for (int i = 0; i < nCtr; i++) {
            ZigBeeNode node = mApp.getNode(i);
            
            for (int j = 0; j < node.getMaxGPIO(); j++) {
                int usage = node.getGpioUsage(j);
                
                if (usage == ZigBeeNode.TYPE_OUTPUT_LIGHT) {
                    LinearLayout layoutPort = (LinearLayout)LayoutInflater.from(this).inflate(R.layout.config_rule_port, null);

                    TableLayout tblLayout = (TableLayout)LayoutInflater.from(this).inflate(R.layout.config_rule_output_row, null);
                    TextView text = (TextView)tblLayout.findViewById(R.id.textGpioOutput);
                    text.setText(node.getGpioName(j));
                    layoutPort.addView(tblLayout);

                    mListPortOutTable.add(tblLayout);
                    
                    final TableLayout tblInput = (TableLayout)LayoutInflater.from(this).inflate(R.layout.config_rule_input_header, null);
                    final Button      btnPlus  = (Button)tblInput.findViewById(R.id.buttonPlus);
                    btnPlus.setOnClickListener(new OnClickListener() {
                        int nPortRowIdx = 0;

                        @Override
                        public void onClick(View v) {
                            addRow(nPortRowIdx++, tblInput, adapterInput);
                        }
                    });
                    
                    //addRow(nPortRowIdx, tblInput, adapterInput);
                    layoutPort.addView(tblInput);
                    layoutContainer.addView(layoutPort, llp);
                }
            }
        }
    }

    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    public synchronized void onResume() {
        super.onResume();
        
        if (ServerApp.isAboveICS()) {
            ActionBar bar = getActionBar();
            bar.setBackgroundDrawable(new ColorDrawable(Color.parseColor("#222222")));
            int titleId = getResources().getIdentifier("action_bar_title", "id", "android");
            TextView abTitle = (TextView) findViewById(titleId);
            abTitle.setTextColor(Color.WHITE);
        }
        
//        ((EditText)findViewById(R.id.editServerPort)).setText(String.valueOf(mApp.getServerPort()));
    }
    
    @Override
    public synchronized void onPause() {
        super.onPause();

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    
    /*
    ***************************************************************************
    * 
    ***************************************************************************
    */

}
