package com.pinggusoft.httpserver;

import java.io.IOException;

import java.io.OutputStream;
import java.io.OutputStreamWriter;

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

import com.pinggusoft.zigbee_server.LogUtil;
import com.thetransactioncompany.jsonrpc2.*;
import com.thetransactioncompany.jsonrpc2.server.*;

import android.content.Context;
import android.net.Uri;

public class RPCHandler implements HttpRequestHandler {
    private Context context = null;
    private Dispatcher mDispatcher;
    private JSONRPC2Response resp = null;
    
    public RPCHandler(Context context){
        this.context = context;
        
        // Create a new JSON-RPC 2.0 request dispatcher
        mDispatcher =  new Dispatcher();
        
        
        // Register the "echo", "getDate" and "getTime" handlers with it
        mDispatcher.register(new EchoHandler());
        mDispatcher.register(new DateTimeHandler());
    }

    @Override
    public void handle(HttpRequest request, HttpResponse response, HttpContext httpContext) throws HttpException, IOException {
        
        if (request instanceof HttpEntityEnclosingRequest) {
            HttpEntity entity = ((HttpEntityEnclosingRequest) request).getEntity();
            if (entity != null) {
                String strRequest = EntityUtils.toString(entity, "UTF-8");
                LogUtil.d("SERVER RECEIVED : " + strRequest);
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
    
    
    
    /*
     ******************************************************************************************************************
     * 
     ******************************************************************************************************************
     */    
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
}
