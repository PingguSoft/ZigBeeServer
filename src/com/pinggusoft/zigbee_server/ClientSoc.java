package com.pinggusoft.zigbee_server;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;

public class ClientSoc {
    private Thread  mThread = null;
    private Boolean mBoolRun = Boolean.valueOf(true);
    
    private Runnable mClientSoc = new Runnable() {
        @Override
        public void run() {
            try {
                InetAddress serverAddr = InetAddress.getByName("192.168.18.101");
                
                LogUtil.d("C : Connecting...");
                Socket socket = new Socket(serverAddr, 4444);
               
                String message = "Hello from Client";
                try {
                    LogUtil.d("C : Sending : '" + message + "'");
                    PrintWriter out = new PrintWriter(new BufferedWriter(
                        new OutputStreamWriter(socket.getOutputStream())), true);
                    out.println(message);
                    LogUtil.d("C : Sent.");
                    LogUtil.d("C : Done.");
                   
                    BufferedReader in = new BufferedReader(
                        new InputStreamReader(socket.getInputStream()));
                   
                    String str = in.readLine();
                    LogUtil.d(str);
                    String str1 = in.readLine();
                    LogUtil.d(str1);
                    String str2 = in.readLine();
                    LogUtil.d(str2);
                } catch (Exception e) {
                    LogUtil.e("C : Error");
                } finally {
                    socket.close();
                }
            } catch (IOException e) {
                LogUtil.e("C : Error");
            }
        }
    };
    
    public ClientSoc() {
        mThread = new Thread(mClientSoc);
        mThread.start();
    }
    
    public void stop() {
        mBoolRun = Boolean.valueOf(false);
    }
}
