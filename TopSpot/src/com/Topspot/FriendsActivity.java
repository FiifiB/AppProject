package com.Topspot;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import DynamoDBTasks.GetUserIdsDynamo;
import android.R.drawable;
import android.app.ListActivity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.RadioButton;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.AuthorizationAndStore.CredentialStore;
import com.AuthorizationAndStore.OAuth2ClientCredentials;
import com.AuthorizationAndStore.SharedPreferencesCredentialStore;
import com.amazonaws.javax.xml.stream.xerces.util.URI;
import com.example.topspot.R;
import com.facebook.android.AsyncFacebookRunner;
import com.facebook.android.AsyncFacebookRunner.RequestListener;
import com.facebook.android.Facebook;
import com.facebook.android.FacebookError;
import com.google.api.client.auth.oauth2.draft10.AccessProtectedResource;
import com.google.api.client.auth.oauth2.draft10.AccessProtectedResource.Method;
import com.google.api.client.auth.oauth2.draft10.AccessTokenResponse;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestFactory;
import com.google.api.client.http.HttpResponse;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;

import fi.foyt.foursquare.api.FoursquareApi;
import fi.foyt.foursquare.api.io.DefaultIOHandler;

/**
 * This Activity is a list containing your friends that are in registered app venues
 * @author Fiifi
 *
 */
