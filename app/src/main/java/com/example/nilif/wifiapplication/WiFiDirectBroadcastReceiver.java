package com.example.nilif.wifiapplication;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.NetworkInfo;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pManager;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by nilif on 2016/5/10.
 */
public class WiFiDirectBroadcastReceiver extends BroadcastReceiver {
    private WifiP2pManager mManager;
    private WifiP2pManager.Channel mchannel;
    private MyWiFiActivity myWiFiActivity;
    private WifiP2pManager.PeerListListener myPeerListListener;
    private List peers = new ArrayList();


    public WiFiDirectBroadcastReceiver(WifiP2pManager manager, WifiP2pManager.Channel channel,
                                       MyWiFiActivity activity) {
        super();
        this.mManager = manager;
        this.mchannel = channel;
        this.myWiFiActivity = activity;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();

        if (WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION.equals(action)) {
            // 检查wifi是否可用，并通知设备
            int state = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, -1);
            if (state == WifiP2pManager.WIFI_P2P_STATE_ENABLED) {
                //wifi P2P is Enable
                myWiFiActivity.setIsWifiP2pEnabled(true);
            } else {
                //wifi P2P is not Enable
                myWiFiActivity.setIsWifiP2pEnabled(false);
                myWiFiActivity.resetData();
            }
            Log.d(MyWiFiActivity.TAG,"P2P state changed - " + state);
        } else if (WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION.equals(action)) {
            // 回调WifiP2pManager.requestPeers(),返回一个当前的热点列表
            if (mManager != null){
                Log.e(MyWiFiActivity.TAG, "onReceive: " );
               /* mManager.requestPeers(mchannel,(WifiP2pManager.PeerListListener) myWiFiActivity
                        .getFragmentManager()
                        .findFragmentById(R.id.frag_list));*/
            }
            Log.d(MyWiFiActivity.TAG, "P2P peers changed");
        } else if (WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION.equals(action)) {
            // Respond to new connection or disconnections
            if (mManager == null) {
                return;
            }
            NetworkInfo networtInfo = (NetworkInfo) intent.getParcelableExtra(WifiP2pManager.EXTRA_NETWORK_INFO);
            if (networtInfo.isConnected()){
                DeviceDetailFragment fragment = (DeviceDetailFragment) myWiFiActivity
                        .getFragmentManager()
                        .findFragmentById(R.id.frag_detail);
                mManager.requestConnectionInfo(mchannel,fragment);
            }else {
                myWiFiActivity.resetData();
            }
        } else if (WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION.equals(action)) {
            // Respond to this device's wifi state changing

            DeviceListFragment fragment = (DeviceListFragment) myWiFiActivity.getFragmentManager().findFragmentById(R.id.frag_list);
            fragment.updateThisDevice((WifiP2pDevice) intent
                    .getParcelableExtra(WifiP2pManager.EXTRA_WIFI_P2P_DEVICE));
        }
    }
}
