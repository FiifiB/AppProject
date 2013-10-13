package com.DatabseTasks;

import java.io.IOException;
import java.util.ArrayList;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.ParseException;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;

import com.aayfi.whrtigo.main.MapsActivity;
import com.google.gson.Gson;

import android.os.AsyncTask;
import android.widget.Toast;

public class GetLocationsTask extends AsyncTask<String, Void, ArrayList<String>> {

	@Override
	protected ArrayList<String> doInBackground(String... params) {
		String Latitude = params[0].trim();
		String Longitude = params[1].trim();
		String uri = 
				"http://192.168.1.130:8088/MongoDBServices/GetLocations?param=" +
				Latitude +	"&param=" +	Longitude;
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
					result = gson.fromJson(EntityUtils.toString(entity), ArrayList.class);
					
				} catch (ParseException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} return result;
		
	}

	

}