@SuppressWarnings("deprecation")
public class FriendsActivity extends ListActivity {
	private Facebook fb;
	private String APP_ID;
	private AsyncFacebookRunner myAsyncRunner;
	private String UserId ;
	private FoursquareApi foursquareApi;
	private SharedPreferences prefs;
	private CredentialStore credentialStore;
	private HashSet<HashMap> UserFriendsID = new HashSet<HashMap>();
	private List<UserAndVen> UserFriends = new ArrayList<UserAndVen>();
	private HashSet<String> IdsInVenues = new HashSet<String>();	
	private String FOURSQUARE_API_ENDPOINT;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_friends);
		this.prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
		this.credentialStore = new SharedPreferencesCredentialStore(prefs);
		APP_ID = getString(R.string.APP_ID);
		fb = new Facebook(APP_ID);
		myAsyncRunner = new AsyncFacebookRunner(fb);
		String access_token = prefs.getString("FBAccessToken", null);
		long expires = prefs.getLong("FBAccessExpires", 0);
		if (access_token != null && expires != 0 ){
			fb.setAccessToken(access_token);
			fb.setAccessExpires(expires);
		}else{
			Intent intent = new Intent(FriendsActivity.this,LoginActivity.class);
			finish();
			startActivity(intent);
		}
		
		
		UserFriends.clear();
		UserFriendsID.clear();
		IdsInVenues.clear();
		
		getUsersAndLocation();
		
		getFacebookFriends();
		
		//Starts the thread shows friends in the activity list adapter
		new FriendsListRefresher().execute();
		
	}
	protected void onPause() {
		super.onPause();
		UserFriends.clear();
		UserFriendsID.clear();
		IdsInVenues.clear();
	}
		

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.friends, menu);
		return true;
	}
	
	public class friendRequest implements RequestListener{

		@Override
		public void onComplete(String response, Object state) {
			JSONObject obj;
			try {
				obj = new JSONObject(response);
					JSONArray data = obj.getJSONArray("data");
			for (int i = 0; i < data.length(); i++) {
				final HashMap<String, String> friend = new HashMap<String,String>();
				JSONObject friendObj =data.getJSONObject(i);
				String id = friendObj.getString("id");
				friend.put("id", id) ;
				friend.put("name", friendObj.getString("name")) ;
				
				//gets picture of friend
				Bundle param = new Bundle();
				param.putString("type", "normal");
				myAsyncRunner.request(id+"/picture",param, new RequestListener() {
					
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
						
						//Algorithm to handle extracting friend picture and saving it to
						//FB friend object
						JSONObject obj;
						Bitmap pic = null;
						try {
							obj = new JSONObject(response);
							JSONObject data = obj.getJSONObject("data");
							URL url = new URL(data.getString("url"));
							friend.put("img", url.toString());
							UserFriendsID.add(friend);
						} catch (JSONException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						} catch (MalformedURLException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						
						
					}
				});
				
			}
			} catch (JSONException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
		
			
		}

		@Override
		public void onIOException(IOException e, Object state) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void onFileNotFoundException(FileNotFoundException e,
				Object state) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void onMalformedURLException(MalformedURLException e,
				Object state) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void onFacebookError(FacebookError e, Object state) {
			// TODO Auto-generated method stub
			
		}
		
	}
	
	private void getFacebookFriends(){
		myAsyncRunner.request("me/friends", new friendRequest());
	}


	/**
	 * This thread compares the ids in the venues with the users foursquare friends ids and
	 * displays the ones that are in the venue and where they are in the activity list
	 * @author Fiifi
	 *
	 */
	private class FriendsListRefresher extends AsyncTask<Void, Void, Void> {

		@Override
		protected Void doInBackground(Void... params) {
			HashSet<String> tempId = new HashSet<String>();
			Iterator<HashMap> FBsetIterator = UserFriendsID.iterator();
			Iterator<String> DBsetIterator = IdsInVenues.iterator();
			
			while (FBsetIterator.hasNext()){
				HashMap<String, String> FBfriend = FBsetIterator.next();
				tempId.add(FBfriend.get("id"));
			}			
			
			while (DBsetIterator.hasNext()){
				String [] split = DBsetIterator.next().split(":");
				if (tempId.contains(split[0])){
					while(FBsetIterator.hasNext()){
						HashMap<String, String> FBfriend = FBsetIterator.next();
						if(FBfriend.containsValue(split[0])){
							UserAndVen UserVen = new UserAndVen();
							UserVen.txtfriendName = FBfriend.get("name");
							UserVen.Venue = split[1];
							URL url = null;
							try {
								url = new URL(FBfriend.get("url"));
								Bitmap pic = BitmapFactory.decodeStream(url.openConnection().getInputStream());
								UserVen.friendImg = new BitmapDrawable(pic);
							} catch (MalformedURLException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							} catch (IOException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
							UserFriends.add(UserVen);
							
							
						}
					}
				}
			}
			
			

//			try {			
//				UserFriends.clear();				
//				for (String s: UserFriendsID){
//					String [] IdandName = s.split(",");
//					for (String t: IdsInVenues) {
//						String[] IdandNameInDB = t.split(":");					
//						if (IdandNameInDB[0].equals(IdandName[0])){
//							 UserAndVen userandvenue = new UserAndVen();
//							 userandvenue.txtfriendName = IdandName[1];
//							 userandvenue.Venue = IdandNameInDB[1];
//							 UserFriends.add(userandvenue);
//							}	
//						}
//					}					
//				} catch (Exception ex) {
//				Log.e(Constants.TAG, "Error retrieving venues", ex);
//			}
			return null;
		}		
		@Override
		protected void onPostExecute(Void result) {
			
			//method used to set friends in the list
			setListAdapter(new TableAdapter(UserFriends));
		}
	}
	
	private UserAndVen getfriendMapFromAdapter(int position) {
		return (((TableAdapter) getListAdapter()).getItem(position));
	}
	
	/**
	 * Class controlling the Activity list
	 * @author Fiifi
	 *
	 */
	class TableAdapter extends ArrayAdapter<UserAndVen> {			
		TableAdapter(List<UserAndVen> list) {
			super(FriendsActivity.this, R.layout.places_list_row, list);					
		}

		public View getView(int position, View convertView, ViewGroup parent) {
			ViewHolder holder = null;
			
			if (convertView == null) {
				convertView = getLayoutInflater().inflate(R.layout.places_list_row, parent, false);
				holder = new ViewHolder();
				holder.txtfriendName = (TextView) convertView.findViewById(R.id.row_placename);
				holder.txtPlaceAddress = (TextView) convertView.findViewById(R.id.row_placeaddress);
				holder.layout = (RelativeLayout) convertView.findViewById(R.id.row_layout);
				convertView.setTag(holder);
			} else {
				holder = (ViewHolder) convertView.getTag();
			}

			UserAndVen Users = getfriendMapFromAdapter(position);			

			try {
				holder.txtfriendName.setText(Users.txtfriendName);
				if (Users.Venue != null && Users.Venue.length() > 0) {
					holder.txtPlaceAddress.setText("At "+Users.Venue + " Now");
				} else {
					holder.txtPlaceAddress.setText("no_friends_info_found");
				}
			} catch (Exception ex) {
				ex.printStackTrace();
			}

			return (convertView);
		}
	}
	
	//Holder used to structure list
	static class ViewHolder {
		TextView txtfriendName;
		TextView txtPlaceAddress;
		RadioButton radio;
		RelativeLayout layout;
	}
	
	/**
	 * This class is used to create objects that store Friends name and the venue they are in
	 * @author Fiifi
	 *
	 */
	static class UserAndVen {
		public String txtfriendName;
		public String Venue;
		public Drawable friendImg;
	}
	
	/**
	 * This method starts the thread that gets all ids in every registered venues 
	 * and stores them in an array.
	 */
	public void getUsersAndLocation(){
		IdsInVenues.clear();
			
			try {
				IdsInVenues = new GetUserIdsDynamo().execute().get();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (ExecutionException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}	
			for (String t: IdsInVenues){
				System.out.println(t);
			}
		
	}
	
	
}
