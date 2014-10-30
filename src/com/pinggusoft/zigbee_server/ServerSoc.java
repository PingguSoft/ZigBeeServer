package com.pinggusoft.zigbee_server;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.ServerSocket;
import java.net.Socket;

public class ServerSoc {
    public static final int SERVERPORT = 4444;
    private Thread  mThread = null;
    private Boolean mBoolRun = Boolean.valueOf(true);
    
    
    private Runnable mServerSoc = new Runnable() {
        ServerSocket serverSocket = null;
        
        @Override
        public void run() {
            try {
                LogUtil.d("S : Connecting...");
                serverSocket = new ServerSocket(SERVERPORT);
                
                while (mBoolRun) {
                    Socket client = serverSocket.accept();
                    LogUtil.d("S : Receiving...");
                   
                    try {
                        BufferedReader in = new BufferedReader(
                            new InputStreamReader(client.getInputStream()));
                        String str = in.readLine();
                        LogUtil.d("S : Receied : '" + str + "'");
                        BufferedWriter out = new BufferedWriter(
                            new OutputStreamWriter(client.getOutputStream()));
                        out.write("echo1 : " + str + "\n");
                        out.flush();
                        out.write("echo2 : " + str + "\n");
                        out.flush();
                        out.write("echo3 : " + str + "\n");
                        out.flush();                   
                    } catch (Exception e) {
                        LogUtil.e("S : Error");
                        e.printStackTrace();
                    } finally {
                        client.close();
                        LogUtil.d("S : Done");
                    }
                }
                
                serverSocket.close();
            } catch (Exception e) {
                LogUtil.d("S : Error");
                e.printStackTrace();
            }
        }
    };
    
    public ServerSoc() {
        mThread = new Thread(mServerSoc);
        mThread.start();
    }
    
    public void stop() {
        mBoolRun = Boolean.valueOf(false);
    }
}
