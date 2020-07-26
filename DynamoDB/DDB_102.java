import java.util.*;
import com.amazonaws.regions.*;
import com.amazonaws.services.dynamodbv2.*;
import com.amazonaws.services.dynamodbv2.model.*;
import com.amazonaws.services.dynamodbv2.document.*;
import com.amazonaws.services.dynamodbv2.datamodeling.*;

public class DDB_102 extends Thread
{
	public AmazonDynamoDBClient client;
	public String tableName = "java-demo";

	public DDB_102()
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
			int threads = Integer.parseInt(args[0]);
			DDB_102[] workers = new DDB_102[threads];
			for (int i=0; i<threads; i++)
			{
				workers[i] = new DDB_102();
				workers[i].start();
			}
		} catch (Exception e) 
		{
			System.out.println(e.getMessage());
			e.printStackTrace();
		}
        }
}
