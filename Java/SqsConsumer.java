import java.io.*;
import java.util.*;
import java.math.*;
import com.amazonaws.*;
import com.amazonaws.auth.*;
import com.amazonaws.auth.profile.*;
import com.amazonaws.regions.*;
import com.amazonaws.services.sqs.*;
import com.amazonaws.services.sqs.model.*;
import org.json.simple.*;
import org.json.simple.parser.*;

public class SqsConsumer
{
        public AmazonSQSClient client;
	public String queueUrl;

        public SqsConsumer()
        {
                client = new AmazonSQSClient();
                client.configureRegion(Regions.AP_SOUTHEAST_2);

		try
		{
			Properties prop = new Properties();
			InputStream input = new FileInputStream("sqs.properties");
			prop.load(input);
			queueUrl = prop.getProperty("queueUrl");
		}catch (Exception e)
		{
			System.out.println(e.getMessage());
			e.printStackTrace();
		}
        }

	public void receive(int time)
	{
		Set<String> attrs = new HashSet<String>();
		attrs.add("ApproximateNumberOfMessages");

		while (true)
		{
			try
			{
				GetQueueAttributesRequest request = new GetQueueAttributesRequest(queueUrl).withAttributeNames(attrs);
				Map<String,String> response = client.getQueueAttributes(request).getAttributes();
				int count = Integer.parseInt(response.get("ApproximateNumberOfMessages"));
				if (count !=0)
				{
					System.out.println("\n");
					System.out.println("Approximate Number of Messages in SQS: " + count);
					System.out.println("\n");
				}

				ReceiveMessageResult result = client.receiveMessage(queueUrl);
				for (Message message : result.getMessages())
				{
					System.out.println(message.getMessageId() + "\t" + message.getBody());
					
					JSONParser parser = new JSONParser();
					Object body = parser.parse(message.getBody());
					JSONObject jsonObj = (JSONObject) body;
					JSONArray records = (JSONArray) jsonObj.get("Records");
					Iterator i = records.iterator();
					while (i.hasNext())
					{
						JSONObject record = (JSONObject) i.next();
						JSONObject s3 = (JSONObject) record.get("s3");
						JSONObject object = (JSONObject) s3.get("object");
						String key = (String) object.get("key");
						System.out.println(key);

						// Do you image conversion here
					}

					client.deleteMessage(queueUrl, message.getReceiptHandle());
				}
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
			SqsConsumer sc = new SqsConsumer();
			int time = Integer.parseInt(args[0]);
			sc.receive(time);
		} catch (Exception e)
		{
			System.out.println(e.getMessage());
			e.printStackTrace();
		}

        }
}
