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

/**
 *
 * A thread to put test items into DynamoDB table. The thread gets the table name and AWS region
 * from a configuration file ddb.properties.
 *
 * The table has a hash key (hash, S) and a sort key (sort, N).
 *
 */
 
public class TestDDB extends Thread
{
	public AmazonDynamoDBClient client;
	public String tableName, region;
	public int batchSize;

	public TestDDB(int batchSize)
	{
		this.batchSize = batchSize;
		try
		{
			Properties prop = new Properties();
			InputStream input = new FileInputStream("ddb.properties");
			prop.load(input);
			tableName = prop.getProperty("tableName");
			region = prop.getProperty("region"); 
			client = new AmazonDynamoDBClient();
			client.configureRegion(Regions.fromName(region));
		}catch (Exception e)
		{
			System.out.println(e.getMessage());
			e.printStackTrace();
		}
	}

	/**
	 *
	 * put() method
	 *
	 * The put() method actually performs a PutItem API call to DynamoDB.
	 * 
	 * @param hash		the hash key for the item
	 * @param sort		the range key for the item
	 * @param value		an additional attribute in the item
	 *
	 */

	public void put(String hash, int sort, String value)
	{
		Random r = new Random();
		HashMap<String, AttributeValue> item = new HashMap<String, AttributeValue>();
		item.put("hash", new AttributeValue(hash));
		item.put("sort", new AttributeValue().withN(Integer.toString(sort)));
		item.put("val", new AttributeValue(value));
		item.put("random", new AttributeValue().withN(Integer.toString(r.nextInt(Integer.MAX_VALUE))));
		// Need TTL to allow the item to expired after 60 seconds.
		long ttl = System.currentTimeMillis()/1000L + 60L;
		item.put("ttl", new AttributeValue().withN(Long.toString(ttl)));
		// Need year to do some further handling
		int year = 1990 + r.nextInt(20);
		item.put("year", new AttributeValue().withN("" + year));

		// Create a PutItemRequest and makes the PutItem API call
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

	/**
	 *
	 * run() method for the thread
	 *
	 * The run method calls the put() method, until the batch is finished.
	 *
	 */
	 
	public void run()
	{
		while (batchSize>0)
		{
			Random r = new Random();
			try 
			{
				String hash = UUID.randomUUID().toString();
				int sort = r.nextInt(Integer.MAX_VALUE);
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

	/**
	 * 
	 * main() method
	 * 
	 * The main method launches multiple threads to put a certain number of items into the 
	 * DynamoDB table.
	 *
	 */
	public static void main(String[] args)
	{
		try 
		{
			// By default, we launch 1 thread only and write 1 item only
			int batch = 1;
			int threads = 1;
			
			// Let's see if the user specifies any runtime parameters
			try
			{
				// The 1st argument specifies the number of items per thread.
				batch   = Integer.parseInt(args[0]);
				// The 2nd argument specifies the number of threads.
				threads = Integer.parseInt(args[1]);				
			} catch (Exception e1)
			{
				// We don't do any exception handling here. It is OK that the 
				// command line parameter contains junk, because we have 
				// already defined the default behavior.
			}

			// Create the threads and launch them
			TestDDB tests[] = new TestDDB [threads];
			for (int i=0; i<threads; i++)
			{
				tests[i] = new TestDDB(batch);
				tests[i].start();
			}
			// Wait for the threads to finish execution
			for (int j=0; j<threads; j++)
			{
				tests[j].join();
			}
		} catch (Exception e) 
		{
			// Exception handling
			System.out.println(e.getMessage());
			e.printStackTrace();
		}
	}
}
