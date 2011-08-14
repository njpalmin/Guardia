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

/**
 * Activity to show the application details.
 * @author Alphalilin@gmail.com
 *
 */
public class AppDetailActivity extends Activity{
	static final String TAG = "AppDetailActivity";
	static final boolean DEBUG=GuardianApp.DEBUG;
	
	private AppEntry mAppEntry;
	private PackageManager mPm;
	private ImageView mIcon;
	private ImageView mCheckBox;
	private TextView  mAppName;
	private TextView  mAppName1;
	private TextView  mAppVersion;
	private TextView  mAppRevoked;
	
	private ImageButton mCancel;
	private ImageButton mUninstall;
	
	@Override
	public void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
	
		setContentView(R.layout.app_entry_detail);
		Intent intent = getIntent();
		Bundle bundle = intent.getExtras();
		mAppEntry = bundle.getParcelable(GuardianApp.APPS_ENTRY);
		mPm= getPackageManager();
		
		initScreen();

	}
	
	/**
	 * Initialize layout.
	 */
	void initScreen(){
		mIcon = (ImageView)findViewById(R.id.app_icon0);
		mCheckBox = (ImageView)findViewById(R.id.app_checkbox);
		mAppName = (TextView)findViewById(R.id.app_name0);
		mAppName1 = (TextView)findViewById(R.id.app_name1);
		mAppVersion = (TextView)findViewById(R.id.app_version);
		mAppRevoked = (TextView)findViewById(R.id.app_cert_state);
		mCancel = (ImageButton)findViewById(R.id.cancel);
		mUninstall = (ImageButton)findViewById(R.id.uninstall);
		
		if(mPm != null){
			mIcon.setImageDrawable(mAppEntry.getInfo().applicationInfo.loadIcon(mPm));
			mAppName.setText(mAppEntry.getInfo().applicationInfo.loadLabel(mPm));
			mAppName1.setText(mAppEntry.getInfo().applicationInfo.loadLabel(mPm));
			
			if ((mAppEntry.mAppCertState & GuardianApp.APP_WITH_ANJO_AKI_NOT_REVOKED) != 0){
				mCheckBox.setImageResource(R.drawable.green);
				mAppRevoked.setText(getString(R.string.verified));
				mAppRevoked.setTextColor(Color.GREEN);
			}else if((mAppEntry.mAppCertState & GuardianApp.APP_WITH_ANJO_AKI_REVOKED) != 0){
				mCheckBox.setImageResource(R.drawable.red);
				mAppRevoked.setText(getString(R.string.revoked));
				mAppRevoked.setTextColor(Color.RED);
			}else  if((mAppEntry.mAppCertState & GuardianApp.APP_WITHOUT_ANJO_AKI) != 0){
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
			
			/**
			 * start to uninstall the application.
			 * Send the intent to package manager to uninstall package.
			 */
			mUninstall.setOnClickListener(new View.OnClickListener() {
	            public void onClick(View v) {
	                if(DEBUG) Log.d(TAG, "mCancel clicked");
	                String pakcageName = mAppEntry.getInfo().packageName;
	                Uri packageURI = Uri.parse("package:"+pakcageName);
	                Intent uninstallIntent = new Intent(Intent.ACTION_DELETE, packageURI);
	                startActivity(uninstallIntent);
	            }
	        });
		}
	}
	
}
