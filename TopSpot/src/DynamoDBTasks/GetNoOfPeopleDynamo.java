package DynamoDBTasks;

import java.io.IOException;
import java.util.Map;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.PropertiesCredentials;
import com.amazonaws.services.dynamodb.AmazonDynamoDBClient;
import com.amazonaws.services.dynamodb.model.AttributeValue;
import com.amazonaws.services.dynamodb.model.GetItemRequest;
import com.amazonaws.services.dynamodb.model.GetItemResult;
import com.amazonaws.services.dynamodb.model.Key;

import android.os.AsyncTask;

public class GetNoOfPeopleDynamo extends AsyncTask<String, Void, Integer> {
	private static AmazonDynamoDBClient client;
	private static String tableName = "Venues";
	private static int people;
	
	private static Integer printItem(Map<String, AttributeValue> attributeList) {
		int res = 0;
        for (Map.Entry<String, AttributeValue> item : attributeList.entrySet()) {
            String attributeName = item.getKey();
            AttributeValue value = item.getValue();
            res = Integer.parseInt(value.getN());
            System.out.println(attributeName + " "
                    + (value.getS() == null ? "" : "S=[" + value.getS() + "]")
                    + (value.getN() == null ? "" : "N=[" + value.getN() + "]")	                   
                    + (value.getSS() == null ? "" : "SS=[" + value.getSS() + "]")
                    + (value.getNS() == null ? "" : "NS=[" + value.getNS() + "]\n"));
                    
        }
        return res;
    }
	
	 private static void retrieveItem(String Location) {
	        try {
	            
	            Key key = new Key(new AttributeValue().withS(Location));				
				GetItemRequest getItemRequest = new GetItemRequest()
	                .withTableName(tableName)
	                .withKey(key)	                
	                .withAttributesToGet("NoOfPeople");
	            
	            GetItemResult result = client.getItem(getItemRequest);
	            people = printItem(result.getItem());
	 

//	            // Check the response.
//	            System.out.println("Printing item after retrieving it....");
//	            printItem(result.getItem());            
	                        
	        }  catch (AmazonServiceException ase) {	                    
						System.err.println("Failed to retrieve item in " + tableName);
	        }   

	    }

	@Override
	protected Integer doInBackground(String... params) {
		String venue = params[0];
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
			
			retrieveItem(venue);
			
			client.shutdown();
		return people;
	}

	

}
