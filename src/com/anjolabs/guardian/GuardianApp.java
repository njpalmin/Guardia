package com.anjolabs.guardian;

import java.io.IOException;
import java.io.InputStream;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.anjolabs.guardian.GuardianUtils.appComparator;

import android.app.Application;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.util.Log;

public class GuardianApp extends Application implements OnSharedPreferenceChangeListener{
    static final String TAG = "GuardianApp";
    static final boolean DEBUG=true;
    
    public static final String APPS_LIST = "com.anjolabs.guardian.appslist";
    public static final String APPS_ENTRY = "com.anjolabs.guardian.appsentry";
    
    public final static int APP_WITHOUT_ANJO_AKI = 1<<0;
    public final static int APP_WITH_ANJO_AKI_REVOKED = 1<<1;
    public final static int APP_WITH_ANJO_AKI_NOT_REVOKED = 1<<2;
    
    //Intents
    public final static String GUARDIAN_RESET_INTERVAL_ACTION="com.anjolabs.guardian.GUARDIAN_RESET_INTERVAL_ACTION";
    public final static String GUARDIAN_CONFIGURATION_CHANGED_ACTION="com.anjolabs.guardian.GUARDIAN_CONFIGURATION_CHANGED_ACTION";
    
    public final static String PREFS_INTERVAL ="prefs_interval";
    public final static String PREFS_ANJOCHECK ="prefs_anjocheck";
    
    public static int DEFAULT_CHECK_INTERVAL = 1;//default 60 mins
    
    //Message codes mHandler use
    private static final int RESET_TIME_INTERVAL = 100;
    private static final int START_UPDATE = 101;
    private static final int STOP_UPDATE = 102;
    
    
    private static GuardianApp sMe;
    private Context mContext;
    public int mInterval;
    public boolean mAnjoCheck;
    public X509Certificate  mX509Cert; 
    public PackageManager mPm;
    public SharedPreferences mPrefs;
    
    public GuardianApp() {
        sMe = this;
    }
    
    static GuardianApp getInstance() {
        return sMe;
    }
    
    private Handler mHandler = new Handler(){
    	@Override
    	public void handleMessage(Message msg) {
    		switch (msg.what){
    			case RESET_TIME_INTERVAL:
    				mHandler.removeCallbacks(mUpdatePkgListTask);
    				mInterval = msg.arg1;
    				if(mInterval == 0){
    					Message message = Message.obtain(mHandler,STOP_UPDATE,0,0);
    					mHandler.sendMessage(message);
    				}
    				mHandler.postDelayed(mUpdatePkgListTask,mInterval*60*1000);
    				break;
    			case START_UPDATE:
    				mHandler.postDelayed(mUpdatePkgListTask,mInterval*60*1000);
    				break;
    			case STOP_UPDATE:
    				mHandler.removeCallbacks(mUpdatePkgListTask);
    				break;
    		}
    	}
    };
    
