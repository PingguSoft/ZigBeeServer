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
import android.view.View.OnFocusChangeListener;
import android.view.ViewGroup.LayoutParams;
import android.webkit.WebView;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.Spinner;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.TimePicker;


import com.pinggusoft.zigbee_server.ServerApp;
import com.pinggusoft.zigbee_server.R;

public class ActivityRuleConfig extends Activity {
    private final int               USAGE_TIME = 0xff;
    private ServerApp               mApp;
    private ZigBeeNode              mNode = null;
    
    private SparseArray <RulePort>  mListRulePort = new SparseArray <RulePort>();
//    private SparseArray <TableRow>  mListPortInRow = new SparseArray <TableRow>();
//    private SparseArray <RuleRow>   mListRules     = new SparseArray <RuleRow>();
    private Vector <Integer>        mListInputID   = new Vector <Integer>();
    
    
    /*
     ***************************************************************************
     * 
     ***************************************************************************
     */
    public class RulePort {
        public  final static int OP_OFF     = 0;
        public  final static int OP_ON      = 1;
        public  final static int OP_TOGGLE  = 2;
        
        private int     nID;
        private int     nOp;
        private int     nRowKey;
        private SparseArray <RuleRow>   listRules     = new SparseArray <RuleRow>();
        private SparseArray <TableRow>  listPortInRow = new SparseArray <TableRow>();
        
        public RulePort(int id) {
            nID = id;
            nOp = OP_OFF;
            nRowKey = 0;
        }
        
        public void setOp(int op) {
            nOp = op;
        }

        public void putRule(int key, RuleRow rule) {
            listRules.put(key, rule);
        }
        
        public void removeRule(int key) {
            listRules.remove(key);
        }
        
        public void putRow(int key, TableRow row) {
            listPortInRow.put(key, row);
            LogUtil.d("ROW added to :%d Total:%d", key, listPortInRow.size());
        }

        public void removeRow(int key) {
            listPortInRow.remove(key);
            LogUtil.d("ROW removed  :%d Total:%d", key, listPortInRow.size());
        }
        
        public void incRowKey() {
            nRowKey++;
        }
        
        public int getRowKey() {
            return nRowKey;
        }
        
        public void redrawRules() {
            TableRow    row;
            Spinner     spinOperator = null;
            
            for (int i = 0; i < listPortInRow.size(); i++) {
                int key = listPortInRow.keyAt(i);
                row = listPortInRow.get(key);
                if (row != null) {
                    spinOperator  = (Spinner)row.findViewById(R.id.spinnerOperator);
                    if (i == listPortInRow.size() - 1) {
                        spinOperator.setVisibility(View.INVISIBLE);
                    } else {
                        spinOperator.setVisibility(View.VISIBLE);
                    }
                }
            }
        }
        
        public void printRule() {
            RuleRow    row;

            LogUtil.d("---- rule for %x op:%d ----", nID, nOp);
            for (int i = 0; i < listRules.size(); i++) {
                int key = listRules.keyAt(i);
                row = listRules.get(key);
                if (row != null) {
                    
                    LogUtil.d("id:%x, min:%d, max:%d", row.nID, row.nMin, row.nMax);
                }
            }
        }
    }
    
    
    /*
    ***************************************************************************
    * 
    ***************************************************************************
    */
    private class RuleRow {
        private int     nID;
        private int     nMin;
        private int     nMax;
        private boolean boolDays[] = new boolean[7];

        public RuleRow(int id) {
            nID   = id;
            nMin  = 0;
            nMax  = 0;
        }
        
        public void setRange(int min, int max) {
            nMin = min;
            nMax = max;
        }
        
