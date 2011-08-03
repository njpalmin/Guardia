package com.anjolabs.guardian;


import java.security.cert.Certificate;
import java.security.cert.X509CRL;

import android.content.pm.ApplicationInfo;
import android.content.pm.Signature;

public class AppEntry{
	static final String TAG = Guardian.TAG;
	static final boolean DEBUG=Guardian.DEBUG;
	
	private ApplicationInfo mInfo;
	private boolean trusted;
	private Certificate [] mCerts;
	public Signature mSignatures[];
	private CertInfo mCertInfo;
	public int mAppCertState;
	public X509CRL mX509Crl;
	
	public AppEntry(ApplicationInfo info){
		mInfo = info;
		trusted = true;
		mCerts = null;
		mSignatures = null;
		mAppCertState = Guardian.APP_WITHOUT_ANJO_AKI;
	}
	
	public ApplicationInfo getInfo() {
		return mInfo;
	}

	public void setInfo(ApplicationInfo info) {
		this.mInfo = info;
	}

	public boolean isTrusted() {
		return trusted;
	}

	public void setTrusted(boolean trusted) {
		this.trusted = trusted;
	}

	public Certificate[] getCerts() {
		return mCerts;
	}

	public CertInfo getCertInfo() {
		return mCertInfo;
	}

	public void setCertInfo(CertInfo certInfo) {
		this.mCertInfo = certInfo;
	}

}
