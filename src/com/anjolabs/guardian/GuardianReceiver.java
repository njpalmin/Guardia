package com.anjolabs.guardian;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class GuardianReceiver extends BroadcastReceiver{
    static final String TAG = "GuardianReceiver";
    static final boolean DEBUG=GuardianApp.DEBUG;
		
	@Override
	public void onReceive(Context context, Intent intent) {
		// TODO Auto-generated method stub
		if(DEBUG)Log.d(TAG,"onReceive intent="+intent+" to trigger to start GuardianApp");
	}

}
