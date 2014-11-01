package com.pinggusoft.httpserver;

import java.text.*;
import java.util.*;

import com.pinggusoft.zigbee_server.LogUtil;
import com.thetransactioncompany.jsonrpc2.*;
import com.thetransactioncompany.jsonrpc2.server.*;

public class RPCServer {


    // Implements a handler for an "echo" JSON-RPC method
    public static class EchoHandler implements RequestHandler {
    
    
        // Reports the method names of the handled requests
        public String[] handledRequests() {
        
            return new String[]{"echo"};
        }
        
        
        // Processes the requests
        public JSONRPC2Response process(JSONRPC2Request req, MessageContext ctx) {
            
            if (req.getMethod().equals("echo")) {
                
                // Echo first parameter
                
                Map<String, Object> params = req.getNamedParams();
            
                Object input = params.get("arg1");
            
                return new JSONRPC2Response(input, req.getID());
            }
            else {
                // Method name not supported
                
                return new JSONRPC2Response(JSONRPC2Error.METHOD_NOT_FOUND, req.getID());
            }
        }
    }
    
    
    // Implements a handler for "getDate" and "getTime" JSON-RPC methods
    // that return the current date and time
    public static class DateTimeHandler implements RequestHandler {
    
    
        // Reports the method names of the handled requests
        public String[] handledRequests() {
        
            return new String[]{"getDate", "getTime"};
        }
        
        
        // Processes the requests
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
            
                // Method name not supported
                
                return new JSONRPC2Response(JSONRPC2Error.METHOD_NOT_FOUND, req.getID());
            }
        }
    }
    
    public void test() {
        // Create a new JSON-RPC 2.0 request dispatcher
        Dispatcher dispatcher =  new Dispatcher();
        
        
        // Register the "echo", "getDate" and "getTime" handlers with it
        dispatcher.register(new EchoHandler());
        dispatcher.register(new DateTimeHandler());
        
        // Simulate an "echo" JSON-RPC 2.0 request
        Map<String, Object> echoParam = new HashMap<String,Object>();
        
        echoParam.put("arg1", "Hello world!");
        byte b[] = new byte[20];
        for (int i = 0; i < b.length; i++)
            b[i] = (byte)i;
        echoParam.put("arg2", b);
        JSONRPC2Request req = new JSONRPC2Request("echo", echoParam, "req-id-01");
        LogUtil.d("Request: " + req);
        
        JSONRPC2Response resp = dispatcher.process(req, null);
        LogUtil.d("Response: " + resp);
        
        
        // Simulate a "getDate" JSON-RPC 2.0 request
        req = new JSONRPC2Request("getDate", "req-id-02");
        LogUtil.d("Request: " + req);
        
        resp = dispatcher.process(req, null);
        LogUtil.d("Response: " + resp);
        
        
        // Simulate a "getTime" JSON-RPC 2.0 request
        req = new JSONRPC2Request("getTime", "req-id-03");
        LogUtil.d("Request: " + req);
        
        resp = dispatcher.process(req, null);
        LogUtil.d("Response: " + resp);

    }
}
