package com.anjolabs.guardian;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.URLConnection;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.cert.X509CRL;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

public class Guardian extends Activity {
    static final String TAG = "Guardian";
    static final boolean DEBUG=false;
    public static final String APPS_LIST = "com.anjolabs.guardian.appslist";
    public static final String APPS_ENTRY = "com.anjolabs.guardian.appsentry";
    
    public final static int APP_WITHOUT_ANJO_AKI = 1<<0;
    public final static int APP_WITH_ANJO_AKI_REVOKED = 1<<1;
    public final static int APP_WITH_ANJO_AKI_NOT_REVOKED = 1<<2;
    static final int CONNECTION_TIMEOUT = 30000;
    
    private Button mRunButton;
    private Button mExitButton;
    private ListView mListView;
    private PackageManager mPm;
    List<ApplicationInfo> mApplications = new ArrayList<ApplicationInfo>();
    List<PackageInfo> mPackages = new ArrayList<PackageInfo>();
    List<ApplicationInfo> mThirdPartyApplications = new ArrayList<ApplicationInfo>();
    ArrayList<AppEntry> mAppList;


	private AppEntry mAppEntry;
    private Context mContext;
    private static final Object mSync = new Object();
    private static WeakReference<byte[]> mReadBuffer;
    private X509Certificate  mX509Cert;
    
   
    
    /** Called when the activity is first created. */
	@Override
    public void onCreate(Bundle savedInstanceState) {
    	Log.d(TAG,"onCreate");
        super.onCreate(savedInstanceState);

        mContext =this;
         
        setContentView(R.layout.guardian_main);
        mRunButton = (Button)findViewById(R.id.run);
        mExitButton = (Button)findViewById(R.id.exit);
        
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
		if(DEBUG) Log.d(TAG,"X509Cert IssuerDN: "+mX509Cert.getIssuerDN().getName());
		if(DEBUG) Log.d(TAG,"X509Cert SubDN: "+mX509Cert.getSubjectDN().getName());
        mPm = getPackageManager();
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
			//mApplications = mPm.getInstalledApplications(PackageManager.GET_UNINSTALLED_PACKAGES);
			mAppList = new ArrayList<AppEntry>();
			mPackages = mPm.getInstalledPackages(0);
			if(DEBUG)Log.d(TAG,"mPackages size="+mPackages.size());
			
			for(int i=0;i<mPackages.size();i++){
				if(filterApp(mPackages.get(i).applicationInfo)){
					mAppEntry = new AppEntry(mPackages.get(i));
					collectCertificates(mAppEntry);
					if(DEBUG) Log.d(TAG,"AppEntry state"+mAppEntry.mAppCertState);
					mAppList.add(mAppEntry);
					mAppEntry = null;
				}
				publishProgress((int) ((i / (float) mPackages.size()) * 100));
			}
			return mAppList;
		}
		
		//Get Application in /data/app
		private boolean filterApp(ApplicationInfo info){
            if ((info.flags & ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0) {
                return true;
            } else if ((info.flags & ApplicationInfo.FLAG_SYSTEM) == 0) {
                return true;
            }
			return false;
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

			Intent intent = new Intent(Guardian.this, PackageListActivity.class);
			intent.putParcelableArrayListExtra(APPS_LIST,mAppList);
			//intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
			startActivity(intent);
			//finish();
		}
	}
    
