package com.sunny.photogallery;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/**
 * 既然该receiver负责发送通知信息，并接收其他接收者返回的结果码，它的运行应该总是在最后。
 * 这就需要将receiver的优先级设置为最低。为保证receiver最后运行，设置其优先级值为999（1000及以下值属系统保留值）。
 * 另外，既然NotificationReceiver仅限PhotoGallery应用内部使用，
 * 还需设置属性值为android:exported="false"，以保证其对外部应用不可见。
 */
public class NotificationReceiver extends BroadcastReceiver {
	
	private static final String TAG = "NotificationReceiver";

	@Override
	public void onReceive(Context context, Intent intent) {
		Log.i(TAG, "Received result: " + getResultCode());
		
		// 应用在前台，取消发送通知
		if (getResultCode() != Activity.RESULT_OK)
			// A foreground activity cancelled the broadcast
			return;
		
		int requestCode = intent.getIntExtra("REQUEST_CODE", 0);
		Notification notification = intent.getParcelableExtra("NOTIFICATION");
		
		// 否则，发送通知
		NotificationManager nm = (NotificationManager) 
				context.getSystemService(Context.NOTIFICATION_SERVICE);
		nm.notify(requestCode, notification);
	}

}
