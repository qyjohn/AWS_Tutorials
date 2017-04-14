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
import java.util.concurrent.*;
import java.math.*;
import org.json.simple.*;
import org.json.simple.parser.*;

public class ImportCloudTrailLogs extends Thread
{
	public Connection conn = null;
	ConcurrentLinkedQueue<File> jobs;

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

	public void setJobs(ConcurrentLinkedQueue<File> jobs)
	{
		this.jobs = jobs;
	}

	public void run()
	{
		while (!jobs.isEmpty())
		{
			File file = jobs.poll();
			System.out.println(file.getName());
			importLog(file);
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

	public void importLog(File file)
	{
		try
		{
			JSONParser parser = new JSONParser();
			Object obj = parser.parse(new FileReader(file));
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
			File file = new File(args[0]);
			if (file.isDirectory())
			{
				File[] files = file.listFiles();
				ConcurrentLinkedQueue<File> jobs = new ConcurrentLinkedQueue<File>();
				for (File currentFile : files)
				{
					
					jobs.add(currentFile);
				}

				int cores = Runtime.getRuntime().availableProcessors();
				ImportCloudTrailLogs[] ictl = new ImportCloudTrailLogs[cores];
				for (int i=0; i<cores; i++)
				{
					ictl[i] = new ImportCloudTrailLogs();
					ictl[i].setJobs(jobs);
					ictl[i].start();
				}
				for (int i=0; i<cores; i++)
				{
					ictl[i].join();
				}
			}
			else if (file.isFile())
			{
				ImportCloudTrailLogs ictl = new ImportCloudTrailLogs();
				ictl.importLog(file);
			}
		} catch (Exception e)
		{
			System.out.println(e.getMessage());
			e.printStackTrace();
		}
	}
}

