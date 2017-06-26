import java.net.*;
import java.io.*;
import java.util.*;
import org.apache.commons.io.*;
import com.amazonaws.*;
import com.amazonaws.auth.*;
import com.amazonaws.auth.profile.*;
import com.amazonaws.regions.*;
import com.amazonaws.services.ec2.*;
import com.amazonaws.services.ec2.model.*;

public class HomeworkEC2_01
{

	public AmazonEC2Client client;

	public HomeworkEC2_01()
	{
		client = new AmazonEC2Client();
                client.configureRegion(Regions.AP_SOUTHEAST_2);
	}

	public String getInstanceId()
	{
		String instanceId = null;

		try
		{
			String metadata = "http://169.254.169.254/latest/meta-data/instance-id";
			URL url = new URL(metadata);
			URLConnection con = url.openConnection();
			InputStream in = con.getInputStream();
			String encoding = con.getContentEncoding();
			encoding = encoding == null ? "UTF-8" : encoding;
			instanceId = IOUtils.toString(in, encoding);
			System.out.println(instanceId);
		} catch (Exception e)
		{
			System.out.println(e.getMessage());
			e.printStackTrace();
		}

		return instanceId;
	}

	public String createAMI(String instanceId)
	{
		String amiId = null;
		try
		{
			String imageName = UUID.randomUUID().toString();
			CreateImageRequest request = new CreateImageRequest(instanceId, imageName);
			request.setNoReboot(true);
			CreateImageResult  result  = client.createImage(request);
			amiId = result.getImageId();
			System.out.println(amiId);
		} catch (Exception e)
                {
                        System.out.println(e.getMessage());
                        e.printStackTrace();
                }

		return amiId;
	}

	public void waitForAMI(String amiId)
	{
                try
                {
			DescribeImagesRequest request = new DescribeImagesRequest();
			LinkedList<String> ids = new LinkedList<String>();
			ids.add(amiId);
			request.setImageIds(ids);

			boolean ready = false;
			while (!ready)
			{
				DescribeImagesResult result = client.describeImages(request);
				List<Image> images = result.getImages();
				Image image = images.get(0);
				String state = image.getState();
				if (state.equals("available"))
				{
					ready = true;
				}
				else
				{
					System.out.println("Image not ready yet, waiting...");
					Thread.sleep(15000);
				}
			}

			System.out.println("Image is now ready.");
                } catch (Exception e)
                {
                        System.out.println(e.getMessage());
                        e.printStackTrace();
                }
	}

	public String launchInstance(String amiId)
	{
                String instanceId = null;
                try
                {
			RunInstancesRequest request = new RunInstancesRequest();
			request.withImageId(amiId)
				.withInstanceType("m3.medium")
				.withMinCount(1)
				.withMaxCount(1)
				.withKeyName("desktop")
				.withSecurityGroups("default");
			RunInstancesResult result = client.runInstances(request);
			Reservation reservation = result.getReservation();
			List<Instance> instances = reservation.getInstances();
			Instance instance = instances.get(0);

			instanceId = instance.getInstanceId();
			System.out.println("New instance id is " + instanceId);
                } catch (Exception e)
                {
                        System.out.println(e.getMessage());
                        e.printStackTrace();
               	}

                return instanceId;
	}

	public void waitForInstance(String instanceId)
	{
               try
                {
                        DescribeInstancesRequest request = new DescribeInstancesRequest();
                        LinkedList<String> ids = new LinkedList<String>();
                        ids.add(instanceId);
                        request.setInstanceIds(ids);

                       	boolean ready = false;
                        while (!ready)
                        {
                               	DescribeInstancesResult result = client.describeInstances(request);
                                List<Reservation> reservations = result.getReservations();
				Reservation reservation = reservations.get(0);
				List<Instance> instances = reservation.getInstances();
                               	Instance instance = instances.get(0);
				InstanceState state = instance.getState();
				Integer code = state.getCode();
				 
                                if (code.equals(new Integer(16)))
                                {
                                       	ready = true;
                               	}
                               	else
                                {
                                        System.out.println("Instance not ready yet, waiting...");
                                       	Thread.sleep(15000);
                                }
                        }

                        System.out.println("Instance is now ready.");
                } catch (Exception e)
                {
                        System.out.println(e.getMessage());
                        e.printStackTrace();
                }
	}

	public void terminateInstance(String instanceId)
	{
		try
		{
			TerminateInstancesRequest request = new TerminateInstancesRequest();
			LinkedList<String> ids = new LinkedList<String>();
			ids.add(instanceId);
			request.setInstanceIds(ids);
			TerminateInstancesResult  result  = client.terminateInstances(request);

			List<InstanceStateChange> changes = result.getTerminatingInstances();
			InstanceStateChange change = changes.get(0);
			String previous = change.getPreviousState().getName();
			String current  = change.getCurrentState().getName();

			System.out.println(instanceId + "\t" + previous + "\t" +current);
		} catch (Exception e)
		{
			System.out.println(e.getMessage());
			e.printStackTrace();
		}
	}


	public static void main(String[] args)
	{
		HomeworkEC2_01 demo = new HomeworkEC2_01();
		String oldInstanceId = demo.getInstanceId();
		String amiId = demo.createAMI(oldInstanceId);
		demo.waitForAMI(amiId);
		String newInstanceId = demo.launchInstance(amiId);
		demo.waitForInstance(newInstanceId);
		demo.terminateInstance(newInstanceId);
	}
}
