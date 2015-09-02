package com.sunny.photogallery;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.net.HttpURLConnection;
import java.net.URL;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import android.net.Uri;
import android.util.Log;

public class DoubanFetcher {
	
	public static final String TAG = "DoubanFetcher";
	public static int BOOK_INDEX = 2023009;
	
	private static final String ENDPOINT = "http://api.douban.com/book/subject/";
	
	private static final String XML_TAG_TITLE = "title";
	private static final String XML_TAG_ID = "id";
	private static final String XML_TAG_LINK = "link";
	
	private static final String XML_ATTRIBUTE_HREF = "href";
	private static final String XML_ATTRIBUTE_REL = "rel";
	
	private static final String XML_VALUE_IMAGE = "image";
	
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
	/**
	 * 获取指定index的GalleryItem对象
	 * @param index
	 * @return
	 */
	public GalleryItem fetchItem(int index)	{
		GalleryItem item = new GalleryItem();
		try {
			// http://api.flickr.com/services/rest/?method=flickr.photos.getRecent&api_key=XXX&extras=url_s
			/*String url = Uri.parse(ENDPOINT).buildUpon()
					.appendQueryParameter("method", METHOD_GET_RECENT)
					.appendQueryParameter("api_key", API_KEY)
					.appendQueryParameter(PARAM_EXTRAS, EXTRA_SMALL_URL)
					.build().toString();*/
			
			// 获取XML格式数据
			String url = Uri.parse(ENDPOINT).buildUpon().toString() + index;
			String xmlString = getUrl(url);
			Log.i(TAG, "Received xml: " + xmlString);
			
			// 将XML格式数据解析为GallerItem对象
			XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
			XmlPullParser parser = factory.newPullParser();
			parser.setInput(new StringReader(xmlString));
			
			item = parseItem(parser);
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
	GalleryItem parseItem(XmlPullParser parser) 
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
			eventType = parser.next();
		}
		
		Log.i(TAG, "Title:" + item.getCaption() + " Id:" + 
				item.getId() + " Url:" + item.getUrl());
		return item;
	}

}
