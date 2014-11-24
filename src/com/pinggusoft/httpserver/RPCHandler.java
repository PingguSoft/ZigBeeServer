package com.pinggusoft.httpserver;

import java.io.IOException;

import java.io.OutputStream;
import java.io.OutputStreamWriter;

import net.minidev.json.JSONArray;

import org.apache.http.HttpEntity;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.entity.ContentProducer;
import org.apache.http.entity.EntityTemplate;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpRequestHandler;
import org.apache.http.util.EntityUtils;


import java.text.*;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import com.pinggusoft.zigbee_server.LogUtil;
import com.pinggusoft.zigbee_server.RuleManager;
import com.pinggusoft.zigbee_server.RuleOutput;
import com.pinggusoft.zigbee_server.ServerApp;
import com.pinggusoft.zigbee_server.ServerService;
import com.pinggusoft.zigbee_server.ServerServiceUtil;
import com.pinggusoft.zigbee_server.ZigBeeNode;
import com.thetransactioncompany.jsonrpc2.*;
import com.thetransactioncompany.jsonrpc2.server.*;

import android.content.Context;
import android.net.Uri;

public class RPCHandler implements HttpRequestHandler {
    private Context mContext = null;
    private Dispatcher mDispatcher;
    private JSONRPC2Response resp = null;
    private ServerServiceUtil mService = null;
    private Lock            mLockACK  = null;
    private Condition       mLockCond = null;
    
    public RPCHandler(Context context, ServerServiceUtil service, Lock lock, Condition cond){
        this.mContext = context;
        
        mLockACK    = lock;
        mLockCond   = cond;
        mService    = service;
        mDispatcher = new Dispatcher();
        mDispatcher.register(new EchoHandler());
        mDispatcher.register(new DateTimeHandler());
        mDispatcher.register(new getNode_Handler());
        mDispatcher.register(new asyncRead_Handler());
        mDispatcher.register(new asyncWrite_Handler());
        mDispatcher.register(new getRule_Handler());
        
    }

    @Override
    public void handle(HttpRequest request, HttpResponse response, HttpContext httpContext) throws HttpException, IOException {
        
        if (request instanceof HttpEntityEnclosingRequest) {
            HttpEntity entity = ((HttpEntityEnclosingRequest) request).getEntity();
            if (entity != null) {
                String strRequest = EntityUtils.toString(entity, "UTF-8");
//                LogUtil.d("SERVER RECEIVED : " + strRequest);
                entity.consumeContent();
                
                JSONRPC2Request req = null;
                try {
                    req = JSONRPC2Request.parse(strRequest);
                } catch (JSONRPC2ParseException e) {
                    e.printStackTrace();
                }
                resp = mDispatcher.process(req, null);
            }
        }
        
        final String contentType = "application/json";
        HttpEntity entity = new EntityTemplate(new ContentProducer() {
            public void writeTo(final OutputStream outstream) throws IOException {
                OutputStreamWriter writer = new OutputStreamWriter(outstream, "UTF-8");

                if (resp != null)
                    writer.write(resp.toJSONString());
                writer.flush();
            }
        });
        
        ((EntityTemplate)entity).setContentType(contentType);
        response.setEntity(entity);
    }
    
    private byte[] getByteArray(JSONArray array) {
        byte[] buf = new byte[array.size()];
        for (int i = 0; i < array.size(); i++) {
            Long val = (Long)array.get(i);
            buf[i] = val.byteValue();
        }
        
        return buf;
    }
    
    private JSONArray getJSONArray(byte[] byteBuf) {
        JSONArray array = new JSONArray();
        for (int i = 0; i < byteBuf.length; i++) {
            array.add(i, byteBuf[i]);
        }
        return array;
    }
    
    /*
     ******************************************************************************************************************
     * 
     ******************************************************************************************************************
     */    
    public class getRule_Handler implements RequestHandler {

        public String[] handledRequests() {
            return new String[]{"getRuleCtr", "getRule", "setRule", "fileRule" };
        }
        
        public JSONRPC2Response process(JSONRPC2Request req, MessageContext ctx) {
            if (req.getMethod().equals("getRuleCtr")) {
                int val = RuleManager.getOutputPortCnt();
                return new JSONRPC2Response(val, req.getID());
            } else if (req.getMethod().equals("getRule")) {
                Map<String, Object> params = req.getNamedParams();
                int idx = ((Long)params.get("idx")).intValue();

                RuleOutput output = RuleManager.getAt(idx);
                Map<String, Object> param = new HashMap<String,Object>();
                param.put("rule", output.serialize());
                
                return new JSONRPC2Response(param, req.getID());
            }  else if (req.getMethod().equals("setRule")) {
                Map<String, Object> params = req.getNamedParams();
                byte[] buf = getByteArray((JSONArray)params.get("rule"));

                RuleOutput output = new RuleOutput();
                output.deserialize(buf);
                RuleManager.put(output.getID(), output);
                mService.startRuleChecking();
                return new JSONRPC2Response(output.getID(), req.getID());
            }  else if (req.getMethod().equals("fileRule")) {
                Map<String, Object> params = req.getNamedParams();
                int opt = ((Long)params.get("save")).intValue();
                if (opt == 1)
                    RuleManager.save(mContext);
                else
                    RuleManager.load(mContext);
                return new JSONRPC2Response(opt, req.getID());
            } else {
                return new JSONRPC2Response(JSONRPC2Error.METHOD_NOT_FOUND, req.getID());
            }
        }
    }
    
    
    public class getNode_Handler implements RequestHandler {

        public String[] handledRequests() {
            return new String[]{"getNodeCtr", "getNode"};
        }
        
