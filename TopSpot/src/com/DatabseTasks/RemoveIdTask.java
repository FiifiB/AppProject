package com.DatabseTasks;

import java.io.IOException;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.ParseException;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;

import android.os.AsyncTask;

import com.google.gson.Gson;

public class RemoveIdTask extends AsyncTask<String, Void, String> {

	@Override
	protected String doInBackground(String... params) {
		String venue = params[0];
		String userId = params[1];
		String uri = 
				"http://192.168.1.64:8080/MongoDBServices/RemoveId?param="+venue+"&param="+userId;
			     HttpClient httpclient = new DefaultHttpClient();
			     
			     HttpGet httpget = new HttpGet(uri);
			     HttpResponse response = null;
			     String result = null;
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
					result = gson.fromJson(EntityUtils.toString(entity), String.class);
					
				} catch (ParseException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} return result;
	}
}