    private Runnable mUpdatePkgListTask = new Runnable() {  
  	  
        public void run() {  
            // TODO Auto-generated method stub  
        		startPkgListActivity();
        		Message msg = Message.obtain(mHandler,START_UPDATE,0,0);
                mHandler.sendMessage(msg);
        }  
    };

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {
			// TODO Auto-generated method stub
			String action = intent.getAction();
			if(action.equals(GUARDIAN_CONFIGURATION_CHANGED_ACTION)){
				if(DEBUG)Log.d(TAG,"onReceive get Action");
				mInterval =intent.getIntExtra("interval",DEFAULT_CHECK_INTERVAL);
				mAnjoCheck = intent.getBooleanExtra("anjocheck",false);
				Message msg = Message.obtain(mHandler,RESET_TIME_INTERVAL,mInterval,0);
				mHandler.sendMessage(msg);
			}
		}
    };
    
    @Override
    public void onCreate(){
    	if(DEBUG) Log.d(TAG,"onCreate....");
    	super.onCreate();
    	mContext = getApplicationContext();
    	
    	registerReceiver(mReceiver, new IntentFilter(GUARDIAN_RESET_INTERVAL_ACTION));
    	PreferenceManager.getDefaultSharedPreferences(this).registerOnSharedPreferenceChangeListener(this);
    	
    	loadSharedPreferences();
    	
        mPrefs =PreferenceManager.getDefaultSharedPreferences(this);
        mPrefs.registerOnSharedPreferenceChangeListener(this);
        onSharedPreferenceChanged(mPrefs,null);
        
        InputStream is = null;
        try{
        	is = getResources().openRawResource(R.raw.anjoprivateca01);
        	CertificateFactory factory = CertificateFactory.getInstance("X.509");
        	mX509Cert = (X509Certificate) factory.generateCertificate(is);
        }catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e.toString());
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException ignored) {}
            }
        }
    	
        mPm = getPackageManager();
        
		Message msg = Message.obtain(mHandler,START_UPDATE,0,0);
        mHandler.sendMessage(msg);
    }
    
    
	private void loadSharedPreferences(){
		mPrefs = PreferenceManager.getDefaultSharedPreferences(this);
		mInterval = mPrefs.getInt(PREFS_INTERVAL,GuardianApp.DEFAULT_CHECK_INTERVAL);
		mAnjoCheck = mPrefs.getBoolean(PREFS_ANJOCHECK,true);
	}
	
	private void startPkgListActivity(){
		if(DEBUG)Log.d(TAG,"startPkgListActivity");
		new updatePacakgeListTask(this).execute();
	}
	
	public class updatePacakgeListTask extends AsyncTask<Void, Integer, ArrayList<AppEntry>>{
		private Context mContext;
		
		public updatePacakgeListTask(Context context){
			super();
			mContext = context;
		}
		
		protected void onPreExecute(){
		}
	
		@Override
		protected  ArrayList<AppEntry> doInBackground(Void... params) {
			// TODO Auto-generated method stub
			if(mPm == null){
				mPm = getPackageManager();
			}
			ArrayList<AppEntry> appList = new ArrayList<AppEntry>();
			List<PackageInfo> mPackages =mPm.getInstalledPackages(0);
			
			for(int i=0;i<mPackages.size();i++){
				if(GuardianUtils.filterApp(mPackages.get(i).applicationInfo)){
					AppEntry appEntry = new AppEntry(mPackages.get(i));
					GuardianUtils.collectCertificates(appEntry);
					if(mAnjoCheck){
						if((appEntry.mAppCertState & (APP_WITH_ANJO_AKI_NOT_REVOKED|APP_WITH_ANJO_AKI_REVOKED)) != 0){
							appList.add(appEntry);
						}
					}else{
						appList.add(appEntry);
					}
				}
			}
			Collections.sort(appList, (new appComparator()));
			
			return appList;
		}
		
		@Override
		protected void onPostExecute(ArrayList<AppEntry> appList){
			Intent intent = new Intent(mContext, PackageListActivity.class);
			intent.putParcelableArrayListExtra(APPS_LIST,appList);
			intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			startActivity(intent);
		}
	}

	@Override
	public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
		// TODO Auto-generated method stub
		if(DEBUG)Log.d(TAG,"Prefs Changed key:"+key);
		
		if(key == null){
			return;
		}
		
		if(key.equals(PREFS_INTERVAL)){
			mInterval = prefs.getInt(PREFS_INTERVAL,GuardianApp.DEFAULT_CHECK_INTERVAL);
			Message msg = Message.obtain(mHandler,RESET_TIME_INTERVAL,mInterval,0);
			mHandler.sendMessage(msg);
		}else if(key.equals(PREFS_ANJOCHECK)){
			mAnjoCheck = mPrefs.getBoolean(PREFS_ANJOCHECK,true);
		}
	}
}