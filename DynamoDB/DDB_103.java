import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import com.amazonaws.regions.*;
import com.amazonaws.services.dynamodbv2.*;
import com.amazonaws.services.dynamodbv2.model.*;
import com.amazonaws.services.dynamodbv2.document.*;
import com.amazonaws.services.dynamodbv2.datamodeling.*;

public class DDB_103 extends Thread
{
        public AmazonDynamoDBClient client;
	public String tableName = "java-demo";
	public AtomicInteger counter;

        public DDB_103(AtomicInteger counter)
        {
		this.counter = counter;
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
				counter.incrementAndGet();
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
			AtomicInteger counter = new AtomicInteger();
			PrintCounter pc = new PrintCounter(counter);
			pc.start();

			DDB_103[] workers = new DDB_103[threads];
			for (int i=0; i<threads; i++)
			{
				workers[i] = new DDB_103(counter);
				workers[i].start();
			}
		} catch (Exception e) 
		{
			System.out.println(e.getMessage());
			e.printStackTrace();
		}
        }
}
