package com.sunny.photogallery;

import java.util.ArrayList;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.SearchManager;
import android.app.SearchableInfo;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.SearchView;

public class PhotoGalleryFragment extends VisibleFragment {
	
	public static int BOOK_NUM = 15;
	
	private static final String TAG = "PhotoGalleryFragment";
	
	GridView mGridView;
	ArrayList<GalleryItem> mItems;
	ThumbnailDownloader<ImageView> mThumbnailThread;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setRetainInstance(true);
		setHasOptionsMenu(true);
		
		updateItems();
		
		// 启动服务
		/*Intent intent = new Intent(getActivity(), PollService.class);
		getActivity().startService(intent);*/
		// 利用定时器延时运行服务
		// PollService.setServiceAlarm(getActivity(), true);
		
		mThumbnailThread = new ThumbnailDownloader<ImageView>(new Handler());
		mThumbnailThread.setListener(new ThumbnailDownloader.Listener<ImageView>() {

			@Override
			public void onThumbnailDownloaded(ImageView token, Bitmap thumbnail) {
				if (isVisible()) { // 保证不会将图片设置到无效的ImageView视图上去
					token.setImageBitmap(thumbnail); // 为ImageView设置要显示的s图片
				}
			}
		});
		mThumbnailThread.start();
		mThumbnailThread.getLooper();
		Log.i(TAG, "Background thread started");
	}

	public void updateItems() {
		new FetchItemsTask().execute();
	}

	@Override
	public View onCreateView(LayoutInflater inflater,
			@Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.fragment_photo_gallery, container, false);
		
		mGridView = (GridView) view.findViewById(R.id.gridView);
		// 在onCreateView(...)方法中调用该方法，这样每次因设备旋转
		// 重新生成GridView视图时，可重新为其配置对应的adapter。
		// 另外，每次模型层对象发生改变时，也应保证该方法的及时调用。
		setupAdapter();
		
		mGridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id) {
				GalleryItem item = mItems.get(position);
				
				Uri webPageUri = Uri.parse(item.getWebPage());
				// 使用隐式intent通过内置浏览器实现网页浏览
				// Intent intent = new Intent(Intent.ACTION_VIEW, webPageUri);
				// 使用显示intent通过WebView实现网页浏览
				Intent intent = new Intent(getActivity(), PhotoPageActivity.class);
				intent.setData(webPageUri);
				
				startActivity(intent);
			}
		});
		
		return view;
	}
	
	private void setupAdapter() {
		if (getActivity() == null || mGridView == null)
			return;
		
		if (mItems != null) {
			/*mGridView.setAdapter(new ArrayAdapter<GalleryItem>(getActivity(), 
					android.R.layout.simple_gallery_item, mItems));*/
			mGridView.setAdapter(new GalleryItemAdapter(mItems));
		} else {
			mGridView.setAdapter(null);
		}
	}

	private class FetchItemsTask extends AsyncTask<Void, Void, ArrayList<GalleryItem>> {

		/**
		 * 使用后台线程最简便的方式是使用AsyncTask工具类。 AsyncTask创建后台线程后，
		 * 便可在该线程上调用doInBackground(...)方法运行代码。
		 */
		@Override
		protected ArrayList<GalleryItem> doInBackground(Void... params) {
			DoubanFetcher fetcher = new DoubanFetcher();
			ArrayList<GalleryItem> items = new ArrayList<GalleryItem>();
			
			Activity activity = getActivity();
			if (activity == null)
				return items;

			String query = PreferenceManager.getDefaultSharedPreferences(activity)
					.getString(DoubanFetcher.PREF_SEARCH_QUERY, null);
			Log.i(TAG, "Pref: " + query);
			if (query != null) {
				items = fetcher.search(query, "", 0, 5);
			} else {
				for (int i = DoubanFetcher.BOOK_INDEX; i < DoubanFetcher.BOOK_INDEX + BOOK_NUM; i++) {
					GalleryItem item = fetcher.fetchItem(i);
					items.add(item);
					Log.i(TAG, "BOOK_INDEX: " + i);
				}
			}

			return items;
		}

		/**
		 * onPostExecute(...)方法在doInBackground(...)方法执行完毕后才会运行，
		 * 而且它是在主线程而非后台线程上运行的。因此，在该方法中更新UI比较安全。
		 */
		@Override
		protected void onPostExecute(ArrayList<GalleryItem> result) {
			mItems = result;
			setupAdapter(); // 更新UI
		}
		
	}
	
	private class GalleryItemAdapter extends ArrayAdapter<GalleryItem> {

		public GalleryItemAdapter(ArrayList<GalleryItem> items) {
			super(getActivity(), 0, items);
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			if (convertView == null) {
				convertView = getActivity().getLayoutInflater()
						.inflate(R.layout.gallery_item, parent, false);
			}
			
			ImageView imageView = (ImageView) convertView
					.findViewById(R.id.gallery_item_imageView);
			imageView.setImageResource(R.drawable.brian_up_close);
			
			// 从网络上获取图片资源
			GalleryItem item = getItem(position);
			mThumbnailThread.queueThumbnail(imageView, item.getUrl());
			
			return convertView;
		}
		
	}

	@Override
	public void onDestroyView() {
		super.onDestroyView();
		
		mThumbnailThread.clearQueue();
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		
		mThumbnailThread.quit();
		Log.i(TAG, "Background thread destoryed");
	}

	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		super.onCreateOptionsMenu(menu, inflater);
		
		inflater.inflate(R.menu.fragment_photo_gallery, menu);
		
		// 在 Android 3.0 以后版本的设备上使用 SearchView
		// SearchView不会产生任何onOptionsItemSelected(...)回调方法。
		// 这反而带来了方便，因为这意味着可将这些回调方法预留给那些不支持操作视图的老设备使用。
		// 因此，在onCreateOptionsMenu(...)方法中添加相应代码，取得搜索配置信息并发送给SearchView。
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
			// Pull out the SearchView
			MenuItem searchItem = menu.findItem(R.id.menu_item_search);
			SearchView searchView = (SearchView) searchItem.getActionView();
			
			// Get the data from out searchable.xml as a SearchableInfo
			SearchManager searchManager = (SearchManager) getActivity()
					.getSystemService(Context.SEARCH_SERVICE);
			ComponentName name = getActivity().getComponentName();
			// 有关搜索的全部信息，包括应该接收intent的activity名称以及所有searchable.xml中的信息，
			// 都存储在了SearchableInfo对象中。
			SearchableInfo searchInfo = searchManager.getSearchableInfo(name);
			
			// SearchView属于操作视图（action view） ，可内置在操作栏里。
			// 因此，不像对话框那样叠置在activity上， SearchView支持将搜索界面内嵌在activity的操作栏里。
			//这意味着SearchView的搜索界面使用了与应用完全一致的样式与主题，这显然再好不过了。
			searchView.setSearchableInfo(searchInfo);
		}
	}

	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.menu_item_search:
			getActivity().onSearchRequested();
			return true;
			
		// 实现搜索的撤销，从shared preferences中清除搜索信息。
		case R.id.menu_item_clear:
			PreferenceManager.getDefaultSharedPreferences(getActivity())
				.edit()
				.putString(DoubanFetcher.PREF_SEARCH_QUERY, null)
				.commit();
			updateItems();
			return true;
			
		// 实现启动或关闭服务。
		case R.id.menu_item_toggle_polling:
			boolean shouldStartAlarm = !PollService.isServiceAlarmOn(getActivity());
			PollService.setServiceAlarm(getActivity(), shouldStartAlarm);
			
			// 更新菜单状态
			getActivity().invalidateOptionsMenu();
			
			return true;

		default:
			return super.onOptionsItemSelected(item);
		}
	}

	/**
	 * 在3.0以前版本的设备中，每次显示菜单时都会调用该方法，而在3.0以后版本的设备中，以上做法就不行了。
	 * 操作栏无法自动更新自己，因此，需通过Activity.invalidateOptionsMenu()方法回调
	 * onPrepareOptionsMenu(Menu)方法并刷新菜单项（在onOptionsItemSelected中实现）。
	 */
	@Override
	public void onPrepareOptionsMenu(Menu menu) {
		// 除了菜单的首次创建外，每次菜单需要配置都会调用该方法。
		// 因此，可以在该方法中更新选项菜单，以反映应用的状态。
		super.onPrepareOptionsMenu(menu);
		
		MenuItem toggleItem = menu.findItem(R.id.menu_item_toggle_polling);
		if (PollService.isServiceAlarmOn(getActivity())) {
			toggleItem.setTitle(R.string.stop_polling);
		} else {
			toggleItem.setTitle(R.string.start_polling);
		}
	}

}
