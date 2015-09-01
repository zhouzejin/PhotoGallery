package com.sunny.photogallery;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;

public abstract class SingleFragmentActivity extends FragmentActivity {
	
	protected int getLayoutResId() {
		return R.layout.activity_fragment;
	}
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		// setContentView(R.layout.activity_fragment);
		setContentView(getLayoutResId());
		
		FragmentManager fm = getSupportFragmentManager();
		Fragment fragment = fm.findFragmentById(R.id.fragment_container);
		
		if (null == fragment) {
			fragment = createFragment();
			fm.beginTransaction()
				.add(R.id.fragment_container, fragment)
				.commit();
		}
	}

	abstract protected Fragment createFragment();

}
