package com.anjolabs.guardian;

import android.app.Activity;
import android.content.Intent;
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
import android.widget.TextView;

public class SettingsActivity extends Activity{
    static final String TAG = "SettingsActivity";
    static final boolean DEBUG= GuardianApp.DEBUG;
    
    private EditText mIntervalText;
    private Button mCancelButton;
    private Button mOkButton;
    private CheckBox mAnjoCheckBox;
    private int mInterval;
    private boolean mAnjoCheck;
    
    @Override
    public void onCreate(Bundle savedInstanceState){
    	super.onCreate(savedInstanceState);
    	if(DEBUG)Log.d(TAG,"onCreate");
    	
    	setContentView(R.layout.guardian_setting);
    	loadSharedPreferences();
    	initView();
    }

    void initView(){
    	mIntervalText = (EditText)findViewById(R.id.interval);
    	Log.d(TAG,"mInterval:"+mInterval );
    	mIntervalText.setText(Integer.toString(mInterval),TextView.BufferType.EDITABLE);
    	
    	mIntervalText.addTextChangedListener(new TextWatcher(){

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
			public void beforeTextChanged(CharSequence s, int start, int count,
					int after) {
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
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean checked) {
				// TODO Auto-generated method stub
				mAnjoCheck = checked;	
			}
    	});
    	
    	
    	mCancelButton = (Button)findViewById(R.id.cancel);
    	mOkButton = (Button)findViewById(R.id.ok);
    	
    	mCancelButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if(DEBUG) Log.d(TAG, "mCancelButton clicked");
                finish();	
            }
        });    
        
    	mOkButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if(DEBUG) Log.d(TAG, "mOkButton clicked");
                savesharedPreferences();
                finish();
            }
        });
    }
    
    //Save the setting values to the sharedPreferences
    void savesharedPreferences(){
    	SharedPreferences prefs = GuardianApp.getInstance().mPrefs;
        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt(GuardianApp.PREFS_INTERVAL, mInterval);
        editor.putBoolean(GuardianApp.PREFS_ANJOCHECK, mAnjoCheck);
        editor.commit();
    }
    
    
	private void loadSharedPreferences(){
		SharedPreferences prefs = GuardianApp.getInstance().mPrefs;
		mInterval = prefs.getInt(GuardianApp.PREFS_INTERVAL,GuardianApp.DEFAULT_CHECK_INTERVAL);
		mAnjoCheck = prefs.getBoolean(GuardianApp.PREFS_ANJOCHECK,true);
	}
}
