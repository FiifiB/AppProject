package com.aayfi.whrtigo;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
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
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
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
import android.graphics.Paint;
import android.graphics.PorterDuff.Mode;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
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
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.DatabseTasks.AddIdTask;
import com.DatabseTasks.GetLocationsTask;
import com.DatabseTasks.GetNoOfPeopleTask;
import com.DatabseTasks.RemoveIdTask;
import com.aayfi.whrtigo.R;
import com.aayfi.whrtigo.AuthorizationAndStore.SharedPreferencesCredentialStore;
import com.aayfi.whrtigo.GeoFencing.GeofenceRemover;
import com.aayfi.whrtigo.GeoFencing.GeofenceRequester;
import com.aayfi.whrtigo.GeoFencing.GeofenceUtils;
import com.aayfi.whrtigo.GeoFencing.GeofenceUtils.REMOVE_TYPE;
import com.aayfi.whrtigo.GeoFencing.GeofenceUtils.REQUEST_TYPE;
import com.aayfi.whrtigo.GeoFencing.SimpleGeofence;
import com.aayfi.whrtigo.GeoFencing.SimpleGeofenceStore;
import com.facebook.android.AsyncFacebookRunner;
import com.facebook.android.AsyncFacebookRunner.RequestListener;
import com.facebook.android.Facebook;
import com.facebook.android.FacebookError;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.location.Geofence;
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
import com.google.gson.Gson;
import com.quickblox.core.QBCallback;
import com.quickblox.core.QBSettings;
import com.quickblox.core.result.Result;
import com.quickblox.internal.core.helper.StringifyArrayList;
import com.quickblox.module.auth.QBAuth;
import com.quickblox.module.users.QBUsers;
import com.quickblox.module.users.model.QBUser;
import com.quickblox.module.users.result.QBUserResult;

@SuppressWarnings("deprecation")
public class MapsActivity extends Activity implements QBCallback {
	private Facebook fb;
	private String APP_ID;
	private AsyncFacebookRunner myAsyncRunner;
	private GoogleMap myGmap;
	private static Location myLoc;
	private LocationManager locationManager;
	private static int time = 50000;
	private static int distance = 50;
	private Criteria criteria = new Criteria();
	private SharedPreferences prefs;
	private String UserId ;
	private String UserFName;
	private String UserLName;
	private List<Overlay> mapOverlays;
	private ArrayList<FBLocationObj> FbLocations= new ArrayList<FBLocationObj>();
	private VenuesOverlay locOverlay;
	private MyLocationOverlay myLocationOverlay;
	private ArrayList<String> RegisteredVenues = new ArrayList<String>();
	private Boolean FbLoggedIn;
	private Menu optionsMenu;
	private BroadcastReceiver proxiReceiver;
	private IntentFilter filter;
	private static final String TREASURE_PROXIMITY_ALERT = "com.topspot.action.proximityalert";
	private QBUser QBUser;
	private final static int CONNECTION_FAILURE_RESOLUTION_REQUEST = 9000;
	
	 /*
     * Use to set an expiration time for a geofence. After this amount
     * of time Location Services will stop tracking the geofence.
     * Remember to unregister a geofence when you're finished with it.
     * Otherwise, your app will use up battery. To continue monitoring
     * a geofence indefinitely, set the expiration time to
     * Geofence#NEVER_EXPIRE.
     */
    private static final long GEOFENCE_EXPIRATION_IN_HOURS = 12;
    private static final long GEOFENCE_EXPIRATION_IN_MILLISECONDS =
            GEOFENCE_EXPIRATION_IN_HOURS * DateUtils.HOUR_IN_MILLIS;

    // Store the current request
    private REQUEST_TYPE mRequestType;

    // Store the current type of removal
    private REMOVE_TYPE mRemoveType;

    // Persistent storage for geofences
    private SimpleGeofenceStore mPrefs;

    // Store a list of geofences to add
    List<Geofence> mCurrentGeofences;

