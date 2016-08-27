package com.example.rashi.anti_theft_proj;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.WifiP2pManager.Channel;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.PopupMenu;
import android.widget.PopupMenu.OnMenuItemClickListener;
import android.widget.Toast;



public class ChatActivity extends Activity {
	private static final String TAG = "ChatActivity";	
	private static final int PICK_IMAGE = 1;
	private static final int TAKE_PHOTO = 2;
	private static final int RECORD_AUDIO = 3;
	private static final int RECORD_VIDEO = 4;;
	private static final int DOWNLOAD_IMAGE = 100;
	private static final int DELETE_MESSAGE = 101;
	private static final int DOWNLOAD_FILE = 102;

	
	private WifiP2pManager mManager;
	private Channel mChannel;
	private WifiDirectBroadcastReceiver mReceiver;
	private IntentFilter mIntentFilter;
	private EditText edit;
	private static ListView listView;
	private static List<Message> listMessage;
	private static ChatAdapter chatAdapter;
	private Uri fileUri;
	private String fileURL;
	private ArrayList<Uri> tmpFilesUri;
	
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_chat);
		
		mManager = (WifiP2pManager) getSystemService(Context.WIFI_P2P_SERVICE);
        mChannel = mManager.initialize(this, getMainLooper(), null);
        mReceiver = WifiDirectBroadcastReceiver.createInstance();
        mReceiver.setmActivity(this);
        
        mIntentFilter = new IntentFilter();
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);
        
        //Start the service to receive
        startService(new Intent(this, MessageService.class));
        
        //Initialize the adapter for the chat
        listView = (ListView) findViewById(R.id.messageList);
        listMessage = new ArrayList<Message>();
        chatAdapter = new ChatAdapter(this, listMessage);
        listView.setAdapter(chatAdapter);
        
        //Initialize the list of temporary files URI
        tmpFilesUri = new ArrayList<Uri>();
        
		//Send a message
        Button button = (Button) findViewById(R.id.sendMessage);
        edit = (EditText) findViewById(R.id.editMessage);
        button.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View arg0) {
				if(!edit.getText().toString().equals("")){
					Log.v(TAG, "Send message");
					sendMessage(Message.TEXT_MESSAGE);
				}				
				else{
					Toast.makeText(ChatActivity.this, "Please enter a nonempty message", Toast.LENGTH_SHORT).show();
				}
			}
		});
        
        //Register the context menu to the list view (for pop up menu)
        registerForContextMenu(listView);
	}
	

	
	@Override
    public void onResume() {
        super.onResume();
        registerReceiver(mReceiver, mIntentFilter);        
        
		mManager.discoverPeers(mChannel, new WifiP2pManager.ActionListener() {
					
			@Override
			public void onSuccess() {
				Log.v(TAG, "Discovery process succeeded");
			}
			
			@Override
			public void onFailure(int reason) {
				Log.v(TAG, "Discovery process failed");
			}
		});
		saveStateForeground(true);
    }

    @Override
    public void onPause() {
        super.onPause();
        unregisterReceiver(mReceiver);
        saveStateForeground(false);
    }    
    
	@Override
	public void onBackPressed() {
		AlertDialog.Builder newDialog = new AlertDialog.Builder(this);
		newDialog.setTitle("Close chatroom");
		newDialog.setMessage("Are you sure you want to close this chatroom?\n"
				+ "You will no longer be able to receive messages, and");
		
		newDialog.setPositiveButton("Yes", new DialogInterface.OnClickListener() {

			@Override
			public void onClick(DialogInterface dialog, int which) {
				clearTmpFiles(getExternalFilesDir(null));
				if (MainActivity.server != null) {
					MainActivity.server.interrupt();
				}
				android.os.Process.killProcess(android.os.Process.myPid());
			}

		});
		
		newDialog.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {

			@Override
			public void onClick(DialogInterface dialog, int which) {
				dialog.cancel();
			}
		});
		
		newDialog.show();
	}
    
    @Override
	protected void onDestroy() {
		super.onDestroy();
		clearTmpFiles(getExternalFilesDir(null));
	}


    @Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		
		switch(requestCode){
			case PICK_IMAGE:
				if (resultCode == RESULT_OK && data.getData() != null) {
					fileUri = data.getData();
					sendMessage(Message.IMAGE_MESSAGE);					
				}
				break;
			case TAKE_PHOTO:
				if (resultCode == RESULT_OK && data.getData() != null) {
					fileUri = data.getData();
					sendMessage(Message.IMAGE_MESSAGE);
					tmpFilesUri.add(fileUri);
				}
				break;
			case RECORD_AUDIO:
				if (resultCode == RESULT_OK) {
					fileURL = (String) data.getStringExtra("audioPath");
					sendMessage(Message.AUDIO_MESSAGE);
				}
				break;
			case RECORD_VIDEO:
				if (resultCode == RESULT_OK) {
					fileUri = data.getData();
					fileURL = MediaFile.getRealPathFromURI(this, fileUri);
					sendMessage(Message.VIDEO_MESSAGE);
				}
				break;


		}
	}
	
	// Hydrate Message object then launch the AsyncTasks to send it
	public void sendMessage(int type){
		Log.v(TAG, "Send message starts");
		// Message written in EditText is always sent
		Message mes = new Message(type, edit.getText().toString(), null, MainActivity.chatName);
		
		switch(type){
			case Message.IMAGE_MESSAGE:
				Image image = new Image(this, fileUri);
				Log.v(TAG, "Bitmap from url ok");
				mes.setByteArray(image.bitmapToByteArray(image.getBitmapFromUri()));				
				mes.setFileName(image.getFileName());
				mes.setFileSize(image.getFileSize());				
				Log.v(TAG, "Set byte array to image ok");
				break;
			case Message.AUDIO_MESSAGE:
				MediaFile audioFile = new MediaFile(this, fileURL, Message.AUDIO_MESSAGE);
				mes.setByteArray(audioFile.fileToByteArray());
				mes.setFileName(audioFile.getFileName());
				mes.setFilePath(audioFile.getFilePath());
				break;
			case Message.VIDEO_MESSAGE:
				MediaFile videoFile = new MediaFile(this, fileURL, Message.AUDIO_MESSAGE);
				mes.setByteArray(videoFile.fileToByteArray());
				mes.setFileName(videoFile.getFileName());
				mes.setFilePath(videoFile.getFilePath());
				tmpFilesUri.add(fileUri);
				break;


		}		

		if(mReceiver.isGroupeOwner() == WifiDirectBroadcastReceiver.IS_OWNER){
			new SendMessageServer(ChatActivity.this, true).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, mes);
		}
		else if(mReceiver.isGroupeOwner() == WifiDirectBroadcastReceiver.IS_CLIENT){

			new SendMessageClient(ChatActivity.this, mReceiver.getOwnerAddr()).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, mes);
		}		
		
		edit.setText("");
	}
	
	// Refresh the message list
	public static void refreshList(Message message, boolean isMine){
		Log.v(TAG, "Refresh message list starts");
		
		message.setMine(isMine);
		
		listMessage.add(message);
    	chatAdapter.notifyDataSetChanged();
    	
    	Log.v(TAG, "Chat Adapter notified of the changes");
    	
    	//Scroll to the last element of the list
    	listView.setSelection(listMessage.size() - 1);
    }	

	// Save the app's state (foreground or background) to a SharedPrefereces
	public void saveStateForeground(boolean isForeground){
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
  		Editor edit = prefs.edit();
  		edit.putBoolean("isForeground", isForeground);
  		edit.commit();
	}
	
	@Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.chat, menu);
        return true;
    }

	// Handle click on the menu
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int idItem = item.getItemId();
        switch(idItem){
	        case R.id.send_image:
	        	showPopup(edit);
	        	return true;
	        	
	        case R.id.send_audio:
	        	Log.v(TAG, "Start activity to record audio");
	        	startActivityForResult(new Intent(this, RecordAudioActivity.class), RECORD_AUDIO);
	        	return true;
	        	
	        case R.id.send_video:
	        	Log.v(TAG, "Start activity to record video");
	        	Intent takeVideoIntent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
	        	takeVideoIntent.putExtra(MediaStore.EXTRA_VIDEO_QUALITY, 0);
	        	if (takeVideoIntent.resolveActivity(getPackageManager()) != null) {
	                startActivityForResult(takeVideoIntent, RECORD_VIDEO);
	            }
	        	return true;
	        	

	        default:
	        	return super.onOptionsItemSelected(item);        	
        }  
    }	
    
    //Show the popup menu
    public void showPopup(View v) {
        PopupMenu popup = new PopupMenu(this, v);
        popup.setOnMenuItemClickListener(new OnMenuItemClickListener() {
			
			@Override
			public boolean onMenuItemClick(MenuItem item) {
				switch(item.getItemId()){
				case R.id.pick_image:
					Log.v(TAG, "Pick an image");
					Intent intent = new Intent(Intent.ACTION_PICK);
					intent.setType("image/*");
					intent.setAction(Intent.ACTION_GET_CONTENT);
					
					// Prevent crash if no app can handle the intent
					if (intent.resolveActivity(getPackageManager()) != null) {
						startActivityForResult(intent, PICK_IMAGE);
				    }
					break;
				
				case R.id.take_photo:
					Log.v(TAG, "Take a photo");
					Intent intent2 = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
					
					if (intent2.resolveActivity(getPackageManager()) != null) {
						startActivityForResult(intent2, TAKE_PHOTO);
				    }				    
				    break;
				}
				return true;
			}
		});
        popup.inflate(R.menu.send_image);
        popup.show();
    }
    
    //Create pop up menu for image download, delete message, etc...
    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        menu.setHeaderTitle("Options");
        
        AdapterContextMenuInfo info = (AdapterContextMenuInfo) menuInfo;
        Message mes = listMessage.get((int) info.position);
        
        //Option to delete message independently of its type
        menu.add(0, DELETE_MESSAGE, Menu.NONE, "Delete message");
        

        
        int type = mes.getmType();
        switch(type){
        	case Message.IMAGE_MESSAGE:
        		menu.add(0, DOWNLOAD_IMAGE, Menu.NONE, "Download image");
        		break;

        	case Message.AUDIO_MESSAGE:
        		menu.add(0, DOWNLOAD_FILE, Menu.NONE, "Download audio file");
        		break;
        	case Message.VIDEO_MESSAGE:
        		menu.add(0, DOWNLOAD_FILE, Menu.NONE, "Download video file");
        		break;
        }
    }
    
    //Handle click event on the pop up menu
    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
        
        switch (item.getItemId()) {
            case DOWNLOAD_IMAGE:
            	downloadImage(info.id);
                return true;
                
            case DELETE_MESSAGE:
            	deleteMessage(info.id);
            	return true;
            	


            	
            default:
                return super.onContextItemSelected(item);
        }
    }
    
    //Download image and save it to Downloads
    public void downloadImage(long id){  
    	Message mes = listMessage.get((int) id);
    	Bitmap bm = mes.byteArrayToBitmap(mes.getByteArray());    	
    	String path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getAbsolutePath();
    	
    	FileUtilities.saveImageFromBitmap(this, bm, path, mes.getFileName());
    	FileUtilities.refreshMediaLibrary(this);
    }
    

    
    //Delete a message from the message list (doesn't delete on other phones)
    public void deleteMessage(long id){
    	listMessage.remove((int) id);
    	chatAdapter.notifyDataSetChanged();
    }
    
    private void clearTmpFiles(File dir){
    	File[] childDirs = dir.listFiles();	
    	for(File child : childDirs){
    		if(child.isDirectory()){
    			clearTmpFiles(child);
    		}
    		else{
    			child.delete();
    		}
    	}
    	for(Uri uri: tmpFilesUri){
    		getContentResolver().delete(uri, null, null);
    	}
    	FileUtilities.refreshMediaLibrary(this);
    }
    
    public void talkTo(String destination){
    	edit.setText("@" + destination + " : ");
    	edit.setSelection(edit.getText().length());
    }
    



}