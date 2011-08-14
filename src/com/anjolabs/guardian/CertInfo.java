package com.anjolabs.guardian;

import java.io.UnsupportedEncodingException;
import java.security.cert.X509Certificate;

/**
 * 
 * @author alphalilin@gmail.com
 *
 */
public class CertInfo {
	final static String TAG = MainMenuActivity.TAG;
	final static boolean DEBUG = MainMenuActivity.DEBUG;

	private X509Certificate mX509Cert;
    private boolean httpCDP, ldapCDP;
    private String  fullNameCDP;
    
    /**
     * Constructor, get the cdp of every certificate instance.
     * @param cert
     */
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
	
	/**
	 * Get full Name CDP
	 * @return full name CDP
	 */
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
    
    /**
     * returns ture if this certificate is signed by the given certificate.
     * @param cert
     * @return
     */
    public boolean hasVeriSignIssuer(X509Certificate cert)
    {
        return mX509Cert.getIssuerDN().equals(cert.getSubjectDN());
    }
}
