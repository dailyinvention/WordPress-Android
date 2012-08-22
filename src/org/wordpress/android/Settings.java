package org.wordpress.android;
import java.util.List;

import org.wordpress.android.util.WPTitleBar;
import org.wordpress.android.util.WPTitleBar.OnBlogChangedListener;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.location.LocationManager;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;


public class Settings extends Activity {
	protected static Intent svc = null;
	private String originalUsername;
	WPTitleBar titleBar;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.settings);
	
        titleBar = (WPTitleBar) findViewById(R.id.settingsActionBar);
        titleBar.refreshButton.setEnabled(false);
		titleBar.setOnBlogChangedListener(new OnBlogChangedListener() {
			// user selected new blog in the title bar
			@Override
			public void OnBlogChanged() {
				
				loadSettingsForBlog();

			}
		});
		  
        Button cancelButton = (Button) findViewById(R.id.cancel);
        cancelButton.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v) {
            	
            	 Bundle bundle = new Bundle();
                 
                 bundle.putString("returnStatus", "CANCEL");
                 Intent mIntent = new Intent();
                 mIntent.putExtras(bundle);
                 setResult(RESULT_CANCELED, mIntent);
                 finish();
            }
        });
        
        loadSettingsForBlog();
        
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		titleBar.setupCurrentBlog();
	}
	
	@Override
	protected void onNewIntent (Intent intent){
		super.onNewIntent(intent);
		
		titleBar.refreshBlog();
		loadSettingsForBlog();
		
	}
	
	private void loadSettingsForBlog() {
		Spinner spinner = (Spinner)this.findViewById(R.id.maxImageWidth);
	    ArrayAdapter<Object> spinnerArrayAdapter = new ArrayAdapter<Object>(this,
	    		R.layout.spinner_textview,
	            new String[] { "Original Size", "100", "200", "300", "400", "500", "600", "700", "800", "900", "1000"});
	    spinnerArrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
	    spinner.setAdapter(spinnerArrayAdapter);

    	EditText usernameET = (EditText)findViewById(R.id.username);
    	usernameET.setText(WordPress.currentBlog.getUsername());
    	originalUsername = WordPress.currentBlog.getUsername();

    	EditText passwordET = (EditText)findViewById(R.id.password);
    	passwordET.setText(WordPress.currentBlog.getPassword());

    	EditText httpUserET = (EditText)findViewById(R.id.httpuser);
    	httpUserET.setText(WordPress.currentBlog.getHttpuser());

    	EditText httpPasswordET = (EditText)findViewById(R.id.httppassword);
    	httpPasswordET.setText(WordPress.currentBlog.getHttppassword());
    	TextView httpPasswordLabel = (TextView) findViewById(R.id.l_httppassword);
		TextView httpUserLabel = (TextView) findViewById(R.id.l_httpuser);
    	if (WordPress.currentBlog.isDotcomFlag()) {
    		httpPasswordLabel.setVisibility(View.GONE);
    		httpPasswordET.setVisibility(View.GONE);

    		httpUserLabel.setVisibility(View.GONE);
    		httpUserET.setVisibility(View.GONE);
    	}
    	else {
    		httpPasswordLabel.setVisibility(View.VISIBLE);
    		httpPasswordET.setVisibility(View.VISIBLE);

    		httpUserLabel.setVisibility(View.VISIBLE);
    		httpUserET.setVisibility(View.VISIBLE);
    	}

    	CheckBox fullSize = (CheckBox)findViewById(R.id.fullSizeImage);
    	fullSize.setChecked(WordPress.currentBlog.isFullSizeImage());
    	CheckBox scaledImage = (CheckBox)findViewById(R.id.scaledImage);
    	scaledImage.setChecked(WordPress.currentBlog.isScaledImage());
    	EditText scaledImageWidth = (EditText)findViewById(R.id.scaledImageWidth);
    	scaledImageWidth.setText(""+WordPress.currentBlog.getScaledImageWidth());
    	showScaledSetting(WordPress.currentBlog.isScaledImage());
    	//sets up a state listener for the scaled image checkbox
        ((CheckBox)findViewById(R.id.scaledImage)).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				CheckBox scaledImage = (CheckBox)findViewById(R.id.scaledImage);
				showScaledSetting(scaledImage.isChecked());
				if(scaledImage.isChecked()){
					CheckBox fullSize = (CheckBox)findViewById(R.id.fullSizeImage);
					fullSize.setChecked(false);
				}
			}
        });
        //sets up a state listener for the fullsize checkbox
        ((CheckBox)findViewById(R.id.fullSizeImage)).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				CheckBox fullSize = (CheckBox)findViewById(R.id.fullSizeImage);
				if(fullSize.isChecked()){
					CheckBox scaledImage = (CheckBox)findViewById(R.id.scaledImage);
					if(scaledImage.isChecked()){
						scaledImage.setChecked(false);
						showScaledSetting(false);
					}
				}
			}
        });
    	//don't show location option for devices that have no location support.
    	boolean hasLocationProvider = false;
    	LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
    	List<String> providers = locationManager.getProviders(true);
    	for (String providerName:providers) {
    	  if (providerName.equals(LocationManager.GPS_PROVIDER) 
    	      || providerName.equals(LocationManager.NETWORK_PROVIDER)) {
    	    hasLocationProvider = true;
    	  }
    	}
    	
    	CheckBox locationCB = (CheckBox)findViewById(R.id.location);
    	if (hasLocationProvider) {
    		locationCB.setChecked(WordPress.currentBlog.isLocation());
    	} else {
    		locationCB.setChecked(false);
    		RelativeLayout locationLayout = (RelativeLayout) findViewById(R.id.section3);
    		locationLayout.setVisibility(View.GONE);
    	}

    	spinner.setSelection(WordPress.currentBlog.getMaxImageWidthId());


        final Button saveButton = (Button) findViewById(R.id.save);
        
        saveButton.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v) {
                
                //capture the entered fields *needs validation*
                EditText usernameET = (EditText)findViewById(R.id.username);
                WordPress.currentBlog.setUsername(usernameET.getText().toString());
                EditText passwordET = (EditText)findViewById(R.id.password);
                WordPress.currentBlog.setPassword(passwordET.getText().toString());
                EditText httpuserET = (EditText)findViewById(R.id.httpuser);
                WordPress.currentBlog.setHttpuser(httpuserET.getText().toString());
                EditText httppasswordET = (EditText)findViewById(R.id.httppassword);
                WordPress.currentBlog.setHttppassword(httppasswordET.getText().toString());
                
                CheckBox fullSize = (CheckBox)findViewById(R.id.fullSizeImage);
                WordPress.currentBlog.setFullSizeImage(fullSize.isChecked());
                CheckBox scaledImage = (CheckBox)findViewById(R.id.scaledImage);
                WordPress.currentBlog.setScaledImage(scaledImage.isChecked());
                if(WordPress.currentBlog.isScaledImage()){
                	EditText scaledImgWidth = (EditText)findViewById(R.id.scaledImageWidth);
                	
                	boolean error = false;
                	int width = 0;
                	try {
                		width = Integer.parseInt(scaledImgWidth.getText().toString().trim());
                	} catch (NumberFormatException e) {
						error = true;
					}
                	
                	if (width == 0)
                		error = true;
                	
                	if (error) {
                		AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(Settings.this);
						  dialogBuilder.setTitle(getResources().getText(R.string.error));
			            dialogBuilder.setMessage(getResources().getText(R.string.scaled_image_error));
			            dialogBuilder.setPositiveButton("OK",  new
			          		  DialogInterface.OnClickListener() {
			                public void onClick(DialogInterface dialog, int whichButton) {
			                }
			            });
			            dialogBuilder.setCancelable(true);
			           dialogBuilder.create().show();
			           return;
                	} else {
                		WordPress.currentBlog.setScaledImageWidth(width);
                	}
                }
                Spinner spinner = (Spinner)findViewById(R.id.maxImageWidth);
                WordPress.currentBlog.setMaxImageWidth(spinner.getSelectedItem().toString());
                
                long maxImageWidthId = spinner.getSelectedItemId();
                int maxImageWidthIdInt = (int) maxImageWidthId;
                
                WordPress.currentBlog.setMaxImageWidthId(maxImageWidthIdInt);
                
                CheckBox locationCB = (CheckBox)findViewById(R.id.location);
                WordPress.currentBlog.setLocation(locationCB.isChecked());

                WordPress.currentBlog.save(Settings.this, originalUsername);
        		//exit settings screen
                Bundle bundle = new Bundle();
                
                bundle.putString("returnStatus", "SAVE");
                Intent mIntent = new Intent();
                mIntent.putExtras(bundle);
                setResult(RESULT_OK, mIntent);
                finish();
                
            }
        }); 
		
	}
	
	/**
	 * Hides / shows the scaled image settings
	 * @param show
	 */
	private void showScaledSetting(boolean show){
		TextView tw = (TextView)findViewById(R.id.l_scaledImage);
		EditText et = (EditText)findViewById(R.id.scaledImageWidth);
		tw.setVisibility(show?View.VISIBLE:View.GONE);
		et.setVisibility(show?View.VISIBLE:View.GONE);
	}

	@Override
    public void onConfigurationChanged(Configuration newConfig) {
      //ignore orientation change
      super.onConfigurationChanged(newConfig);
    } 
	
}
