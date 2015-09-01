package com.sunny.photogallery;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import android.net.Uri;
import android.util.Log;

public class DoubanFetcher {
	
	public static final String TAG = "DoubanFetcher";
	public static int BOOK_INDEX = 2023013;
	
	private static final String ENDPOINT = "http://api.douban.com/book/subject/";
	
	/*private static final String ENDPOINT = "http://api.flickr.com/services/rest/";
    private static final String API_KEY = "XXX";
    private static final String METHOD_GET_RECENT = "flickr.photos.getRecent";
    private static final String PARAM_EXTRAS = "extras";

    private static final String EXTRA_SMALL_URL = "url_s";

    private static final String XML_PHOTO = "photo";*/
	
	byte[] getUrlBytes(String urlSpec) throws IOException {
		URL url = new URL(urlSpec);
		HttpURLConnection connection = (HttpURLConnection) url.openConnection();
		
		try {
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			InputStream in = connection.getInputStream();
			// HttpURLConnection对象虽然提供了一个连接，但只有在调用getInputStream()方法时
			// （如果是POST请求，则调用getOutputStream()方法），它才会真正连接到指定的URL地址。在
			// 此之前我们无法获得有效的返回代码。
			if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
				return null;
			}
			int bytesRead = 0;
			byte[] buffer = new byte[1024];
			while ((bytesRead = in.read(buffer)) > 0) {
				out.write(buffer, 0, bytesRead);
			}
			
			out.close();
			return out.toByteArray();
		} finally {
			connection.disconnect();
		}
	}
	
	public String getUrl(String urlSpec) throws IOException {
		return new String(getUrlBytes(urlSpec));
	}
	
	public void fetchItem(int index)	{
		try {
			// http://api.flickr.com/services/rest/?method=flickr.photos.getRecent&api_key=XXX&extras=url_s
			/*String url = Uri.parse(ENDPOINT).buildUpon()
					.appendQueryParameter("method", METHOD_GET_RECENT)
					.appendQueryParameter("api_key", API_KEY)
					.appendQueryParameter(PARAM_EXTRAS, EXTRA_SMALL_URL)
					.build().toString();*/
			
			String url = Uri.parse(ENDPOINT).buildUpon().toString() + index;
			String xmlString = getUrl(url);
			Log.i(TAG, "Received xml: " + xmlString);
		} catch (Exception ioe) {
			Log.e(TAG, "Failed to fetch item", ioe);
		}
	}
	
	/*void parseItem(ArrayList<GalleryItem> items, XmlPullParser parser) 
			throws XmlPullParserException, IOException {
		int eventType = parser.next();
	}*/

}
