package com.Topspot;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

import com.AuthorizationAndStore.OAuthAccessTokenActivity;
import com.example.topspot.R;
import com.facebook.Session;
import com.facebook.Session.NewPermissionsRequest;
import com.facebook.SessionState;
import com.facebook.android.AsyncFacebookRunner;
import com.facebook.android.DialogError;
import com.facebook.android.Facebook;
import com.facebook.android.Facebook.DialogListener;
import com.facebook.android.FacebookError;

import android.os.Bundle;
import android.preference.PreferenceManager;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.Signature;
import android.util.Base64;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

/**
 * Login activity that directs users to authorization page
 * @author Fiifi
 *
 */
@SuppressWarnings("deprecation")
public class LoginActivity extends Activity {
	private Facebook fb;
	private String APP_ID;
	private AsyncFacebookRunner myAsyncRunner;
	private SharedPreferences prefs;
	private String[] permissions ={"user_location","user_checkins","user_status","friends_online_presence"}; 
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_login);
		APP_ID = getString(R.string.APP_ID);
		this.prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
		fb = new Facebook(APP_ID);
		myAsyncRunner = new AsyncFacebookRunner(fb);
		String access_token = prefs.getString("FBAccessToken", null);
		long expires = prefs.getLong("FBAccessExpires", 0);
		
		if (access_token != null && expires != 0 ){
			fb.setAccessToken(access_token);
			fb.setAccessExpires(expires);
			Intent intent = new Intent(LoginActivity.this,ContainActivity.class);
			finish();
			startActivity(intent);
		}
		
		ImageButton fblogin = (ImageButton)findViewById(R.id.fbbttn);
		fblogin.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				
				if(fb.isSessionValid()){
					Intent intent = new Intent(LoginActivity.this,ContainActivity.class);
					finish();
					startActivity(intent);
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
							
							Intent intent = new Intent(LoginActivity.this,ContainActivity.class);
							finish();
							startActivity(intent);
							
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
	
	private class SessionStatusCallback implements Session.StatusCallback {
	    @Override
	    public void call(Session session, SessionState state, Exception exception) {
	            // Respond to session state changes, ex: updating the view
	    }
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
}