    // Add geofences handler
    private GeofenceRequester mGeofenceRequester;
    // Remove geofences handler
    private GeofenceRemover mGeofenceRemover;
    
    /*
     * An instance of an inner class that receives broadcasts from listeners and from the
     * IntentService that receives geofence transition events
     */
    private GeofenceSampleReceiver mBroadcastReceiver;

    // An intent filter for the broadcast receiver
    private IntentFilter mIntentFilter;

    // Store the list of geofences to remove
    private List<String> mGeofenceIdsToRemove;
	
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		// Create a new broadcast receiver to receive updates from the listeners and service
        mBroadcastReceiver = new GeofenceSampleReceiver();

        // Create an intent filter for the broadcast receiver
        mIntentFilter = new IntentFilter();

        // Action for broadcast Intents that report successful addition of geofences
        mIntentFilter.addAction(GeofenceUtils.ACTION_GEOFENCES_ADDED);

        // Action for broadcast Intents that report successful removal of geofences
        mIntentFilter.addAction(GeofenceUtils.ACTION_GEOFENCES_REMOVED);

        // Action for broadcast Intents containing various types of geofencing errors
        mIntentFilter.addAction(GeofenceUtils.ACTION_GEOFENCE_ERROR);

        // All Location Services sample apps use this category
        mIntentFilter.addCategory(GeofenceUtils.CATEGORY_LOCATION_SERVICES);

        // Instantiate a new geofence storage area
        mPrefs = new SimpleGeofenceStore(this);

        // Instantiate the current List of geofences
        mCurrentGeofences = new ArrayList<Geofence>();

        // Instantiate a Geofence requester
        mGeofenceRequester = new GeofenceRequester(this);

        // Instantiate a Geofence remover
        mGeofenceRemover = new GeofenceRemover(this);
		
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
		
		
		FbLoggedIn = getIntent().getExtras().getBoolean("already fb logged");
		
		if (access_token != null && expires != 0 ){
			fb.setAccessToken(access_token);
			fb.setAccessExpires(expires);
			if (FbLoggedIn != null && FbLoggedIn == true){
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
			}
//			getFacebookID();
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
//		filter = new IntentFilter(TREASURE_PROXIMITY_ALERT);
//		proxiReceiver = new ProximityIntentReceiver();
//		registerReceiver(proxiReceiver, filter);		
//		registerIntents(locationManager);
		
		//Launches thread to get Locations in the Database 
//		getRegisteredLocation();
		
		getFacebookID();
		
		//populates the map with places near your location
		updateWithNewLocation(myLoc);

		myGmap.setInfoWindowAdapter(new myInfoWindowAdapter());
		
	}
	
	
    /*
     * Handle results returned to this Activity by other Activities started with
     * startActivityForResult(). In particular, the method onConnectionFailed() in
     * GeofenceRemover and GeofenceRequester may call startResolutionForResult() to
     * start an Activity that handles Google Play services problems. The result of this
     * call returns here, to onActivityResult.
     * calls
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        // Choose what to do based on the request code
        switch (requestCode) {

            // If the request code matches the code sent in onConnectionFailed
            case GeofenceUtils.CONNECTION_FAILURE_RESOLUTION_REQUEST :

                switch (resultCode) {
                    // If Google Play services resolved the problem
                    case Activity.RESULT_OK:

                        // If the request was to add geofences
                        if (GeofenceUtils.REQUEST_TYPE.ADD == mRequestType) {

                            // Toggle the request flag and send a new request
                            mGeofenceRequester.setInProgressFlag(false);

                            // Restart the process of adding the current geofences
                            mGeofenceRequester.addGeofences(mCurrentGeofences);

                        // If the request was to remove geofences
                        } else if (GeofenceUtils.REQUEST_TYPE.REMOVE == mRequestType ){

                            // Toggle the removal flag and send a new removal request
                            mGeofenceRemover.setInProgressFlag(false);

                            // If the removal was by Intent
                            if (GeofenceUtils.REMOVE_TYPE.INTENT == mRemoveType) {

                                // Restart the removal of all geofences for the PendingIntent
                                mGeofenceRemover.removeGeofencesByIntent(
                                    mGeofenceRequester.getRequestPendingIntent());

                            // If the removal was by a List of geofence IDs
                            } else {

                                // Restart the removal of the geofence list
                                mGeofenceRemover.removeGeofencesById(mGeofenceIdsToRemove);
                            }
                        }
                    break;

                    // If any other result was returned by Google Play services
                    default:

                        // Report that Google Play services was unable to resolve the problem.
                        Log.d(GeofenceUtils.APPTAG, getString(R.string.no_resolution));
                }

            // If any other request code was received
            default:
               // Report that this Activity received an unknown requestCode
               Log.d(GeofenceUtils.APPTAG,
                       getString(R.string.unknown_activity_request_code, requestCode));

               break;
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
	            intent.putExtra("myLat", myLoc.getLatitude());
	            intent.putExtra("myLat", myLoc.getLongitude());
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
	
	
	/**
	 * Checks that the app has gps and network connectivity
	 */
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
	
	
	/**
	 * Gets The user's facebook ID
	 */
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
	
