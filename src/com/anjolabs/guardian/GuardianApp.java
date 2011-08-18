package com.anjolabs.guardian;

import java.io.IOException;
import java.io.InputStream;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import android.app.Application;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
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
import android.widget.RemoteViews;

import com.anjolabs.guardian.GuardianUtils.appComparator;
/**
 * 
 * @author Alphalilin@gmail.com
 *
 */
public class GuardianApp extends Application implements OnSharedPreferenceChangeListener{
    static final String TAG = "GuardianApp";
    static final boolean DEBUG=false;
    
    public static final String APPS_LIST = "com.anjolabs.guardian.appslist";
    public static final String APPS_ENTRY = "com.anjolabs.guardian.appsentry";
    
    public final static int APP_WITHOUT_ANJO_AKI = 1<<0;
    public final static int APP_WITH_ANJO_AKI_NOT_REVOKED = 1<<1;
    public final static int APP_WITH_ANJO_AKI = 1<<2;
    
    //Intents
    public final static String GUARDIAN_RESET_INTERVAL_ACTION="com.anjolabs.guardian.GUARDIAN_RESET_INTERVAL_ACTION";
    public final static String GUARDIAN_CONFIGURATION_CHANGED_ACTION="com.anjolabs.guardian.GUARDIAN_CONFIGURATION_CHANGED_ACTION";
    
    public final static String PREFS_INTERVAL ="prefs_interval";
    public final static String PREFS_ANJOCHECK ="prefs_anjocheck";
    
    public static int DEFAULT_CHECK_INTERVAL = 60;//default 60 mins
    
    public static final String APP_CHG = "change";
    
    //Message codes mHandler use
    private static final int RESET_TIME_INTERVAL = 100;
    private static final int START_UPDATE = 101;
    private static final int STOP_UPDATE = 102;
    
    
    private static GuardianApp sMe;
    private Context mContext;
    private boolean  mRevokedFound = false;
    public int mInterval;
    public boolean mAnjoCheck;
    public X509Certificate  mX509Cert; 
    public PackageManager mPm;
    public SharedPreferences mPrefs;
    public NotificationManager mNm;
    
    
    public GuardianApp() {
        sMe = this;
    }
    
    static GuardianApp getInstance() {
        return sMe;
    }
    
    /**
     * This handler is used to start a checking thread to parse packages.
     */
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
    
    /**
     * This task is used to update package list.
     */
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
    
    /**
     * Called when the application is starting, before any other application
     * objects have been created.  Implementations should be as quick as
     * possible (for example using lazy initialization of state) since the time
     * spent in this function directly impacts the performance of starting the
     * first activity, service, or receiver in a process.
     * If you override this method, be sure to call super.onCreate().
     */
    @Override
    public void onCreate(){
    	if(DEBUG) Log.d(TAG,"GuardianApp onCreate....");
    	super.onCreate();
    	mContext = getApplicationContext();
    	
    	registerReceiver(mReceiver, new IntentFilter(GUARDIAN_RESET_INTERVAL_ACTION));
    	PreferenceManager.getDefaultSharedPreferences(this).registerOnSharedPreferenceChangeListener(this);
    	
    	loadSharedPreferences();
    	
        mPrefs =PreferenceManager.getDefaultSharedPreferences(this);
        mPrefs.registerOnSharedPreferenceChangeListener(this);
        onSharedPreferenceChanged(mPrefs,null);
        
        /**
         * Get the pre-built certificate. 
         */
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
        mNm = (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);
        
        /**
         * Also start the monitor thread.
         */
		Message msg = Message.obtain(mHandler,START_UPDATE,0,0);
        mHandler.sendMessage(msg);
    }
    
    /**
     * Read the settings values(interval and anjo check) from sharedPrefrences.
     */
	private void loadSharedPreferences(){
		mPrefs = PreferenceManager.getDefaultSharedPreferences(this);
		mInterval = mPrefs.getInt(PREFS_INTERVAL,GuardianApp.DEFAULT_CHECK_INTERVAL);
		mAnjoCheck = mPrefs.getBoolean(PREFS_ANJOCHECK,true);
	}
	
	/**
	 * When time interval is timeout, it will start the thread to parse the applications and get the certificate information.
	 */
	private void startPkgListActivity(){
		if(DEBUG)Log.d(TAG,"startPkgListActivity");
		new updatePacakgeListTask(this).execute();
	}

