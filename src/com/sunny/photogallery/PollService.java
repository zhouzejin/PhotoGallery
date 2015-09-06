package com.sunny.photogallery;

import java.util.ArrayList;

import android.app.AlarmManager;
import android.app.IntentService;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.preference.PreferenceManager;
import android.util.Log;

public class PollService extends IntentService {
	
	private static final String TAG = "PollService";
	
	private static final int POLL_INTERVAL = 1000 * 15; // 15 seconds
	
	public static void setServiceAlarm(Context context, boolean isOn) {
		Intent intent = new Intent(context, PollService.class);
		PendingIntent pendingIntent = PendingIntent
				.getService(context, 0, intent, 0);
		
		AlarmManager alarmManager = (AlarmManager) 
				context.getSystemService(Context.ALARM_SERVICE);
		
		if (isOn) {
			alarmManager.setRepeating(AlarmManager.RTC, 
					System.currentTimeMillis(), POLL_INTERVAL, pendingIntent);
		} else {
			alarmManager.cancel(pendingIntent);
			pendingIntent.cancel();
		}
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
		} else {
			Log.i(TAG, "Got a old result: " + resultId);
		}
		
		prefs.edit()
			.putString(DoubanFetcher.PREF_LAST_RESULT_ID, resultId)
			.commit();
	}

}