	//Gets Nearby Venues That have been registered with this app
	private class GetNearbyVenues extends AsyncTask<Void, Void, Void> {
		
        @Override
        protected void onPreExecute() {
        	setRefreshActionButtonState(true);
        	LatLng latLng = new LatLng(myLoc.getLatitude(), myLoc.getLongitude());
    		myGmap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 13));
    		System.out.println("============"+myLoc.getLatitude()+"==="+myLoc.getLongitude());
    		
    		
        }
		
		@Override
		protected Void doInBackground(Void... params) {	
			RegisteredVenues.clear();
			getPlacesNearby(myLoc);
		if(RegisteredVenues.size()!= 0){
			for (int i = 0; i < RegisteredVenues.size(); i++){
				String res[] = RegisteredVenues.get(i).split(":");
				String ID = res[0].trim();
				String res2[] = res[1].split(";");
				String VenueName = res2[0];
				JSONArray LonLat = null;
				try {
					LonLat = new JSONArray(res2[1]);
				} catch (JSONException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
				//creates Google LatLng to add location to the map
				LatLng p = null;
				try {
					p = new LatLng(LonLat.getDouble(0),LonLat.getDouble(1));
				} catch (JSONException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
				String Location = null;
				try {
					Location = Double.toString(LonLat.getDouble(0))+","+Double.toString(LonLat.getDouble(1));
				} catch (JSONException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				//gets the number of people in that Place
				Integer count;
				count = getpeeps(Location);
				
				//create the item to add to the map
				String message = "Population at "+VenueName+": "+count;
				final MarkerOptions item = new MarkerOptions();
				item.position(p);
				item.title(VenueName);
				item.snippet(message);
				
				boolean imgYes = false;
				Bitmap venImg = null;
				venImg = getFBpic(ID);
				
				if (venImg != null){
					imgYes = true;
					item.icon(BitmapDescriptorFactory.fromBitmap(addBorder(venImg, 10)));
				}
				runOnUiThread(new  Runnable() {
					public void run() {
						myGmap.addMarker(item);
						
					}
				});
				

			}	
		}
			
			return null;
		}
		
		@Override
		protected void onPostExecute(Void result) {
			
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

	//Get Database Locations nearby 	
	private void getPlacesNearby(Location location){
		String uri = 
				"http://ec2-54-226-63-49.compute-1.amazonaws.com:8080/MongoDBServices/GetLocations?param=" +
				Double.toString(location.getLatitude()) +	"&param=" +	Double.toString(location.getLongitude());
			     HttpClient httpclient = new DefaultHttpClient();			     
			     HttpGet httpget = new HttpGet(uri);
			     HttpResponse response = null;
			     ArrayList<String> result = null;
				try {
					response = httpclient.execute(httpget);
				} catch (ClientProtocolException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				HttpEntity entity = null;
				
//			     System.out.println(response.getStatusLine().toString());
			    entity = response.getEntity();
			     
			     Gson gson = new Gson();
			     try {
					RegisteredVenues = gson.fromJson(EntityUtils.toString(entity), ArrayList.class);
					
				} catch (ParseException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} ;
	}

	
	//returns an image for the id given
	private Bitmap getFBpic (String Id){
		final IMgStore imgstore = new IMgStore();
		
				final String uri = 
						"https://graph.facebook.com/"+Id+"/picture?type=large";
				
						HttpClient httpclient = new DefaultHttpClient();			     
					     HttpGet httpget = new HttpGet(uri);
					     HttpResponse response = null;
					     HttpEntity entity;
						try {
							response = httpclient.execute(httpget);
							entity = response.getEntity();
							imgstore.setImg(BitmapFactory.decodeStream(entity.getContent()));
						} catch (ClientProtocolException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
				return imgstore.getImg();
			
	}
	
	public class ProximityIntentReceiver extends BroadcastReceiver {	

		@Override
		public void onReceive(Context context, Intent intent) {
			String Location = intent.getStringExtra("Location");
			String key = LocationManager.KEY_PROXIMITY_ENTERING;
			Boolean entering = intent.getBooleanExtra(key, false);			
			if(entering){
				new AddIdTask().execute(Location,UserId);
				Toast.makeText(getApplicationContext(), "im entering", Toast.LENGTH_LONG).show();
			}else {
				new RemoveIdTask().execute(Location,UserId);
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
	
	public Integer getPeople(String Location){
		Integer count = null;		
		try {
			count = new GetNoOfPeopleTask().execute(Location).get();
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
		for(int i = 0; i < RegisteredVenues.size(); i++) {
			String res[] = RegisteredVenues.get(i).split(":");
			String res2[] = res[1].split(";");			
			JSONArray LatLon = null;
			double latit = 0;
			double longit = 0;
			try {
				LatLon = new JSONArray(res2[1]);
				latit = LatLon.getDouble(0);
				longit = LatLon.getDouble(1);
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			
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
	private class IMgStore{
		private Bitmap Img;

		public Bitmap getImg() {
			return Img;
		}

		public void setImg(Bitmap img) {
			Img = img;
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
//	    canvas.drawColor(Color.LTGRAY);
	    

        int color = 0xff424242;
        Paint paint = new Paint();
        Rect rect = new Rect(0, 0, bmpWithBorder.getWidth(), bmpWithBorder.getHeight());
        RectF rectF = new RectF(rect);
        float roundPx = 25;
        paint.setAntiAlias(true);
        canvas.drawARGB(0, 0, 0, 0);
        paint.setColor(Color.parseColor("#c4c2c7"));
        canvas.drawRoundRect(rectF, roundPx, roundPx, paint);

        paint.setXfermode(new PorterDuffXfermode(Mode.SRC_IN));
	    
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
//            populTxt.setText("Population at "+marker.getTitle()+": " +getPeople(marker.getPosition().latitude+","+marker.getPosition().longitude));
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
	private Integer getpeeps(String venue){
		String uri = 
				"http://ec2-54-226-63-49.compute-1.amazonaws.com:8080/MongoDBServices/NoOfPeople?param="+venue;
			     HttpClient httpclient = new DefaultHttpClient();			     
			     HttpGet httpget = new HttpGet(uri);
			     HttpResponse response = null;
			     int result = 0;
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
					result = gson.fromJson(EntityUtils.toString(entity), Integer.class);
					
				} catch (ParseException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} return result;
		
		
	}
	
	public static Location getUserCurrentLoc(){
		return myLoc;
	}
	
private void signInOrSignUptoQB() {
		
		QBUser = new QBUser("00"+UserId+"11", UserId+UserLName);
		Integer integ = new Integer(UserId);							
		QBUser.setId(integ);
		QBUser.setFacebookId(UserId);
		QBUser.setFullName(UserFName + ""+ UserLName);
		StringifyArrayList<String> tags = new StringifyArrayList<String>();
		tags.add("AppUser");
		QBUser.setTags(tags);	
		
		// ================= QuickBlox ===== Step 3 =================
        // Register user or sign in QuickBlox. 
		QBUsers.signUp(QBUser, MapsActivity.this);
		
		
	}

@Override
public void onComplete(Result result) {
	if (result.isSuccess()) {
        QBUserResult qbUserResult = (QBUserResult) result;
        Log.d("Registration was successful","user: " + qbUserResult.getUser().toString());
    } else {
//    	List<String> error = null;
//    	error.add("login has already been taken");							        	
//    	if (result.getErrors().equals(error)){
    		
    		// ================= QuickBlox ===== Step 3 =================
            // Login user into QuickBlox.
            // Pass this activity , because it implements QBCallback interface.
            // Callback result will come into onComplete method below.
    		QBUsers.signIn(QBUser, MapsActivity.this);
    	
        Log.e("Errors",result.getErrors().toString()); 
	
			}
}

@Override
public void onComplete(Result arg0, Object arg1) {
	// TODO Auto-generated method stub
	
}
//====================================================================================================================
//========================================GeoFencing Code ============================================================
//====================================================================================================================

	

	/**
	 * Called when the app needs to add a geofence to the list of simple geofences
	 * Get the geofence parameters for each geofence and add them to
	 * a List. 
	 */
	public void onRegisterGeofence(View view, String ID, String mLatitude, String mLongitude, String mRadius) {
		
		SimpleGeofence mUIGeofence;
	
	    /*
	     * Record the request as an ADD. If a connection error occurs,
	     * the app can automatically restart the add request if Google Play services
	     * can fix the error
	     */
	    mRequestType = GeofenceUtils.REQUEST_TYPE.ADD;
	
	    /*
	     * Check for Google Play services. Do this after
	     * setting the request type. If connecting to Google Play services
	     * fails, onActivityResult is eventually called, and it needs to
	     * know what type of request was in progress.
	     */
	    if (!servicesConnected()) {
	
	        return;
	    }
	
//	    /*
//	     * Check that the input fields have values and that the values are with the
//	     * permitted range
//	     */
//	    if (!checkInputFields()) {
//	        return;
//	    }
	
	    /*
	     * Create a version of geofence 1 that is "flattened" into individual fields. This
	     * allows it to be stored in SharedPreferences.
	     */
	    mUIGeofence = new SimpleGeofence(
	        ID,
	        // Get latitude, longitude, and radius from the UI
	        Double.valueOf(mLatitude),
	        Double.valueOf(mLongitude),
	        Float.valueOf(mRadius),
	        // Set the expiration time
	        GEOFENCE_EXPIRATION_IN_MILLISECONDS,
	        // Only detect entry transitions
	        Geofence.GEOFENCE_TRANSITION_ENTER);
	
	    // Store this flat version in SharedPreferences
	    mPrefs.setGeofence(ID, mUIGeofence);
	
	    /*
	     * Add Geofence objects to a List. toGeofence()
	     * creates a Location Services Geofence object from a
	     * flat object
	     */
	    mCurrentGeofences.add(mUIGeofence.toGeofence());
	
	    
	}
	
	
	/**
	 * Starts Geofence Requests from list of simple Geofences.
	 * Create the PendingIntent containing an Intent that
	 * Location Services sends to this app's broadcast receiver when
	 * Location Services detects a geofence transition. Send the List
	 * and the PendingIntent to Location Services.
	 */
	public void RequestGeofences(){
		
		// Start the request. Fail if there's already a request in progress
	    try {
	        // Try to add geofences
	        mGeofenceRequester.addGeofences(mCurrentGeofences);
	    } catch (UnsupportedOperationException e) {
	        // Notify user that previous request hasn't finished.
	        Toast.makeText(this, R.string.add_geofences_already_requested_error,
	                    Toast.LENGTH_LONG).show();
	    }	
	}
	
	
	/**
	 * Verify that Google Play services is available before making a request.
	 *
	 * @return true if Google Play services is available, otherwise false
	 */
	private boolean servicesConnected() {
	
	    // Check that Google Play services is available
	    int resultCode =
	            GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
	
	    // If Google Play services is available
	    if (ConnectionResult.SUCCESS == resultCode) {
	
	        // In debug mode, log the status
	        Log.d(GeofenceUtils.APPTAG, getString(R.string.play_services_available));
	
	        // Continue
	        return true;
	
	    // Google Play services was not available for some reason
	    } else {
	
	        // Display an error dialog
	        Dialog dialog = GooglePlayServicesUtil.getErrorDialog(resultCode, this, 0);
	        if (dialog != null) {
	            ErrorDialogFragment errorFragment = new ErrorDialogFragment();
	            errorFragment.setDialog(dialog);
	            errorFragment.show(getFragmentManager(), GeofenceUtils.APPTAG);
	        }
	        return false;
	    }
	}
	
	
	  /**
     * Called when the app needs to remove all geofences on the app
     *
     * @param view The view that triggered this callback
     */
    public void onUnregisterByPendingIntentClicked(View view) {
        /*
         * Remove all geofences set by this app. To do this, get the
         * PendingIntent that was added when the geofences were added
         * and use it as an argument to removeGeofences(). The removal
         * happens asynchronously; Location Services calls
         * onRemoveGeofencesByPendingIntentResult() (implemented in
         * the current Activity) when the removal is done
         */

        /*
         * Record the removal as remove by Intent. If a connection error occurs,
         * the app can automatically restart the removal if Google Play services
         * can fix the error
         */
        // Record the type of removal
        mRemoveType = GeofenceUtils.REMOVE_TYPE.INTENT;

        /*
         * Check for Google Play services. Do this after
         * setting the request type. If connecting to Google Play services
         * fails, onActivityResult is eventually called, and it needs to
         * know what type of request was in progress.
         */
        if (!servicesConnected()) {

            return;
        }

        // Try to make a removal request
        try {
        /*
         * Remove the geofences represented by the currently-active PendingIntent. If the
         * PendingIntent was removed for some reason, re-create it; since it's always
         * created with FLAG_UPDATE_CURRENT, an identical PendingIntent is always created.
         */
        mGeofenceRemover.removeGeofencesByIntent(mGeofenceRequester.getRequestPendingIntent());

        } catch (UnsupportedOperationException e) {
            // Notify user that previous request hasn't finished.
            Toast.makeText(this, R.string.remove_geofences_already_requested_error,
                        Toast.LENGTH_LONG).show();
        }

    }
    
    
    /**
     * Called when the user wants to hide themselves from being showed in a specified
     * location (identified by the ID) which removes that geofence
     * @param view The view that triggered this callback
     * @param GeofenceID The geofence ID of the venue that needs to be unregistered
     */
    public void onUnregisterVenueGeofence(View view, String GeofenceID) {
        /*
         * Remove the geofence by creating a List of geofences to
         * remove and sending it to Location Services. The List
         * contains the id of the venue's geofence.
         * The removal happens asynchronously; Location Services calls
         * onRemoveGeofencesByPendingIntentResult() (implemented in
         * the current Activity) when the removal is done.
         */

        // Create a List of 1 Geofence with the ID "1" and store it in the global list
        mGeofenceIdsToRemove = Collections.singletonList(GeofenceID);

        /*
         * Record the removal as remove by list. If a connection error occurs,
         * the app can automatically restart the removal if Google Play services
         * can fix the error
         */
        mRemoveType = GeofenceUtils.REMOVE_TYPE.LIST;

        /*
         * Check for Google Play services. Do this after
         * setting the request type. If connecting to Google Play services
         * fails, onActivityResult is eventually called, and it needs to
         * know what type of request was in progress.
         */
        if (!servicesConnected()) {

            return;
        }

        // Try to remove the geofence
        try {
            mGeofenceRemover.removeGeofencesById(mGeofenceIdsToRemove);

        // Catch errors with the provided geofence IDs
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        } catch (UnsupportedOperationException e) {
            // Notify user that previous request hasn't finished.
            Toast.makeText(this, R.string.remove_geofences_already_requested_error,
                        Toast.LENGTH_LONG).show();
        }
    }
    

	/**
	 * Define a DialogFragment to display the error dialog generated in
	 * showErrorDialog.
	 */
	public static class ErrorDialogFragment extends DialogFragment {
	
	    // Global field to contain the error dialog
	    private Dialog mDialog;
	
	    /**
	     * Default constructor. Sets the dialog field to null
	     */
	    public ErrorDialogFragment() {
	        super();
	        mDialog = null;
	    }
	
	    /**
	     * Set the dialog to display
	     *
	     * @param dialog An error dialog
	     */
	    public void setDialog(Dialog dialog) {
	        mDialog = dialog;
	    }
	
	    /*
	     * This method must return a Dialog to the DialogFragment.
	     */
	    @Override
	    public Dialog onCreateDialog(Bundle savedInstanceState) {
	        return mDialog;
	    }
	}
	
	
    /**
     * Define a Broadcast receiver that receives updates from connection listeners and
     * the geofence transition service.
     */
    public class GeofenceSampleReceiver extends BroadcastReceiver {
        /*
         * Define the required method for broadcast receivers
         * This method is invoked when a broadcast Intent triggers the receiver
         */
        @Override
        public void onReceive(Context context, Intent intent) {

            // Check the action code and determine what to do
            String action = intent.getAction();

            // Intent contains information about errors in adding or removing geofences
            if (TextUtils.equals(action, GeofenceUtils.ACTION_GEOFENCE_ERROR)) {

                handleGeofenceError(context, intent);

            // Intent contains information about successful addition or removal of geofences
            } else if (
                    TextUtils.equals(action, GeofenceUtils.ACTION_GEOFENCES_ADDED)
                    ||
                    TextUtils.equals(action, GeofenceUtils.ACTION_GEOFENCES_REMOVED)) {

                handleGeofenceStatus(context, intent);

            // Intent contains information about a geofence transition
            } else if (TextUtils.equals(action, GeofenceUtils.ACTION_GEOFENCE_TRANSITION)) {

                handleGeofenceTransition(context, intent);

            // The Intent contained an invalid action
            } else {
                Log.e(GeofenceUtils.APPTAG, getString(R.string.invalid_action_detail, action));
                Toast.makeText(context, R.string.invalid_action, Toast.LENGTH_LONG).show();
            }
        }

        /**
         * If you want to display a UI message about adding or removing geofences, put it here.
         *
         * @param context A Context for this component
         * @param intent The received broadcast Intent
         */
        private void handleGeofenceStatus(Context context, Intent intent) {

        }

        /**
         * Report geofence transitions to the UI
         *
         * @param context A Context for this component
         * @param intent The Intent containing the transition
         */
        private void handleGeofenceTransition(Context context, Intent intent) {
            /*
             * If you want to change the UI when a transition occurs, put the code
             * here. The current design of the app uses a notification to inform the
             * user that a transition has occurred.
             */
        }

        /**
         * Report addition or removal errors to the UI, using a Toast
         *
         * @param intent A broadcast Intent sent by ReceiveTransitionsIntentService
         */
        private void handleGeofenceError(Context context, Intent intent) {
            String msg = intent.getStringExtra(GeofenceUtils.EXTRA_GEOFENCE_STATUS);
            Log.e(GeofenceUtils.APPTAG, msg);
            Toast.makeText(context, msg, Toast.LENGTH_LONG).show();
        }
    }

}
