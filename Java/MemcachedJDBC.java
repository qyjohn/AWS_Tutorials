import java.io.*;
import java.net.*;
import java.sql.*;
import java.util.*;
import java.util.concurrent.*;
import java.security.*;
import net.spy.memcached.*;
import javax.sql.rowset.*;

public class MemcachedJDBC
{
	Connection conn;
	MemcachedClient mcc;

	public MemcachedJDBC()
	{
		try 
		{
			// Getting database properties from db.properties
			Properties prop = new Properties();
			InputStream input = new FileInputStream("db.properties");
			prop.load(input);
			String mc_hostname = prop.getProperty("mc_hostname");
			String db_hostname = prop.getProperty("db_hostname");
			String db_username = prop.getProperty("db_username");
			String db_password = prop.getProperty("db_password");
			String db_database = prop.getProperty("db_database");

			// Load the MySQL JDBC driver
			Class.forName("com.mysql.jdbc.Driver");
			String jdbc_url = "jdbc:mysql://" + db_hostname + "/" + db_database + "?user=" + db_username + "&password=" + db_password;

			// Connecting to RDS MySQL instance
			conn = DriverManager.getConnection(jdbc_url);	

			// Connecting to ElastiCached Memcached instance
			mcc = new MemcachedClient(new InetSocketAddress(mc_hostname, 11211));
		} catch (Exception ex)
		{
			System.out.println( ex.getMessage() );
		}
	}

	public String MD5encode(String input)
	{
		StringBuffer hexString = new StringBuffer();
		try
		{
			MessageDigest md = MessageDigest.getInstance("MD5");
			byte[] hash = md.digest(input.getBytes());

			for (int i = 0; i < hash.length; i++) 
			{
				if ((0xff & hash[i]) < 0x10) 
				{
					hexString.append("0" + Integer.toHexString((0xFF & hash[i])));
				} 
				else 
				{
					hexString.append(Integer.toHexString(0xFF & hash[i]));
				}
			}
		} catch (Exception ex)
		{
			System.out.println( ex.getMessage() );
		}

		return hexString.toString();
	}
	
	public void test(String firstname)
	{
		try 
		{
			// Build the SQL query
			String query = "SELECT * FROM names WHERE firstname = '" + firstname + "'";
			String query_md5 = MD5encode(query);
			System.out.println(query_md5);
			Future<Object> f = mcc.asyncGet(query_md5);
			
			// Check if the query result is already in cache
			Object result=f.get();
			CachedRowSet crs = RowSetProvider.newFactory().createCachedRowSet();
			if (result == null)
			{
				System.out.println("\n----\nFetching data from database\n----\n");
				ResultSet rs = conn.createStatement().executeQuery(query);
				crs.populate(rs);
				mcc.set(query_md5, 60, crs);
				while (crs.next()) 
				{
					System.out.print(crs.getString("firstname") + "\t" + crs.getString("lastname") + "\n");
				}
				rs.close();
			}
			else
			{
				System.out.println("\n----\nFetching data from cache\n----\n");
				crs = (CachedRowSet) result;
				crs.beforeFirst();
				while (crs.next()) 
				{
					System.out.print(crs.getString("firstname") + "\t" + crs.getString("lastname") + "\n");
				}
			}
		} catch(Exception ex)
		{
			System.out.println( ex.getMessage() );
		}
	}

	public void done()
	{
		try
		{
			conn.close();
			mcc.shutdown();
		} catch(Exception ex)
		{
			System.out.println( ex.getMessage() );
		}
	}
	
	public static void main(String[] args) 
	{
		// Create an instance of the MemcachedJDBC class
		MemcachedJDBC mj = new MemcachedJDBC();
		// Print out current timestamp
		java.sql.Timestamp t0 = new java.sql.Timestamp(System.currentTimeMillis());
		System.out.println(t0 + "\n");
		// Do a query
		mj.test(args[0]);
		// Print out current timestamp again
		java.sql.Timestamp t1 = new java.sql.Timestamp(System.currentTimeMillis());
		System.out.println("\n" + t1);
		// Shutdown everything
		mj.done();
	}
}
