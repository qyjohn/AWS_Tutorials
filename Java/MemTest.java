import java.io.*;
import java.net.*;
import java.util.*;
import net.spy.memcached.*;

public class MemTest 
{
	public static void main(String[] args) 
	{
		try 
		{
			String test = UUID.randomUUID().toString();
			while (test.length() < 500000)
			{
				test = test + test;
			}

			int total = Integer.parseInt(args[0]);
			MemcachedClient mc = new MemcachedClient(new InetSocketAddress("simple-lamp.rmzfbh.0001.apse2.cache.amazonaws.com", 11211));
			for (int j=0; j<100; j++)
			{
				System.out.println("Start working on round " + j);
				for (int i=0; i<total; i++)
				{
					String key = UUID.randomUUID().toString();
					mc.set(key, 0, test);
				}
			}
			System.exit(0);
		} catch (IOException e) 
		{
			// handle exception
			System.out.println(e.getMessage());
			e.printStackTrace();
		}
	}
}

