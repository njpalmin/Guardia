package com.anjolabs.guardian;

import java.io.UnsupportedEncodingException;
import java.security.cert.X509Certificate;

import android.util.Log;

public class CertInfo {
	final static String TAG = Guardian.TAG;
	final static boolean DEBUG = Guardian.DEBUG;

	private X509Certificate mX509Cert;
    private boolean httpCDP, ldapCDP;
    private boolean httpAIA, ldapAIA;
    private String  fullNameCDP, aiaLocation;
    

	public 	CertInfo(X509Certificate cert){
    	mX509Cert = cert;
    	byte[] cdp = cert.getExtensionValue("2.5.29.31");
    	
    	if(cdp != null){
    		String str;
            try {
                str = new String(cdp, "US-ASCII");
            } catch (UnsupportedEncodingException e) {
                throw new IllegalStateException(e.toString());
            }
            //if(DEBUG) Log.d(TAG,"Str:"+str);
    	    int i = str.indexOf("http");
    	    if( i != -1){
    			httpCDP = true;
    			ldapCDP = false;
    	    }else{
    	    	i = str.indexOf("ldap");
    			httpCDP = false;
    			ldapCDP = true;
    	    }
    	    if (i != -1) {
    	    	fullNameCDP = new String(cdp, i, cdp[i-1]);
    	    }
    	}
    }
	
    public String getFullNameCDP() {
		return fullNameCDP;
	}
    /**
     * returns true if the CDP fullName field starts with http
     */
    public boolean isHttpCDP()
    {
    	return httpCDP;
    }

    /**
     * returns true if the CDP fullName field starts with ldap
     */
    public boolean isLdapCDP()
    {
    	return ldapCDP;
    }
    
    public boolean hasVeriSignIssuer(X509Certificate cert)
    {
        return mX509Cert.getIssuerDN().equals(cert.getSubjectDN());
    }
}
