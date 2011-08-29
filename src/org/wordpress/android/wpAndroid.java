package org.wordpress.android;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.Vector;

import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;

import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.ListActivity;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Configuration;
import android.graphics.PixelFormat;
import android.location.LocationManager;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.View.OnCreateContextMenuListener;
import android.view.ViewGroup.LayoutParams;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;

import com.commonsware.cwac.cache.SimpleWebImageCache;
import com.commonsware.cwac.thumbnail.ThumbnailAdapter;
import com.commonsware.cwac.thumbnail.ThumbnailBus;
import com.commonsware.cwac.thumbnail.ThumbnailMessage;

public class wpAndroid extends ListActivity {
	/** Called when the activity is first created. */
	public Vector<?> accounts;
	public Vector<String> accountNames = new Vector<String>();
	public String[] accountIDs;
	public String[] blogNames;
	public String[] accountUsers;
	public String[] blavatars;
	protected String selectedID = "";
	protected ThumbnailAdapter thumbs = null;
	protected static final int[] IMAGE_IDS = { R.id.blavatar };

	@Override
	public void onCreate(Bundle icicle) {
		super.onCreate(icicle);
		getWindow().setFormat(PixelFormat.RGBA_8888);
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_DITHER);
		// verify that the user has accepted the EULA
		boolean eula = checkEULA();
		if (eula == false) {
			AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(
					wpAndroid.this);
			dialogBuilder.setTitle(R.string.eula);
			dialogBuilder.setMessage(R.string.eula_content);
			dialogBuilder.setPositiveButton(R.string.accept,
					new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog,
								int whichButton) {
							// User clicked Accept so set that they've agreed to
							// the eula.
							WordPressDB eulaDB = new WordPressDB(wpAndroid.this);
							eulaDB.setEULA(wpAndroid.this);
							displayAccounts();

						}
					});
			dialogBuilder.setNegativeButton(R.string.decline,
					new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog,
								int whichButton) {
							finish(); // goodbye!
						}
					});
			dialogBuilder.setCancelable(false);
			dialogBuilder.create().show();
		} else {
			displayAccounts();
		}
	}

	public boolean checkEULA() {
		WordPressDB eulaDB = new WordPressDB(this);
		boolean sEULA = eulaDB.checkEULA(this);

		return sEULA;

	}

	public void displayAccounts() {

		setContentView(R.layout.home);
		setTitle(getResources().getText(R.string.app_name));

		// settings time!
		WordPressDB settingsDB = new WordPressDB(this);
		accounts = settingsDB.getAccounts(this);

		final int readerBlogID = settingsDB.getWPCOMBlogID(this);
		Button readerButton = (Button) findViewById(R.id.readerButton);
		if (readerBlogID >= 0) {
		    readerButton.setOnClickListener(new Button.OnClickListener() {
                public void onClick(View v) {
                    Bundle bundle = new Bundle();
                    bundle.putString("id", String.valueOf(readerBlogID));
                    bundle.putBoolean("loadReader", true);
                    Intent webViewIntent = new Intent(wpAndroid.this,
                            viewPost.class);
                    webViewIntent.putExtras(bundle);
                    startActivity(webViewIntent);
                    
                }
            });
		}
		else {
		    readerButton.setVisibility(View.GONE);
		}
		
		// upload stats
		checkStats(accounts.size());

		ListView listView = (ListView) findViewById(android.R.id.list);
		listView.setVerticalFadingEdgeEnabled(false);
		listView.setVerticalScrollBarEnabled(true);

		listView.setOnItemClickListener(new OnItemClickListener() {

			public void onItemClick(AdapterView<?> arg0, View row,
					int position, long id) {
				Bundle bundle = new Bundle();
				bundle.putString("accountName", escapeUtils.unescapeHtml(blogNames[position]));
				bundle.putString("id", String.valueOf(row.getId()));
				Intent viewPostsIntent = new Intent(wpAndroid.this,
						tabView.class);
				viewPostsIntent.putExtras(bundle);
				startActivityForResult(viewPostsIntent, 1);

			}

		});

		listView
				.setOnCreateContextMenuListener(new OnCreateContextMenuListener() {

					public void onCreateContextMenu(ContextMenu menu, View v,
							ContextMenuInfo menuInfo) {

						AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuInfo;

						View row = info.targetView;

						selectedID = String.valueOf(row.getId());

						menu.add(0, 0, 0, getResources().getText(
								R.string.remove_account));
					}
				});

		if (accounts.size() > 0) {
			setTitle(getResources().getText(R.string.app_name) + " - "
					+ getResources().getText(R.string.blogs));
			ScrollView sv = new ScrollView(this);
			sv.setLayoutParams(new LayoutParams(LayoutParams.FILL_PARENT,
					LayoutParams.WRAP_CONTENT));
			LinearLayout layout = new LinearLayout(this);
			layout.setPadding(10, 10, 10, 0);
			layout.setLayoutParams(new LayoutParams(LayoutParams.FILL_PARENT,
					LayoutParams.WRAP_CONTENT));

			layout.setOrientation(LinearLayout.VERTICAL);

			blogNames = new String[accounts.size()];
			accountIDs = new String[accounts.size()];
			accountUsers = new String[accounts.size()];
			blavatars = new String[accounts.size()];
			int validBlogCtr = 0;
			for (int i = 0; i < accounts.size(); i++) {

				HashMap<?, ?> curHash = (HashMap<?, ?>) accounts.get(i);
				if (curHash.get("blogName") == null){
					//cleaning up accounts added before v1.3.8
					String deleteID = curHash.get("id").toString();
					settingsDB.deleteAccount(this, deleteID);
					if (validBlogCtr > 0){
						validBlogCtr--;
					}
				}
				else {					
					blogNames[validBlogCtr] = curHash.get("blogName").toString();
					accountUsers[validBlogCtr] = curHash.get("username").toString();
					accountIDs[validBlogCtr] = curHash.get("id").toString();
					String url = curHash.get("url").toString();
					url = url.replace("http://", "");
					url = url.replace("https://", "");
					String[] urlSplit = url.split("/");
					url = urlSplit[0];
					url = "http://gravatar.com/blavatar/"
							+ moderateCommentsTab.getMd5Hash(url.trim())
							+ "?s=60&d=404";
					blavatars[validBlogCtr] = url;
					accountNames.add(validBlogCtr, blogNames[i]);
					validBlogCtr++;
				}
			}
			
			if (validBlogCtr < accounts.size()){
				accounts = settingsDB.getAccounts(this);
			}

			ThumbnailBus bus = new ThumbnailBus();
			thumbs = new ThumbnailAdapter(this, new HomeListAdapter(this),
					new SimpleWebImageCache<ThumbnailBus, ThumbnailMessage>(
							null, null, 101, bus), IMAGE_IDS);

			setListAdapter(thumbs);
			
			//start the comment service (it will kill itself if no blogs want notifications)
			Intent intent = new Intent(wpAndroid.this, broadcastReceiver.class);
        	PendingIntent pIntent = PendingIntent.getBroadcast(wpAndroid.this, 0, intent, 0);
        	
        	AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
        	String updateInterval = settingsDB.getInterval(wpAndroid.this);
        	if (updateInterval == ""){
        		updateInterval = "1 Hour";
        	}
        	int UPDATE_INTERVAL = 3600000;
        	if (updateInterval.equals("5 Minutes")){
	        	 UPDATE_INTERVAL = 300000;
	        }
	        else if (updateInterval.equals("10 Minutes")){
	        	UPDATE_INTERVAL = 600000;
	        }
	        else if (updateInterval.equals("15 Minutes")){
	        	UPDATE_INTERVAL = 900000;
	        }
	        else if (updateInterval.equals("30 Minutes")){
	        	UPDATE_INTERVAL = 1800000;
	        }
	        else if (updateInterval.equals("1 Hour")){
	        	UPDATE_INTERVAL = 3600000;
	        }
	        else if (updateInterval.equals("3 Hours")){
	        	UPDATE_INTERVAL = 10800000;
	        }
	        else if (updateInterval.equals("6 Hours")){
	        	UPDATE_INTERVAL = 21600000;
	        }
	        else if (updateInterval.equals("12 Hours")){
	        	UPDATE_INTERVAL = 43200000;
	        }
	        else if (updateInterval.equals("Daily")){
	        	UPDATE_INTERVAL = 86400000;
	        }
        	alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + (5 * 1000), UPDATE_INTERVAL, pIntent);
			
		} else {
			// no account, load new account view
			Intent i = new Intent(wpAndroid.this, newAccount.class);

			startActivityForResult(i, 0);

		}
	}

	private void checkStats(final int numBlogs) {

		WordPressDB eulaDB = new WordPressDB(this);
		long lastStatsDate = eulaDB.getStatsDate(this);
		long now = System.currentTimeMillis();

		if ((now - lastStatsDate) > 604800000) { // works for first check as
													// well
			new Thread() {
				public void run() {
					uploadStats(numBlogs);
				}
			}.start();
			eulaDB.setStatsDate(this);
		}

	}

	private void uploadStats(int numBlogs) {

		// gather all of the device info
		WordPressDB eulaDB = new WordPressDB(this);
		String uuid = eulaDB.getUUID(this);
		if (uuid == ""){
			uuid = UUID.randomUUID().toString();
			eulaDB.updateUUID(this, uuid);
		}
		PackageManager pm = getPackageManager();
		String app_version = "";
		try {
			try {
				PackageInfo pi = pm.getPackageInfo("org.wordpress.android", 0);
				app_version = pi.versionName;
			} catch (NameNotFoundException e) {
				e.printStackTrace();
				app_version = "N/A";
			}

			TelephonyManager tm = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
			String device_language = getResources().getConfiguration().locale
					.getLanguage();
			String mobile_country_code = tm.getNetworkCountryIso();
			String mobile_network_number = tm.getNetworkOperator();
			int network_type = tm.getNetworkType();

			// get the network type string
			String mobile_network_type = "N/A";
			switch (network_type) {
			case 0:
				mobile_network_type = "TYPE_UNKNOWN";
				break;
			case 1:
				mobile_network_type = "GPRS";
				break;
			case 2:
				mobile_network_type = "EDGE";
				break;
			case 3:
				mobile_network_type = "UMTS";
				break;
			case 4:
				mobile_network_type = "CDMA";
				break;
			case 5:
				mobile_network_type = "EVDO_0";
				break;
			case 6:
				mobile_network_type = "EVDO_A";
				break;
			case 7:
				mobile_network_type = "1xRTT";
				break;
			case 8:
				mobile_network_type = "HSDPA";
				break;
			case 9:
				mobile_network_type = "HSUPA";
				break;
			case 10:
				mobile_network_type = "HSPA";
				break;
			}

			String device_version = android.os.Build.VERSION.RELEASE;

			if (device_version == null) {
				device_version = "N/A";
			}
			int num_blogs = numBlogs;

			// post the data
			HttpClient client = new DefaultHttpClient();
			HttpPost post = new HttpPost("http://api.wordpress.org/androidapp/update-check/1.0/");
			post.setHeader("Content-Type", "application/x-www-form-urlencoded");

			List<NameValuePair> pairs = new ArrayList<NameValuePair>();
			pairs.add(new BasicNameValuePair("device_uuid", uuid));
			pairs.add(new BasicNameValuePair("app_version", app_version));
			pairs.add(new BasicNameValuePair("device_language", device_language));
			pairs.add(new BasicNameValuePair("mobile_country_code", mobile_country_code));
			pairs.add(new BasicNameValuePair("mobile_network_number", mobile_network_number));
			pairs.add(new BasicNameValuePair("mobile_network_type", mobile_network_type));
			pairs.add(new BasicNameValuePair("device_version", device_version));
			pairs.add(new BasicNameValuePair("num_blogs", String.valueOf(num_blogs)));
			try {
				post.setEntity(new UrlEncodedFormEntity(pairs));
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
			}

			try {
				client.execute(post);
			} catch (Exception e) {
				e.printStackTrace();
			}

		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	// Add settings to menu
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		menu.add(0, 0, 0, getResources().getText(R.string.add_account));
		MenuItem menuItem1 = menu.findItem(0);
		menuItem1.setIcon(R.drawable.ic_menu_add);

		menu.add(0, 1, 0, getResources()
				.getText(R.string.preferences));
		MenuItem menuItem2 = menu.findItem(1);
		menuItem2.setIcon(R.drawable.ic_menu_prefs);

		return true;
	}

	// Menu actions
	@Override
	public boolean onOptionsItemSelected(final MenuItem item) {
		switch (item.getItemId()) {
		case 0:
			Intent i = new Intent(this, newAccount.class);

			startActivityForResult(i, 0);

			return true;
		case 1:
			Intent i2 = new Intent(this, Preferences.class);

			startActivity(i2);

			return true;
		}
		return false;

	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if (data != null) {
			switch (requestCode) {
			case 0:
				WordPressDB settingsDB = new WordPressDB(this);
				accounts = settingsDB.getAccounts(this);

				if (accounts.size() == 0) {
					finish();
				} else {
					displayAccounts();
				}
				break;
			}
		}// end null check
		else {
			displayAccounts();
		}
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {

		/* Switch on the ID of the item, to get what the user selected. */
		switch (item.getItemId()) {
		case 0:
			AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(
					wpAndroid.this);
			dialogBuilder.setTitle(getResources().getText(
					R.string.remove_account));
			dialogBuilder.setMessage(getResources().getText(
					R.string.sure_to_remove_account));
			dialogBuilder.setPositiveButton(getResources()
					.getText(R.string.yes),
					new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog,
								int whichButton) {
							// remove the account
							WordPressDB settingsDB = new WordPressDB(
									wpAndroid.this);
							boolean deleteSuccess = settingsDB.deleteAccount(
									wpAndroid.this, selectedID);
							if (deleteSuccess) {
								Toast
										.makeText(
												wpAndroid.this,
												getResources()
														.getText(
																R.string.account_removed_successfully),
												Toast.LENGTH_SHORT).show();
								displayAccounts();
							} else {
								AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(
										wpAndroid.this);
								dialogBuilder.setTitle(getResources().getText(
										R.string.error));
								dialogBuilder
										.setMessage(getResources()
												.getText(
														R.string.could_not_remove_account));
								dialogBuilder.setPositiveButton("OK",
										new DialogInterface.OnClickListener() {
											public void onClick(
													DialogInterface dialog,
													int whichButton) {
												// just close the dialog
											}
										});
								dialogBuilder.setCancelable(true);
								dialogBuilder.create().show();
							}

						}
					});
			dialogBuilder.setNegativeButton(
					getResources().getText(R.string.no),
					new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog,
								int whichButton) {
							// Just close the window.

						}
					});
			dialogBuilder.setCancelable(false);
			dialogBuilder.create().show();

			return true;
		}
		return false;
	}

	protected class HomeListAdapter extends BaseAdapter {

		public HomeListAdapter(Context context) {
		}

		public int getCount() {
			return accounts.size();
		}

		public Object getItem(int position) {
			return position;
		}

		public long getItemId(int position) {
			return position;
		}

		public View getView(int position, View convertView, ViewGroup parent) {
			View pv = convertView;
			ViewWrapper wrapper = null;
			if (pv == null) {
				LayoutInflater inflater = getLayoutInflater();
				pv = inflater.inflate(R.layout.home_row, parent, false);
				wrapper = new ViewWrapper(pv);
				/*if (position == 0) {
					usenameHeight = wrapper.getBlogUsername().getHeight();
				}*/
				pv.setTag(wrapper);
				wrapper = new ViewWrapper(pv);
				pv.setTag(wrapper);
			} else {
				wrapper = (ViewWrapper) pv.getTag();
			}
			String username = accountUsers[position];
			pv.setBackgroundDrawable(getResources().getDrawable(
					R.drawable.list_bg_selector));
			pv.setId(Integer.valueOf(accountIDs[position]));
			if (wrapper.getBlogUsername().getHeight() == 0) {
				wrapper.getBlogUsername().setHeight(
						(int) wrapper.getBlogName().getTextSize()
								+ wrapper.getBlogUsername().getPaddingBottom());
			}

			wrapper.getBlogName().setText(
					escapeUtils.unescapeHtml(blogNames[position]));
			wrapper.getBlogUsername().setText(
					escapeUtils.unescapeHtml(username));

			if (wrapper.getBlavatar() != null) {
				try {
					wrapper.getBlavatar().setImageResource(R.drawable.app_icon);
					wrapper.getBlavatar().setTag(blavatars[position]);
				} catch (Throwable t) {
					t.printStackTrace();
				}
			}

			return pv;

		}

	}

	class ViewWrapper {
		View base;
		TextView blogName = null;
		TextView blogUsername = null;
		ImageView blavatar = null;

		ViewWrapper(View base) {
			this.base = base;
		}

		TextView getBlogName() {
			if (blogName == null) {
				blogName = (TextView) base.findViewById(R.id.blogName);
			}
			return (blogName);
		}

		TextView getBlogUsername() {
			if (blogUsername == null) {
				blogUsername = (TextView) base.findViewById(R.id.blogUser);
			}
			return (blogUsername);
		}

		ImageView getBlavatar() {
			if (blavatar == null) {
				blavatar = (ImageView) base.findViewById(R.id.blavatar);
			}
			return (blavatar);
		}
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		// ignore orientation change
		super.onConfigurationChanged(newConfig);
	}

}
