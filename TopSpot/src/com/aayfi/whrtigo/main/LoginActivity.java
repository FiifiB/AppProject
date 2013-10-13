package com.aayfi.whrtigo.main;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.ActionBar;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageButton;
import android.widget.Toast;

import com.aayfi.whrtigo.R;
import com.facebook.android.AsyncFacebookRunner;
import com.facebook.android.AsyncFacebookRunner.RequestListener;
import com.facebook.android.DialogError;
import com.facebook.android.Facebook;
import com.facebook.android.Facebook.DialogListener;
import com.facebook.android.FacebookError;
import com.quickblox.core.QBCallback;
import com.quickblox.core.QBSettings;
import com.quickblox.core.result.Result;
import com.quickblox.internal.core.helper.StringifyArrayList;
import com.quickblox.module.auth.QBAuth;
import com.quickblox.module.users.QBUsers;
import com.quickblox.module.users.model.QBUser;
import com.quickblox.module.users.result.QBUserResult;

/**
 * Login activity that directs users to authorization page
 * @author Fiifi
 *
 */
@SuppressWarnings("deprecation")
public class LoginActivity extends Activity implements QBCallback {
	private Facebook fb;
	private String APP_ID;
	private AsyncFacebookRunner myAsyncRunner;
	private String UserId;
	private String UserFName;
	private String UserLName;
	private QBUser QBUser;
	private SharedPreferences prefs;
	private String[] permissions ={"user_location","user_checkins","user_status","friends_online_presence","friends_status"}; 
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_login);
		ActionBar actionBar = getActionBar();
		actionBar.hide();
		APP_ID = getString(R.string.APP_ID);
		this.prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
		fb = new Facebook(APP_ID);
		myAsyncRunner = new AsyncFacebookRunner(fb);
		String access_token = prefs.getString("FBAccessToken", null);
		long expires = prefs.getLong("FBAccessExpires", 0);
		
		
        //initialise Facebook with the access token expirey date from shared preference
		if (access_token != null && expires != 0 ){
			fb.setAccessToken(access_token);
			fb.setAccessExpires(expires);
			
			Intent intent = new Intent(LoginActivity.this,MapsActivity.class);
			intent.putExtra("already fb logged", "true");
			finish();
			startActivity(intent);
			
		}
		
		 // ================= QuickBlox ===== Step 1 =================
        // Initialize QuickBlox application with credentials.
        // Getting app credentials -- http://quickblox.com/developers/Getting_application_credentials
        QBSettings.getInstance().fastConfigInit("3795", "gZ3hsD4n-Qpjata", "XRDDFnuAd6e7KGx");

        // ================= QuickBlox ===== Step 2 =================
        // Authorize application.
        QBAuth.createSession(new QBCallback() {
			
			@Override
			public void onComplete(Result arg0, Object arg1) {
				// TODO Auto-generated method stub
				
			}
			
			@Override
			public void onComplete(Result arg0) {
				// TODO Auto-generated method stub
				
			}
		});
		
	
		
		ImageButton fblogin = (ImageButton)findViewById(R.id.fbbttn);
		fblogin.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				
				if(fb.isSessionValid()){					
					getFacebookID();					
					
				}else {
					fb.authorize(LoginActivity.this, permissions,Facebook.FORCE_DIALOG_AUTH, new DialogListener() {
						
						@Override
						public void onFacebookError(FacebookError e) {
							Toast.makeText(LoginActivity.this, "Facebook Error", Toast.LENGTH_SHORT).show();
							
						}
						
						@Override
						public void onError(DialogError e) {
							Toast.makeText(LoginActivity.this, "App Error", Toast.LENGTH_SHORT).show();
							
						}
						
						@Override
						public void onComplete(Bundle values) {
							Editor editor  = prefs.edit();
							editor.putString("FBAccessToken", fb.getAccessToken());
							editor.putLong("FBAccessExpires", fb.getAccessExpires());
							editor.commit();
							
							getFacebookID();							
							
						}
						
						@Override
						public void onCancel() {
							Toast.makeText(LoginActivity.this, "You canceled", Toast.LENGTH_SHORT).show();							
						}
					});						
				}				
			}
		});		
	}

	private void signInOrSignUptoQB() {
		
		QBUser = new QBUser("00"+UserId+"11", UserId+UserLName);
		Integer integ = Integer.valueOf(UserId);							
		QBUser.setId(integ);
		QBUser.setFacebookId(UserId);
		QBUser.setFullName(UserFName + ""+ UserLName);
		StringifyArrayList<String> tags = new StringifyArrayList<String>();
		tags.add("AppUser");
		QBUser.setTags(tags);	
		
		// ================= QuickBlox ===== Step 3 =================
        // Register user or sign in QuickBlox. 
		QBUsers.signUp(QBUser, LoginActivity.this);
		
		Intent intent = new Intent(LoginActivity.this,MapsActivity.class);
		intent.putExtra("already fb logged", "false");
		startActivity(intent);
		
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		// TODO Auto-generated method stub
		super.onActivityResult(requestCode, resultCode, data);
		fb.authorizeCallback(requestCode, resultCode, data);
	}
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.login, menu);
		return true;
	}
