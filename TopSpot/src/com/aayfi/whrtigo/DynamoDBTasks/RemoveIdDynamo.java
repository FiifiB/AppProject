package com.aayfi.whrtigo.DynamoDBTasks;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.PropertiesCredentials;
import com.amazonaws.services.dynamodb.AmazonDynamoDBClient;
import com.amazonaws.services.dynamodb.model.AttributeAction;
import com.amazonaws.services.dynamodb.model.AttributeValue;
import com.amazonaws.services.dynamodb.model.AttributeValueUpdate;
import com.amazonaws.services.dynamodb.model.GetItemRequest;
import com.amazonaws.services.dynamodb.model.GetItemResult;
import com.amazonaws.services.dynamodb.model.Key;
import com.amazonaws.services.dynamodb.model.ReturnValue;
import com.amazonaws.services.dynamodb.model.UpdateItemRequest;
import com.amazonaws.services.dynamodb.model.UpdateItemResult;

import android.os.AsyncTask;

/**
 * This thread removes the user's ID from the venue they were in and decreases the number count of people in the venue
 * @author Fiifi
 *
 */
public class RemoveIdDynamo extends AsyncTask<String, Void, String>{
	
	private static AmazonDynamoDBClient client;
	private static String tableName = "Venues";
	private static String VenueName;
	
	
	private static void getNameItem(Map<String, AttributeValue> attributeList) {
        for (Map.Entry<String, AttributeValue> item : attributeList.entrySet()) {
            String attributeName = item.getKey();
            AttributeValue value = item.getValue();
            VenueName = item.getValue().getS();
            System.out.println(VenueName);
//            System.out.println(attributeName + " "
//                    + (value.getS() == null ? "" : "S=[" + value.getS() + "]")
//                    + (value.getN() == null ? "" : "N=[" + value.getN() + "]")	                   
//                    + (value.getSS() == null ? "" : "SS=[" + value.getSS() + "]")
//                    + (value.getNS() == null ? "" : "NS=[" + value.getNS() + "]\n"));
                    
        }
    }
	
	private String getName(String Loc){
		 try {
	            
	            Key key = new Key(new AttributeValue().withS(Loc));				
				GetItemRequest getItemRequest = new GetItemRequest()
	                .withTableName(tableName)
	                .withKey(key)	                
	                .withAttributesToGet("Name");
	            
	            GetItemResult result = client.getItem(getItemRequest);

	            // Gets the name of the location from the lat and long
	            
	            getNameItem(result.getItem());            
	                        
	        }  catch (AmazonServiceException ase) {	                    
						System.err.println("Failed to retrieve item in " + tableName);
	        }   
		
		return VenueName;		
	}
	
    private static void updateMultipleAttributes(String Location, String UserID) {
        try {
            Map<String, AttributeValueUpdate> updateItems = 
                new HashMap<String, AttributeValueUpdate>();

            Key key = new Key(new AttributeValue().withS(Location));		
            
            // Adds Persons Id to list in venue
            updateItems.put("People", 
                    new AttributeValueUpdate()
                        .withAction(AttributeAction.DELETE)
                        .withValue(new AttributeValue().withSS(UserID+":"+VenueName)));
            // Increases count of people in venue by one.
            updateItems.put("NoOfPeople", 
                    new AttributeValueUpdate()
            			.withAction(AttributeAction.DELETE)	            
                        .withValue(new AttributeValue().withN("1")));
            
            ReturnValue returnValues = ReturnValue.ALL_NEW;
            
            UpdateItemRequest updateItemRequest = new UpdateItemRequest()
                .withTableName(tableName)
                .withKey(key)
                .withAttributeUpdates(updateItems)
                .withReturnValues(returnValues);
            
            UpdateItemResult result = client.updateItem(updateItemRequest);
            
//            // Check the response.
//            System.out.println("Printing item after multiple attribute update...");
//            printItem(result.getAttributes());            
                            
        }   catch (AmazonServiceException ase) {
                    System.err.println("Failed to update multiple attributes in " + tableName);
        }
    }

	@Override
	protected String doInBackground(String... params) {
		String venue = params[0];
		String userId = params[1];
		AWSCredentials credentials = null;
		getName(venue);
		try {
			credentials = new PropertiesCredentials(
			        AddIdDynamo.class.getResourceAsStream("AwsCredentials.properties"));
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

			client = new AmazonDynamoDBClient(credentials);	
			client.setEndpoint("dynamodb.us-east-1.amazonaws.com");
			
			updateMultipleAttributes(venue, userId);
			
			client.shutdown();
		return null;
	}



	

}
