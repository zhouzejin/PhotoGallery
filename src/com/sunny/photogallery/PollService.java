package com.sunny.photogallery;

import java.util.ArrayList;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.IntentService;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.net.ConnectivityManager;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

public class PollService extends IntentService {
	
	public static final String PREF_IS_ALARM_ON = "is_alarm_on";
	public static final String ACTION_SHOW_NOTIFICATION = 
			"com.sunny.photogallery.SHOW_NOTIFICATION";
	public static final String PREM_PRIVATE = 
			"com.sunny.photogallery.PRIVATE";
	
	private static final String TAG = "PollService";
	
	private static final int POLL_INTERVAL = 1000 * 60 * 1; // 1 minute
	
	public static void setServiceAlarm(Context context, boolean isOn) {
		Intent intent = new Intent(context, PollService.class);
		PendingIntent pendingIntent = PendingIntent
				.getService(context, 0, intent, 0);
		
		// 利用定时器间断性地启动服务
		AlarmManager alarmManager = (AlarmManager) 
				context.getSystemService(Context.ALARM_SERVICE);
		
		if (isOn) {
			alarmManager.setRepeating(AlarmManager.RTC, 
					System.currentTimeMillis(), POLL_INTERVAL, pendingIntent);
		} else {
			alarmManager.cancel(pendingIntent);
			pendingIntent.cancel();
		}
		
		// 添加定时器状态preference
		PreferenceManager.getDefaultSharedPreferences(context)
			.edit()
			.putBoolean(PREF_IS_ALARM_ON, isOn)
			.commit();
	}
	
	public static boolean isServiceAlarmOn(Context context) {
		Intent intent = new Intent(context, PollService.class);
		PendingIntent pi = PendingIntent.getService(
				context, 0, intent, PendingIntent.FLAG_NO_CREATE);
		return pi != null;
	}
	
	public PollService() {
		super(TAG);
	}

	@Override
	protected void onHandleIntent(Intent intent) {
		Log.i(TAG, "Received an intent: " + intent);
		
		// 检查后台网络的可用性
		ConnectivityManager cm = (ConnectivityManager) 
				getSystemService(Context.CONNECTIVITY_SERVICE);
		@SuppressWarnings("deprecation")
		// getActiveNetworkInfo()方法，还需获取ACCESS_NETWORK_STATE权限
		boolean isNerworkAvailable = cm.getBackgroundDataSetting() &&
				cm.getActiveNetworkInfo() != null;
		if (!isNerworkAvailable)
			return;
		
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
		String query = prefs.getString(DoubanFetcher.PREF_SEARCH_QUERY, null);
		String lastResultID = prefs.getString(DoubanFetcher.PREF_LAST_RESULT_ID, null);
		
		DoubanFetcher fetcher = new DoubanFetcher();
		ArrayList<GalleryItem> items = new ArrayList<GalleryItem>();
		
		if (query != null) {
			items = fetcher.search(query, "", 0, 5);
		} else {
			for (int i = DoubanFetcher.BOOK_INDEX; i < DoubanFetcher.BOOK_INDEX + 
					PhotoGalleryFragment.BOOK_NUM; i++) {
				GalleryItem item = fetcher.fetchItem(i);
				items.add(item);
			}
		}
		
		if (items.size() == 0)
			return;
		
		String resultId = items.get(0).getId();
		if (!resultId.equals(lastResultID)) {
			Log.i(TAG, "Got a new result: " + resultId);
			
			// 使用通知信息（notification） 实现让PollService通知新结果信息给用户。
			Resources resources = getResources();
			PendingIntent pi = PendingIntent
					.getActivity(this, 0, new Intent(this, PhotoGalleryActivity.class), 0);
			
			Notification notification = new NotificationCompat.Builder(this)
				.setTicker(resources.getString(R.string.new_pictures_title))
				.setSmallIcon(android.R.drawable.ic_menu_report_image)
				.setContentTitle(resources.getString(R.string.new_pictures_title))
				.setContentText(resources.getString(R.string.new_pictures_text))
				.setContentIntent(pi)
				.setAutoCancel(true)
				.build();
			
			/*NotificationManager notificationManager = (NotificationManager) 
					getSystemService(NOTIFICATION_SERVICE);
			notificationManager.notify(0, notification);*/
			
			// 发生显示通知的广播
			// sendBroadcast(new Intent(ACTION_SHOW_NOTIFICATION));
			// 发送带权限的广播，只有具有该权限的应用才能收到该广播
			// sendBroadcast(new Intent(ACTION_SHOW_NOTIFICATION), PREM_PRIVATE);
			// 通过发送带权限的有序广播来推送通知，从而去过滤通知
			showBackgroundNotification(0, notification);
		} else {
			Log.i(TAG, "Got a old result: " + resultId);
		}
		
		prefs.edit()
			.putString(DoubanFetcher.PREF_LAST_RESULT_ID, resultId)
			.commit();
	}
	
	/**
	 * 为让能够取消通知， broadcast必须有序。
	 * 该方法打包一个Notification调用，然后作为一个broadcast发出。
	 * 只要通知信息还没被撤消，可指定一个result receiver发出打包的Notification。
	 * 
	 * @param requestCode
	 * @param notification
	 */
	void showBackgroundNotification(int requestCode, Notification notification) {
		Intent intent = new Intent(ACTION_SHOW_NOTIFICATION);
		intent.putExtra("REQUEST_CODE", requestCode);
		intent.putExtra("NOTIFICATION", notification);
		
		// 发送有序广播
		sendOrderedBroadcast(intent, PREM_PRIVATE, null, null, Activity.RESULT_OK, 
				null, null);
	}

}