//	Get Facebook Id and user Info
//	private void getFacebookID(){
//		String json = null;
//		try {
//			json = fb.request("me");
//		} catch (MalformedURLException e1) {
//			// TODO Auto-generated catch block
//			e1.printStackTrace();
//		} catch (IOException e1) {
//			// TODO Auto-generated catch block
//			e1.printStackTrace();
//		}
//        try {
//            JSONObject profile = new JSONObject(json);
//            // getting id and user info
//            UserId = profile.getString("id");
//            UserFName = profile.getString("first_name");
//            UserLName = profile.getString("last_name");           
//        } catch (JSONException e) {
//            e.printStackTrace();
//        } 
//	}
	
	private void getFacebookID(){
		myAsyncRunner.request("me", new RequestListener() {
			
			@Override
			public void onMalformedURLException(MalformedURLException e, Object state) {
				// TODO Auto-generated method stub
				
			}
			
			@Override
			public void onIOException(IOException e, Object state) {
				// TODO Auto-generated method stub
				
			}
			
			@Override
			public void onFileNotFoundException(FileNotFoundException e, Object state) {
				// TODO Auto-generated method stub
				
			}
			
			@Override
			public void onFacebookError(FacebookError e, Object state) {
				// TODO Auto-generated method stub
				
			}
			
			@Override
			public void onComplete(String response, Object state) {
	            String json = response;
	            try {
	                JSONObject profile = new JSONObject(json);
	                // getting id and user info
	                UserId = profile.getString("id");
	                UserFName = profile.getString("first_name");
	                UserLName = profile.getString("last_name"); 
	                
	                runOnUiThread(new Runnable() {
						public void run() {
							signInOrSignUptoQB();							
						}
					});
	                
	 
	            } catch (JSONException e) {
	                e.printStackTrace();
	            }
				
			}
		});
	}
	
	@Override
	public void onComplete(Result arg0, Object arg1) {
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public void onComplete(Result result) {
		if (result.isSuccess()) {
            QBUserResult qbUserResult = (QBUserResult) result;
            Log.d("Registration was successful","user: " + qbUserResult.getUser().toString());
        } else {
//        	List<String> error = null;
//        	error.add("login has already been taken");							        	
//        	if (result.getErrors().equals(error)){
        		
        		// ================= QuickBlox ===== Step 3 =================
                // Login user into QuickBlox.
                // Pass this activity , because it implements QBCallback interface.
                // Callback result will come into onComplete method below.
        		QBUsers.signIn(QBUser, LoginActivity.this);
        	
            Log.e("Errors",result.getErrors().toString()); 
		
				}
	}
	
}
