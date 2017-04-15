import java.io.*;
import java.util.*;
import com.amazonaws.*;
import com.amazonaws.auth.*;
import com.amazonaws.auth.profile.*;
import com.amazonaws.regions.*;
import com.amazonaws.services.s3.*;
import com.amazonaws.services.s3.model.*;

public class TestS3
{
        public AmazonS3Client client;

        public TestS3()
        {
                client = new AmazonS3Client();
                client.configureRegion(Regions.AP_SOUTHEAST_2);
        }

        public void listBuckets()
        {
                try 
                {
			List<Bucket> buckets = client.listBuckets();
			for (Bucket bucket : buckets)
			{
				System.out.println(bucket.getName());
			}
                } catch (Exception e) 
                {
                        System.out.println(e.getMessage());
                        e.printStackTrace();
                }
        }

	public void listObjects(String bucket)
	{
                try 
                {
			ObjectListing listing = client.listObjects(bucket);
			List<S3ObjectSummary> objects = listing.getObjectSummaries();
			for (S3ObjectSummary object : objects)
			{
				System.out.println(object.getKey());
			}

			while (listing.isTruncated())
			{
				ListNextBatchOfObjectsRequest request = new ListNextBatchOfObjectsRequest(listing);
				listing = client.listNextBatchOfObjects(request);
				objects = listing.getObjectSummaries();
				for (S3ObjectSummary object : objects)
				{
					System.out.println(object.getKey());
				}
			}
                } catch (Exception e) 
                {
                        System.out.println(e.getMessage());
                        e.printStackTrace();
                }
	}


	public void putFileToS3(String filename, String bucket, String folder)
	{
                try 
                {
			File file = new File(filename);
			String key = folder + "/" + file.getName();
			client.putObject(bucket, key, file);

			ObjectListing listing = client.listObjects(bucket, folder);
			List<S3ObjectSummary> objects = listing.getObjectSummaries();
			for (S3ObjectSummary object : objects)
			{
				System.out.println(object.getKey());
			}
                } catch (Exception e) 
                {
                        System.out.println(e.getMessage());
                        e.printStackTrace();
                }
	}

	public void putManyObjectsToS3(String bucket, String folder, int count)
	{
                try 
                {
			for (int i=0; i<count; i++)
			{
				String uuid = UUID.randomUUID().toString();
				String key = folder + "/" + uuid;
				String content = i + "\n" + uuid;
				client.putObject(bucket, key, content);
			}
                } catch (Exception e) 
                {
                        System.out.println(e.getMessage());
                        e.printStackTrace();
                }	
	}

        public static void main(String[] args)
        {
                TestS3 test = new TestS3();
		test.putManyObjectsToS3(args[0], args[1], Integer.parseInt(args[2]));
        }
}
