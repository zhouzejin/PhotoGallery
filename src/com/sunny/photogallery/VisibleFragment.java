package com.sunny.photogallery;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v4.app.Fragment;
import android.util.Log;

/**
 * 使用动态广播过滤前台通知消息
 * 顺便要说的是，我们应注意保留fragment中的onCreate(...)和onDestroy()方法的运用。
 * 设备发生旋转时， onCreate(...)和onDestroy()方法中的getActivity()方法会返回不同的值。
 * 因此，如想在Fragment.onCreate(Bundle)和Fragment.onDestroy()方法中实现登记或撤销登记，
 * 应使用getActivity().getApplicationContext()方法。
 */
public abstract class VisibleFragment extends Fragment {
	
	public static final String TAG = "VisibleFragment";
	
	private BroadcastReceiver mOnShowNotification = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {
			/*Toast.makeText(getActivity(), 
					"Got a broadcast: " + intent.getAction(), 
					Toast.LENGTH_LONG)
				.show();*/
			// 将取消通知的信息发送给SHOW_NOTIFICATION的发送者
			// If we receive this, we're visible, so cancel the notification
			Log.i(TAG, "canceling notification");
			setResultCode(Activity.RESULT_CANCELED); // 设定返回值后，每个后续接收者均可看到或修改返回值。
		}
		
	};

	@Override
	public void onResume() {
		super.onResume();
		
		IntentFilter filter = new IntentFilter(PollService.ACTION_SHOW_NOTIFICATION);
		// getActivity().registerReceiver(mOnShowNotification, filter);
		// 指定具有指定权限的应用发送的广播才能促发该Receiver
		getActivity().registerReceiver(mOnShowNotification, filter, 
				PollService.PREM_PRIVATE, null);
	}

	@Override
	public void onPause() {
		super.onPause();
		
		getActivity().unregisterReceiver(mOnShowNotification);
	}

}
