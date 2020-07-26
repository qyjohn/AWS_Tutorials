import java.util.*;
import com.amazonaws.regions.*;
import com.amazonaws.services.dynamodbv2.*;
import com.amazonaws.services.dynamodbv2.model.*;
import com.amazonaws.services.dynamodbv2.document.*;
import com.amazonaws.services.dynamodbv2.datamodeling.*;

public class DDB_101
{
        public AmazonDynamoDBClient client;
	public String tableName = "java-demo";

        public DDB_101()
        {
                client = new AmazonDynamoDBClient();
                client.configureRegion(Regions.AP_SOUTHEAST_2);
        }

	public void run()
	{
		Random r = new Random();
		while (true)
		{
			try 
			{
				String hash = UUID.randomUUID().toString();
				int sort = r.nextInt(1000000);
				String value = hash + "-" + sort;

				HashMap<String, AttributeValue> item = new HashMap<String, AttributeValue>();
				item.put("hash", new AttributeValue(hash));
				item.put("sort", new AttributeValue().withN(Integer.toString(sort)));
				item.put("val", new AttributeValue(value));
				PutItemRequest putItemRequest = new PutItemRequest().withTableName(tableName).withItem(item);
	                        client.putItem(putItemRequest);
			} catch (Exception e) 
			{
				System.out.println(e.getMessage());
				e.printStackTrace();
			}
		}
	}

        public static void main(String[] args)
        {
		try 
		{
			DDB_101 test = new DDB_101();
			test.run();
		} catch (Exception e) 
		{
			System.out.println(e.getMessage());
			e.printStackTrace();
		}
        }
}
