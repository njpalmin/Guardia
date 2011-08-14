package com.anjolabs.guardian;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;

/**
 * Set the configuration (interval and anjocheck) values.
 * @author Alphalilin@gmail.com
 *
 */
public class SettingsActivity extends Activity{
    static final String TAG = "SettingsActivity";
    static final boolean DEBUG= GuardianApp.DEBUG;
    
    private EditText mIntervalText;
    private ImageButton mCancelButton;
    private ImageButton mOkButton;
    private CheckBox mAnjoCheckBox;
    private int mInterval;
    private boolean mAnjoCheck;
    
    /**
     * Called when the activity is starting.  This is where most initialization
     * should go: calling {@link #setContentView(int)} to inflate the
     * activity's UI, using {@link #findViewById} to programmatically interact
     * with widgets in the UI, calling
     * {@link #managedQuery(android.net.Uri , String[], String, String[], String)} to retrieve
     * cursors for data being displayed, etc.
     * 
     * <p>You can call {@link #finish} from within this function, in
     * which case onDestroy() will be immediately called without any of the rest
     * of the activity lifecycle ({@link #onStart}, {@link #onResume},
     * {@link #onPause}, etc) executing.
     * 
     * <p><em>Derived classes must call through to the super class's
     * implementation of this method.  If they do not, an exception will be
     * thrown.</em></p>
     * 
     * @param savedInstanceState If the activity is being re-initialized after
     *     previously being shut down then this Bundle contains the data it most
     *     recently supplied in {@link #onSaveInstanceState}.  <b><i>Note: Otherwise it is null.</i></b>
     * 
     * @see #onStart
     * @see #onSaveInstanceState
     * @see #onRestoreInstanceState
     * @see #onPostCreate
     */
    @Override
    public void onCreate(Bundle savedInstanceState){
    	super.onCreate(savedInstanceState);
    	if(DEBUG)Log.d(TAG,"onCreate");
    	
    	setContentView(R.layout.guardian_setting);
    	loadSharedPreferences();
    	initView();
    }

    /**
     * Initialize Setting layout.
     */
    void initView(){
    	mIntervalText = (EditText)findViewById(R.id.interval);
    	Log.d(TAG,"mInterval:"+mInterval );
    	mIntervalText.setText(Integer.toString(mInterval),TextView.BufferType.EDITABLE);
    	
    	mIntervalText.addTextChangedListener(new TextWatcher(){


    	    /**
    	     * This method is called to notify you that, somewhere within
    	     * <code>s</code>, the text has been changed.
    	     * It is legitimate to make further changes to <code>s</code> from
    	     * this callback, but be careful not to get yourself into an infinite
    	     * loop, because any changes you make will cause this method to be
    	     * called again recursively.
    	     * (You are not told where the change took place because other
    	     * afterTextChanged() methods may already have made other changes
    	     * and invalidated the offsets.  But if you need to know here,
    	     * you can use {@link Spannable#setSpan} in {@link #onTextChanged}
    	     * to mark your place and then look up from here where the span
    	     * ended up.
    	     */
			@Override
			public void afterTextChanged(Editable s) {
				// TODO Auto-generated method stub
				if( s.length() == 0){
					Log.d(TAG,"afterTextChanged s: 0");
					mInterval = 0;
				}else{
					mInterval = Integer.parseInt(s.toString());
				}
			}

			@Override
			public void beforeTextChanged(CharSequence arg0, int arg1,
					int arg2, int arg3) {
				// TODO Auto-generated method stub
				
			}

			@Override
			public void onTextChanged(CharSequence s, int start, int before,
					int count) {
				// TODO Auto-generated method stub
				
			}
    	});
    	
    	
    	mAnjoCheckBox = (CheckBox)findViewById(R.id.anjo_checkbox);
    	mAnjoCheckBox.setChecked(mAnjoCheck ? true:false);
    	mAnjoCheckBox.setOnCheckedChangeListener(new OnCheckedChangeListener(){
    		
    		/**
             * Called when the checked state of a compound button has changed.
             *
             * @param buttonView The compound button view whose state has changed.
             * @param isChecked  The new checked state of buttonView.
             */
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean checked) {
				// TODO Auto-generated method stub
				mAnjoCheck = checked;	
			}
    	});
    	
    	
    	mCancelButton = (ImageButton)findViewById(R.id.cancel);
    	mOkButton = (ImageButton)findViewById(R.id.ok);
    	
    	mCancelButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if(DEBUG) Log.d(TAG, "mCancelButton clicked");
                finish();	
            }
        });    
        
    	mOkButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if(DEBUG) Log.d(TAG, "mOkButton clicked");
                saveSharedPreferences();
                finish();
            }
        });
    }
    
    /**
     * Save the settings values( interval and anjo check) into sharedPreferences.
     */
    void saveSharedPreferences(){
    	SharedPreferences prefs = GuardianApp.getInstance().mPrefs;
        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt(GuardianApp.PREFS_INTERVAL, mInterval);
        editor.putBoolean(GuardianApp.PREFS_ANJOCHECK, mAnjoCheck);
        editor.commit();
    }
    
    /**
     * Read the settings values(interval and anjo check) from sharedPrefrences.
     */
	private void loadSharedPreferences(){
		SharedPreferences prefs = GuardianApp.getInstance().mPrefs;
		mInterval = prefs.getInt(GuardianApp.PREFS_INTERVAL,GuardianApp.DEFAULT_CHECK_INTERVAL);
		mAnjoCheck = prefs.getBoolean(GuardianApp.PREFS_ANJOCHECK,true);
	}
}
