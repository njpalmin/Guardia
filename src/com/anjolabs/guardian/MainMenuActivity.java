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

/*
 * MainMenuActivity is the main entry of Guardian application, and it's the only one.
 * In MainMenuActivity, it describes the layout of the main menu.
 */
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
	
	/*
	 * This is  main menu layout initialization.
	 */
	void initView(){
        mRunButton = (ImageButton)findViewById(R.id.run);
        mSetButton = (ImageButton)findViewById(R.id.set);
        mExitButton = (ImageButton)findViewById(R.id.exit);
        
        mRunButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if(DEBUG) Log.d(TAG, "mRunButton clicked");
                
                new getPkgListTask().execute();
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
	
	/**
	 * (non-Javadoc)
	 * @see android.app.Activity#onDestroy()
	 */
	@Override
    protected void onDestroy() {
		if(DEBUG) Log.d(TAG,"onDestroy");
		super.onDestroy();
		mPrefs.unregisterOnSharedPreferenceChangeListener(this);
	}
	
	/**
	 * getPkgListTask is the background task when user pressing "Run Manual Check" button.
	 * This is background thread to get the applications certificates info, and show the progress bar
	 * in the main Thread. 
	 */
	public class getPkgListTask extends AsyncTask<Void, Integer, List<AppEntry>>{
		int mProgress;
		ProgressBar mProgressBar;
		TextView mProgressText;
		
		/**
		 * Constructor, init progress bar and text.
		 */
		public getPkgListTask(){
			super();
			mProgressBar = (ProgressBar)findViewById(R.id.progress);
			mProgressText = (TextView)findViewById(R.id.progress_percentage);
		}
		
	    /**
	     * Runs on the UI thread before {@link #doInBackground}.
	     *
	     */
		protected void onPreExecute(){
			mProgress = 0;
			mProgressBar.setProgress(0);
			mRunButton.setClickable(false);
		}
		
		/**
		 * Override this method to perform a computation on a background thread. The
	     * specified parameters are the parameters passed to {@link #execute}
	     * by the caller of this task.
	     *
	     * This method can call {@link #publishProgress} to publish updates
	     * on the UI thread.
	     *
	     * @param params The parameters of the task.
	     *
	     * @return A result, defined by the subclass of this task.
	     */
		@Override
		protected  List<AppEntry> doInBackground(Void... params) {
			// TODO Auto-generated method stub
			if(mPm == null){
				mPm = getPackageManager();
			}
			mAppList = new ArrayList<AppEntry>();
			mPackages = mPm.getInstalledPackages(0);
			if(DEBUG)Log.d(TAG,"mPackages size="+mPackages.size());
			
			/*
			 * Get the packages in /data/apps.. and check these applications certificates information.
			 * Add these applications in another list by the special order.
			 */
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
				}
				
				publishProgress((int) ((i / (float) mPackages.size()) * 100));
			}
			Collections.sort(mAppList, (new appComparator()));

			return mAppList;
		}
		
	    /**
	     * Runs on the UI thread after {@link #publishProgress} is invoked.
	     * The specified values are the values passed to {@link #publishProgress}.
	     *
	     * @param values The values indicating progress.
	     *
	     * @see #publishProgress
	     * @see #doInBackground
	     */
		@Override 
		protected void onProgressUpdate(Integer... values){
			mProgressBar.setProgress(values[0]);
			CharSequence progressText = getString(R.string.percentage)+" "+String.valueOf(values[0])+"%";
			mProgressText.setText(progressText);
		}
		
	    /**
	     * Runs on the UI thread after {@link #doInBackground}. The
	     * specified result is the value returned by {@link #doInBackground}
	     * or null if the task was cancelled or an exception occured.
	     *
	     * @param result The result of the operation computed by {@link #doInBackground}.
	     *
	     * @see #onPreExecute
	     * @see #doInBackground
	     */
		@Override
		protected void onPostExecute(List<AppEntry> application){
			CharSequence progressText = getString(R.string.finish)+" 100%";
			
			mProgressText.setText(progressText);
			mRunButton.setClickable(true);
			if(DEBUG) Log.d(TAG,"onPostExecute application size="+application.size());

			/**
			 * Start the activity to list the parsed applications.
			 */
			Intent intent = new Intent(MainMenuActivity.this, PackageListActivity.class);
			intent.putParcelableArrayListExtra(GuardianApp.APPS_LIST,mAppList);
			startActivity(intent);

		}
	}
      	
	/**
	 * Load the settings values(interval and anjo check) from shared preferences.
	 */
 	private void loadSharedPreferences(){
		mInterval = mPrefs.getInt(GuardianApp.PREFS_INTERVAL,GuardianApp.DEFAULT_CHECK_INTERVAL);
		mAnjoCheck = mPrefs.getBoolean(GuardianApp.PREFS_ANJOCHECK,true);
 	}

    /**
     * Called when a shared preference is changed, added, or removed. This
     * may be called even if a preference is set to its existing value.
     *
     * <p>This callback will be run on your main thread.
     *
     * @param sharedPreferences The {@link SharedPreferences} that received
     *            the change.
     * @param key The key of the preference that was changed, added, or
     *            removed.
     */
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