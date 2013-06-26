package DynamoDBTasks;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.PropertiesCredentials;
import com.amazonaws.services.dynamodb.AmazonDynamoDBClient;
import com.amazonaws.services.dynamodb.model.AttributeValue;
import com.amazonaws.services.dynamodb.model.GetItemRequest;
import com.amazonaws.services.dynamodb.model.GetItemResult;
import com.amazonaws.services.dynamodb.model.Key;
import com.amazonaws.services.dynamodb.model.ScanRequest;
import com.amazonaws.services.dynamodb.model.ScanResult;

import android.os.AsyncTask;

public class GetUserIdsDynamo extends AsyncTask<Void, Void, ArrayList<String>>{
	private static AmazonDynamoDBClient client;
	private static String tableName = "Venues";
	private ArrayList<String> UserId = new ArrayList<String>();
	
	private void printItem(Map<String, AttributeValue> attributeList) {
		
		UserId.clear();
        for (Map.Entry<String, AttributeValue> item : attributeList.entrySet()) {
            String attributeName = item.getKey();
            AttributeValue value = item.getValue();
            List<String> temp = value.getSS();            
            for(String i: temp){
            	UserId.add(i);
            }                    
        }        
    }
	

	@Override
	protected ArrayList<String> doInBackground(Void... params) {
		AWSCredentials credentials = null;
		try {
			credentials = new PropertiesCredentials(
			        GetUserIdsDynamo.class.getResourceAsStream("AwsCredentials.properties"));
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

			client = new AmazonDynamoDBClient(credentials);	
			client.setEndpoint("dynamodb.us-east-1.amazonaws.com");
		
			//A scan request to get all userIDs in every location. 
			//Each userID comes with the name of the location the userID is in
			ScanRequest scanRequest = new ScanRequest()
		    .withTableName("Venues").withAttributesToGet(Arrays.asList("People"));
			

		ScanResult result = client.scan(scanRequest);
		for (Map<String, AttributeValue> item : result.getItems()){			
			//Prints Items to array of userIDs
		    printItem(item);
		}
		
			client.shutdown();
		return UserId;
	}



}
