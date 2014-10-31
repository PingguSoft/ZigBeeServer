package com.pinggusoft.httpserver;

//The Client sessions package
import com.pinggusoft.zigbee_server.LogUtil;
import com.thetransactioncompany.jsonrpc2.client.*;

//The Base package for representing JSON-RPC 2.0 messages
import com.thetransactioncompany.jsonrpc2.*;

//The JSON Smart package for JSON encoding/decoding (optional)
import net.minidev.json.*;

//For creating URLs
import java.net.*;
import java.util.HashMap;
import java.util.Map;


public class RPCClient extends Thread {
    
    private Object callRPC(JSONRPC2Session session, String strMethod, Map<String, Object> param, int id)
    {
        JSONRPC2Request req;
        
        if (param == null)
            req = new JSONRPC2Request(strMethod, id);
        else
            req = new JSONRPC2Request(strMethod, param, id);

        JSONRPC2Response response = null;
        try {
            response = session.send(req);
        } catch (JSONRPC2SessionException e) {
            LogUtil.d(e.getMessage());
        }
        if (response.indicatesSuccess())
            LogUtil.d("CLIENT RECEIVED : " + response.getResult());
        else
            LogUtil.d(response.getError().getMessage());
        
        return response.getResult();
    }

    @Override
    public void run() {
        URL serverURL = null;

        try {
            //serverURL = new URL("http://jsonrpc.example.com:8080");
            serverURL = new URL("http://127.0.0.1:8080/json");

        } catch (MalformedURLException e) {
        }

        JSONRPC2Session mySession = new JSONRPC2Session(serverURL);

        callRPC(mySession, "getDate", null, 0);
        callRPC(mySession, "getTime", null, 0);
        
        Map<String, Object> echoParam = new HashMap<String,Object>();
        echoParam.put("arg1", "Hello world!");
        callRPC(mySession, "echo", echoParam, 0);
    }
    
    public synchronized void startThread() {
        super.start();
    }
}
