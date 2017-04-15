import java.io.*;
import java.net.*;
import java.util.*;
import com.amazonaws.*;
import com.amazonaws.auth.*;
import com.amazonaws.auth.profile.*;
import com.amazonaws.regions.*;
import com.amazonaws.services.sqs.*;
import com.amazonaws.services.sqs.model.*;

public class SqsProducer
{
        public AmazonSQSClient client;
	public String queueUrl;
	public String ip;

        public SqsProducer()
        {
                client = new AmazonSQSClient();
                client.configureRegion(Regions.AP_SOUTHEAST_2);
                client = new AmazonSQSClient();
                client.configureRegion(Regions.AP_SOUTHEAST_2);

		try
		{
			Properties prop = new Properties();
			InputStream input = new FileInputStream("sqs.properties");
			prop.load(input);
			queueUrl = prop.getProperty("queueUrl");

			ip = "" + InetAddress.getLocalHost().getHostAddress();
			System.out.println(ip);
		}catch (Exception e)
		{
			System.out.println(e.getMessage());
			e.printStackTrace();
		}
        }

	public void send(int sleep)
	{
		int start = 100000;
		while (true)
		{
			try
			{
				String msg = ip + "-" + start;
				client.sendMessage(queueUrl, msg);
				start++;
				Thread.sleep(sleep);
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
	                SqsProducer sp = new SqsProducer();
			int sleep = Integer.parseInt(args[0]);
			sp.send(sleep);
		} catch (Exception e)
		{
			System.out.println(e.getMessage());
			e.printStackTrace();
		}
        }
}
