package com.sunny.photogallery;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.util.Log;

/**
 * 主线程是一个拥有handler和Looper的消息循环。主线程上创建的Handler会自动与它的Looper相关联。
 * 我们可以将主线程上创建的Handler传递给另一线程。传递出去的Handler与创建它的线程Looper始终保持着联系。
 * 因此，任何已传出Handler负责处理的消息都将在主线程的消息队列中处理。
 * 
 * @author Zhou Zejin
 *
 * @param <Token>
 */
public class ThumbnailDownloader<Token> extends HandlerThread {

	private static final String TAG = "ThumbnailDownloader";
	private static final int MESSAGE_DOWNLOAD = 0;
	
	Handler mHandler;
	Map<Token, String> requestMap = 
			Collections.synchronizedMap(new HashMap<Token, String>());
	Handler mResponseHandler;
	Listener<Token> mListener;
	
	public interface Listener<Token> {
		void onThumbnailDownloaded(Token token, Bitmap thumbnail);
	}
	
	public void setListener(Listener<Token> listener) {
		mListener = listener;
	}
	
	public ThumbnailDownloader(Handler responseHandler) {
		super(TAG);
		mResponseHandler = responseHandler;
	}
	
	/**
	 * 因为HandlerThread.onLooperPrepared()方法的调用发生在Looper第一次检查消息队列之前，
	 * 所以该方法成了我们创建Handler实现的好地方。
	 */
	@SuppressLint("HandlerLeak")
	@Override
	protected void onLooperPrepared() {
		mHandler = new Handler() {

			@Override
			public void handleMessage(Message msg) {
				if (msg.what == MESSAGE_DOWNLOAD) {
					@SuppressWarnings("unchecked")
					Token token = (Token) msg.obj;
					Log.i(TAG, "Got a request for url: " + requestMap.get(token));
					handleRequest(token);
				}
			}
			
		};
	}

	public void queueThumbnail(Token token, String url) {
		Log.i(TAG, "Got an URL: " + url);
		requestMap.put(token, url);
		
		mHandler.obtainMessage(MESSAGE_DOWNLOAD, token).sendToTarget();
	}
	
	protected void handleRequest(final Token token) {
		try {
			final String url = requestMap.get(token);
			if (url == null)
				return;
			
			byte[] bitmapBytes = new DoubanFetcher().getUrlBytes(url);
			final Bitmap bitmap = BitmapFactory
					.decodeByteArray(bitmapBytes, 0, bitmapBytes.length);
			Log.i(TAG, "Bitmap created");
			
			// 因为mResponseHandler与主线程的Looper相关联，所以UI更新代码也是在主线程中完成的。
			mResponseHandler.post(new Runnable() {
				
				@Override
				public void run() {
					// 再次检查了requestMap。这很有必要， 因为GridView会循环使用它的视图。
					// ThumbnailDownloader完成Bitmap下载后， GridView可能已经循环使用了ImageView，
					// 并继续请求一个不同的URL。该检查可保证每个Token都能获取到正确的图片，即使中间发生了其他请求也无妨。
					if (requestMap.get(token) != url)
						return;
					
					requestMap.remove(token);
					mListener.onThumbnailDownloaded(token, bitmap);
				}
			});
		} catch (IOException ioe) {
			Log.e(TAG, "Error downloading image", ioe);
		}
	}
	
	/**
	 * 如果用户旋转屏幕，因ImageView视图的失效， ThumbnailDownloader则可能会挂起。
	 * 如果点击这些ImageView，就可能发生异常。新增下列方法清除队列外的所有请求。
	 * 在Fragment的OnDestoryView()方法中调用。
	 */
	public void clearQueue() {
		mHandler.removeMessages(MESSAGE_DOWNLOAD);
		requestMap.clear();
	}
	
}
