package com.tony.wifip2p.activity;

import android.content.Intent;
import android.net.Uri;
import android.net.wifi.WpsInfo;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import java.io.BufferedInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collection;

import com.tony.wifip2p.bean.FileBean;

import com.tony.wifip2p.R;
import com.tony.wifip2p.utils.FileUtils;
import com.tony.wifip2p.utils.Md5Util;

/**
 * 发送文件界面
 *
 * 1、搜索设备信息
 * 2、选择设备连接服务端组群信息
 * 3、选择要传输的文件路径
 * 4、把该文件通过socket发送到服务端
 */
public class SendFileActivity extends BaseActivity implements View.OnClickListener {

    private static final String TAG = "SendFileActivity";
    private ListView mTvDevice;
    private ArrayList<String> mListDeviceName = new ArrayList();
    private ArrayList<WifiP2pDevice> mListDevice = new ArrayList<>();
    private AlertDialog mDialog;

    @Override
    void netChangeListen(boolean isWifi, String tip) {

    }

    @Override
    void connectCallBack(boolean isSuccess) {

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_send_file);
        Button mBtnChoseFile = (Button) findViewById(R.id.btn_chosefile);
        Button mBtnConnectServer = (Button) findViewById(R.id.btn_connectserver);
        mTvDevice = (ListView) findViewById(R.id.lv_device);

        mBtnChoseFile.setOnClickListener(this);
        mBtnConnectServer.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_chosefile:
                chooseFile();
                break;
            case R.id.btn_connectserver:
                mDialog = new AlertDialog.Builder(this, R.style.Transparent).create();
                mDialog.show();
                mDialog.setCancelable(false);
                mDialog.setContentView(R.layout.loading_progressba);
                //搜索设备
                connectServer();
                break;
            default:
                break;

        }
    }
    //----------------------------------------------Socket-----------------------------------------
    private Socket mMsgSocket;
    private ReadThread readThread = null;
    public static final int MSG_PORT = 1989;
    public  void  sendMsg(String msg ){


        if (mMsgSocket == null){

            return;
        }

        try {

            DataOutputStream writer = new DataOutputStream( mMsgSocket.getOutputStream());
            writer.writeUTF("send msg to client socket");

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    //启动一个线程，一直读取从服务端发送过来的消息
    private class ReadThread extends Thread {
        @Override
        public void run() {
            while (true) {
                try {
                    Log.e(TAG,"ReadThread");
                    BufferedInputStream bis = new BufferedInputStream(mMsgSocket.getInputStream());
                    byte[] data = new byte[1024];
                    int size = 0;

                    //收到客服端发送的消息后，返回一个消息给客户端
                    while((size = bis.read(data)) != -1) {
                        String str = new String(data, 0, size);
                       Log.e("TAG","socket msg =>"+str);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    break;
                }
            }
        }
    }

    @Override
    public void onConnection(WifiP2pInfo wifiP2pInfo) {
        super.onConnection(wifiP2pInfo);
        Log.e("TAG","sSend onConnection");
        new Thread(){

            @Override
            public void run() {
                super.run();
                connectServerWithTCPSocket();
            }
        }.start();

    }

    public  void connectServerWithTCPSocket() {
        Log.e(TAG,"connectServerWithTCPSocket");
        try {// 创建一个Socket对象，并指定服务端的IP及端口号

            mMsgSocket = new Socket();
            InetSocketAddress inetSocketAddress = new InetSocketAddress( mWifiP2pInfo.groupOwnerAddress.getHostAddress(), MSG_PORT);
            Log.e(TAG,"connect  .....");
            mMsgSocket.connect(inetSocketAddress);
            //通过Socket实例获取输入输出流，作为和服务器交换数据的通道
            Log.e(TAG,"connect  .... success.");
            readThread = new   ReadThread();
            readThread.start();

        } catch (UnknownHostException e) {
            e.printStackTrace();
            Log.e(TAG,"exception->"+e.toString());
        } catch (IOException e) {
            e.printStackTrace();
            Log.e(TAG,"exception->"+e.toString());
        }

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mMsgSocket!=null)
        {
            try {
                mMsgSocket.close();
                if (readThread!=null){

                    readThread.interrupt();
                    readThread = null;

                }

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void sendMsg(View view) {

        new  Thread(){

            @Override
            public void run() {
                super.run();
                sendMsg("");
            }
        }.start();


    }
    /**
     * 搜索设备
     */
    protected void connectServer() {
        mWifiP2pManager.discoverPeers(mChannel, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                // WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION 广播，此时就可以调用 requestPeers 方法获取设备列表信息
                Log.e(TAG, "搜索设备成功");
            }

            @Override
            public void onFailure(int reasonCode) {
                Log.e(TAG, "搜索设备失败");
            }
        });
    }

    /**
     * 连接设备
     */
    protected void connect(WifiP2pDevice wifiP2pDevice) {
        WifiP2pConfig config = new WifiP2pConfig();
        if (wifiP2pDevice != null) {
            config.deviceAddress = wifiP2pDevice.deviceAddress;
            config.wps.setup = WpsInfo.PBC;
            mWifiP2pManager.connect(mChannel, config, new WifiP2pManager.ActionListener() {
                @Override
                public void onSuccess() {
                    Log.e(TAG, "连接成功");
                    Toast.makeText(SendFileActivity.this, "连接成功", Toast.LENGTH_SHORT).show();
                  //  connectServerWithTCPSocket();
                }

                @Override
                public void onFailure(int reason) {
                    Log.e(TAG, "连接失败");
                    Toast.makeText(SendFileActivity.this, "连接失败", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }


    /**
     * 客户端进行选择文件
     */
    private void chooseFile() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        startActivityForResult(intent, 10);
    }

    /**
     * 客户端选择文件回调
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 10) {
            if (resultCode == RESULT_OK) {
                Uri uri = data.getData();
                if (uri != null) {
                    String path = FileUtils.getAbsolutePath(this, uri);
                    if (path != null) {
                        final File file = new File(path);
                        if (!file.exists() || mWifiP2pInfo == null) {
                            Toast.makeText(SendFileActivity.this,"文件路径找不到",Toast.LENGTH_SHORT).show();
                            return;
                        }
                        String md5 = Md5Util.getMd5(file);
                        FileBean fileBean = new FileBean(file.getPath(), file.length(), md5);
                        String hostAddress = mWifiP2pInfo.groupOwnerAddress.getHostAddress();
                        new SendTask(SendFileActivity.this, fileBean).execute(hostAddress);
                    }
                }
            }
        }
    }

    @Override
    public void onPeersInfo(Collection<WifiP2pDevice> wifiP2pDeviceList) {
        super.onPeersInfo(wifiP2pDeviceList);

        for (WifiP2pDevice device : wifiP2pDeviceList) {
            if (!mListDeviceName.contains(device.deviceName) && !mListDevice.contains(device)) {
                mListDeviceName.add("设备：" + device.deviceName + "----" + device.deviceAddress);
                mListDevice.add(device);
            }
        }

        //进度条消失
        mDialog.dismiss();
        showDeviceInfo();
    }


    /**
     * 展示设备信息
     */
    private void showDeviceInfo() {
        ArrayAdapter<String> adapter = new ArrayAdapter(this, android.R.layout.simple_list_item_1, mListDeviceName);
        mTvDevice.setAdapter(adapter);
        mTvDevice.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                WifiP2pDevice wifiP2pDevice = mListDevice.get(i);
                connect(wifiP2pDevice);
            }
        });
    }


}
