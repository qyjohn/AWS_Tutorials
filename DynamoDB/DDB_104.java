import java.io.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.ConcurrentLinkedQueue;
import com.amazonaws.regions.*;
import com.amazonaws.services.dynamodbv2.*;
import com.amazonaws.services.dynamodbv2.model.*;
import com.amazonaws.services.dynamodbv2.document.*;
import com.amazonaws.services.dynamodbv2.datamodeling.*;

public class DDB_104 extends Thread
{
	public AmazonDynamoDBClient client;
	public String tableName = "java-demo";
	public AtomicInteger counter;
	public ConcurrentLinkedQueue<String> queue;

	public DDB_104(AtomicInteger counter, ConcurrentLinkedQueue<String> queue)
	{
		this.counter = counter;
		this.queue = queue;
		client = new AmazonDynamoDBClient();
		client.configureRegion(Regions.AP_SOUTHEAST_2);
	}

	public void run()
	{
		String line = queue.poll();
		while (line != null)
		{
			try 
			{
				String data[] = line.split(",");
				HashMap<String, AttributeValue> item = new HashMap<String, AttributeValue>();
				item.put("hash", new AttributeValue(data[0]));
				item.put("sort", new AttributeValue().withN(data[1]));
				item.put("val", new AttributeValue(data[2]));
				PutItemRequest putItemRequest = new PutItemRequest().withTableName(tableName).withItem(item);
				client.putItem(putItemRequest);
				counter.incrementAndGet();
				line = queue.poll();
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
			String file = args[1];
			ConcurrentLinkedQueue<String> queue = new ConcurrentLinkedQueue<String>();
			BufferedReader reader = new BufferedReader(new FileReader(file));
			String line = reader.readLine();
			while (line != null) {
				queue.add(line);
				line = reader.readLine();
			}

                        int threads = Integer.parseInt(args[0]);
                        AtomicInteger counter = new AtomicInteger();
                        PrintCounter pc = new PrintCounter(counter);
                        pc.start();
			DDB_104[] workers = new DDB_104[threads];
			for (int i=0; i<threads; i++)
			{
				workers[i] = new DDB_104(counter, queue);
				workers[i].start();
			}
			for (int j=0; j<threads; j++)
			{
				workers[j].join();
			}
			System.exit(0);
		} catch (Exception e) 
		{
			System.out.println(e.getMessage());
			e.printStackTrace();
		}
	}
}