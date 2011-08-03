package com.anjolabs.guardian;


import java.security.cert.Certificate;
import java.security.cert.X509CRL;

import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.Signature;
import android.os.Parcel;
import android.os.Parcelable;

public class AppEntry implements Parcelable{
	static final String TAG = Guardian.TAG;
	static final boolean DEBUG=Guardian.DEBUG;
	
	private PackageInfo mInfo;
	//private boolean trusted;
	//private Certificate [] mCerts;
	//public Signature mSignatures[];
	//private CertInfo mCertInfo;
	public int mAppCertState;
	private boolean isRevoked;
	
	//public X509CRL mX509Crl;
	/*
	public AppEntry(ApplicationInfo info){
		//mInfo = info;
		//trusted = true;
		//mCerts = null;
		//mSignatures = null;
		mAppCertState = Guardian.APP_WITHOUT_ANJO_AKI;
	}*/
	
	public boolean isRevoked() {
		return isRevoked;
	}

	public void setRevoked(boolean isRevoked) {
		this.isRevoked = isRevoked;
	}

	public AppEntry(PackageInfo info){
		mInfo = info;
	}
	
	public PackageInfo getInfo() {
		return mInfo;
	}
	/*
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
*/

	@Override
	public int describeContents() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int parcelableFlags) {
		// TODO Auto-generated method stub
		  dest.writeInt(mAppCertState);
		  //dest.writeBooleanArray(new boolean[] {isRevoked});
		  dest.writeInt(isRevoked ? 0 : 1 );
		  //dest.writeBooleanArray(isRevoked);
		  if (mInfo != null) {
	            dest.writeInt(1);
	            mInfo.writeToParcel(dest, parcelableFlags);
	      } else {
	            dest.writeInt(0);
	      }
	}
	private AppEntry(Parcel source){
		mAppCertState=source.readInt();
		isRevoked = source.readInt() == 0;
		int hasApp = source.readInt();
		if(hasApp != 0){
			mInfo = PackageInfo.CREATOR.createFromParcel(source);
		}
	}
	
    public static final Parcelable.Creator<AppEntry> CREATOR = new Parcelable.Creator<AppEntry>() {
    	public AppEntry createFromParcel(Parcel source) {
    		return new AppEntry(source);
    	}

    	public AppEntry[] newArray(int size) {
    		return new AppEntry[size];
    	}
    };
}
