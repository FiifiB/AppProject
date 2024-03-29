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
 * This thread adds the user's ID to the venue they are in and increments the count of number of people in the venue
 * @author Fiifi
 *
 */
public class AddIdDynamo extends AsyncTask<String, Void, String> {
	private static AmazonDynamoDBClient client;
	private static String tableName = "Venues";
	private static String VenueName;
	
	/**
	 * The method is used to get the name of the venue needed to store id in
	 * @param attributeList
	 * This Map contains information about an item in the DataBase.
	 */
	private static void getNameItem(Map<String, AttributeValue> attributeList) {
        for (Map.Entry<String, AttributeValue> item : attributeList.entrySet()) {
            String attributeName = item.getKey();
            AttributeValue value = item.getValue();
            VenueName = item.getValue().getS();            
        }
    }
	
	/**
	 * Gets name of the venue with the specified coordinates
	 * @param Loc
	 * The coordinates of the selected location
	 * @return
	 * The name of the location
	 */
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
	
	/**
	 * This method inputs the users id into a specified location
	 * @param Location
	 * @param UserID
	 */
    private static void updateMultipleAttributes(String Location, String UserID) {
        try {
            Map<String, AttributeValueUpdate> updateItems = 
                new HashMap<String, AttributeValueUpdate>();

            Key key = new Key(new AttributeValue().withS(Location));		
            
            // Adds Persons Id to list in venue
            updateItems.put("People", 
                    new AttributeValueUpdate()
                        .withAction(AttributeAction.ADD)
                        .withValue(new AttributeValue().withSS(UserID+":"+VenueName)));
            // Increases count of people in venue by one.
            updateItems.put("NoOfPeople", 
                    new AttributeValueUpdate()
            			.withAction(AttributeAction.ADD)	            
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
			//gets credentials needed to access the Database
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
