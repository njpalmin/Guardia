package com.anjolabs.guardian;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

public class AppDetailActivity extends Activity{
	static final String TAG = "AppDetailActivity";
	static final boolean DEBUG=Guardian.DEBUG;
	
	private AppEntry mAppEntry;
	private PackageManager mPm;
	private ImageView mIcon;
	private ImageView mCheckBox;
	private TextView  mAppName;
	private TextView  mAppName1;
	private TextView  mAppVersion;
	private TextView  mAppRevoked;
	
	private Button mCancel;
	private Button mUninstall;
	
	@Override
	public void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		
		
		setContentView(R.layout.app_entry_detail);
		Intent intent = getIntent();
		Bundle bundle = intent.getExtras();
		mAppEntry = bundle.getParcelable(Guardian.APPS_ENTRY);
		mPm= getPackageManager();
		
		initScreen();

	}
	
	void initScreen(){
		
		
		mIcon = (ImageView)findViewById(R.id.app_icon);
		mCheckBox = (ImageView)findViewById(R.id.app_checkbox);
		mAppName = (TextView)findViewById(R.id.app_name);
		mAppName1 = (TextView)findViewById(R.id.app_name1);
		mAppVersion = (TextView)findViewById(R.id.app_version);
		mAppRevoked = (TextView)findViewById(R.id.app_cert_state);
		mCancel = (Button)findViewById(R.id.cancel);
		mUninstall = (Button)findViewById(R.id.uninstall);
		
		if(mPm != null){
			mIcon.setImageDrawable(mAppEntry.getInfo().applicationInfo.loadIcon(mPm));
			mAppName.setText(mAppEntry.getInfo().applicationInfo.loadLabel(mPm));
			mAppName1.setText(mAppEntry.getInfo().applicationInfo.loadLabel(mPm));
			
			if ((mAppEntry.mAppCertState & Guardian.APP_WITH_ANJO_AKI_NOT_REVOKED) != 0){
				mCheckBox.setImageResource(R.drawable.green);
				mAppRevoked.setText(getString(R.string.verified));
				mAppRevoked.setTextColor(Color.GREEN);
			}else if((mAppEntry.mAppCertState & Guardian.APP_WITH_ANJO_AKI_REVOKED) != 0){
				mCheckBox.setImageResource(R.drawable.red);
				mAppRevoked.setText(getString(R.string.revoked));
				mAppRevoked.setTextColor(Color.RED);
			}else  if((mAppEntry.mAppCertState & Guardian.APP_WITHOUT_ANJO_AKI) != 0){
				mCheckBox.setImageResource(R.drawable.yellow);
				mAppRevoked.setText(getString(R.string.non));
				mAppRevoked.setTextColor(Color.YELLOW);
			}
			
			
			String versionName = getString(R.string.version) + " "+mAppEntry.getInfo().versionName; 
			mAppVersion.setText(versionName);
			
			mCancel.setOnClickListener(new View.OnClickListener() {
	            public void onClick(View v) {
	                if(DEBUG) Log.d(TAG, "mCancel clicked");
	                finish();
	            }
	        });
			mUninstall.setOnClickListener(new View.OnClickListener() {
	            public void onClick(View v) {
	                if(DEBUG) Log.d(TAG, "mCancel clicked");
	                String pakcageName = mAppEntry.getInfo().packageName;
	                Uri packageURI = Uri.parse("package:"+pakcageName);
	                Intent uninstallIntent = new Intent(Intent.ACTION_DELETE, packageURI);
	                startActivity(uninstallIntent);
	                //finish();
	            }
	        });
		}
	}
	
	@Override
	protected void onStop(){
		super.onStop();
	}
	
	@Override
	protected void onDestroy(){
		super.onDestroy();
	}
}
