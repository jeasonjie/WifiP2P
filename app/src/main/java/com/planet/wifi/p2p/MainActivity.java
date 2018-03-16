package com.planet.wifi.p2p;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.NetworkInfo;
import android.net.wifi.WpsInfo;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.NumberPicker;
import android.widget.TextView;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements  WifiP2pManager.PeerListListener, WifiP2pManager.ConnectionInfoListener{

    private final IntentFilter intentFilter = new IntentFilter();
    WifiP2pManager.Channel mChannel;
    WifiP2pManager mManager;
    NumberPicker picker;
    Button button;
    TextView textView;
    private final String TAG = "MainActivity";
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION.equals(action)) {//可以判断当前 Wifi P2P是否可用
                Log.e(TAG,"WIFI_P2P_STATE_CHANGED_ACTION");
            } else if (WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION.equals(action)) {//意味设备周围的可用设备列表发生了变化
                Log.e(TAG,"WIFI_P2P_PEERS_CHANGED_ACTION");
                if (mManager != null) {
                    mManager.requestPeers(mChannel, MainActivity.this);
                }
                Log.e(TAG, "P2P peers changed");
            } else if (WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION.equals(action)) {//意味着 Wifi P2P 的连接状态发生了变化，可能是连接到了某设备，或者是与某设备断开了连接
                Log.e(TAG,"WIFI_P2P_CONNECTION_CHANGED_ACTION");
                if (mManager == null) {
                    return;
                }
                NetworkInfo networkInfo = (NetworkInfo) intent
                        .getParcelableExtra(WifiP2pManager.EXTRA_NETWORK_INFO);

                if (networkInfo.isConnected()) {
                    mManager.requestConnectionInfo(mChannel, MainActivity.this);//获取连接的设备信息
                    Log.e(TAG, "已连接p2p设备");
                }
            } else if (WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION.equals(action)) {//可以获取到本设备变化后的设备信息
                Log.e(TAG,"WIFI_P2P_THIS_DEVICE_CHANGED_ACTION");
                WifiP2pDevice device = (WifiP2pDevice) intent.getParcelableExtra(WifiP2pManager.EXTRA_WIFI_P2P_DEVICE);
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);
        mManager = (WifiP2pManager) getSystemService(Context.WIFI_P2P_SERVICE);
        mChannel = mManager.initialize(this, getMainLooper(), null);
        picker = (NumberPicker) findViewById(R.id.pick);
        picker.setDescendantFocusability(NumberPicker.FOCUS_BLOCK_DESCENDANTS);
        peersName = new String[1];
        peersName[0]="No Devices";
        picker.setDisplayedValues(peersName);

        button = (Button) findViewById(R.id.button);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int num = picker.getValue();
                connect(num);
            }
        });
        textView = (TextView) findViewById(R.id.text);
    }

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(mReceiver, intentFilter);
        //client start 客户端如下代码，用于查找可连接的设备
        //请求discoverPeers查找设备，发现设备会进入WIFI_P2P_PEERS_CHANGED_ACTION
