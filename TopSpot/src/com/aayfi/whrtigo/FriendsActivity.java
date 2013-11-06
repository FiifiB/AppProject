package com.aayfi.whrtigo;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.ParseException;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.ActionBar;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.aayfi.whrtigo.R;
import com.aayfi.whrtigo.DynamoDBTasks.GetUserIdsDynamo;
import com.facebook.android.AsyncFacebookRunner;
import com.facebook.android.AsyncFacebookRunner.RequestListener;
import com.facebook.android.Facebook;
import com.facebook.android.FacebookError;
import com.google.gson.Gson;

/**
 * This Activity is a list containing your friends that are in registered app venues
 * @author Fiifi
 *
 */
@SuppressWarnings("deprecation")
public class FriendsActivity extends ListActivity {
	private Facebook fb;
	private String APP_ID;
	private SharedPreferences prefs;
	private Map<String, FBperson> UserFriendsID = new HashMap<String, FBperson>() ;
	private List<UserAndVen> UserFriends = new ArrayList<UserAndVen>();
	private HashSet<String> IdsInVenues = new HashSet<String>();
	private AsyncFacebookRunner myAsyncRunner;
	private String UserId ;
	private String UserLName;
	private String MyLat;
	private String MyLon;
	private ListView listview;
	private Menu optionsMenu;
	private Intent overlay;
	
