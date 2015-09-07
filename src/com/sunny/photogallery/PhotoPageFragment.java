package com.sunny.photogallery;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;
import android.widget.TextView;

public class PhotoPageFragment extends VisibleFragment {
	
	private String mUrl;
	private WebView mWebView;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setRetainInstance(true);
		
		mUrl = getActivity().getIntent().getData().toString();
	}

	@SuppressLint("SetJavaScriptEnabled")
	@Override
	public View onCreateView(LayoutInflater inflater,
			@Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.fragment_photo_page, container, false);
		
		final ProgressBar progressBar = (ProgressBar) view.findViewById(R.id.progressBar);
		progressBar.setMax(100); // WebChromeClient reports in range 0-100
		
		final TextView titleTextView = (TextView) view.findViewById(R.id.titleTextView);
		
		mWebView = (WebView) view.findViewById(R.id.webView);
		mWebView.getSettings().setJavaScriptEnabled(true);
		// WebViewClient是响应渲染事件的接口
		mWebView.setWebViewClient(new WebViewClient() {

			@Override
			public boolean shouldOverrideUrlLoading(WebView view, String url) {
				// 如返回true值，意即：不要处理这个URL，我自己来。
				// 如返回false值，意即：WebView，去加载这个URL，我不会对它做任何处理。
				return false;
			}
		});
		// WebChromeClient是一个响应那些改变浏览器中装饰元素的事件接口。
		// 这包括JavaScript警告信息、网页图标、状态条加载，以及当前网页标题的刷新。
		mWebView.setWebChromeClient(new WebChromeClient() {

			@Override
			public void onProgressChanged(WebView view, int newProgress) {
				if (newProgress == 100) {
					progressBar.setVisibility(View.INVISIBLE);
				} else {
					progressBar.setVisibility(View.VISIBLE);
					progressBar.setProgress(newProgress);
				}
			}

			@Override
			public void onReceivedTitle(WebView view, String title) {
				titleTextView.setText(title);
			}
		});
		mWebView.loadUrl(mUrl);
		
		return view;
	}

}
