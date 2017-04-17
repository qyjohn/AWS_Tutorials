/*

This log import utility assumes that you have a MySQL table with the following schema to 
store the CloudTrail logs. During the log import process, remove all index from your table
to improve bulk import.

mysql> describe logs;
+--------------------+-------------+------+-----+---------+-------+
| Field              | Type        | Null | Key | Default | Extra |
+--------------------+-------------+------+-----+---------+-------+
| eventVersion       | varchar(10) | YES  |     | NULL    |       |
| userIdentity       | text        | YES  |     | NULL    |       |
| eventTime          | varchar(24) | YES  |     | NULL    |       |
| eventSource        | varchar(80) | YES  |     | NULL    |       |
| eventName          | varchar(80) | YES  |     | NULL    |       |
| awsRegion          | varchar(20) | YES  |     | NULL    |       |
| sourceIPAddress    | varchar(40) | YES  |     | NULL    |       |
| userAgent          | text        | YES  |     | NULL    |       |
| errorCode          | varchar(80) | YES  |     | NULL    |       |
| errorMessage       | text        | YES  |     | NULL    |       |
| requestParameters  | text        | YES  |     | NULL    |       |
| responseElements   | longtext    | YES  |     | NULL    |       |
| requestID          | varchar(80) | YES  |     | NULL    |       |
| eventID            | varchar(80) | YES  |     | NULL    |       |
| eventType          | varchar(24) | YES  |     | NULL    |       |
| recipientAccountId | varchar(12) | YES  |     | NULL    |       |
+--------------------+-------------+------+-----+---------+-------+
16 rows in set (1.59 sec)
*/



import java.io.*;
import java.sql.*;
import java.util.*;
import java.util.zip.*;
import java.util.concurrent.*;
import java.math.*;
import org.json.simple.*;
import org.json.simple.parser.*;
import com.amazonaws.regions.*;
import com.amazonaws.services.s3.*;
import com.amazonaws.services.s3.model.*;


public class ImportCloudTrailLogs extends Thread
{
	// JDBC connection to MySQL database
	public Connection conn = null;
	// S3 related stuff
	public boolean is_s3 = false;
	public AmazonS3Client s3_client;
	public String s3_region, s3_bucket;
	// A list of jobs to process
	ConcurrentLinkedQueue<String> jobs;

	/**
	 *
	 * Constructor
	 *
	 */

	public ImportCloudTrailLogs()
	{
		try
		{
			// Getting database properties from db.properties
			Properties prop = new Properties();
			InputStream input = new FileInputStream("db.properties");
			prop.load(input);
			String db_hostname = prop.getProperty("db_hostname");
			String db_username = prop.getProperty("db_username");
			String db_password = prop.getProperty("db_password");
			String db_database = prop.getProperty("db_database");

			// Load the MySQL JDBC driver
			Class.forName("com.mysql.jdbc.Driver");
			String jdbc_url = "jdbc:mysql://" + db_hostname + "/" + db_database + "?user=" + db_username + "&password=" + db_password;
			// Create a connection using the JDBC driver
			conn = DriverManager.getConnection(jdbc_url);
		} catch (Exception e)
		{
			System.out.println(e.getMessage());
			e.printStackTrace();
		}
	}

	/**
	 *
	 * Job definitions, including S3 stuff derived from input parameter
	 *
	 */

	public void setJobs(boolean is, String region, String bucket, ConcurrentLinkedQueue<String> jobs)
	{
		is_s3 = is;
		if (is_s3)
		{
			s3_region = region;
			s3_bucket = bucket;
			s3_client = new AmazonS3Client();
			s3_client.configureRegion(Regions.fromName(s3_region));
		}
		this.jobs = jobs;
	}


	public void run()
	{
		while (!jobs.isEmpty())
		{
			String job = jobs.poll();
			System.out.println(job);
			importLog(job);
		}
	}
	
	public void insertRecord(String[] attributes)
	{
		try
		{
			String sql = "INSERT INTO logs (eventVersion, userIdentity, eventTime, eventSource, eventName, awsRegion, sourceIPAddress, userAgent, errorCode, errorMessage, requestParameters, responseElements, requestID, eventID, eventType, recipientAccountId) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
			PreparedStatement preparedStatement = conn.prepareStatement(sql);

			for (int i=0; i<attributes.length; i++)
			{
				preparedStatement.setString(i+1, attributes[i]);
			}
			preparedStatement.executeUpdate();
		} catch (Exception e)
		{
			System.out.println(e.getMessage());
			e.printStackTrace();
		}
	}

