package com.planet.wifi.p2p;

import android.app.IntentService;
import android.content.Intent;
import android.support.annotation.Nullable;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Created by Lee on 2018/3/15.
 */

public class WifiServerService extends IntentService {
    private static final String TAG = "WifiServerService";

    public WifiServerService() {
        super("WifiServerService");
        Log.e(TAG, "WifiServerService");
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.e(TAG, "onCreate()");
    }

    @Override
    protected void onHandleIntent(@Nullable Intent intent) {
        ServerSocket serverSocket = null;
        InputStream is = null;
        InputStreamReader isr = null;
        BufferedReader br = null;

        try {
            serverSocket = new ServerSocket();
            serverSocket.setReuseAddress(true);
            serverSocket.bind(new InetSocketAddress(9999));
            Socket socket = serverSocket.accept();
            Log.e(TAG, "客户端IP地址 : " + socket.getInetAddress().getHostAddress());

            //获取输入流，并读取客户端信息
            is = socket.getInputStream();
            isr = new InputStreamReader(is);
            br = new BufferedReader(isr);
            String info=null;
            while((info=br.readLine())!=null){//循环读取客户端的信息
                Log.e(TAG, "我是服务器，客户端说："+info);
            }
            socket.shutdownInput();//关闭输入流
            //获取输出流，响应客户端的请求
//            os = socket.getOutputStream();
//            pw = new PrintWriter(os);
//            pw.write("欢迎您！");
//            pw.flush();//调用flush()方法将缓冲输出

            br.close();
            isr.close();
            is.close();
            serverSocket.close();
            serverSocket = null;
            is = null;
            isr = null;
            br = null;
        } catch (IOException e) {
            e.printStackTrace();
        } finally {

            //再次启动服务，等待客户端下次连接
            startService(new Intent(this, WifiServerService.class));
        }

    }
}