        public void setDay(int day, boolean check) {
            if (0 <= day && day < boolDays.length)
                boolDays[day] = check;
        }
    };
    
    
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
    public void onClickTime(View v, final RulePort rulePort, final int rowKey, final int id) {
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
                RuleRow rule = new RuleRow(id);
                
                TimePicker tp = (TimePicker)mDialog.findViewById(R.id.timePickerStart);
                int min = (tp.getCurrentHour() << 16) | tp.getCurrentMinute();
                tp = (TimePicker)mDialog.findViewById(R.id.timePickerEnd);
                int max = (tp.getCurrentHour() << 16) | tp.getCurrentMinute();
                rule.setRange(min, max);
                for (int i = R.id.checkBoxSun; i <= R.id.checkBoxSat; i++) {
                    CheckBox cb = (CheckBox)mDialog.findViewById(i);
                    rule.setDay(i - R.id.checkBoxSun, cb.isChecked());
                }
                
                rulePort.putRule(rowKey, rule);
                mDialog.dismiss();
            }
        });

        mDialog.show();
    }
    
    
    
    public void onClickThermo(View v, final RulePort rulePort, final int rowKey, final int id) {
        mDialog = new Dialog(this);
        mDialog.setTitle(R.string.config_rule_thermo);
        mDialog.setContentView(R.layout.config_rule_thermo);
        
        final TextView textMin = (TextView)mDialog.findViewById(R.id.textThermoMin);
        final SeekBar thermoMin = (SeekBar)mDialog.findViewById(R.id.sliderThermoMin);
        thermoMin.setOnFocusChangeListener(new OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
            }
        });
        thermoMin.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress,
                    boolean fromUser) {
                progress -= 20;
                textMin.setText(String.format("%d กษ", progress));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
            
        });
        thermoMin.setProgress(20);
        
        final TextView textMax = (TextView)mDialog.findViewById(R.id.textThermoMax);
        final SeekBar thermoMax = (SeekBar)mDialog.findViewById(R.id.sliderThermoMax);
        thermoMax.setOnFocusChangeListener(new OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
            }
        });
        thermoMax.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress,
                    boolean fromUser) {
                progress -= 20;
                textMax.setText(String.format("%d กษ", progress));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
            
        });
        thermoMax.setProgress(20);
        

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
                RuleRow rule = new RuleRow(id);
                
                SeekBar tp = (SeekBar)mDialog.findViewById(R.id.sliderThermoMin);
                int min = tp.getProgress();
                tp = (SeekBar)mDialog.findViewById(R.id.sliderThermoMax);
                int max = tp.getProgress();

                rule.setRange(min, max);
                rulePort.putRule(rowKey, rule);
                mDialog.dismiss();
            }
        });

        mDialog.show();
    }
    
    
    
    private TableRow addRow(final RulePort rulePort, final TableLayout tblInput, final ArrayAdapter<String> adapterInput) {
        final TableRow    tblRow        = (TableRow)LayoutInflater.from(this).inflate(R.layout.config_rule_input_row, null);
        final Spinner     spinInput     = (Spinner)tblRow.findViewById(R.id.spinnerGpioInput);
        final Spinner     spinCondition = (Spinner)tblRow.findViewById(R.id.spinnerCondition);
        final Spinner     spinOperator  = (Spinner)tblRow.findViewById(R.id.spinnerOperator);
        final Button      btnCondition  = (Button)tblRow.findViewById(R.id.buttonRangeTime);
        final Button      btnMinus      = (Button)tblRow.findViewById(R.id.buttonMinus);
        final int         nRowKey       = rulePort.getRowKey();
        final int         ids           = mListInputID.get(0);
        
        spinInput.setAdapter(adapterInput);
        spinInput.setOnItemSelectedListener(new OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent,
                    View view, int position, long id) {
                final int ids = mListInputID.get(position);
                final int usage = (int)(ids & 0xff);
                
                if (usage == USAGE_TIME) {
                    spinCondition.setVisibility(View.GONE);
                    btnCondition.setVisibility(View.VISIBLE);
                    btnCondition.setText("12:00 ~ 15:00");
                    btnCondition.setOnClickListener(new OnClickListener() {

                        @Override
                        public void onClick(View v) {
                            // TODO Auto-generated method stub
                            Calendar mcurrentTime = Calendar.getInstance();
                            int hour = mcurrentTime.get(Calendar.HOUR_OF_DAY);
                            int minute = mcurrentTime.get(Calendar.MINUTE);
                            onClickTime(v, rulePort, nRowKey, ids);
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
                    
                } else if (usage == ZigBeeNode.TYPE_INPUT_ANALOG){
                    spinCondition.setVisibility(View.GONE);
                    btnCondition.setVisibility(View.VISIBLE);
                    btnCondition.setText("Thermo");
                    btnCondition.setOnClickListener(new OnClickListener() {

                        @Override
                        public void onClick(View v) {
                            onClickThermo(v, rulePort, nRowKey, ids);
                        }
                    });
                } else {
                    spinCondition.setVisibility(View.VISIBLE);
                    btnCondition.setVisibility(View.GONE);
                    
                    RuleRow rule = new RuleRow(ids);
                    rule.setRange(0, 0);
                    
                    spinCondition.setOnItemSelectedListener(new OnItemSelectedListener() {
                        @Override
                        public void onItemSelected(AdapterView<?> parent,
                                View view, int position, long id) {
                            RuleRow rule = new RuleRow(ids);
                            rule.setRange(position, position);
                            rulePort.putRule(nRowKey, rule);
                        }

                        @Override
                        public void onNothingSelected(AdapterView<?> parent) {
                            
                        }
                    });
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
                rulePort.removeRow(nRowKey);
                rulePort.removeRule(nRowKey);
                
                tblInput.requestLayout();
                rulePort.redrawRules();
            }
        });
        
        tblInput.addView(tblRow);

        rulePort.putRow(nRowKey, tblRow);
        rulePort.putRule(nRowKey, new RuleRow(ids));
        rulePort.incRowKey();
        rulePort.redrawRules();

        return tblRow;
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
                int id = (i << 16) | (j << 8) | usage;
                
                if (usage == ZigBeeNode.TYPE_OUTPUT_LIGHT) {
                    LinearLayout layoutPort = (LinearLayout)LayoutInflater.from(this).inflate(R.layout.config_rule_port, null);

                    TableLayout tblLayout = (TableLayout)LayoutInflater.from(this).inflate(R.layout.config_rule_output_row, null);
                    TextView text = (TextView)tblLayout.findViewById(R.id.textGpioOutput);
                    text.setText(node.getGpioName(j));
                    layoutPort.addView(tblLayout);

                    final RulePort rulePort = new RulePort(id);
                    mListRulePort.put(id, rulePort);
                    
                    //mListPortOutTable.add(tblLayout);
                    
                    final TableLayout tblInput = (TableLayout)LayoutInflater.from(this).inflate(R.layout.config_rule_input_header, null);
                    final Button      btnPlus  = (Button)tblInput.findViewById(R.id.buttonPlus);
                    btnPlus.setOnClickListener(new OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            addRow(rulePort, tblInput, adapterInput);
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
        for (int i = 0; i < mListRulePort.size(); i++) {
            int key = mListRulePort.keyAt(i);
            RulePort port = mListRulePort.get(key);
            port.printRule();
        }
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
