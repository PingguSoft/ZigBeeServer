package com.pinggusoft.httpserver;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.http.HttpException;
import org.apache.http.impl.DefaultConnectionReuseStrategy;
import org.apache.http.impl.DefaultHttpResponseFactory;
import org.apache.http.impl.DefaultHttpServerConnection;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.BasicHttpProcessor;
import org.apache.http.protocol.HttpRequestHandlerRegistry;
import org.apache.http.protocol.HttpService;
import org.apache.http.protocol.ResponseConnControl;
import org.apache.http.protocol.ResponseContent;
import org.apache.http.protocol.ResponseDate;
import org.apache.http.protocol.ResponseServer;

import com.pinggusoft.zigbee_server.LogUtil;
import com.pinggusoft.zigbee_server.ServerApp;
import com.pinggusoft.zigbee_server.ServerService;
import com.pinggusoft.zigbee_server.ServerServiceUtil;

import android.app.NotificationManager;
import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.os.Messenger;

public class HTTPServer extends Thread {
    private static final String SERVER_NAME = "AndWebServer";
    private static final String ALL_PATTERN = "*";
    private static final String MESSAGE_PATTERN = "/message*";
    private static final String FOLDER_PATTERN = "/dir*";
    private static final String RPC_PATTERN = "/json*";
    
    private boolean isRunning = false;
    private Context context = null;
    private int serverPort = 0;
    
    private BasicHttpProcessor httpproc = null;
    private BasicHttpContext httpContext = null;
    private HttpService httpService = null;
    private HttpRequestHandlerRegistry registry = null;
    private NotificationManager notifyManager = null;
    private ServerSocket serverSocket = null;
    private ServerServiceUtil   mService = null;
    
    private Lock            mLockACK  = null;
    private Condition       mLockCond = null;
    
    public HTTPServer(Context context, NotificationManager notifyManager){
        super(SERVER_NAME);
        
        this.setContext(context);
        this.setNotifyManager(notifyManager);

        httpproc    = new BasicHttpProcessor();
        httpContext = new BasicHttpContext();
        
        httpproc.addInterceptor(new ResponseDate());
        httpproc.addInterceptor(new ResponseServer());
        httpproc.addInterceptor(new ResponseContent());
        httpproc.addInterceptor(new ResponseConnControl());

        httpService = new HttpService(httpproc, 
                                        new DefaultConnectionReuseStrategy(),
                                        new DefaultHttpResponseFactory());
        
        mLockACK  = new ReentrantLock();
        mLockCond = mLockACK.newCondition();
        
        LogUtil.d("WEBSERVER IS CREATED !!");
        mService = new ServerServiceUtil(context, new Messenger(new ServiceHandler(this)));
        registry = new HttpRequestHandlerRegistry();
        registry.register(ALL_PATTERN, new HomePageHandler(context));
        registry.register(RPC_PATTERN, new RPCHandler(context, mService, mLockACK, mLockCond));
        registry.register(MESSAGE_PATTERN, new MessageCommandHandler(context, notifyManager));
        registry.register(FOLDER_PATTERN, new FolderCommandHandler(context, serverPort));
        httpService.setHandlerResolver(registry);
    }
    
    @Override
    public void run() {
        super.run();
        
        try {
            LogUtil.d("WEBSERVER IS STARTED !!");
            serverPort = ((ServerApp)context).getServerPort();
            LogUtil.d("SERVER PORT :" + serverPort);
            
            serverSocket = new ServerSocket(serverPort);
            serverSocket.setReuseAddress(true);
            
            while(isRunning){
                try {
                    final Socket socket = serverSocket.accept();
                    
                    DefaultHttpServerConnection serverConnection = new DefaultHttpServerConnection();
                    serverConnection.bind(socket, new BasicHttpParams());
                    httpService.handleRequest(serverConnection, httpContext);
                    
                    serverConnection.shutdown();
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (HttpException e) {
                    e.printStackTrace();
                }
            }
            
            serverSocket.close();
            LogUtil.d("WEBSERVER IS STOPPED !!");
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    public synchronized void startThread() {
        if (isRunning)
            return;
        
        isRunning = true;
        super.start();
    }
    
    public synchronized void stopThread(){
        if (!isRunning)
            return;
        
        if (serverSocket != null && !serverSocket.isClosed()) {
            try {
                LogUtil.d("SERVER SOCKET CLOSE !!");
                serverSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if (mService != null)
            mService.unbind();
        
        isRunning = false;
    }

    public void setNotifyManager(NotificationManager notifyManager) {
        this.notifyManager = notifyManager;
    }

    public NotificationManager getNotifyManager() {
        return notifyManager;
    }

    public void setContext(Context context) {
        this.context = context;
    }

    public Context getContext() {
        return context;
    }
    
    static class ServiceHandler extends Handler {
        private WeakReference<HTTPServer>    mParent;
        
        ServiceHandler(HTTPServer parent) {
            mParent = new WeakReference<HTTPServer>(parent);
        }

        @Override
        public void handleMessage(Message msg) {
            final HTTPServer parent = mParent.get();
            
            switch (msg.what) {
            case ServerService.CMD_READ_GPIO:
            case ServerService.CMD_WRITE_GPIO:
            case ServerService.CMD_READ_ANALOG:
                if (parent.mLockACK != null && parent.mLockCond != null){
                    parent.mLockACK.lock();
                    parent.mLockCond.signal();
                    parent.mLockACK.unlock();
                }
                break;
            }
        }
    }
}
