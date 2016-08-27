package com.example.rashi.anti_theft_proj;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;

//import com.example.rashi.anti_theft_proj.MainActivity;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.NetworkInfo;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.WifiP2pManager.Channel;
import android.net.wifi.p2p.WifiP2pManager.ConnectionInfoListener;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

/*
 * This class implements the Singleton pattern
 */
public class WifiDirectBroadcastReceiver extends BroadcastReceiver{
	public static final int IS_OWNER = 1;
	public static final int IS_CLIENT = 2;
	private static final String TAG = "WifiDirectBroadcastReceiver";
	
	private WifiP2pManager mManager;
	private Channel mChannel;
	private Activity mActivity;
	private List<String> peersName = new ArrayList<String>();
	private List<WifiP2pDevice> peers = new ArrayList<WifiP2pDevice>();
	private int isGroupeOwner;
	private InetAddress ownerAddr;
	
	private static WifiDirectBroadcastReceiver instance;
	
	private WifiDirectBroadcastReceiver(){
		super();
	}
	
	public static WifiDirectBroadcastReceiver createInstance(){
		if(instance == null){
			instance = new WifiDirectBroadcastReceiver();
		}
		return instance;
	}

	public int isGroupeOwner() { return isGroupeOwner; }
	public InetAddress getOwnerAddr() { return ownerAddr; }
	public void setmManager(WifiP2pManager mManager) { this.mManager = mManager; }
	public void setmChannel(Channel mChannel) { this.mChannel = mChannel; }
	public void setmActivity(Activity mActivity) { this.mActivity = mActivity; }

	@Override
	public void onReceive(Context context, Intent intent) {
		String action = intent.getAction();

		 //Wifi P2P is enabled or disabled

		if(action.equals(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION)){
			
			//check if Wifi P2P is supported
			int state = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE,-1);
			if(state == WifiP2pManager.WIFI_P2P_STATE_ENABLED){
				Toast.makeText(mActivity, "Your device is supported", Toast.LENGTH_SHORT).show();
			} else{
				Toast.makeText(mActivity, "Your device is not supported", Toast.LENGTH_SHORT).show();
			}
		}

		 //Available peer list has changed

		else if(action.equals(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION)){ 
//			Log.v(TAG, "WIFI_P2P_PEERS_CHANGED_ACTION");
		}
		

		else if(action.equals(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION)){ 
			//Log.v(TAG, "WIFI_P2P_THIS_DEVICE_CHANGED_ACTION");
			
		}

		else if(action.equals(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION)){
		//	Log.v(TAG, "WIFI_P2P_CONNECTION_CHANGED_ACTION");
			
			if(mManager == null){
				return;
			}
			NetworkInfo networkInfo = (NetworkInfo) intent.getParcelableExtra(WifiP2pManager.EXTRA_NETWORK_INFO);
			if(networkInfo.isConnected()){
				mManager.requestConnectionInfo(mChannel, new ConnectionInfoListener() {
					
					@Override
					public void onConnectionInfoAvailable(WifiP2pInfo info) {
						InetAddress groupOwnerAddress = info.groupOwnerAddress;
						ownerAddr= groupOwnerAddress;
						

						// create a server thread
						if (info.groupFormed && info.isGroupOwner) { 
							isGroupeOwner = IS_OWNER;	
							activateGoToChat("server");
						}

						else if (info.groupFormed) { 
							isGroupeOwner = IS_CLIENT;		
							activateGoToChat("client");
						}	
					}
				});				
			}
		}
	}
	
	public void activateGoToChat(String role){
		if(mActivity.getClass() == MainActivity.class){
			((MainActivity)mActivity).getGoToChat().setText("Start the chat "+role);
			((MainActivity)mActivity).getGoToChat().setVisibility(View.VISIBLE);
			((MainActivity)mActivity).getSetChatName().setVisibility(View.VISIBLE);
			((MainActivity)mActivity).getSetChatNameLabel().setVisibility(View.VISIBLE);
			((MainActivity)mActivity).getDisconnect().setVisibility(View.VISIBLE);
			((MainActivity)mActivity).getGoToSettings().setVisibility(View.GONE);
			((MainActivity)mActivity).getGoToSettingsText().setVisibility(View.GONE);
		}
	}

}