    private void collectCertificates(AppEntry appEntry){
    	 File sourceFile;
    	 
      	 WeakReference<byte[]> readBufferRef;
         byte[] readBuffer = null;
         
         synchronized (mSync) {
             readBufferRef = mReadBuffer;
             if (readBufferRef != null) {
                 mReadBuffer = null;
                 readBuffer = readBufferRef.get();
             }
             if (readBuffer == null) {
                 readBuffer = new byte[8192];
                 readBufferRef = new WeakReference<byte[]>(readBuffer);
             }
         }
    
    	 sourceFile = new File(((appEntry.getInfo()).applicationInfo).sourceDir);
    	 Certificate[] certs = null;
    	 try{
    		 
    		 JarFile jarFile = new JarFile(sourceFile);
    		
             JarEntry jarEntry = jarFile.getJarEntry("AndroidManifest.xml");
             certs = loadCertificates(jarFile, jarEntry, readBuffer);
             if(DEBUG) Log.d(TAG, "File " + sourceFile + ": entry=" + jarEntry
                     + " certs=" + (certs != null ? certs.length : 0));
             if (certs != null) {
            	 final int N = certs.length;
                 for (int i=0; i<N; i++) {
                	 if(certs[i] instanceof X509Certificate){
						X509Certificate x509 = (X509Certificate) certs[i];
						if(DEBUG) Log.d(TAG,"X509Cert IssuerDN: "+x509.getIssuerDN().getName());
						CertInfo certInfo = new CertInfo(x509);
						if(certInfo.hasVeriSignIssuer(mX509Cert)){
							appEntry.mAppCertState |= Guardian.APP_WITH_ANJO_AKI_REVOKED;
							X509CRL crl = getX509CRL(certInfo);
							if(DEBUG)Log.d(TAG,"CRL:"+crl);
							if( crl != null){
								appEntry.setRevoked(crl.isRevoked(x509));
							}else{
								appEntry.setRevoked(true);
							}
							if(!appEntry.isRevoked()){
								appEntry.mAppCertState |= Guardian.APP_WITH_ANJO_AKI_NOT_REVOKED;
							}

						}else{
							appEntry.mAppCertState |= Guardian.APP_WITHOUT_ANJO_AKI;
						}
                	 }
                 }            	 
             }
            
             /*
             Enumeration entries = jarFile.entries();
             
             while (entries.hasMoreElements()) {
            	 JarEntry je = (JarEntry)entries.nextElement();
            	 if (je.isDirectory()) continue;
                 if (je.getName().startsWith("META-INF/")) continue;
                 certs = loadCertificates(jarFile, je,readBuffer);
       
                 if(DEBUG)Log.d(TAG, "File " + sourceFile + " entry " + je.getName()
                             + ": certs=" + certs + " ("
                             + (certs != null ? certs.length : 0) + ")");
                    
                 if (certs != null) {
                     final int N = certs.length;
                     for (int i=0; i<N; i++) {
                    	 if(certs[i] instanceof X509Certificate){
     						X509Certificate x509 = (X509Certificate) certs[i];
     						if(DEBUG) Log.d(TAG,"X509Cert IssuerDN: "+x509.getIssuerDN().getName());
     						CertInfo certInfo = new CertInfo(x509);
     						if(certInfo.hasVeriSignIssuer(mX509Cert)){
     							appEntry.mAppCertState |= Guardian.APP_WITH_ANJO_AKI_REVOKED;
     							X509CRL crl = getX509CRL(certInfo);
     							if( crl != null){
     								if(DEBUG)Log.d(TAG,"CRL:"+crl);
     								if(DEBUG)Log.d(TAG,"Revoked:"+crl.isRevoked(x509));
     								//appEntry.setRevoked(crl.isRevoked(mX509Cert));
     								appEntry.setRevoked(crl.isRevoked(x509));
     							}else{
     								appEntry.setRevoked(true);
     							}
     							if(!appEntry.isRevoked()){
     								appEntry.mAppCertState |= Guardian.APP_WITH_ANJO_AKI_NOT_REVOKED;
     							}

     						}else{
     							appEntry.mAppCertState |= Guardian.APP_WITHOUT_ANJO_AKI;
     						}
                     	 }
                     }
                 }
                 if (certs == null) {
                     Log.e(TAG, "Package " + (appEntry.getInfo()).packageName
                             + " has no certificates at entry "
                             + je.getName() + "; ignoring!");
                     jarFile.close();
                     return;
                 }
             }*/
             jarFile.close();

             synchronized (mSync) {
                 mReadBuffer = readBufferRef;
             }
             /*
	         if (certs != null && certs.length > 0) {
	             final int N = certs.length;
	             appEntry.mSignatures =new Signature[certs.length];
	             for (int i=0; i<N; i++) {
	            	 appEntry.mSignatures[i] = new Signature(certs[i].getEncoded());
	             }
	         } else {
	             Log.e(TAG, "Package " + appEntry.getInfo().packageName
	                     + " has no certificates; ignoring!");
	             return;
	         }
         }catch (CertificateEncodingException e) {
             Log.e(TAG, "Exception reading " + sourceFile, e);
             return ;*/
         } catch (IOException e) {
             Log.e(TAG, "Exception reading " + sourceFile, e);
             return;
         } catch (RuntimeException e) {
             Log.e(TAG, "Exception reading " + sourceFile, e);
             return;
         }
     }
     
     private Certificate[] loadCertificates(JarFile jarFile, JarEntry je,
             byte[] readBuffer) {
         try {
             // We must read the stream for the JarEntry to retrieve
             // its certificates.
             InputStream is = new BufferedInputStream(jarFile.getInputStream(je));
             while (is.read(readBuffer, 0, readBuffer.length) != -1) {
                 // not using
             }
             is.close();
             return je != null ? je.getCertificates() : null;
         } catch (IOException e) {
             Log.e(TAG, "Exception reading " + je.getName() + " in "
                     + jarFile.getName(), e);
         } catch (RuntimeException e) {
             Log.e(TAG, "Exception reading " + je.getName() + " in "
                     + jarFile.getName(), e);
         }
         return null;
     }
     
     private X509CRL getX509CRL(CertInfo certinfo){
    	 X509CRL crl = null;
    	 String cdp = certinfo.getFullNameCDP();
		 InputStream is = null;
    	 if(certinfo.isHttpCDP()){
			try {
				 // handle PKCS #7 CRLs by getting the X509 CRL
				 if (cdp.endsWith("LatestCRL")) {
					 cdp = cdp + ".crl";
				 }
				 //if(DEBUG) Log.d(TAG,"cdp:"+ cdp);
				 URL url = new URL(cdp);
				 //URLConnection urlConnection = url.openConnection();
				 //urlConnection.setConnectTimeout(CONNECTION_TIMEOUT);
				 
				 //is = new BufferedInputStream(urlConnection.getInputStream());

				 is = url.openStream();
				 //if(DEBUG) Log.d(TAG,"is:"+ is);
				 //readStream(is);
				 CertificateFactory cf = CertificateFactory.getInstance("X.509");
				 crl = (X509CRL)cf.generateCRL(is);
				 is.close();
			}catch (Exception e) {
	         	e.printStackTrace();
	         	/*
	         	Toast toast;
	         	toast = Toast.makeText(mContext,"Can't connect to server to get CRL",1);
	         	toast.show();*/
	         	return null;
	         }
    	 }
    	 return crl;
     }
     
     private boolean isOnline() {
    	 ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
    	 return cm.getActiveNetworkInfo().isConnectedOrConnecting();

     }
     
}