	public String getAttribute(JSONObject record, String attribute)
	{
		Object obj = record.get(attribute);

		if (obj != null)
		{
			if (obj.getClass() == JSONObject.class)
			{
				JSONObject jsonObj = (JSONObject) obj;
				return jsonObj.toJSONString();
			}
			else
			{
				return (String) obj;
			}
		}
		else
		{
			return null;
		}
	}

	public void importLog(String job)
	{
		try
		{
			InputStream stream = null;
			if (is_s3)
			{
				S3Object s3 = s3_client.getObject(s3_bucket, job);
				stream = s3.getObjectContent();
			}
			else
			{
				stream = new FileInputStream(job);
			}
			InputStream gzipStream = new GZIPInputStream(stream);
			Reader reader = new InputStreamReader(gzipStream);

			JSONParser parser = new JSONParser();
			Object obj = parser.parse(reader);
			JSONObject jsonObj = (JSONObject) obj;
			JSONArray records = (JSONArray) jsonObj.get("Records");
			Iterator it = records.iterator();
			while (it.hasNext())
			{
				String[] attributes = new String[16];
				JSONObject record = (JSONObject) it.next();
				attributes[0] = getAttribute(record, "eventVersion");
				attributes[1] = getAttribute(record, "userIdentity");
				attributes[2] = getAttribute(record, "eventTime");
				attributes[3] = getAttribute(record, "eventSource");
				attributes[4] = getAttribute(record, "eventName");
				attributes[5] = getAttribute(record, "awsRegion");
				attributes[6] = getAttribute(record, "sourceIPAddress");
				attributes[7] = getAttribute(record, "userAgent");
				attributes[8] = getAttribute(record, "errorCode");
				attributes[9] = getAttribute(record, "errorMessage");
				attributes[10] = getAttribute(record, "requestParameters");
				attributes[11] = getAttribute(record, "responseElements");
				attributes[12] = getAttribute(record, "requestID");
				attributes[13] = getAttribute(record, "eventID");
				attributes[14] = getAttribute(record, "EventType");
				attributes[15] = getAttribute(record, "recipientAccountId");

				insertRecord(attributes);
			}
		} catch (Exception e)
		{
			System.out.println(e.getMessage());
			e.printStackTrace();
		}
	}


	public static void main(String[] args)
	{
		try
		{
			boolean is_s3 = false;
			String s3_region = null, s3_bucket = null, s3_key = null;
			ConcurrentLinkedQueue<String> jobs = new ConcurrentLinkedQueue<String>();

			if (args[0].startsWith("s3://"))
			{
				is_s3 = true;

				// Input location is in S3, probably with a prefix
				AmazonS3URI s3Uri = new AmazonS3URI(args[0]);
				s3_bucket = s3Uri.getBucket();
				s3_key = s3Uri.getKey();

				// Obtain the bucket location
				AmazonS3Client client = new AmazonS3Client();
				s3_region = client.getBucketLocation(s3_bucket);
				client.configureRegion(Regions.fromName(s3_region));

				// List all the S3 objects
				ObjectListing listing = client.listObjects(s3_bucket, s3_key);
				for (S3ObjectSummary object : listing.getObjectSummaries())
				{
					jobs.add(object.getKey());
				}
				// Recursively listing
				while (listing.isTruncated())
				{
					ListNextBatchOfObjectsRequest request = new ListNextBatchOfObjectsRequest(listing);
					listing = client.listNextBatchOfObjects(request);
					for (S3ObjectSummary object : listing.getObjectSummaries())
					{
						jobs.add(object.getKey());
					}
				}


			}
			else
			{
				// Input location is local disk
				File file = new File(args[0]);
				if (file.isDirectory())
				{
					File[] files = file.listFiles();
					for (File currentFile : files)
					{
					
						jobs.add(currentFile.getPath());
					}
				}
				else
				{
					jobs.add(file.getPath());
				}
			}


			int threads = Runtime.getRuntime().availableProcessors();
			ImportCloudTrailLogs[] ictl = new ImportCloudTrailLogs[threads];
			for (int i=0; i<threads; i++)
			{
				ictl[i] = new ImportCloudTrailLogs();
				ictl[i].setJobs(is_s3, s3_region, s3_bucket, jobs);
				ictl[i].start();
			}
			for (int i=0; i<threads; i++)
			{
				ictl[i].join();
			}
		} catch (Exception e)
		{
			System.out.println(e.getMessage());
			e.printStackTrace();
		}
	}
}

