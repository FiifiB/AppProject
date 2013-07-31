package com.aayfi.whrtigo.main;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.aayfi.whrtigo.R;
import com.aayfi.whrtigo.AuthorizationAndStore.SharedPreferencesCredentialStore;
import com.aayfi.whrtigo.DynamoDBTasks.AddIdDynamo;
import com.aayfi.whrtigo.DynamoDBTasks.GetLocationsDynamo;
import com.aayfi.whrtigo.DynamoDBTasks.GetNoOfPeopleDynamo;
import com.aayfi.whrtigo.DynamoDBTasks.RemoveIdDynamo;
import com.facebook.android.AsyncFacebookRunner;
import com.facebook.android.AsyncFacebookRunner.RequestListener;
import com.facebook.android.Facebook;
import com.facebook.android.FacebookError;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMap.InfoWindowAdapter;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.UiSettings;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.maps.MyLocationOverlay;
import com.google.android.maps.Overlay;

@SuppressWarnings("deprecation")
public class MapsActivity extends Activity {
	private Facebook fb;
	private String APP_ID;
	private AsyncFacebookRunner myAsyncRunner;
	private GoogleMap myGmap;
	private Location myLoc;
	private LocationManager locationManager;
	private static int time = 50000;
	private static int distance = 50;
	private Criteria criteria = new Criteria();
	private SharedPreferences prefs;
	private String UserId ;
	private List<Overlay> mapOverlays;
	private ArrayList<FBLocationObj> FbLocations= new ArrayList<FBLocationObj>();
	private VenuesOverlay locOverlay;
	private MyLocationOverlay myLocationOverlay;
	private List<String> RegisteredVeneus = new ArrayList<String>();
	private Menu optionsMenu;
	private BroadcastReceiver proxiReceiver;
	private IntentFilter filter;
	private static final String TREASURE_PROXIMITY_ALERT = "com.topspot.action.proximityalert";
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_maps);
		this.prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
		new SharedPreferencesCredentialStore(prefs);
		
		ActionBar actionBar = getActionBar();
		actionBar.setDisplayShowTitleEnabled(false);
		actionBar.setIcon(R.drawable.whrt2golabel);
		
		APP_ID = getString(R.string.APP_ID);
		fb = new Facebook(APP_ID);
		myAsyncRunner = new AsyncFacebookRunner(fb);
		String access_token = prefs.getString("FBAccessToken", null);
		long expires = prefs.getLong("FBAccessExpires", 0);
		
		if (access_token != null && expires != 0 ){
			fb.setAccessToken(access_token);
			fb.setAccessExpires(expires);
		}else{
			Intent intent = new Intent(MapsActivity.this,LoginActivity.class);
			finish();
			startActivity(intent);
		}
		
		
		//get the location manageer
		locationManager = (LocationManager)getSystemService(Context.LOCATION_SERVICE);
		
		//Check to see if gps is on
		checkConnectivity();
			
		
		
		//Settings for the map
		myGmap = ((MapFragment) getFragmentManager().findFragmentById(R.id.mapview)).getMap();
		UiSettings mapSettings = myGmap.getUiSettings();
		mapSettings.setMyLocationButtonEnabled(true);
		mapSettings.setCompassEnabled(true);
		mapSettings.setZoomControlsEnabled(true);
		mapSettings.setZoomGesturesEnabled(true);
		mapSettings.setMyLocationButtonEnabled(true);
		myGmap.setMyLocationEnabled(true);
		
		
		
		
		
		//Sets Criteria to get the best location provider		
		criteria.setAccuracy(Criteria.ACCURACY_COARSE);
		criteria.setPowerRequirement(Criteria.POWER_LOW);
		criteria.setAltitudeRequired(false);
		criteria.setBearingRequired(false);
		criteria.setSpeedRequired(false);
		criteria.setCostAllowed(true);
		String bestProvider = locationManager.getBestProvider(criteria, true);		
		
		//gets your current location
		myLoc = locationManager.getLastKnownLocation(bestProvider);
		
		
		
		locationManager.requestLocationUpdates(bestProvider, time, distance, myLocationListner);
		
		//Put map to current location		
		LatLng latLng = new LatLng(myLoc.getLatitude(), myLoc.getLongitude());
		myGmap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 13));
		
		//Set up for proximity alerts
		filter = new IntentFilter(TREASURE_PROXIMITY_ALERT);
		proxiReceiver = new ProximityIntentReceiver();
		registerReceiver(proxiReceiver, filter);		
		registerIntents(locationManager);
		
		//Launches thread to get Locations in the Database 
		getRegisteredLocation();
		
		getFacebookID();
		
		myGmap.setInfoWindowAdapter(new myInfoWindowAdapter());
		
		//populates the map with places near your location
		updateWithNewLocation(myLoc);

		
		
	}
	
	public void checkConnectivity(){
		if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)){    	      
    				
    			AlertDialog.Builder notice = new AlertDialog.Builder(this);
    			notice.setTitle("Turn on GPS")
    			.setMessage("Please turn on GPS Location to use the app")
    			.setPositiveButton("Location Settings", new DialogInterface.OnClickListener() {
    				
    				@Override
    				public void onClick(DialogInterface dialog, int which) {
    					Intent in = new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS);
    					finish();
    					startActivity(in);
    					
    				}
    			});	
    			AlertDialog alert = notice.create();
    			alert.show();
		}
	}


	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		optionsMenu = menu;
		getMenuInflater().inflate(R.menu.maps, menu);
		return super.onCreateOptionsMenu(menu);
	}
	public boolean onOptionsItemSelected(MenuItem item) {
	    switch (item.getItemId()) {
	        case R.id.menu_friends:
	            // app icon in action bar clicked; go home
	            Intent intent = new Intent(this, FriendsActivity.class);
	            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
	            startActivity(intent);
	            return true;
	        case R.id.refreshermaps:
	        	updateWithNewLocation(myLoc);
	        	return true;
	        	
	        default:
	            return super.onOptionsItemSelected(item);
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
	 
	            } catch (JSONException e) {
	                e.printStackTrace();
	            }
				
			}
		});
	}
	
	//Gets Nearby Venues That have been registered with this app
	private class GetNearbyVenues extends AsyncTask<Void, Void, Void> {
		
		private ProgressDialog pd;
        @Override
        protected void onPreExecute() {
        	setRefreshActionButtonState(true);
        	LatLng latLng = new LatLng(myLoc.getLatitude(), myLoc.getLongitude());
    		myGmap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 13));
        }
		
		@Override
		protected Void doInBackground(Void... params) {			
			
			getFBPlacesNearby(myLoc);			
			return null;
		}
		
		@Override
		protected void onPostExecute(Void result) {
			for (String Rvenue: RegisteredVeneus){
				for (int i = 0; i < FbLocations.size(); i++){
					//Compares Lat and Lon in Database and FB Places to see if they match
					if (Rvenue.equals(FbLocations.get(i).getLocation())){				
						String[] LatLon = Rvenue.split(",");
						Double Dlat = Double.parseDouble(LatLon[0]);
						Double Dlon = Double.parseDouble(LatLon[1]);
						//creates Google LatLng to add location to the map
						LatLng p = 
						new LatLng(Dlat,Dlon);
						Toast.makeText(MapsActivity.this, "found.....", Toast.LENGTH_SHORT).show();
						
						//gets the number of people in that Place
						Integer count;
						count = getPeople(FbLocations.get(i).getLocation());
						
						//create the item to add to the map
						String message = "Population at "+FbLocations.get(i).getName()+": "+count;
						MarkerOptions item = new MarkerOptions();
						item.position(p);
						item.title(FbLocations.get(i).getName());
						item.snippet(message);
								
							
						item.icon(BitmapDescriptorFactory.fromBitmap(addBorder(FbLocations.get(i).Img, 10)));
//						Bitmap.createScaledBitmap(addBorder(FbLocations.get(i).Img, 3),350,250,true)
						Marker marker = myGmap.addMarker(item);
					}
				}
			}
			
//			pd.dismiss();
			setRefreshActionButtonState(false);
		}

	}
	
	//Location Listener to perform tasks on locations changed or status of GPS
	LocationListener myLocationListner = new LocationListener() {
		
		@Override
		public void onStatusChanged(String provider, int status, Bundle extras) {
			//Update app if provider hardware status changed
			
		}
		
		@Override
		public void onProviderEnabled(String provider) {
			//Update app if provider is enabled
			
		}
		
		@Override
		public void onProviderDisabled(String provider) {
			//Update app if provider is disabled
			if (!locationManager.isProviderEnabled(provider)){
				AlertDialog.Builder notice = new AlertDialog.Builder(getApplicationContext());
				notice.setTitle("Turn on GPS")
				.setMessage("Please turn on GPS Location to use the app")
				.setPositiveButton("Location Settings", new DialogInterface.OnClickListener() {
					
					@Override
					public void onClick(DialogInterface dialog, int which) {
						Intent in = new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS);
						finish();
						startActivity(in);
						
					}
				});	
				AlertDialog alert = notice.create();
				alert.show();
			}
			
		}
		
		@Override
		public void onLocationChanged(Location location) {
			//Update app based on new location
			updateWithNewLocation(location);
		}
	};
	
	//Updates map with locations near your and how many people there are in the locations
	private void updateWithNewLocation (Location location){
		new GetNearbyVenues().execute();
//		mapOverlays.add(locOverlay);
	}

	//Get Facebook Locations nearby and their pictures	
	private void getFBPlacesNearby(Location location){
		String locat = location.getLatitude()+","+location.getLongitude();		
		Bundle params = new Bundle();
		params.putString("type", "place");
		params.putString("center", locat);
		params.putString("distance", "800");
		params.putString("fields", "location,name,id");
		String result = null;
		try {
			result = fb.request("search", params);
		} catch (MalformedURLException e3) {
			// TODO Auto-generated catch block
			e3.printStackTrace();
		} catch (IOException e3) {
			// TODO Auto-generated catch block
			e3.printStackTrace();
		}
		
		JSONArray locations = null;
		
		try {
			JSONObject data = new JSONObject(result);
			locations = data.getJSONArray("data");
			if (locations ==null){
				System.out.println(data.optJSONArray("data"));
			}
		} catch (JSONException e2) {
			e2.printStackTrace();
		}
		
		
		//Goes through array of results from the fb request and
		//creates and adds facebook location objects to an array.
		for (int i = 0; i < locations.length(); i++) {
			String Name = null;
			String Location = null;
			String id = null;
			Bitmap pict = null;
			try {
				JSONObject place = locations.getJSONObject(i);
				
				//Gets name of place
				Name = place.getString("name");
				
				//Get Lat and Lon from JSON Object
				JSONObject loc = place.getJSONObject("location");
				Location = 
					loc.getString("latitude")+","+loc.getString("longitude");
				
				//Gets picture of Place from FB
				id = place.getString("id");
				pict = 
//						BitmapFactory.decodeResource(getResources(), R.drawable.btmpme);
				getFBpic(id);
			} catch (JSONException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}			
			FBLocationObj FBobj = new FBLocationObj(Name, Location, pict);
			FbLocations.add(FBobj);
		}
	}

	
	//returns an image for the id given
	private Bitmap getFBpic(String id){
		Bitmap img = null;
		String uri = 
				"https://graph.facebook.com/"+id+"/picture?type=large";
			     HttpClient httpclient = new DefaultHttpClient();			     
			     HttpGet httpget = new HttpGet(uri);
			     HttpResponse response = null;				     
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
	
	public class ProximityIntentReceiver extends BroadcastReceiver {	

		@Override
		public void onReceive(Context context, Intent intent) {
			String Location = intent.getStringExtra("Location");
			String key = LocationManager.KEY_PROXIMITY_ENTERING;
			Boolean entering = intent.getBooleanExtra(key, false);			
			if(entering){
				new AddIdDynamo().execute(Location,UserId);
				Toast.makeText(getApplicationContext(), "im entering", Toast.LENGTH_LONG).show();
			}else {
				new RemoveIdDynamo().execute(Location,UserId);
				Toast.makeText(getApplicationContext(), "im exiting", Toast.LENGTH_LONG).show();
			}
						
			
		
		}
	}
	
	public void setProximityAlert(LocationManager locManger, double lat, double lon){
		float radius = 50f;
		long expiration = -1;
//		final long eventID;
//		int requestCode;
		String loc = lat+","+lon;
		Intent intent = new Intent(TREASURE_PROXIMITY_ALERT);
		intent.putExtra("Location", loc);
		PendingIntent pendingIntent = PendingIntent.getBroadcast(getApplicationContext(), -1, intent, PendingIntent.FLAG_CANCEL_CURRENT);
		
		locManger.addProximityAlert(lat, lon, radius, expiration, pendingIntent);		
	}
	
	public void getRegisteredLocation(){
		try {
			RegisteredVeneus =	new GetLocationsDynamo().execute().get();			
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ExecutionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
	public Integer getPeople(String Location){
		Integer count = null;		
		try {
			count = new GetNoOfPeopleDynamo().execute(Location).get();			
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ExecutionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return count;
	}
	
	private void registerIntents(LocationManager locManager) {
		for(int i = 0; i < RegisteredVeneus.size(); i++) {
			String locat = RegisteredVeneus.get(i);
			String[] loc = locat.split(",");
			double latit = Double.parseDouble(loc[0]);
			double longit = Double.parseDouble(loc[1]);
	    	setProximityAlert(locManager,latit,longit);
	    }
	}
	
	
	

	
	//Creates Facebook Location Object contain a Name its Location and picture
	private class FBLocationObj{
		private String Name;		
		private String Location;
		private Bitmap Img;
		public FBLocationObj(String _Name, String _Location, Bitmap _img){
			Name = _Name;
			Location = _Location;
			Img = _img;
		}
		
		//Getters for all fields
		public String getName() {
			return Name;
		}
		public String getLocation() {
			return Location;
		}
		
		public Bitmap getImg() {
			return Img;
		}
		
	}
	public void setRefreshActionButtonState(final boolean refreshing) {
	    if (optionsMenu != null) {
	        final MenuItem refreshItem = optionsMenu
	            .findItem(R.id.refreshermaps);
	        if (refreshItem != null) {
	            if (refreshing) {
	                refreshItem.setActionView(R.layout.refresher_progress);
	            } else {
	                refreshItem.setActionView(null);
	            }
	        }
	    }
	}
	
	private Bitmap addBorder(Bitmap bmp, int borderSize) {
	    Bitmap bmpWithBorder = Bitmap.createBitmap(bmp.getWidth() + borderSize * 2, bmp.getHeight() + borderSize * 2, bmp.getConfig());
	    Canvas canvas = new Canvas(bmpWithBorder);
	    canvas.drawColor(Color.LTGRAY);
	    canvas.drawBitmap(bmp, borderSize, borderSize, null);
	    return bmpWithBorder;
	}
	class myInfoWindowAdapter implements InfoWindowAdapter{
		 private final View myContentsView;
		  
		  myInfoWindowAdapter(){
		   myContentsView = getLayoutInflater().inflate(R.layout.custom_infowindow, null);
		  }

		@Override
		public View getInfoContents(Marker marker) {
			TextView VenTxt = (TextView)myContentsView.findViewById(R.id.VenueName);
            TextView populTxt = (TextView)myContentsView.findViewById(R.id.VenuePopulation);
            VenTxt.setText(marker.getTitle());
            populTxt.setText(marker.getSnippet());
			return myContentsView;
		}

		@Override
		public View getInfoWindow(Marker marker) {
			// TODO Auto-generated method stub
			return null;
		}
		
	}
	

	/**
	 * Calculate distance between two points in latitude and longitude taking
	 * into account height difference. If you are not interested in height
	 * difference pass 0.0. Uses Haversine method as its base.
	 * 
	 * @param lat1	latitude of first point 
	 * @param lat2	latitude of second point
	 * @param lon1	longitude of first point
	 * @param lon2	longitude of second point
	 * @param el1	height of first point
	 * @param el2	height of second poing
	 * @return
	 */
	private double distance(double lat1, double lat2, double lon1, double lon2,
	        double el1, double el2) {

	    final int R = 6371; // Radius of the earth

	    Double latDistance = deg2rad(lat2 - lat1);
	    Double lonDistance = deg2rad(lon2 - lon1);
	    Double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
	            + Math.cos(deg2rad(lat1)) * Math.cos(deg2rad(lat2))
	            * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
	    Double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
	    double distance = R * c * 1000; // convert to meters

	    double height = el1 - el2;
	    distance = Math.pow(distance, 2) + Math.pow(height, 2);
	    return Math.sqrt(distance);
	}

	private double deg2rad(double deg) {
	    return (deg * Math.PI / 180.0);
	}
	

}
