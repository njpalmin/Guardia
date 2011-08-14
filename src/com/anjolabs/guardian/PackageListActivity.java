package com.anjolabs.guardian;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
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

/*
 * This Activity is to list all parsed applications.
 */
public class PackageListActivity extends Activity implements OnItemClickListener{
	static final String TAG = "PackageListActivity";
	static final boolean DEBUG=MainMenuActivity.DEBUG;
	
	private PackageManager mPm;
	private ListView mListView;
	private List<AppEntry> mAppEntryList = new ArrayList<AppEntry>();
	private AppEntryAdapter mAdapter;
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
		if(DEBUG) Log.d(TAG,"onCreate!");
		super.onCreate(savedInstanceState);
		
		Intent intent = getIntent();
		mPm = getPackageManager();
		mAppEntryList = intent.getParcelableArrayListExtra(GuardianApp.APPS_LIST);
		
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

    /**
     * Callback method to be invoked when an item in this AdapterView has
     * been clicked.
     * <p>
     * Implementers can call getItemAtPosition(position) if they need
     * to access the data associated with the selected item.
     *
     * @param parent The AdapterView where the click happened.
     * @param view The view within the AdapterView that was clicked (this
     *            will be a view provided by the adapter)
     * @param position The position of the view in the adapter.
     * @param id The row id of the item that was clicked.
     */
	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		// TODO Auto-generated method stub

		AppEntry appEntry;
		appEntry = mAppEntryList.get(position);
		if(DEBUG) Log.d(TAG,"AppEntry ="+ appEntry.getInfo().packageName+" position="+position);

		/*
		 * Start activity to show the application details information.
		 * Pass the application data with bundle.
		 */
		Intent intent = new Intent(this,AppDetailActivity.class);
		Bundle bundle = new Bundle();
		bundle.putParcelable(GuardianApp.APPS_ENTRY,appEntry);
		intent.putExtras(bundle);
		startActivity(intent);
	}


	/*
	 * Customized adapter for Guardian to list all parsed application.
	 */
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

	    /**
	     * Get a View that displays the data at the specified position in the data set. You can either
	     * create a View manually or inflate it from an XML layout file. When the View is inflated, the
	     * parent View (GridView, ListView...) will apply default layout parameters unless you use
	     * {@link android.view.LayoutInflater#inflate(int, android.view.ViewGroup, boolean)}
	     * to specify a root view and to prevent attachment to the root.
	     * 
	     * @param position The position of the item within the adapter's data set of the item whose view
	     *        we want.
	     * @param convertView The old view to reuse, if possible. Note: You should check that this view
	     *        is non-null and of an appropriate type before using. If it is not possible to convert
	     *        this view to display the correct data, this method can create a new view.
	     * @param parent The parent that this view will eventually be attached to
	     * @return A View corresponding to the data at the specified position.
	     */
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
			
			holder.text.setText(appEntry.getInfo().applicationInfo.loadLabel(mPm));
			holder.icon.setImageDrawable(appEntry.getInfo().applicationInfo.loadIcon(mPm));
		
			if ((appEntry.mAppCertState & GuardianApp.APP_WITH_ANJO_AKI_NOT_REVOKED) != 0){
				holder.checkbox.setImageResource(R.drawable.green);
			}else if((appEntry.mAppCertState & GuardianApp.APP_WITH_ANJO_AKI_REVOKED) != 0){
				holder.checkbox.setImageResource(R.drawable.red);
			}else  if((appEntry.mAppCertState & GuardianApp.APP_WITHOUT_ANJO_AKI) != 0){
				holder.checkbox.setImageResource(R.drawable.yellow);
			}	
			
			return convertView;
		}
		
		
		/*
		 * The class for the every application, and ready to show it in listview. 
		 */
		
		class ViewHolder {
            TextView text;
            ImageView icon;
            ImageView checkbox;
        }
	}
}
