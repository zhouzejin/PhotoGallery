package com.sunny.photogallery;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

public class StartupReceiver extends BroadcastReceiver {
	
	private static final String TAG = "StartupReceiver";

	/**
	 * onReceive(Context,Intent)方法同样运行在主线程上，
	 * 因此不能在该方法内做一些耗时的重度任务，如网络连接或数据的永久存储等。
	 */
	@Override
	public void onReceive(Context context, Intent intent) {
		Log.i(TAG, "Received broadcast intent: " + intent.getAction());
		
		// 设备重启后启动定时器
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
		boolean isOn = prefs.getBoolean(PollService.PREF_IS_ALARM_ON, false);
		PollService.setServiceAlarm(context, isOn);
	}

}
