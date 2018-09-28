import java.io.*;
import java.util.*;
import com.amazonaws.*;
import com.amazonaws.auth.*;
import com.amazonaws.auth.profile.*;
import com.amazonaws.regions.*;
import com.amazonaws.services.dynamodbv2.*;
import com.amazonaws.services.dynamodbv2.model.*;
import com.amazonaws.services.dynamodbv2.document.*;
import com.amazonaws.services.dynamodbv2.datamodeling.*;

public class TestDDB extends Thread
{
        public AmazonDynamoDBClient client;
	public String tableName;
	public int batchSize;
	
        public TestDDB()
        {
                client = new AmazonDynamoDBClient();
                client.configureRegion(Regions.AP_SOUTHEAST_2);
		try
		{
			Properties prop = new Properties();
			InputStream input = new FileInputStream("ddb.properties");
			prop.load(input);
			tableName = prop.getProperty("tableName");
		}catch (Exception e)
		{
			System.out.println(e.getMessage());
			e.printStackTrace();
		}
        }

	public void setBatchSize(int size)
	{
		batchSize = size;
	}

	public void put(String hash, int sort, String value)
	{
		HashMap<String, AttributeValue> item = new HashMap<String, AttributeValue>();
		item.put("hash", new AttributeValue(hash));
		item.put("sort", new AttributeValue().withN(Integer.toString(sort)));
		item.put("val", new AttributeValue(value));

		PutItemRequest putItemRequest = new PutItemRequest().withTableName(tableName).withItem(item);

		try 
		{
			client.putItem(putItemRequest);
		} catch (Exception e) 
		{
			System.out.println(e.getMessage());
			e.printStackTrace();
		}
	}

	public void run()
	{
		while (batchSize >= 0)
		{
			try 
			{
				String hash = UUID.randomUUID().toString();
				int sort = batchSize;
				String value = hash + "-" + sort;
				put(hash, sort, value);

				batchSize--;
			} catch (ConditionalCheckFailedException e) 
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
			int items   = Integer.parseInt(args[0]);
			int threads = Integer.parseInt(args[1]);
			int batch   = (int) (items / threads) + 1;
			TestDDB tests[] = new TestDDB [threads];
			for (int i=0; i<threads; i++)
			{
				tests[i] = new TestDDB();
				tests[i].setBatchSize(batch);
				tests[i].start();
			}

			for (int j=0; j<threads; j++)
			{
				tests[j].join();
			}
		} catch (Exception e) 
		{
			System.out.println(e.getMessage());
			e.printStackTrace();
		}
        }
}
