package com.sunny.photogallery;

public class GalleryItem {

	/**
	 * <title>倘若我在彼岸</title>
	 */
	private String mCaption;
	/**
	 * <id>http://api.douban.com/book/subject/2023013</id>
	 */
	private String mId;
	/**
	 * <link href="http://img4.douban.com/spic/s9097939.jpg" rel="image"/>
	 */
	private String mUrl;
	/**
	 * XML: <link href="http://m.douban.com/book/subject/1003078/" rel="mobile"/>
	 * JSON: "alt":"http:\\book.douban.com\subject\1003078\",
	 */
	private String mWebPage;

	public String getCaption() {
		return mCaption;
	}

	public void setCaption(String caption) {
		mCaption = caption;
	}

	public String getId() {
		return mId;
	}

	public void setId(String id) {
		mId = id;
	}

	public String getUrl() {
		return mUrl;
	}

	public void setUrl(String url) {
		mUrl = url;
	}

	public String getWebPage() {
		return mWebPage;
	}

	public void setWebPage(String webPage) {
		mWebPage = webPage;
	}

	@Override
	public String toString() {
		return mCaption;
	}

}
