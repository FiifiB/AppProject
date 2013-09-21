package com.aayfi.whrtigo.DynamoDBTasks;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.PropertiesCredentials;
import com.amazonaws.services.dynamodb.AmazonDynamoDBClient;
import com.amazonaws.services.dynamodb.model.AttributeValue;
import com.amazonaws.services.dynamodb.model.ScanRequest;
import com.amazonaws.services.dynamodb.model.ScanResult;

import android.os.AsyncTask;

/**
 * This thread gets all locations in the Database
 * @author Fiifi
 *
 */
public class GetLocationsDynamo extends AsyncTask<Void, Void, ArrayList<String>>{
	private static AmazonDynamoDBClient client;
	private static String tableName = "Venues";
	private ArrayList<String> loc = new ArrayList<String>();

	@Override
	protected ArrayList<String> doInBackground(Void... params) {
		AWSCredentials credentials = null;
		try {
			credentials = new PropertiesCredentials(
			        AddIdDynamo.class.getResourceAsStream("AwsCredentials.properties"));
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

			client = new AmazonDynamoDBClient(credentials);	
			client.setEndpoint("dynamodb.us-east-1.amazonaws.com");
			
			ScanRequest scanRequest = new ScanRequest()
		    .withTableName(tableName).withAttributesToGet(Arrays.asList("Location","ID"));

		ScanResult result = client.scan(scanRequest);
		for (Map<String, AttributeValue> item : result.getItems()){
			 for (Map.Entry<String, AttributeValue> items : item.entrySet()) {
		            String attributeName = items.getKey();
		            AttributeValue value = items.getValue();
		            loc.add(value.getS());
		            System.out.println(attributeName + " "
		                    + (value.getS() == null ? "" : "S=[" + value.getS() + "]")
		                    + (value.getN() == null ? "" : "N=[" + value.getN() + "]")	                   
		                    + (value.getSS() == null ? "" : "SS=[" + value.getSS() + "]")
		                    + (value.getNS() == null ? "" : "NS=[" + value.getNS() + "]\n"));
		                    
		        }
		}
			
			client.shutdown();
		return loc;
	}



}
