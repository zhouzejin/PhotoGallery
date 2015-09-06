package com.sunny.photogallery;

import android.app.SearchManager;
import android.content.Intent;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.util.Log;

public class PhotoGalleryActivity extends SingleFragmentActivity {
	
	private static final String TAG = "PhotoGalleryActivity";

	@Override
	protected Fragment createFragment() {
		return new PhotoGalleryFragment();
	}

	@Override
	protected void onNewIntent(Intent intent) {
		// 通过覆盖Activity中的onNewIntent(Intent)方法，可接收到新的intent。
		super.onNewIntent(intent);
		
		PhotoGalleryFragment fragment = (PhotoGalleryFragment) 
				getSupportFragmentManager().findFragmentById(R.id.fragment_container);
		
		if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
			String query = intent.getStringExtra(SearchManager.QUERY);
			Log.i(TAG, "Received a new search query: " + query);
			
			// 关于onNewIntent(Intent)方法的使用应重点注意：如果需要新intent的值，切记将其储存起来。
			// 从getIntent()方法获取的是旧intent的值。这是因为getIntent()方法只返回启动了当前activity的intent，
			// 而非接收到的最新intent。
			PreferenceManager.getDefaultSharedPreferences(this)
				.edit()
				.putString(DoubanFetcher.PREF_SEARCH_QUERY, query)
				.commit();
		}
		
		// 无论该intent是何时接收到的，都需在PhotoGalleryFragment中刷新显示图片项。
		fragment.updateItems();
	}

}
