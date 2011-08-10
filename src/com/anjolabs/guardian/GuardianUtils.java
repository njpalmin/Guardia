package com.anjolabs.guardian;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.net.URL;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.cert.X509CRL;
import java.security.cert.X509Certificate;
import java.util.Comparator;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import android.content.pm.ApplicationInfo;
import android.util.Log;

public class GuardianUtils {
    static final String TAG = "GuardianUtils";
    static final boolean DEBUG = GuardianApp.DEBUG;

    private static final Object mSync = new Object();
    private static WeakReference<byte[]> mReadBuffer;
    
    public static void collectCertificates(AppEntry appEntry){
		File sourceFile;
		WeakReference<byte[]> readBufferRef;
		byte[] readBuffer = null;
		
		GuardianApp app = GuardianApp.getInstance();
		
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
							if(certInfo.hasVeriSignIssuer(app.mX509Cert)){
								appEntry.mAppCertState |= GuardianApp.APP_WITH_ANJO_AKI_REVOKED;
								X509CRL crl = getX509CRL(certInfo);
								if(DEBUG)Log.d(TAG,"CRL:"+crl);
								if( crl != null){
									appEntry.setRevoked(crl.isRevoked(x509));
								}else{
									appEntry.setRevoked(true);
								}
								if(!appEntry.isRevoked()){
									appEntry.mAppCertState |= GuardianApp.APP_WITH_ANJO_AKI_NOT_REVOKED;
								}
	
							}else{
								appEntry.mAppCertState |= GuardianApp.APP_WITHOUT_ANJO_AKI;
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
        } catch (IOException e) {
            Log.e(TAG, "Exception reading " + sourceFile, e);
            return;
        } catch (RuntimeException e) {
            Log.e(TAG, "Exception reading " + sourceFile, e);
            return;
        }
    
    }
    
    private static Certificate[] loadCertificates(JarFile jarFile, JarEntry je,
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
    
    private static X509CRL getX509CRL(CertInfo certinfo){
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
		 is = url.openStream();
		 //if(DEBUG) Log.d(TAG,"is:"+ is);
		 CertificateFactory cf = CertificateFactory.getInstance("X.509");
					 crl = (X509CRL)cf.generateCRL(is);
					 is.close();
				}catch (Exception e) {
		         	e.printStackTrace();
		         	return null;
		         }
		 }
    	return crl;
    }
    
	//Get Application in /data/app
	public static boolean filterApp(ApplicationInfo info){
        if ((info.flags & ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0) {
            return true;
        } else if ((info.flags & ApplicationInfo.FLAG_SYSTEM) == 0) {
            return true;
        }
		return false;
	}
	
	
	public static class appComparator implements Comparator{

		@Override
		public int compare(Object e1, Object e2) {
			// TODO Auto-generated method stub
			int i1 = ((AppEntry)e1).mAppCertState;
			int i2 = ((AppEntry)e2).mAppCertState;
			
			return (i2 - i1);

		}
		
	}
}