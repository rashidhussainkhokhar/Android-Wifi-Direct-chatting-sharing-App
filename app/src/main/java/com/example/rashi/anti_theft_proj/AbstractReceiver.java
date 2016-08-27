package com.example.rashi.anti_theft_proj;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.preference.PreferenceManager;


public class AbstractReceiver extends AsyncTask<Void, Message, Void>{
	
	@Override
	protected Void doInBackground(Void... params) {
		return null;
	}
	
	protected void playNotification(Context context, Message message){
		Uri notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

		Intent intent = new Intent(context, MainActivity.class);
		intent.setAction(Intent.ACTION_MAIN);
		intent.addCategory(Intent.CATEGORY_LAUNCHER);

		PendingIntent pIntent = PendingIntent.getActivity(context, 0, intent, 0);
		
		Notification mNotification = new Notification.Builder(context)
			.setContentTitle(message.getChatName())
			.setContentText(message.getmText())
			.setSmallIcon(R.drawable.icon_notification)
			.setContentIntent(pIntent)
			.setSound(notification)			
			.build();

		NotificationManager mNotificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
		
		mNotification.flags |= Notification.FLAG_AUTO_CANCEL;
		mNotification.defaults |= Notification.DEFAULT_VIBRATE;
		
		//Check for foregroung
		SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);
		if(!pref.getBoolean("isForeground", false)){
			mNotificationManager.notify(0, mNotification);
		}
	}
}
