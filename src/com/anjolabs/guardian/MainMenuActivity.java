package com.anjolabs.guardian;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.anjolabs.guardian.GuardianUtils.appComparator;

public class MainMenuActivity extends Activity implements OnSharedPreferenceChangeListener{
    static final String TAG = "MainMenuActivity";
    static final boolean DEBUG = GuardianApp.DEBUG;
    private ImageButton mRunButton;
    private ImageButton mSetButton;
    private ImageButton mExitButton;
    private PackageManager mPm;
    List<ApplicationInfo> mApplications = new ArrayList<ApplicationInfo>();
    List<PackageInfo> mPackages = new ArrayList<PackageInfo>();
    List<ApplicationInfo> mThirdPartyApplications = new ArrayList<ApplicationInfo>();
    ArrayList<AppEntry> mAppList;

	private AppEntry mAppEntry;
    private Context mContext;
    private boolean mAnjoCheck;
    private int mInterval;
    private SharedPreferences mPrefs;
    
    /** Called when the activity is first created. */
	@Override
    public void onCreate(Bundle savedInstanceState) {
    	Log.d(TAG,"onCreate");
        super.onCreate(savedInstanceState);

        mContext =this;
         
        setContentView(R.layout.guardian_main);

        initView();//Init MainMenu Screen
        
        mPrefs = GuardianApp.getInstance().mPrefs;
        loadSharedPreferences();
        
        mPrefs =PreferenceManager.getDefaultSharedPreferences(this);
        mPrefs.registerOnSharedPreferenceChangeListener(this);
        onSharedPreferenceChanged(mPrefs,null);
        
        if(mPm == null){
        	mPm = getPackageManager();
        }
    }
	
	void initView(){
        mRunButton = (ImageButton)findViewById(R.id.run);
        mSetButton = (ImageButton)findViewById(R.id.set);
        mExitButton = (ImageButton)findViewById(R.id.exit);
        
        mRunButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if(DEBUG) Log.d(TAG, "mRunButton clicked");
    
                if(isOnline()){
                	new getPkgListTask().execute();
                }else{
                	Toast toast;
                	toast=Toast.makeText(mContext,"Can't connect to Internet,please check!",2);
                	toast.show();
                }
            }
        });
        
        mExitButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if(DEBUG) Log.d(TAG, "mExitButton clicked");
                finish();	
            }
        });    
        
        mSetButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if(DEBUG) Log.d(TAG, "mSetButton clicked");
                Intent intent = new Intent(MainMenuActivity.this, SettingsActivity.class);
                startActivity(intent);
            }
        });
        
	}
	
	@Override
    protected void onResume(){
		if(DEBUG) Log.d(TAG,"onResume");
		super.onResume();
	}
	
	@Override
    protected void onDestroy() {
		if(DEBUG) Log.d(TAG,"onDestroy");
		super.onDestroy();
		mPrefs.unregisterOnSharedPreferenceChangeListener(this);
	}
	
	
	
	public class getPkgListTask extends AsyncTask<Void, Integer, List<AppEntry>>{
		int mProgress;
		ProgressBar mProgressBar;
		TextView mProgressText;
		
		public getPkgListTask(){
			super();
			mProgressBar = (ProgressBar)findViewById(R.id.progress);
			mProgressText = (TextView)findViewById(R.id.progress_percentage);
		}
		
		protected void onPreExecute(){
			mProgress = 0;
			mProgressBar.setProgress(0);
			mRunButton.setClickable(false);
		}

		@Override
		protected  List<AppEntry> doInBackground(Void... params) {
			// TODO Auto-generated method stub
			if(mPm == null){
				mPm = getPackageManager();
			}
			mAppList = new ArrayList<AppEntry>();
			mPackages = mPm.getInstalledPackages(0);
			if(DEBUG)Log.d(TAG,"mPackages size="+mPackages.size());
			
			for(int i=0;i<mPackages.size();i++){
				if(GuardianUtils.filterApp(mPackages.get(i).applicationInfo)){
					mAppEntry = new AppEntry(mPackages.get(i));
					GuardianUtils.collectCertificates(mAppEntry);
					if(DEBUG) Log.d(TAG,"AppEntry state"+mAppEntry.mAppCertState);
					if(mAnjoCheck){
						if((mAppEntry.mAppCertState & (GuardianApp.APP_WITH_ANJO_AKI_NOT_REVOKED 
														| GuardianApp.APP_WITH_ANJO_AKI_REVOKED)) != 0){
							mAppList.add(mAppEntry);
						}
					}else{
						mAppList.add(mAppEntry);
					}
					mAppEntry = null;
					//Collections.sort(mAppList, (new appComparator()));
				}
				publishProgress((int) ((i / (float) mPackages.size()) * 100));
			}
			
			//GuardianUtils.appComparator comparator = new GuardianUtils.appComparator ();
			
			Collections.sort(mAppList, (new appComparator()));
			
			return mAppList;
		}
		
		@Override 
		protected void onProgressUpdate(Integer... values){
			mProgressBar.setProgress(values[0]);
			CharSequence progressText = getString(R.string.percentage)+" "+String.valueOf(values[0])+"%";
			mProgressText.setText(progressText);
		}
		
		@Override
		protected void onPostExecute(List<AppEntry> application){
			CharSequence progressText = getString(R.string.finish)+" 100%";
			
			mProgressText.setText(progressText);
			mRunButton.setClickable(true);
			if(DEBUG) Log.d(TAG,"onPostExecute application size="+application.size());

			Intent intent = new Intent(MainMenuActivity.this, PackageListActivity.class);
			intent.putParcelableArrayListExtra(GuardianApp.APPS_LIST,mAppList);
			startActivity(intent);

		}
	}
    
     private boolean isOnline() {
    	 ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
    	 return cm.getActiveNetworkInfo().isConnectedOrConnecting();

     }
      	
 	private void loadSharedPreferences(){
		mInterval = mPrefs.getInt(GuardianApp.PREFS_INTERVAL,GuardianApp.DEFAULT_CHECK_INTERVAL);
		mAnjoCheck = mPrefs.getBoolean(GuardianApp.PREFS_ANJOCHECK,true);
 	}

	@Override
	public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
		// TODO Auto-generated method stub
		if(key == null){
			return;
		}
		if(key.equals(GuardianApp.PREFS_INTERVAL)){
			mInterval = prefs.getInt(GuardianApp.PREFS_INTERVAL,GuardianApp.DEFAULT_CHECK_INTERVAL);
		}else if(key.equals(GuardianApp.PREFS_ANJOCHECK)){
			mAnjoCheck = mPrefs.getBoolean(GuardianApp.PREFS_ANJOCHECK,true);
		}
	}
}