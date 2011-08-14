package com.anjolabs.guardian;

import android.content.pm.PackageInfo;
import android.os.Parcel;
import android.os.Parcelable;

/**
 * 
 * @author Alphalilin@gmail.com
 *
 */
public class AppEntry implements Parcelable{
	static final String TAG = MainMenuActivity.TAG;
	static final boolean DEBUG=MainMenuActivity.DEBUG;
	
	private PackageInfo mInfo;
	public int mAppCertState;
	private boolean isRevoked;
	
	/**
	 * return true if application is revoked.
	 * @return
	 */
	public boolean isRevoked() {
		return isRevoked;
	}

	/**
	 * Set application revoked state.
	 * @param isRevoked
	 */
	public void setRevoked(boolean isRevoked) {
		this.isRevoked = isRevoked;
	}

	public AppEntry(PackageInfo info){
		mInfo = info;
	}
	
	public PackageInfo getInfo() {
		return mInfo;
	}

    /**
     * Describe the kinds of special objects contained in this Parcelable's
     * marshalled representation.
     *  
     * @return a bitmask indicating the set of special object types marshalled
     * by the Parcelable.
     */
	@Override
	public int describeContents() {
		// TODO Auto-generated method stub
		return 0;
	}

    /**
     * Flatten this object in to a Parcel.
     * 
     * @param dest The Parcel in which the object should be written.
     * @param flags Additional flags about how the object should be written.
     * May be 0 or {@link #PARCELABLE_WRITE_RETURN_VALUE}.
     */
	@Override
	public void writeToParcel(Parcel dest, int parcelableFlags) {
		// TODO Auto-generated method stub
		  dest.writeInt(mAppCertState);
		  dest.writeInt(isRevoked ? 0 : 1 );
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
	
    /**
     * Interface that must be implemented and provided as a public CREATOR
     * field that generates instances of your Parcelable class from a Parcel.
     */
    public static final Parcelable.Creator<AppEntry> CREATOR = new Parcelable.Creator<AppEntry>() {
    	public AppEntry createFromParcel(Parcel source) {
    		return new AppEntry(source);
    	}

    	public AppEntry[] newArray(int size) {
    		return new AppEntry[size];
    	}
    };
}
