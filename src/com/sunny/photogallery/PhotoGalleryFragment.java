package com.sunny.photogallery;

import java.util.ArrayList;

import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.GridView;
import android.widget.ImageView;

public class PhotoGalleryFragment extends Fragment {
	
	public static int BOOK_NUM = 15;
	
	private static final String TAG = "PhotoGalleryFragment";
	
	GridView mGridView;
	ArrayList<GalleryItem> mItems;
	ThumbnailDownloader<ImageView> mThumbnailThread;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setRetainInstance(true);
		new FetchItemsTask().execute();
		
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

	@Override
	public View onCreateView(LayoutInflater inflater,
			@Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.fragment_photo_gallery, container, false);
		
		mGridView = (GridView) view.findViewById(R.id.gridView);
		// 在onCreateView(...)方法中调用该方法，这样每次因设备旋转
		// 重新生成GridView视图时，可重新为其配置对应的adapter。
		// 另外，每次模型层对象发生改变时，也应保证该方法的及时调用。
		setupAdapter();
		
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
			for (int i = DoubanFetcher.BOOK_INDEX; i < DoubanFetcher.BOOK_INDEX + BOOK_NUM; i++) {
				GalleryItem item = fetcher.fetchItem(i);
				items.add(item);
				Log.i(TAG, "BOOK_INDEX: " + i);
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

}
