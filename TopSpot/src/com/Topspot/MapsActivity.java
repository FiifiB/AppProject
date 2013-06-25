package com.Topspot;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

import DynamoDBTasks.AddIdDynamo;
import DynamoDBTasks.GetLocationsDynamo;
import DynamoDBTasks.GetNoOfPeopleDynamo;
import DynamoDBTasks.RemoveIdDynamo;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.widget.Toast;

import com.AuthorizationAndStore.CredentialStore;
import com.AuthorizationAndStore.OAuth2ClientCredentials;
import com.AuthorizationAndStore.SharedPreferencesCredentialStore;
import com.example.topspot.R;
import com.google.api.client.auth.oauth2.draft10.AccessTokenResponse;
import com.mapquest.android.maps.GeoPoint;
import com.mapquest.android.maps.MapActivity;
import com.mapquest.android.maps.MapController;
import com.mapquest.android.maps.MapView;
import com.mapquest.android.maps.MyLocationOverlay;
import com.mapquest.android.maps.Overlay;
import com.mapquest.android.maps.OverlayItem;

import fi.foyt.foursquare.api.FoursquareApi;
import fi.foyt.foursquare.api.FoursquareApiException;
import fi.foyt.foursquare.api.Result;
import fi.foyt.foursquare.api.entities.CompactVenue;
import fi.foyt.foursquare.api.entities.VenuesSearchResult;
import fi.foyt.foursquare.api.io.DefaultIOHandler;

public class MapsActivity extends MapActivity {
	private MapView mapview;
	private MapController mcontroller;
	private Location myLoc;
	private LocationManager locationManager;
	private static int time = 50000;
	private static int distance = 50;
	private Criteria criteria = new Criteria();
	private FoursquareApi foursquareApi;
	private SharedPreferences prefs;
	private CredentialStore credentialStore;
	private String UserId ;
	private double lat;
	private double lng;
	private List<Overlay> mapOverlays;
	private VenuesOverlay locOverlay;
	private MyLocationOverlay myLocationOverlay;
	private CompactVenue[] compactVenues ;
	private List<String> RegisteredVeneus = new ArrayList<String>();
	private BroadcastReceiver proxiReceiver;
	private IntentFilter filter;
	private static final String TREASURE_PROXIMITY_ALERT = "com.topspot.action.proximityalert";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_maps);
		this.prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
		this.credentialStore = new SharedPreferencesCredentialStore(prefs);
		AccessTokenResponse accessTokenResponse = credentialStore.read();
		
		System.out.println(accessTokenResponse.refreshToken);
		
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
		
		//get the location manageer
		locationManager = (LocationManager)getSystemService(Context.LOCATION_SERVICE);
		
		//Sets Criteria to get the best location provider		
		criteria.setAccuracy(Criteria.ACCURACY_COARSE);
		criteria.setPowerRequirement(Criteria.POWER_LOW);
		criteria.setAltitudeRequired(false);
		criteria.setBearingRequired(false);
		criteria.setSpeedRequired(false);
		criteria.setCostAllowed(true);
		String bestProvider = locationManager.getBestProvider(criteria, true);
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
		
		getRegisteredLocation();		
		updateWithNewLocation(myLoc);
		new getUserId().execute();
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
				UserId = getFoursquareApi().user("self").getResult().getId();
				System.out.println("my UserId: "+UserId);
			} catch (FoursquareApiException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			return null;
		}
		
	}
	
	
	//Gets Nearby Venues That have been registered with this app
	private class GetNearbyVenues extends AsyncTask<Uri, Void, Void> {

		@Override
		protected Void doInBackground(Uri... params) {

			try {				
				Log.i(Constants.TAG, "Retrieving places at " + lat + "," + lng);
				Result<VenuesSearchResult> venues = getFoursquareApi().venuesSearch(
						lat + "," + lng, null, null, null, null, null, null,
						null, null, null, null);				
				if (venues.getMeta().getCode()==200){
				compactVenues = venues.getResult().getVenues();
				System.out.println("success compact venue");
				Log.i(Constants.TAG, "found " + compactVenues.length
						+ " places");					
				
				
				}else{System.out.println("cant retrieve venues" + venues.getMeta().getCode() + venues.getMeta().getErrorDetail());}				
				
				
			} catch (Exception ex) {
				Log.e(Constants.TAG, "Error retrieving venues", ex);
			}
			return null;
		}
		
		@Override
		protected void onPostExecute(Void result) {
			for (CompactVenue compactVenue : compactVenues) {					
				
				for(String s: RegisteredVeneus){
					String[]locat = s.split(",");
					if ((compactVenue.getLocation().getLat().toString()).contentEquals(locat[0]) && (compactVenue.getLocation().getLng().toString()).contentEquals(locat[1])){							
						Double lat =compactVenue.getLocation().getLat()*1E6;
						Double lng =compactVenue.getLocation().getLng()*1E6;
						GeoPoint p = new GeoPoint(lat.intValue(), lng.intValue());
						double lclat = compactVenue.getLocation().getLat();
						double lclng = compactVenue.getLocation().getLng();
						String Location = lclat+","+lclng;
						Integer count;
						count = getPeople(Location);
						String message = "No. of people at "+compactVenue.getName()+": "+count;
						OverlayItem item = new OverlayItem(p, compactVenue.getName(), message);
						locOverlay.addOverlay(item);
					}
				}									
			}
		}

	}
	
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
			
		}
		
		@Override
		public void onLocationChanged(Location location) {
			//Update app based on new location
			updateWithNewLocation(location);
		}
	};
	
	private void updateWithNewLocation (Location location){
		lat = myLoc.getLatitude();
		lng = myLoc.getLongitude();
		new GetNearbyVenues().execute();
		mapOverlays.add(locOverlay);
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
		super.onPause();
		unregisterReceiver(proxiReceiver);
		myLocationOverlay.disableCompass();
	    myLocationOverlay.disableMyLocation();
		
	}
	
	protected void onResume() {
	      myLocationOverlay.enableMyLocation();
	      myLocationOverlay.enableCompass();
	      registerReceiver(proxiReceiver, filter);
	      super.onResume();
	    }

}
