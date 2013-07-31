package com.aayfi.whrtigo.main;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutionException;

import org.apache.http.HttpEntity;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
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
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.aayfi.whrtigo.R;
import com.aayfi.whrtigo.DynamoDBTasks.GetUserIdsDynamo;
import com.facebook.android.AsyncFacebookRunner;
import com.facebook.android.Facebook;

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
	private SharedPreferences prefs;
	private HashSet<HashMap> UserFriendsID = new HashSet<HashMap>();
	private List<UserAndVen> UserFriends = new ArrayList<UserAndVen>();
	private HashSet<String> IdsInVenues = new HashSet<String>();	
	
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
			finish();
			startActivity(intent);
		}
		
		
		UserFriends.clear();
		UserFriendsID.clear();
		IdsInVenues.clear();
		
		UserAndVen user = new UserAndVen();
		user.friendImg = getResources().getDrawable(R.drawable.mefb);
		user.txtfriendName = "Fiifi Botchway";
		user.Venue = "Marlowe Academy";
		UserFriends.add(user);
		
		//Gets the people in every location in the database
		getUsersAndLocation();
		
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
	
	public boolean onOptionsItemSelected(MenuItem item) {
	    switch (item.getItemId()) {
	        case R.drawable.whrt2golabel:
	            // app icon in action bar clicked; go home
	            Intent intent = new Intent(this, FriendsActivity.class);
	            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
	            startActivity(intent);
	            return true;
	        default:
	            return super.onOptionsItemSelected(item);
	    }
	}
	
	private void getFacebookFriends(){
		Bitmap img;
		JSONObject obj;
			try {
				String response = fb.request("me/friends");
				obj = new JSONObject(response);
				JSONArray data = obj.getJSONArray("data");
					for (int i = 0; i < data.length(); i++) {
						HashMap friend = new HashMap();
						JSONObject friendObj =data.getJSONObject(i);
						String id = friendObj.getString("id");
						friend.put("id", id) ;
						friend.put("name", friendObj.getString("name")) ;
						
						//gets picture of friend
						img = getFBpic(id);
						friend.put("img", img);
				
					}
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
                 pd = new ProgressDialog(FriendsActivity.this);
                 pd.setTitle("Processing Friends...");
                 pd.setMessage("Retrieving friends in venues.\nPlease wait........");
                 pd.setCancelable(false);
                 pd.setIndeterminate(true);
                 pd.show();
        }
        
		@Override
		protected Void doInBackground(Void... params) {
			getFacebookFriends();
			HashSet<String> tempId = new HashSet<String>();
			Iterator<HashMap> FBsetIterator = UserFriendsID.iterator();
			Iterator<String> DBsetIterator = IdsInVenues.iterator();
			
			while (FBsetIterator.hasNext()){
				HashMap FBfriend = FBsetIterator.next();
				tempId.add(FBfriend.get("id").toString());
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
							//								url = new URL(FBfriend.get("url"));
//								Bitmap pic = BitmapFactory.decodeStream(url.openConnection().getInputStream());
							UserVen.friendImg = new BitmapDrawable(FBfriend.get("img"));
							
							
						}
					}
				}
			}
			return null;
		}		
		@Override
		protected void onPostExecute(Void result) {
			
			//method used to set friends in the list
			setListAdapter(new TableAdapter(UserFriends));
			pd.dismiss();
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
