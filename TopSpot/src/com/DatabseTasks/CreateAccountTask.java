package com.DatabseTasks;

import java.io.IOException;

import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import android.os.AsyncTask;

public class CreateAccountTask extends AsyncTask<String, Void, String> {

	@Override
	protected String doInBackground(String... params) {
		String FName = params[0];
		String SName = params[1];
		String Username = params[2];
		String Password = params[3];
		String Email = params[4];
		String uri = 
				"http://192.168.1.64:8080/MongoDBServices/AddAccount?param=" +
				FName +	"&param=" +	SName + "&param=" +
				Username +	"&param=" +	Password + "&params=" +	Email;
			     HttpClient httpclient = new DefaultHttpClient();
			     HttpGet httpget = new HttpGet(uri);			    
			     String message = "Thank you for registering,"+ FName;
			     
			     try {
					httpclient.execute(httpget);
				} catch (ClientProtocolException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			     
		return message;
	}

}
