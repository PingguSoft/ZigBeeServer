package com.pinggusoft.zigbee_server;

import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import android.app.ActionBar;
import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnDismissListener;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnFocusChangeListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
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
    private final int               USAGE_TIME = 0x0f;
    private ServerApp               mApp;
    private ZigBeeNode              mNode = null;
    
    private SparseArray <RulePort>  mListRulePort = new SparseArray <RulePort>();
    private Vector <Integer>        mListInputID  = new Vector <Integer>();
    private Vector <Integer>        mListOutputID = new Vector <Integer>();
    
    
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
    public class RulePort {
        public  final static int OP_OFF     = 0;
        public  final static int OP_ON      = 1;
        public  final static int OP_TOGGLE  = 2;
        
        private int     nID;            // (node_nid << 16) | (gpio_no << 8) | (op << 4) | usage
        private int     nRowKey;
        private SparseArray <RuleRow>   listRules    = new SparseArray <RuleRow>();
        private SparseArray <TableRow>  listTableRow = new SparseArray <TableRow>();
        
        public RulePort(int id) {
            nID = id;
            nRowKey = 0;
        }

        public RuleRow getRule(int key) {
            return listRules.get(key);
        }
        
        public void putRule(int key, RuleRow rule) {
            listRules.put(key, rule);
        }
        
        public void removeRule(int key) {
            listRules.remove(key);
        }
        
        public void putRow(int key, TableRow row) {
            listTableRow.put(key, row);
            LogUtil.d("ROW added to :%d Total:%d", key, listTableRow.size());
        }

        public void removeRow(int key) {
            listTableRow.remove(key);
            LogUtil.d("ROW removed  :%d Total:%d", key, listTableRow.size());
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
            
            for (int i = 0; i < listTableRow.size(); i++) {
                int key = listTableRow.keyAt(i);
                row = listTableRow.get(key);
                if (row != null) {
                    spinOperator  = (Spinner)row.findViewById(R.id.spinnerOperator);
                    if (i == listTableRow.size() - 1) {
                        spinOperator.setVisibility(View.INVISIBLE);
                    } else {
                        spinOperator.setVisibility(View.VISIBLE);
                    }
                }
            }
        }
        
        public void printRule() {
            RuleRow    row;

            LogUtil.d("---- rule for %x ----", nID);
            for (int i = 0; i < listRules.size(); i++) {
                int key = listRules.keyAt(i);
                row = listRules.get(key);
                if (row != null)
                    row.printRule();
            }
        }
    }
    
    
    /*
    ***************************************************************************
    * 
    ***************************************************************************
    */
    public class RuleRow {
        public  final static int OP_AND = 0;
        public  final static int OP_OR  = 1;
        
        public  final static int THERMO_OFFSET = 20; 
        
        private int     nID;    // (node_nid << 16) | (gpio_no << 8) | usage
        private int     nMin;
        private int     nMax;
        private int     nOP;
        private boolean boolDays[] = new boolean[7];

        public RuleRow(int id) {
            nID   = id;
            nMin  = 0;
            nMax  = 0;
            nOP   = OP_AND;
        }
        
        public void setID(int id) {
            nID = id;
        }
        
        public void setRange(int min, int max) {
            nMin = min;
            nMax = max;
        }
        
        public void setDay(int day, boolean check) {
            if (0 <= day && day < boolDays.length)
                boolDays[day] = check;
        }
        
        public void setOP(int op) {
            nOP = op;
        }
        
        public int getMin() {
            return nMin;
        }
        
        public int getMax() {
            return nMax;
        }
        
        public int getStartHour() {
            return nMin >> 16;
        }
        
        public int getStartMin() {
            return nMin & 0xffff;
        }
        
        public int getEndHour() {
            return nMax >> 16;
        }
        
        public int getEndMin() {
            return nMax & 0xffff;
        }
        
        public void setStartTime(int hour, int min) {
            nMin = (hour << 16) | min;
        }
        
        public void setEndTime(int hour, int min) {
            nMax = (hour << 16) | min;
        }
        
        public void printRule() {
            LogUtil.d("id:%x, min:%d, max:%d, op:%d", nID, nMin, nMax, nOP);
        }
        
        public String getTimeString() {
            return String.format("%d:%02d ~ %d:%02d", nMin >> 16, (nMin & 0xffff), nMax >> 16, (nMax & 0xffff));
        }
        
        public String getThermoString() {
            return String.format("%d ~ %d", nMin, nMax);
        }
    };
    
    /*
    ***************************************************************************
    * 
    ***************************************************************************
    */
    public Dialog onClickTime(View v, final RulePort rulePort, final int rowKey, final int id) {
        final Dialog dlgView = new Dialog(this);
        dlgView.setTitle(R.string.config_rule_time);
        dlgView.setContentView(R.layout.config_rule_time);
        
        final TimePicker tpStart = (TimePicker)dlgView.findViewById(R.id.timePickerStart);
        final TimePicker tpEnd   = (TimePicker)dlgView.findViewById(R.id.timePickerEnd);
        final RuleRow    rule    = rulePort.getRule(rowKey);
        
        tpStart.setIs24HourView(true);
        tpStart.setCurrentHour(rule.getStartHour());
        tpStart.setCurrentMinute(rule.getStartMin());
        
        tpEnd.setIs24HourView(true);
        tpEnd.setCurrentHour(rule.getEndHour());
        tpEnd.setCurrentMinute(rule.getEndMin());
        
        final Button btnCancel = (Button)dlgView.findViewById(R.id.buttonCancel);
        btnCancel.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                dlgView.dismiss();
            }
        });
        
        final Button btnOK = (Button)dlgView.findViewById(R.id.buttonDone);
        btnOK.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                rule.setStartTime(tpStart.getCurrentHour(), tpStart.getCurrentMinute());
                rule.setEndTime(tpEnd.getCurrentHour(), tpEnd.getCurrentMinute());
                for (int i = R.id.checkBoxSun; i <= R.id.checkBoxSat; i++) {
                    CheckBox cb = (CheckBox)dlgView.findViewById(i);
                    rule.setDay(i - R.id.checkBoxSun, cb.isChecked());
                }
                rulePort.putRule(rowKey, rule);
                dlgView.dismiss();
            }
        });

        dlgView.show();
        return dlgView;
    }
        
    
    public Dialog onClickThermo(View v, final RulePort rulePort, final int rowKey, final int id) {
        final Dialog dlgView = new Dialog(this);
        dlgView.setTitle(R.string.config_rule_thermo);
        dlgView.setContentView(R.layout.config_rule_thermo);
        
        final RuleRow rule = rulePort.getRule(rowKey);
        final TextView textMin = (TextView)dlgView.findViewById(R.id.textThermoMin);
        final SeekBar thermoMin = (SeekBar)dlgView.findViewById(R.id.sliderThermoMin);
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
        thermoMin.setProgress(rule.getMin() + RuleRow.THERMO_OFFSET);
        
        final TextView textMax = (TextView)dlgView.findViewById(R.id.textThermoMax);
        final SeekBar thermoMax = (SeekBar)dlgView.findViewById(R.id.sliderThermoMax);
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
        thermoMax.setProgress(rule.getMax() + RuleRow.THERMO_OFFSET);
        

        final Button btnCancel = (Button)dlgView.findViewById(R.id.buttonCancel);
        btnCancel.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                dlgView.dismiss();
            }
        });
        
        final Button btnOK = (Button)dlgView.findViewById(R.id.buttonDone);
        btnOK.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                SeekBar tp = (SeekBar)dlgView.findViewById(R.id.sliderThermoMin);
                int min = tp.getProgress();
                tp = (SeekBar)dlgView.findViewById(R.id.sliderThermoMax);
                int max = tp.getProgress();

                rule.setRange(min - RuleRow.THERMO_OFFSET, max - RuleRow.THERMO_OFFSET);
                rulePort.putRule(rowKey, rule);
                dlgView.dismiss();
            }
        });

        dlgView.show();
        
        return dlgView;
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
                final int usage = (int)(ids & 0x0f);
                final RuleRow rule = rulePort.getRule(nRowKey);
                
                if (rule == null) {
                    LogUtil.e("NO ROW : %d", nRowKey);
                    return;
                }
                
                rule.setID(ids);
                if (usage == USAGE_TIME) {
                    spinCondition.setVisibility(View.GONE);
                    btnCondition.setVisibility(View.VISIBLE);
                    btnCondition.setText(rule.getTimeString());
                    btnCondition.setOnClickListener(new OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            Dialog dlg = onClickTime(v, rulePort, nRowKey, ids);
                            dlg.setOnDismissListener(new OnDismissListener() {
                                @Override
                                public void onDismiss(DialogInterface dialog) {
                                    btnCondition.setText(rule.getTimeString());
                                }
                            });
                        }
                    });
                } else if (usage == ZigBeeNode.TYPE_INPUT_ANALOG){
                    spinCondition.setVisibility(View.GONE);
                    btnCondition.setVisibility(View.VISIBLE);
                    btnCondition.setText(rule.getThermoString());
                    btnCondition.setOnClickListener(new OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            Dialog dlg = onClickThermo(v, rulePort, nRowKey, ids);
                            dlg.setOnDismissListener(new OnDismissListener() {
                                @Override
                                public void onDismiss(DialogInterface dialog) {
                                    btnCondition.setText(rule.getThermoString());
                                }
                            });
                        }
                    });
                } else {
                    spinCondition.setVisibility(View.VISIBLE);
                    btnCondition.setVisibility(View.GONE);
                    rule.setRange(0, 0);
                    

                    ArrayAdapter<CharSequence> adapterCondition = ArrayAdapter.createFromResource(ActivityRuleConfig.this, R.array.config_rule_condition_gpio, R.layout.config_spinner); 
                    spinCondition.setAdapter(adapterCondition);
                    
                    spinCondition.setOnItemSelectedListener(new OnItemSelectedListener() {
                        @Override
                        public void onItemSelected(AdapterView<?> parent,
                                View view, int position, long id) {
                            RuleRow rule = rulePort.getRule(nRowKey);
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

        ArrayAdapter<CharSequence> adapterOperator = ArrayAdapter.createFromResource(ActivityRuleConfig.this, R.array.config_rule_operator, R.layout.config_spinner); 
        spinOperator.setAdapter(adapterOperator);
        spinOperator.setOnItemSelectedListener(new OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view,
                    int position, long id) {
                RuleRow rule = rulePort.getRule(nRowKey);
                if (rule == null)
                    rule = new RuleRow(ids);
                rule.setOP(position);
                rulePort.putRule(nRowKey, rule);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
            
        });
        
        tblInput.addView(tblRow);

        rulePort.putRow(nRowKey, tblRow);
        rulePort.putRule(nRowKey, new RuleRow(ids));
        rulePort.incRowKey();
        rulePort.redrawRules();

        return tblRow;
    }
    
    private void createRuleTable(int pos) {
        if (mListOutputID.size() < (pos + 1))
            return;
        
        int id = mListOutputID.get(pos);
        
        LinearLayout layoutContainer = (LinearLayout)findViewById(R.id.container);
        layoutContainer.removeAllViewsInLayout();
        
        LinearLayout.LayoutParams llp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        llp.setMargins(10, 10, 10, 10);
        
        LinearLayout.LayoutParams llpBlank = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 60);
        llpBlank.setMargins(10, 50, 10, 50);
        
        int mask = ~(0x0f << 4);
        for (int k = 0; k < 3; k++) {
            id = (id & mask) | (k << 4);

            LinearLayout layoutPort = (LinearLayout)LayoutInflater.from(this).inflate(R.layout.config_rule_port, null);
            TextView text = (TextView)layoutPort.findViewById(R.id.textGpioAction);
            text.setText(R.string.config_rule_action_off + k);
            final RulePort rulePort = new RulePort(id);
            mListRulePort.put(id, rulePort);

            // rule row
            final TableLayout tblInput = (TableLayout)LayoutInflater.from(this).inflate(R.layout.config_rule_input_header, null);
            final Button      btnPlus  = (Button)tblInput.findViewById(R.id.buttonPlus);
            btnPlus.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    addRow(rulePort, tblInput, mAdapterInput);
                }
            });
            
            layoutPort.addView(tblInput);
            layoutContainer.addView(layoutPort, llp);
            
            if (k < 2) {
                LinearLayout blankLayout = new LinearLayout(this);
                blankLayout.setBackgroundColor(0x7f00007f);
                layoutContainer.addView(blankLayout, llpBlank);
            }
        }
    }
    
    
    private ArrayAdapter<String> mAdapterInput = null;
    private ArrayAdapter<String> mAdapterOutput = null;
    
    private void createScreen() {
        int nCtr = mApp.getNodeCtr();
        
        List<String> listInputs = new ArrayList<String>();
        List<String> listOutputs = new ArrayList<String>();
        for (int i = 0; i < nCtr; i++) {
            ZigBeeNode node = mApp.getNode(i);
            for (int j = 0; j < node.getMaxGPIO(); j++) {
                int usage = node.getGpioUsage(j);
                if (ZigBeeNode.TYPE_INPUT_TOUCH <= usage && usage <= ZigBeeNode.TYPE_INPUT_ANALOG) {
                    listInputs.add(node.getGpioName(j));
                    int id = (i << 16) | (j << 8) | usage;
                    mListInputID.add((Integer)(id));
                } else if (usage == ZigBeeNode.TYPE_OUTPUT_LIGHT) {
                    listOutputs.add(node.getGpioName(j));
                    int id = (i << 16) | (j << 8) | usage;
                    mListOutputID.add((Integer)(id));
                }
            }
        }
        listInputs.add(getResources().getString(R.string.config_rule_input_time));
        mListInputID.add((Integer)(USAGE_TIME));

        mAdapterInput  = new ArrayAdapter<String>(this, R.layout.config_spinner, listInputs);
        mAdapterOutput = new ArrayAdapter<String>(this, R.layout.config_spinner, listOutputs);
        
        final Spinner spinPort = (Spinner)findViewById(R.id.spinnerGpioOutput);
        spinPort.setAdapter(mAdapterOutput);
        spinPort.setOnItemSelectedListener(new OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view,
                    int position, long id) {
                createRuleTable(position);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
            
        });
        createRuleTable(0);
    }


    
    /*
    ***************************************************************************
    * 
    ***************************************************************************
    */

}
