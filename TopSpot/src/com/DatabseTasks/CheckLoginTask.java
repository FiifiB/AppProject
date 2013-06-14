package com.DatabseTasks;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.ParseException;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;

import com.google.gson.Gson;

import android.app.AlertDialog;
import android.os.AsyncTask;

public class CheckLoginTask extends AsyncTask<String, Integer, Boolean> {
	
	

	@Override
	protected Boolean doInBackground(String... params) {
		String Username = params[0];
		String Password = params[1];
		String uri = 
				"http://192.168.1.64:8080/MongoDBServices/CheckLogin?param=" +
				Username +	"&param=" +	Password;
			     HttpClient httpclient = new DefaultHttpClient();			     
			     HttpGet httpget = new HttpGet(uri);
			     HttpResponse response = null;
			     Boolean result = null;
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
					result = gson.fromJson(EntityUtils.toString(entity), Boolean.class);
					
				} catch (ParseException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} return result;
		
	}

}