//        mManager.discoverPeers(mChannel, new WifiP2pManager.ActionListener() {
//
//            @Override
//            public void onSuccess() {
//                Log.e(TAG,"onSuccess");
//            }
//
//            @Override
//            public void onFailure(int reasonCode) {
//                Log.e(TAG,"onFailure");
//            }
//        });
        // client end

        //service start 服务器时如下代码
        //测试为了简便，服务器主动创建一个group供客户端发送消息
        //默认为系统会自己创建一个group和群组成员
        mManager.createGroup(mChannel, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                Log.e(TAG, "createGroup onSuccess");
            }

            @Override
            public void onFailure(int reason) {
                Log.e(TAG, "createGroup onFailure: " + reason);
            }
        });

        startService(new Intent(this, WifiServerService.class));
        //service end
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(mReceiver);
    }
    private List<WifiP2pDevice> peers = new ArrayList<>();
    private String[] peersName;

    @Override
    public void onPeersAvailable(WifiP2pDeviceList peerList) {
        //展现所有的可连接设备列表
        peers.clear();
        peers.addAll(peerList.getDeviceList());
        if (peers != null) {
            if (peers.size() == 0) {
                Log.e(TAG, "No devices found");
                textView.setVisibility(View.INVISIBLE);
                if (peersName.length>0){
                    peersName[0]="No Devices";
                }else {
                    peersName = new String[1];
                    peersName[0]="No Devices";
                }
                return;
            }else{
                peersName = new String[peers.size()];
                int i=0;
                for(WifiP2pDevice device: peers){
                    peersName[i++]=device.deviceName;
                }
                textView.setVisibility(View.VISIBLE);
                textView.setText("(avaliable)");
            }
        }
        Log.e(TAG, "size: " + peersName.length);
        picker.setDisplayedValues(peersName);
        if(peersName.length > 1) {
            picker.setMinValue(0);
            picker.setMaxValue(peersName.length - 1);
        }
    }

    /**
     * 连接选中设备
     * @param num
     */
    public void connect(final int num) {
        // Picking the first device found on the network.
        WifiP2pDevice device = peers.get(num);

        WifiP2pConfig config = new WifiP2pConfig();
        config.deviceAddress = device.deviceAddress;
        config.wps.setup = WpsInfo.PBC;

        //尝试连接设备.连接成功会进入WIFI_P2P_CONNECTION_CHANGED_ACTION
        mManager.connect(mChannel, config, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                Log.e(TAG,"connect sucess");
            }

            @Override
            public void onFailure(int reason) {
                Log.e(TAG,"connect fail");
            }
        });
    }

    @Override
    public void onConnectionInfoAvailable(WifiP2pInfo info) {
        //获取的设备信息回调
        textView.setVisibility(View.VISIBLE);
        textView.setText("(connected)");
        // InetAddress from WifiP2pInfo struct.

        InetAddress groupOwnerAddress = info.groupOwnerAddress;

        Log.e(TAG,"onConnectionInfoAvailable");
        Log.e(TAG,info.toString());
        Log.e(TAG, groupOwnerAddress.getHostAddress());//服务器设备的ip
        if (info.groupFormed && info.isGroupOwner) {
//            startService(new Intent(this, WifiServerService.class));
            Log.e(TAG, "isGroupOwner");//是群主
        } else if (info.groupFormed) {
            new WifiClientTask().execute(groupOwnerAddress.getHostAddress());
            Log.e(TAG, "groupFormed");//是成员
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mManager.clearLocalServices(mChannel, null);

        mManager.clearServiceRequests(mChannel, null);
    }

    private class WifiClientTask extends AsyncTask<String, Object, Object> {

        @Override
        protected Object doInBackground(String... objects) {
            String str = "你好啊";
            Socket socket = null;
            OutputStream outputStream = null;
//            InputStream is = null;
            try {
                //测试效果，简单的发送一个string串，可以演变成发送file（FileOutputStream）
                Log.e(TAG, "ip: " + objects[0]);
                socket = new Socket();
                socket.bind(null);
                socket.connect(new InetSocketAddress(InetAddress.getByName(objects[0]), 9999));
                OutputStream os=socket.getOutputStream();//字节输出流
                PrintWriter pw=new PrintWriter(os);//将输出流包装为打印流
                pw.write(str);
                pw.flush();
                socket.shutdownOutput();//关闭输出流
                //3.获取输入流，并读取服务器端的响应信息
//                InputStream is=socket.getInputStream();
//                BufferedReader br=new BufferedReader(new InputStreamReader(is));
//                String info=null;
//                while((info=br.readLine())!=null){
//                    System.out.println("我是客户端，服务器说："+info);
//                }
                //4.关闭资源
//                br.close();
//                is.close();
                pw.close();
                os.close();
                socket.close();

            } catch (IOException e) {
                e.printStackTrace();
            }

            return null;
        }
    }
}
