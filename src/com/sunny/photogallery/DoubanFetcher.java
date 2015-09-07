package com.sunny.photogallery;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import android.net.Uri;
import android.util.Log;

public class DoubanFetcher {
	
	public static final String TAG = "DoubanFetcher";
	public static int BOOK_INDEX = 2023009;
	
	/**
	 * 使用shared preferences实现轻量级数据存储
	 */
	public static final String PREF_SEARCH_QUERY = "search_query";
	public static final String PREF_LAST_RESULT_ID = "last_result_id"; // 存储最近一次获取图片的ID
	
	private static final String ENDPOINT = "http://api.douban.com/book/subject/";
	
	private static final String XML_TAG_TITLE = "title";
	private static final String XML_TAG_ID = "id";
	private static final String XML_TAG_LINK = "link";
	
	private static final String XML_ATTRIBUTE_HREF = "href";
	private static final String XML_ATTRIBUTE_REL = "rel";
	
	private static final String XML_VALUE_IMAGE = "image";
	private static final String XML_VALUE_MOBILE = "mobile";
	
	private static final String ENDPOINT_SEARCH = "https://api.douban.com/v2/book/search";
	
	/**
	 * q和tag必传其一
	 */
	private static final String PARAM_Q = "q"; // 查询关键字
	private static final String PARAM_TAG = "tag"; // 查询的tag
	/**
	 * 默认为0
	 */
	private static final String PARAM_START = "start"; // 取结果的offset
	/**
	 * 默认为20，最大为100
	 */
	private static final String PARAM_COUNT = "count"; // 取结果的条数
	
	private static final String JSON_KEY_BOOKS = "books";
	private static final String JSON_KEY_TITLE = "title";
	private static final String JSON_KEY_URL = "url";
	private static final String JSON_KEY_IMAGES = "images";
	private static final String JSON_KEY_LARGE = "large";
	private static final String JSON_KEY_ALT = "alt";
	
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
	
	/**
	 * 获取指定index的GalleryItem对象
	 * @param index
	 * @return
	 */
	public GalleryItem fetchItem(int index)	{
		GalleryItem item = new GalleryItem();
		try {
			// 获取XML格式数据
			String url = Uri.parse(ENDPOINT).buildUpon().toString() + index;
			String xmlString = getUrl(url);
			// Log.i(TAG, "Received xml: " + xmlString);
			
			// 将XML格式数据解析为GallerItem对象
			XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
			XmlPullParser parser = factory.newPullParser();
			parser.setInput(new StringReader(xmlString));
			item = parseXml(parser);
		} catch (IOException ioe) {
			Log.e(TAG, "Failed to fetch item", ioe);
		} catch (XmlPullParserException xppe) {
			Log.e(TAG, "Failed to parse item", xppe);
		}
		return item;
	}
	
	/**
	 * 解析XML格式数据
	 * @param parser
	 * @return
	 * @throws XmlPullParserException
	 * @throws IOException
	 */
	GalleryItem parseXml(XmlPullParser parser) 
			throws XmlPullParserException, IOException {
		GalleryItem item = new GalleryItem();
		int eventType = parser.next();
		
		while (eventType != XmlPullParser.END_DOCUMENT) {
			if (eventType == XmlPullParser.START_TAG && 
					XML_TAG_TITLE.equals(parser.getName())) {
				String caption = parser.nextText();
				item.setCaption(caption);
			}
			if (eventType == XmlPullParser.START_TAG && 
					XML_TAG_ID.equals(parser.getName())) {
				String id = parser.nextText();
				item.setId(id);
			}
			if (eventType == XmlPullParser.START_TAG && 
					XML_TAG_LINK.equals(parser.getName())) {
				String rel = parser.getAttributeValue(null, XML_ATTRIBUTE_REL);
				if (XML_VALUE_IMAGE.equals(rel)) {
					String url = parser.getAttributeValue(null, XML_ATTRIBUTE_HREF);
					item.setUrl(url);
				}
			}
			if (eventType == XmlPullParser.START_TAG && 
					XML_TAG_LINK.equals(parser.getName())) {
				String rel = parser.getAttributeValue(null, XML_ATTRIBUTE_REL);
				if (XML_VALUE_MOBILE.equals(rel)) {
					String webPage = parser.getAttributeValue(null, XML_ATTRIBUTE_HREF);
					item.setWebPage(webPage);
				}
			}
			eventType = parser.next();
		}
		
		Log.i(TAG, "Title:" + item.getCaption() + " Id:" + 
				item.getId() + " Url:" + item.getUrl() + " WebPage:" + item.getWebPage());
		return item;
	}
	
	/**
	 * 根据参数进行搜索
	 * @param query
	 * @param tag
	 * @param start
	 * @param count
	 * @return
	 */
	public ArrayList<GalleryItem> search(String query, String tag, int start, int count) {
		ArrayList<GalleryItem> items = new ArrayList<GalleryItem>();
		try {
			// https://api.douban.com/v2/book/search?q=java&tag=java&start=7&count=1
			String url = Uri.parse(ENDPOINT_SEARCH).buildUpon()
					.appendQueryParameter(PARAM_Q, query)
					.appendQueryParameter(PARAM_TAG, tag)
					.appendQueryParameter(PARAM_START, String.valueOf(start))
					.appendQueryParameter(PARAM_COUNT, String.valueOf(count))
					.build().toString();
			Log.i(TAG, url);
			
			// 获取JSON格式数据
			String jsonString = getUrl(url);
			// Log.i(TAG, "Received json: " + jsonString);
			
			// 将JSON格式数据解析为GallerItem对象
			items = parseJson(jsonString);
		} catch (IOException ioe) {
			Log.e(TAG, "Failed to search", ioe);
		} catch (JSONException jsone) {
			Log.e(TAG, "Failed to parse item", jsone);
		}
		return items;
	}

	/**
	 * 解析JSON格式的数据
	 * @param json
	 * @return
	 * @throws JSONException
	 */
	ArrayList<GalleryItem> parseJson(String json) throws JSONException {
		ArrayList<GalleryItem> items = new ArrayList<GalleryItem>();
		
		JSONTokener jsonTokener = new JSONTokener(json);
		JSONObject result = (JSONObject) jsonTokener.nextValue();
		JSONArray books = result.getJSONArray(JSON_KEY_BOOKS);
		
		for (int i = 0; i < books.length(); i++) {
			JSONObject book = books.getJSONObject(i);
			// Log.i(TAG, "book" + i + ": " + book.toString());
			
			GalleryItem item = new GalleryItem();
			String caption = book.getString(JSON_KEY_TITLE);
			item.setCaption(caption);
			String id = book.getString(JSON_KEY_URL);
			item.setId(id);
			JSONObject images = book.getJSONObject(JSON_KEY_IMAGES);
			String url = images.getString(JSON_KEY_LARGE);
			item.setUrl(url);
			String webPage = book.getString(JSON_KEY_ALT);
			item.setWebPage(webPage);
			
			items.add(item);
			
			Log.i(TAG, "Title:" + item.getCaption() + " Id:" + 
					item.getId() + " Url:" + item.getUrl() + " WebPage:" + item.getWebPage());
		}
		
		return items;
	}

}
