package com.anjolabs.guardian;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

//import com.anjolabs.Guardian.ApplicationAdapter.ViewHolder;

public class PackageListActivity extends Activity implements OnItemClickListener{
	static final String TAG = "PackageListActivity";
	static final boolean DEBUG=Guardian.DEBUG;
	
	private PackageManager mPm;
	private ListView mListView;
	private List<AppEntry> mAppEntryList = new ArrayList<AppEntry>();
	private AppEntryAdapter mAdapter;
	
		
	@Override
	public void onCreate(Bundle savedInstanceState){
		if(DEBUG) Log.d(TAG,"onCreate!");
		super.onCreate(savedInstanceState);
		
		Intent intent = getIntent();
		mPm = getPackageManager();
		//Bundle bundle = intent.getExtras();
		//mAdapter = (AppEntryAdapterbak)bundle.getSerializable(Guardian.APPS_LIST);
		//mAppEntryList = intent.getParcelableArrayListExtra(Guardian.APPS_LIST);
		mAppEntryList = Guardian.getAppList();
		
		if(mAppEntryList != null){
			mAdapter = new AppEntryAdapter(this,R.layout.app_entry,mAppEntryList);
		}else{
			Log.d(TAG,"CAN't find mAppEntryList");
		}
		
		setContentView(R.layout.guardian_packages_list);
		mListView = (ListView) findViewById(R.id.list);
		mListView.setOnItemClickListener(this);
		mListView.setAdapter(mAdapter);
	}

	
	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		// TODO Auto-generated method stub
		
	}


	private class AppEntryAdapter extends ArrayAdapter<AppEntry>{
		private LayoutInflater mInflater;
		private List<AppEntry> items;
		
		public AppEntryAdapter(Context context, int textViewResourceId,
				List<AppEntry> items) {
			super(context, textViewResourceId, items);
			// TODO Auto-generated constructor stub
			mInflater = LayoutInflater.from(context);
			this.items = items;
		}


		@Override
		public View getView(int position, View convertView, ViewGroup parent){
			ViewHolder holder;
			
			if( convertView == null){
				convertView = mInflater.inflate(R.layout.app_entry, null);
                holder = new ViewHolder();
                holder.text = (TextView) convertView.findViewById(R.id.text1);
                holder.icon = (ImageView) convertView.findViewById(R.id.icon1);
                holder.checkbox = (ImageView) convertView.findViewById(R.id.icon2);

                convertView.setTag(holder);
			}else{
				holder = (ViewHolder) convertView.getTag();
			}
			AppEntry appEntry = items.get(position);
			
			holder.text.setText(appEntry.getInfo().loadLabel(mPm));
			holder.icon.setImageDrawable(appEntry.getInfo().loadIcon(mPm));
		
			if ((appEntry.mAppCertState & Guardian.APP_WITH_ANJO_AKI_NOT_REVOKED) != 0){
				holder.checkbox.setImageResource(R.drawable.green);
			}else if((appEntry.mAppCertState & Guardian.APP_WITH_ANJO_AKI_REVOKED) != 0){
				holder.checkbox.setImageResource(R.drawable.red);
			}else  if((appEntry.mAppCertState & Guardian.APP_WITHOUT_ANJO_AKI) != 0){
				holder.checkbox.setImageResource(R.drawable.yellow);
			}	
			
			return convertView;
		}
		
		class ViewHolder {
            TextView text;
            ImageView icon;
            ImageView checkbox;
        }
	}
}