	/**
	 * 
	 */
	private void showNotification(PendingIntent pendingIntent){
		if(DEBUG)Log.d(TAG,"showNotification pendingIntent="+pendingIntent);
		
		Notification notification=new Notification();
		String text=null;

		if(mRevokedFound){
		    notification = new Notification(
	                R.drawable.statusbarred,
	                getText(R.string.revoked_notification), System.currentTimeMillis());
		    text = getText(R.string.revoked_notification).toString();
		}else{
		    notification = new Notification(
	                R.drawable.statusbar,
	                getText(R.string.verified_notification), System.currentTimeMillis());
		    text = getText(R.string.verified_notification).toString();
		}
		
		notification.setLatestEventInfo(mContext, text, 
									getText(R.string.notification_text),pendingIntent);
		
		if(mNm != null){
			mNm.notify(R.string.revoked_notification, notification);
		}
    }
	/**
	 * 
	 * This task to background task to check applications in /data/apps and collect their certificates information.
	 * @author alphalilin@gmail.com
	 *
	 */
	public class updatePacakgeListTask extends AsyncTask<Void, Integer, ArrayList<AppEntry>>{
		private Context mContext;
		
		public updatePacakgeListTask(Context context){
			super();
			mContext = context;
		}
		
	    /**
	     * Runs on the UI thread before {@link #doInBackground}.
	     *
	     */
		protected void onPreExecute(){
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
		protected  ArrayList<AppEntry> doInBackground(Void... params) {
			// TODO Auto-generated method stub
			if(mPm == null){
				mPm = getPackageManager();
			}
			ArrayList<AppEntry> appList = new ArrayList<AppEntry>();
			List<PackageInfo> mPackages =mPm.getInstalledPackages(0);
			
			mRevokedFound = false;
			
			for(int i=0;i<mPackages.size();i++){
				if(GuardianUtils.filterApp(mPackages.get(i).applicationInfo)){
					AppEntry appEntry = new AppEntry(mPackages.get(i));
					GuardianUtils.collectCertificates(appEntry);
					
					if(DEBUG) Log.d(TAG,"AppEntry state:"+appEntry.mAppCertState);
					
					if((appEntry.mAppCertState & APP_WITH_ANJO_AKI) != 0){
						
						if(!((appEntry.mAppCertState & APP_WITH_ANJO_AKI_NOT_REVOKED) != 0) && !mRevokedFound){
							mRevokedFound = true;
						}
					}
					
					if(mAnjoCheck){
						if((appEntry.mAppCertState & (APP_WITH_ANJO_AKI_NOT_REVOKED|APP_WITH_ANJO_AKI)) != 0){
							appList.add(appEntry);
						}
					}else{
						appList.add(appEntry);
					}
					
					appEntry = null;
				}
			}
			Collections.sort(appList, (new appComparator()));
			
			if(DEBUG)Log.d(TAG,"mRevokedFound:"+mRevokedFound);
			return appList;
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
		protected void onPostExecute(ArrayList<AppEntry> appList){
			if(DEBUG)Log.d(TAG,"AppList:"+appList);
			Intent intent = new Intent(mContext, PackageListActivity.class);
			intent.putParcelableArrayListExtra(APPS_LIST,appList);
			//intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			PendingIntent pendingIntent = PendingIntent.getActivity(mContext,0,intent,PendingIntent.FLAG_UPDATE_CURRENT);
			showNotification(pendingIntent);
		}
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
		if(DEBUG)Log.d(TAG,"Prefs Changed key:"+key);
		
		if(key == null){
			return;
		}
		
		
		if(key.equals(PREFS_INTERVAL)){
			/**
			 * reset time interval.
			 */
			mInterval = prefs.getInt(PREFS_INTERVAL,GuardianApp.DEFAULT_CHECK_INTERVAL);
			Message msg = Message.obtain(mHandler,RESET_TIME_INTERVAL,mInterval,0);
			mHandler.sendMessage(msg);
		}else if(key.equals(PREFS_ANJOCHECK)){
			mAnjoCheck = mPrefs.getBoolean(PREFS_ANJOCHECK,true);
		}
	}
}