        public JSONRPC2Response process(JSONRPC2Request req, MessageContext ctx) {
            if (req.getMethod().equals("getNodeCtr")) {
                ServerApp app = (ServerApp)mContext;
                int val       = app.getNodeCtr();

                return new JSONRPC2Response(val, req.getID());
            } else if (req.getMethod().equals("getNode")) {
                Map<String, Object> params = req.getNamedParams();
                int idx = ((Long)params.get("idx")).intValue();
                
                ServerApp app = (ServerApp)mContext;
                ZigBeeNode node = app.getNode(idx);
                
                Map<String, Object> param = new HashMap<String,Object>();
                param.put("node", node.serialize());
                
                return new JSONRPC2Response(param, req.getID());
            } else {
                return new JSONRPC2Response(JSONRPC2Error.METHOD_NOT_FOUND, req.getID());
            }
        }
    }
    
    public class asyncRead_Handler implements RequestHandler {

        public String[] handledRequests() {
            return new String[]{"asyncReadGpio", "asyncReadAnalog"};
        }
        
        public JSONRPC2Response process(JSONRPC2Request req, MessageContext ctx) {
            int  id;
            int  gpio;
            int  idx;
            ZigBeeNode node;
            
            if (req.getMethod().equals("asyncReadGpio")) {
                Map<String, Object> params = req.getNamedParams();
                
                id   = ((Long)params.get("id")).intValue();
                gpio = ZigBeeNode.getGpioFromID(id);
                idx  = ZigBeeNode.getNIDFromID(id);
                node = ((ServerApp)mContext).getNode((int)idx);
                mService.asyncReadGpio(id, node);
                
                mLockACK.lock();
                boolean boolRet = false;
                try {
                    boolRet = mLockCond.await(300, TimeUnit.MILLISECONDS);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                mLockACK.unlock();
                
                if (boolRet) {
                    int  val  = node.getGpioValue(gpio);
                    return new JSONRPC2Response(val, req.getID());
                }
                return new JSONRPC2Response(JSONRPC2Error.INTERNAL_ERROR, req.getID());
            } else if (req.getMethod().equals("asyncReadAnalog")) {
                Map<String, Object> params = req.getNamedParams();
                
                id   = ((Long)params.get("id")).intValue();
                gpio = ZigBeeNode.getGpioFromID(id);
                idx  = ZigBeeNode.getNIDFromID(id);
                node = ((ServerApp)mContext).getNode((int)idx);
                mService.asyncReadAnalog((int)id, node);
                
                mLockACK.lock();
                boolean boolRet = false;
                try {
                    boolRet = mLockCond.await(300, TimeUnit.MILLISECONDS);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                mLockACK.unlock();
                
                if (boolRet) {
                    int  val  = node.getGpioAnalog(gpio);
                    return new JSONRPC2Response(val, req.getID());
                }
                return new JSONRPC2Response(JSONRPC2Error.INTERNAL_ERROR, req.getID());
            } else {
                return new JSONRPC2Response(JSONRPC2Error.METHOD_NOT_FOUND, req.getID());
            }
        }
    }
    
    public class asyncWrite_Handler implements RequestHandler {

        public String[] handledRequests() {
            return new String[]{"asyncWriteGpio"};
        }
        
        public JSONRPC2Response process(JSONRPC2Request req, MessageContext ctx) {
            int  id;
            int  value;
            int  gpio;
            int  idx;
            ZigBeeNode node;
            
            if (req.getMethod().equals("asyncWriteGpio")) {
                Map<String, Object> params = req.getNamedParams();

                id    = ((Long)params.get("id")).intValue();
                value = ((Long)params.get("value")).intValue();
                gpio = ZigBeeNode.getGpioFromID(id);
                idx  = ZigBeeNode.getNIDFromID(id);
                node  = ((ServerApp)mContext).getNode((int)idx);
                mService.asyncWriteGpio((int)id, (int)value, node);
                
                mLockACK.lock();
                boolean boolRet = false;
                try {
                    boolRet = mLockCond.await(300, TimeUnit.MILLISECONDS);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                mLockACK.unlock();
                
                if (boolRet) {
                    return new JSONRPC2Response(value, req.getID());
                }
                return new JSONRPC2Response(JSONRPC2Error.INTERNAL_ERROR, req.getID());
            } 
            else {
                return new JSONRPC2Response(JSONRPC2Error.METHOD_NOT_FOUND, req.getID());
            }
        }
    } 
    
    
    
    
    
    
    
    
    public class EchoHandler implements RequestHandler {

        public String[] handledRequests() {
            return new String[]{"echo"};
        }
        
        public JSONRPC2Response process(JSONRPC2Request req, MessageContext ctx) {
            if (req.getMethod().equals("echo")) {
                Map<String, Object> params = req.getNamedParams();
                Object input = params.get("arg1");
                return new JSONRPC2Response(input, req.getID());
            }
            else {
                return new JSONRPC2Response(JSONRPC2Error.METHOD_NOT_FOUND, req.getID());
            }
        }
    }
    
    public class DateTimeHandler implements RequestHandler {

        public String[] handledRequests() {
            return new String[]{"getDate", "getTime"};
        }
        
        public JSONRPC2Response process(JSONRPC2Request req, MessageContext ctx) {
            if (req.getMethod().equals("getDate")) {
                DateFormat df = DateFormat.getDateInstance();
                String date = df.format(new Date());
                return new JSONRPC2Response(date, req.getID());
            }
            else if (req.getMethod().equals("getTime")) {
                DateFormat df = DateFormat.getTimeInstance();
                String time = df.format(new Date());
                return new JSONRPC2Response(time, req.getID());
            }
            else {
                return new JSONRPC2Response(JSONRPC2Error.METHOD_NOT_FOUND, req.getID());
            }
        }
    }
}