	@Override
	protected void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_friends);
		this.prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
		APP_ID = getString(R.string.APP_ID);
		fb = new Facebook(APP_ID);
		myAsyncRunner = new AsyncFacebookRunner(fb);
		String access_token = prefs.getString("FBAccessToken", null);
		long expires = prefs.getLong("FBAccessExpires", 0);
		
		ActionBar actionBar = getActionBar();
		actionBar.setHomeButtonEnabled(true);
	    actionBar.setDisplayHomeAsUpEnabled(true);
	    actionBar.setDisplayShowTitleEnabled(false);
		actionBar.setIcon(R.drawable.whrt2golabel);
	    
	    
		if (access_token != null && expires != 0 ){
			fb.setAccessToken(access_token);
			fb.setAccessExpires(expires);
		}else{
			Intent intent = new Intent(this,LoginActivity.class);
			startActivity(intent);
		}
		listview = getListView();
		
		//gets Location from intent 
		MyLat = Double.toString(getIntent().getExtras().getDouble("myLat"));
		MyLon = Double.toString(getIntent().getExtras().getDouble("myLon"));
		
		getFacebookID();
		
		UserFriends.clear();
		UserFriendsID.clear();
		IdsInVenues.clear();
		
		UserAndVen user = new UserAndVen();
		user.friendImg = getResources().getDrawable(R.drawable.mefb);
		user.txtfriendName = "Fiifi Botchway";
		user.Venue = "Marlowe Academy";
		UserFriends.add(user);
		
		//Starts the thread shows friends in the activity list adapter
		new FriendsListRefresher().execute();
		
		listview.setOnItemClickListener(new AdapterView.OnItemClickListener() {

		      @Override
		      public void onItemClick(AdapterView<?> parent, final View view,
		          int position, long id) {
		    	  
		    	  final UserAndVen friend = (UserAndVen) parent.getItemAtPosition(position);
		    	  Intent intent = new Intent(FriendsActivity.this,FriendProfileActivity.class);
		    	  Bundle bundle = new Bundle();
		    	  
		    	  bundle.putString("ID", friend.ID);
		    	  bundle.putString("MyID", UserId);
		    	  bundle.putString("MyLName", UserLName);
		    	  bundle.putString("Location", friend.Venue);
		    	  bundle.putString("Name", friend.txtfriendName);
		    	  bundle.putString("FriendFName", friend.FName);
		    	  bundle.putString("FriendLName", friend.LName);
		    	  
		    	  intent.putExtras(bundle);
		    	  startActivity(intent);		        
		      }
		    });
		
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
		optionsMenu = menu;
		getMenuInflater().inflate(R.menu.friends, menu);
		return super.onCreateOptionsMenu(menu);
	}
	
	public boolean onOptionsItemSelected(MenuItem item) {
		
	    switch (item.getItemId()) {
	        case android.R.id.home:
	            // app icon in action bar clicked; go home
	            Intent intent = new Intent(this, MapsActivity.class);
	            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
	            intent.putExtra("already fb logged", "false");
	            startActivity(intent);
	            return true;
	        case R.id.refresherfriends:
	        	new FriendsListRefresher().execute();
	        	return true;
	        default:
	            return super.onOptionsItemSelected(item);
	    }
	}
	
	private void getFacebookFriends(){
		final String uri = 
				"https://graph.facebook.com/me/friends?fields=name,id,first_name,last_name&access_token="+fb.getAccessToken();
		
				HttpClient httpclient = new DefaultHttpClient();			     
			    HttpGet httpget = new HttpGet(uri);
			    HttpEntity entity;
			    HttpResponse response = null;	
				JSONObject obj;
				try {					
						String responseString ;
						response = httpclient.execute(httpget);
						entity = response.getEntity();
						BufferedReader br = new BufferedReader(
			            new InputStreamReader((entity.getContent())));
						String line = null;
						StringBuffer theText = new StringBuffer();
						while((line=br.readLine())!=null){
						theText.append(line);
						}
						responseString = theText.toString();
						obj = new JSONObject(responseString);
						System.out.println(responseString);
						JSONArray data = obj.getJSONArray("data");
						for (int i = 0; i < data.length(); i++) {
							FBperson friend = new FBperson();
							JSONObject friendObj =data.getJSONObject(i);
							String id = friendObj.getString("id");
							friend.setId(id) ;
							friend.setName( friendObj.getString("name"));
							friend.setFName(friendObj.getString("first_name"));
							friend.setLName(friendObj.getString("last_name"));
							UserFriendsID.put(id, friend);
						}
					
				} catch (ClientProtocolException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (JSONException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}		
		

	}
	
	private Bitmap getFBpic(String id){
		Bitmap img = null;
		String uri = 
				"https://graph.facebook.com/"+id+"/picture?type=large";
			     HttpClient httpclient = new DefaultHttpClient();			     
			     HttpGet httpget = new HttpGet(uri);
			     org.apache.http.HttpResponse response = null;				     
				try {
					response = httpclient.execute(httpget);						
					HttpEntity entity = response.getEntity();
					img = BitmapFactory.decodeStream(entity.getContent());
				} catch (ClientProtocolException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
		
		return img;
	}
	
	


	/**
	 * This thread compares the ids in the venues with the users foursquare friends ids and
	 * displays the ones that are in the venue and where they are in the activity list
	 * @author Fiifi
	 *
	 */
	private class FriendsListRefresher extends AsyncTask<Void, Void, Void> {
		
		private ProgressDialog pd;
        @Override
        protected void onPreExecute() {
        	UserFriends.clear();
        	UserFriendsID.clear();
        	//start refresher spinner in action bar
        	setRefreshActionButtonState(true);
        	
        	overlay = new Intent(FriendsActivity.this,LoadingFriendsActivity.class);
        	startActivity(overlay);
        }
        
		@Override
		protected Void doInBackground(Void... params) {
			
			getFacebookFriends();
			
			int num = UserFriendsID.size();
			Location myloc = MapsActivity.getUserCurrentLoc();
			ArrayList<String> usersNearby = GetUsersNearby(Double.toString(myloc.getLatitude()),Double.toString(myloc.getLongitude()));
			for(String User: usersNearby){
				String verdict = null;
				int h = num;
				String [] SplitOne = User.split(";");
				System.out.println(SplitOne[0]+" "+ SplitOne[1]);
				JSONArray people = null;
				try {
					people = new JSONArray(SplitOne[1]);
					System.out.println("Success Array"+ " JSON Array Complete");
					Log.d("Success Array", "JSON Array Complete");
				} catch (JSONException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
				
				for (int i = 0; i < people.length(); i++) {
					FBperson test = new FBperson();
					try {
						test.setId(people.getString(i));
					} catch (JSONException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					try {
						if (UserFriendsID.containsKey(people.getString(i))){
							System.out.println("Found friend"+ " Found friend in loop");
							Log.d("Found friend", "Found friend in loop");
							Bitmap img = null;
							String index = people.getString(i);
							UserAndVen UserVen = new UserAndVen();
							UserVen.txtfriendName = UserFriendsID.get(index).getName();
							UserVen.FName = UserFriendsID.get(index).getFName();
							UserVen.LName = UserFriendsID.get(index).getLName();
							UserVen.Venue = SplitOne[0];
							UserVen.ID = UserFriendsID.get(index).getId();
							//gets picture of friend
							img = getFBpic(UserFriendsID.get(index).getId());
							UserFriendsID.get(index).setPic(img);
							UserVen.friendImg = new BitmapDrawable(UserFriendsID.get(index).getPic());
							System.out.println("Acting populating.........");
							UserFriends.add(UserVen);
						}
					} catch (JSONException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
				
			}			
			return null;
		}		
		@Override
		protected void onPostExecute(Void result) {
			
			//method used to set friends in the list
			listview.setAdapter(new TableAdapter(UserFriends));
			
//			stop refresher spinner
			setRefreshActionButtonState(false);
			LoadingFriendsActivity.instance.finish();

		}
	}
	
	private UserAndVen getfriendMapFromAdapter(int position) {
		return (((TableAdapter) listview.getAdapter()).getItem(position));
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
			//Makes sure we have a view to work with
			if (convertView == null) {
				convertView = getLayoutInflater().inflate(R.layout.places_list_row, parent, false);
				holder = new ViewHolder();
				holder.txtfriendName = (TextView) convertView.findViewById(R.id.row_placename);
				holder.txtPlaceAddress = (TextView) convertView.findViewById(R.id.row_placeaddress);
				holder.friendPic = (ImageView) convertView.findViewById(R.id.profilePictureView1);
				holder.layout = (RelativeLayout) convertView.findViewById(R.id.row_layout);
				convertView.setTag(holder);
			} else {
				holder = (ViewHolder) convertView.getTag();
			}

			UserAndVen Users = getfriendMapFromAdapter(position);			

			try {
				holder.txtfriendName.setText(Users.txtfriendName);
				if (Users.Venue != null && Users.Venue.length() > 0) {
					holder.txtPlaceAddress.setText("Is at "+Users.Venue + " Now");
					holder.friendPic.setImageDrawable(Users.friendImg);
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
		ImageView friendPic;
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
		private String FName;
		private String LName;
		public String Venue;
		public Drawable friendImg;
		public String ID;
		
	}
	
	/**
	 * This method starts the thread that gets all ids in every registered venues 
	 * and stores them in an array.
	 */
	private void getUsersAndLocation(){
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
	
	private ArrayList<String> GetUsersNearby(String Lat,String Lon){
		ArrayList<String> result = null;
		String uri = 
				"http://ec2-54-226-63-49.compute-1.amazonaws.com:8080/MongoDBServices/CheckUserId?param="+Lat+"&param="+Lon;
			     HttpClient httpclient = new DefaultHttpClient();			     
			     HttpGet httpget = new HttpGet(uri);
			     HttpResponse response = null;
				try {
					response = httpclient.execute(httpget);
				} catch (ClientProtocolException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
//			     System.out.println(response.getStatusLine().toString());
			     HttpEntity entity = response.getEntity();
			     
			     Gson gson = new Gson();
			     try {
					result = gson.fromJson(EntityUtils.toString(entity), ArrayList.class);
					
				} catch (ParseException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
		return result;
		
	}
	private class FBperson{
		private String Id;
		private String Name;
		private String FName;
		private String LName;
		private Bitmap pic;
		
		public String getId() {
			return Id;
		}
		public void setId(String id) {
			Id = id;
		}
		public String getName() {
			return Name;
		}
		public void setName(String name) {
			Name = name;
		}
		public Bitmap getPic() {
			return pic;
		}
		public void setPic(Bitmap pic) {
			this.pic = pic;
		}
		public String getFName() {
			return FName;
		}
		public void setFName(String fName) {
			FName = fName;
		}
		public String getLName() {
			return LName;
		}
		public void setLName(String lName) {
			LName = lName;
		}
	}
	
	//Get Facebook Id
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
		                // getting id of the user
		                UserId = profile.getString("id");
		                UserLName = profile.getString("last_name");
		 
		            } catch (JSONException e) {
		                e.printStackTrace();
		            }
					
				}
			});
		}
	
		public void setRefreshActionButtonState(final boolean refreshing) {
		    if (optionsMenu != null) {
		        final MenuItem refreshItem = optionsMenu
		            .findItem(R.id.refresherfriends);
		        if (refreshItem != null) {
		            if (refreshing) {
		                refreshItem.setActionView(R.layout.refresher_progress);
		            } else {
		                refreshItem.setActionView(null);
		            }
		        }
		    }
		}
	
}
