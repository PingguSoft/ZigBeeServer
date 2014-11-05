package com.pinggusoft.zigbee_server;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import android.app.ActionBar;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Messenger;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
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
import com.pinggusoft.zigbee_server.ServerApp;
import com.pinggusoft.zigbee_server.R;

public class ActivityRuleConfig extends Activity {
    private ServerApp           mApp;
    private ZigBeeNode          mNode = null;
    private Vector <TableLayout> mListPortOutTable = new Vector <TableLayout>();
    private Vector <TableLayout> mListPortInTable  = new Vector <TableLayout>();
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
        mListInputID.add((Integer)(0xff));
        
        LinearLayout.LayoutParams llp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        llp.setMargins(10, 10, 10, 60);

        
        ArrayAdapter<String> adapterInput         = new ArrayAdapter<String>(this, R.layout.config_spinner, listInputs);
        
        
        for (int i = 0; i < nCtr; i++) {
            ZigBeeNode node = mApp.getNode(i);
            for (int j = 0; j < node.getMaxGPIO(); j++) {
                int usage = node.getGpioUsage(j);
                
                if (usage == ZigBeeNode.TYPE_OUTPUT_LIGHT) {
                    LinearLayout layoutPort = (LinearLayout)LayoutInflater.from(this).inflate(R.layout.config_rule_port, null);
                    
                        TableLayout tl = (TableLayout)LayoutInflater.from(this).inflate(R.layout.config_rule_output_row, null);
                        TextView text = (TextView)tl.findViewById(R.id.textGpioOutput);
                        text.setText(node.getGpioName(j));
                        layoutPort.addView(tl);
                        mListPortOutTable.add(tl);
                        
                        tl = (TableLayout)LayoutInflater.from(this).inflate(R.layout.config_rule_input_header, null);
                        final Spinner spinInput     = (Spinner)tl.findViewById(R.id.spinnerGpioInput);
                        final Spinner spinCondition = (Spinner)tl.findViewById(R.id.spinnerCondition);
                        final Spinner spinOperator  = (Spinner)tl.findViewById(R.id.spinnerOperator);
                        final Button  btnCondition  = (Button)tl.findViewById(R.id.buttonRangeTime);

                        int spinID = (i << 16) | (j << 8);
                        spinInput.setId(spinID);
                        spinInput.setAdapter(adapterInput);
                        spinInput.setOnItemSelectedListener(new OnItemSelectedListener() {
                            @Override
                            public void onItemSelected(AdapterView<?> parent,
                                    View view, int position, long id) {
                                int ids = mListInputID.get(position);
                                int usage = (int)(ids & 0xff);
                                if (usage == 0xff) {
                                    spinCondition.setVisibility(View.GONE);
                                    btnCondition.setVisibility(View.VISIBLE);
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
                        
                        Button btnPlus  = (Button)tl.findViewById(R.id.buttonPlus);
                        Button btnMinus = (Button)tl.findViewById(R.id.buttonMinus);
                        layoutPort.addView(tl);
                        mListPortInTable.add(tl);

                        //ImageView iv = (ImageView)LayoutInflater.from(this).inflate(R.layout.config_rule_seperator, null);
                        //iv.setBackgroundResource(R.drawable.shape_seperator);
                        //layoutPort.addView(iv,llp);
                    
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
