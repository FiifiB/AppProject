package com.Topspot;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.Array;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import DynamoDBTasks.AddIdDynamo;
import DynamoDBTasks.GetLocationsDynamo;
import DynamoDBTasks.GetNoOfPeopleDynamo;
import DynamoDBTasks.RemoveIdDynamo;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.widget.Toast;

import com.AuthorizationAndStore.SharedPreferencesCredentialStore;
import com.example.topspot.R;
import com.facebook.android.AsyncFacebookRunner;
import com.facebook.android.AsyncFacebookRunner.RequestListener;
import com.facebook.android.Facebook;
import com.facebook.android.FacebookError;
import com.mapquest.android.maps.GeoPoint;
import com.mapquest.android.maps.MapActivity;
import com.mapquest.android.maps.MapController;
import com.mapquest.android.maps.MapView;
import com.mapquest.android.maps.MyLocationOverlay;
import com.mapquest.android.maps.Overlay;
import com.mapquest.android.maps.OverlayItem;


@SuppressWarnings("deprecation")
public class MapsActivity extends MapActivity {
	private Facebook fb;
	private String APP_ID;
	private AsyncFacebookRunner myAsyncRunner;
	private MapView mapview;
	private MapController mcontroller;
	private Location myLoc;
	private LocationManager locationManager;
	private static int time = 50000;
	private static int distance = 50;
	private Criteria criteria = new Criteria();
	private SharedPreferences prefs;
	private String UserId ;
	private List<Overlay> mapOverlays;
	private ArrayList<FBLocationObj> FbLocationObj= new ArrayList<FBLocationObj>();
	private VenuesOverlay locOverlay;
	private MyLocationOverlay myLocationOverlay;
	private List<String> RegisteredVeneus = new ArrayList<String>();
	private BroadcastReceiver proxiReceiver;
	private IntentFilter filter;
	private static final String TREASURE_PROXIMITY_ALERT = "com.topspot.action.proximityalert";
	private Bitmap pic = null;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_maps);
		this.prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
		new SharedPreferencesCredentialStore(prefs);
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
		if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)){
			
		
		
		//Settings for the map
		mapview = (MapView)findViewById(R.id.map);
		mapview.setBuiltInZoomControls(true);
		mcontroller = mapview.getController();
		mapOverlays = mapview.getOverlays();
		myLocationOverlay = new MyLocationOverlay(this, mapview);
		myLocationOverlay.enableCompass();
		myLocationOverlay.enableMyLocation();
		mapOverlays.add(myLocationOverlay);
		Drawable drawable = this.getResources().getDrawable(R.drawable.androidmarker);
		locOverlay = new VenuesOverlay(drawable,this);
		
		
		
		
		
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
		Double geoLat = myLoc.getLatitude()*1E6;
		Double geoLng = myLoc.getLongitude()*1E6;
		GeoPoint point = new GeoPoint(geoLat.intValue(), geoLng.intValue());
		mcontroller.animateTo(point);
		mcontroller.setZoom(18);
		
		//Set up for proximity alerts
		filter = new IntentFilter(TREASURE_PROXIMITY_ALERT);
		proxiReceiver = new ProximityIntentReceiver();
		registerReceiver(proxiReceiver, filter);		
		registerIntents(locationManager);
		
		//Launches thread to get Locations in the Database 
		getRegisteredLocation();
		
		getFacebookID();
		
		//populates the map with places near your location
		updateWithNewLocation(myLoc);

		
		
		
		}else if(!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)){
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
		getMenuInflater().inflate(R.menu.maps, menu);
		return true;
	}

	@Override
	protected boolean isRouteDisplayed() {
		// TODO Auto-generated method stub
		return false;
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
				Log.d("Profile", response);
	            String json = response;
	            try {
	                JSONObject profile = new JSONObject(json);
	                // getting id of the user
	                UserId = profile.getString("id");
	                
	 
	                runOnUiThread(new Runnable() {
	 
	                    @Override
	                    public void run() {
	                        Toast.makeText(getApplicationContext(), "User Id: " + UserId, Toast.LENGTH_LONG).show();
	                    }
	 
	                });
	 
	            } catch (JSONException e) {
	                e.printStackTrace();
	            }
				
			}
		});
	}
	
	//Gets Nearby Venues That have been registered with this app
	private class GetNearbyVenues extends AsyncTask<Void, Void, Void> {

		@Override
		protected Void doInBackground(Void... params) {
			getFBPlacesNearby(myLoc);
			return null;
		}
		
		@Override
		protected void onPostExecute(Void result) {
			for (String Rvenue: RegisteredVeneus){
				for (int i = 0; i < FbLocationObj.size(); i++){
					//Compares Lat and Lon in Database and FB Places to see if they match
					if (Rvenue.equals(FbLocationObj.get(i).getLocation())){
						String[] LatLon = Rvenue.split(",");
						
						//creates Geopoint to add location to the map
						GeoPoint p = 
						new GeoPoint(Double.parseDouble(LatLon[0])*1E6,Double.parseDouble(LatLon[1])*1E6);
						
						//gets the number of people in that Place
						Integer count;
						count = getPeople(FbLocationObj.get(i).getLocation());
						
						//create the item to add to the map
						String message = "No. of people at "+FbLocationObj.get(i).getName()+": "+count;
						OverlayItem item = new OverlayItem(p, FbLocationObj.get(i).getName(), message);
						Drawable place_img = new BitmapDrawable(FbLocationObj.get(i).getImg());
						item.setMarker(place_img);						
						locOverlay.addOverlay(item);
						
					}
				}
			}
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
	
	private void updateWithNewLocation (Location location){
		myLoc.getLatitude();
		myLoc.getLongitude();
		new GetNearbyVenues().execute();
		mapOverlays.add(locOverlay);
	}

	//Get Facebook Locations nearby and their pictures
	
	private void getFBPlacesNearby(Location loc){
		String locat = loc.getLatitude()+","+loc.getLongitude();		
		Bundle params = new Bundle();
		params.putString("type", "place");
		params.putString("center", locat);
		params.putString("distance", "800");
		params.putString("fields", "location,name,id");
		
		myAsyncRunner.request("search",params, new RequestListener() {
			
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
				String res = response;
				JSONArray locations = null;
				
				try {
					JSONObject data = new JSONObject(res);
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
//					Bitmap pic = null;
					String id = null;
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
					} catch (JSONException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}
					
					Bundle param = new Bundle();
					param.putString("type", "normal");
					
					//Gets Picture from FB place Id
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
							
							try {
								JSONObject locId = new JSONObject(response);
								JSONObject data = locId.getJSONObject("data");
								String url = data.getString("url");
								URL imgUrl = new URL(url);
								pic = BitmapFactory.decodeStream(imgUrl.openConnection().getInputStream());
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
					
					FBLocationObj FBobj = new FBLocationObj(Name, Location, pic);
					FbLocationObj.add(FBobj);
				}
				
			}
		});
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
			RegisteredVeneus = new GetLocationsDynamo().execute().get();			
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
	
	public void onPause(){
		if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)){
			super.onPause();
			unregisterReceiver(proxiReceiver);
			myLocationOverlay.disableCompass();
		    myLocationOverlay.disableMyLocation();
		}else {
			super.onPause();			
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
	
	protected void onResume() {
		if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)){
	      myLocationOverlay.enableMyLocation();
	      myLocationOverlay.enableCompass();
	      registerReceiver(proxiReceiver, filter);
	      super.onResume();
		}else {
			super.onResume();			
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

}
