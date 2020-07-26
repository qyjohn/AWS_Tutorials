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
 * The table has a hash key (hash, S) and a sort key (sort, N). The TTL attribute is (ttl, N).
 *
 * How many threads do I need to force partition splits?
 *
 * Assuming an average of 8 ms round-trip latency, each thread is able to do 125 PutItem in
 * a second. Therefore 8 threads is needed to consume 1000 WCU from a partition, and 80 
 * threads are needed to saturate the capacity of 10 partitions. To ensure that partition
 * split is happening, we use 128 threads to make sure that we are exceeding the capacity
 * of the hot partitions. 
 * 
 * Run this with 128 threads x 600000 items in each thread. This will run for approximately 
 * 2 hours. Run this against a newly created on-demand table. This will drive the continous
 * split of the hot partitions. Since we define 10 hashKeys, we will have 10 hot partitions 
 * for split. If you need more partitions to split at the same time, increase the number of
 * hash keys to use. When doing this, you need to increase the number of threads as well to
 * drive enough traffic to the table.
 *
 * java ForcePartitionSplit 600000 128
 *
 * Keep the program running for about an hour. In the test I saw the consumed WCU first 
 * capped at around 3000+, then 6000+, then 12000+, then 20000+, then dropped to 19000+. At 
 * this point, the number of active parittions became stablized at 56. 
 *
 * With only 10 keys, we can drive up to 20000 WCU. That means each partition can actually 
 * handle 2000 WCU?
 *
 * After running the program for an hour, it seemed that 128 threads are not enough to 
 * drive further partition splits. So, I ran it with 256 threads to give some more pressure
 * on the table. 
 *
 * java ForcePartitionSplit 600000 256
 * 
 * Run the program for another hour, I saw the consumed WCU being capped at 23000, without
 * any change in the number of partitions. More interestingly, there was no write throttling 
 * this one hour. I consistently achieved 23000 WCU with 10 hot keys. That is, each hot key 
 * can achieve 2300 WCU.
 *
 * At this point I noticed that I had about 20% sys usage in top. Upgrading the EC2 instance
 * from c3.2xlarge to c3.8xlarge. Run the test program with 512 threads.
 *
 * java ForcePartitionSplit 600000 512
 *
 * Now I reached 40000 WCU, with some throttling, and saw further partition splits. The 
 * throttling told me that I was reaching table / account level limit. So I double the table
 * and account limit to 80000. Now I am able to achieve 70000 WCU consistently, with both 
 * system errors and throttling. 
 *
 * Stop the traffic for about 10 minutes, run the same test program again. Now I was able
 * to reach 80000 WCU, with both system errors and throttling. At this point, I stopped
 * the test. At the end of the test, there were 212 active partitions in the table. 
 *
 * It is good to know that we can achiving 80000 WCU from a single machine, with simple
 * PutItem API calls. 
 *
 * After the test program was stopped, observe the "TTL deleted items" in CloudWatch. In the
 * tests, we set the items to expired after 60 seconds. Several hours after the test, we 
 * still see expired items being deleted. The peak of item expiry wa about 400,000 items 
 * per minute, not sure how this is calculated. Anyway, the documentation does not have
 * any service level agreement on immediate deletion, but says "TTL typically deletes expired 
 * items within 48 hours of expiration."
 * 
 * During the writes we consumed 80000 WCU for about an hour. Now we have 400000 items being
 * deleted per minute. 80000 x 3600 / (40000 / 60) = 432432 seconds = 120 hours. Will need
 * to monitor the "TTL deleted items" metric for the next two days to see what happens.
 *
 */
 
public class ForcePartitionSplit extends Thread
{
	public AmazonDynamoDBClient client;
	public String tableName, region;
	public int batchSize;

	// Define 10 keys to work on. This will create 10 hot partitions to split.
	public String[] hashKeys = {
		"II8ATBT5U6DUCIHGNSV1LNQTPRVV4KQNSO5AEMVJF66Q9ASUAA01",
		"II8ATBT5U6DUCIHGNSV1LNQTPRVV4KQNSO5AEMVJF66Q9ASUAA02",
		"II8ATBT5U6DUCIHGNSV1LNQTPRVV4KQNSO5AEMVJF66Q9ASUAA03",
		"II8ATBT5U6DUCIHGNSV1LNQTPRVV4KQNSO5AEMVJF66Q9ASUAA04",
		"II8ATBT5U6DUCIHGNSV1LNQTPRVV4KQNSO5AEMVJF66Q9ASUAA05",
		"II8ATBT5U6DUCIHGNSV1LNQTPRVV4KQNSO5AEMVJF66Q9ASUAA06",
		"II8ATBT5U6DUCIHGNSV1LNQTPRVV4KQNSO5AEMVJF66Q9ASUAA07",
		"II8ATBT5U6DUCIHGNSV1LNQTPRVV4KQNSO5AEMVJF66Q9ASUAA08",
		"II8ATBT5U6DUCIHGNSV1LNQTPRVV4KQNSO5AEMVJF66Q9ASUAA09",
		"II8ATBT5U6DUCIHGNSV1LNQTPRVV4KQNSO5AEMVJF66Q9ASUAA10"};
	public int totalKeys = hashKeys.length;

	public ForcePartitionSplit(int batchSize)
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
				// Randomly pick a hash key to use
				String hash = hashKeys[r.nextInt(totalKeys)];
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
			ForcePartitionSplit workers[] = new ForcePartitionSplit [threads];
			for (int i=0; i<threads; i++)
			{
				workers[i] = new ForcePartitionSplit(batch);
				workers[i].start();
			}
			// Wait for the threads to finish execution
			for (int j=0; j<threads; j++)
			{
				workers[j].join();
			}
		} catch (Exception e) 
		{
			// Exception handling
			System.out.println(e.getMessage());
			e.printStackTrace();
		}
	}
}
