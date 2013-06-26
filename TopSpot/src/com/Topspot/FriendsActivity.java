package com.Topspot;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

import org.json.JSONArray;
import org.json.JSONObject;

import DynamoDBTasks.GetUserIdsDynamo;
import android.app.ListActivity;
import android.content.SharedPreferences;
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
import com.example.topspot.R;
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
public class FriendsActivity extends ListActivity {
	private FoursquareApi foursquareApi;
	private SharedPreferences prefs;
	private CredentialStore credentialStore;
	private List<String> UserFriendsID = new ArrayList<String>();
	private List<UserAndVen> UserFriends = new ArrayList<UserAndVen>();
	private List<String> IdsInVenues = new ArrayList<String>();	
	private String FOURSQUARE_API_ENDPOINT;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_friends);
		this.prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
		this.credentialStore = new SharedPreferencesCredentialStore(prefs);
		
		//This is the https uri to get users friends id
		FOURSQUARE_API_ENDPOINT = "https://api.foursquare.com/v2/users/self/friends?oauth_token="+prefs.getString("AccessToken", null);
		
		
		UserFriends.clear();
		UserFriendsID.clear();
		IdsInVenues.clear();
		
		getUsersAndLocation();
		
		//Starts the thread that gets the foursquare user id used to perform foursquare get friends tasks that 
		new getUserId().execute();		
		
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
	//gets api needed to perform calls
	public FoursquareApi getFoursquareApi() {
		if (this.foursquareApi==null) {
			this.prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
			CredentialStore credentialStore = new SharedPreferencesCredentialStore(prefs);
			
			AccessTokenResponse accessTokenResponse = credentialStore.read();
			this.foursquareApi = new FoursquareApi(OAuth2ClientCredentials.CLIENT_ID,
					OAuth2ClientCredentials.CLIENT_SECRET,
					OAuth2ClientCredentials.REDIRECT_URI,
					prefs.getString("AccessToken", null), new DefaultIOHandler());
		}
		return this.foursquareApi;
		
	}
	private class getUserId extends AsyncTask<String, Void, Void>{


		@Override
		protected Void doInBackground(String... params) {
			try {
				performFoursquareIDCallUsingGoogleApiJavaClient();
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			return null;
		}
		
	}
	protected HttpRequestFactory createApiRequestFactory(HttpTransport transport, String accessToken) {
		return transport.createRequestFactory(new AccessProtectedResource(accessToken, Method.AUTHORIZATION_HEADER) {
			protected void onAccessToken(String accessToken) {
				// Called when a new access token is issues. Not applicable for Foursquare as Foursquare access tokens are long-lived.");
			};
			
			
		});
	}
	
	public void performFoursquareIDCallUsingGoogleApiJavaClient() throws Exception {
		AccessTokenResponse accessTokenResponse = credentialStore.read();
		HttpTransport transport = new NetHttpTransport();
		GenericUrl genericUrl = new GenericUrl(FOURSQUARE_API_ENDPOINT);		
		HttpRequest httpRequest = createApiRequestFactory(transport, accessTokenResponse.accessToken).buildGetRequest(
				genericUrl);
		
		HttpResponse httpResponse = httpRequest.execute();
		JSONObject object = new JSONObject(httpResponse.parseAsString());
		JSONObject fourSquareResponse = (JSONObject) object.get("response");
		JSONObject groups = (JSONObject) fourSquareResponse.get("friends");
		JSONArray friends = groups.getJSONArray("items");
		
		
		int id = 0;
		String name;
		String user;
		
		UserFriends.clear();
		System.out.println(friends.length());
		for (int i = 0; i < friends.length(); i++) {
		    JSONObject row = friends.getJSONObject(i);
		    id = row.getInt("id");
		    name = row.getString("firstName");
		    user = id+","+name;
		    UserFriendsID.add(user);
		    System.out.println(user);
		}
		System.out.println("UserFrID: " + UserFriendsID.size());
		
		
				
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

			try {			
				UserFriends.clear();				
				for (String s: UserFriendsID){
					String [] IdandName = s.split(",");
					for (String t: IdsInVenues) {
						String[] IdandNameInDB = t.split(":");					
						if (IdandNameInDB[0].equals(IdandName[0])){
							 UserAndVen userandvenue = new UserAndVen();
							 userandvenue.txtfriendName = IdandName[1];
							 userandvenue.Venue = IdandNameInDB[1];
							 UserFriends.add(userandvenue);
							}	
						}
					}					
				} catch (Exception ex) {
				Log.e(Constants.TAG, "Error retrieving venues", ex);
			}
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
