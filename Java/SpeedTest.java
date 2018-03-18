import java.io.*;
import java.net.*;

public class SpeedTest extends Thread
{
	String url;
	
	public SpeedTest(String url)
	{
		this.url = url;
	}

	public void run()
	{
		try
		{
                        byte[] buffer = new byte[1024*1024];
                        InputStream in = new URL(url).openStream();
                        int size = 0, length = 0;
                        boolean go = true;
                        while (size != -1)
                        {
                                size = in.read(buffer);
                        }
                        in.close();
		} catch (Exception e)
		{
		}
	}

	public static void main(String[] args)
	{
		try
		{
			// args[0] is the IP address or hostname of the target
			String url = "http://" + args[0] + "/test.dat";
			int repeat = Integer.parseInt(args[1]);

			byte[] buffer = new byte[1024*1024];

			// Warming up test
			long time0 = System.currentTimeMillis();
			InputStream in = new URL(url).openStream();
			long size = 0, length = 0;
			boolean go = true;
			while (size != -1)
			{
				size = in.read(buffer);
				length = length + size;
			}
			in.close();
			long time1 = System.currentTimeMillis();
			long t = time1 - time0;
			float speed = length * 1000 / t;
			System.out.println("Data Size: " + length);
			System.out.println("Time: " + t);
			System.out.println("Speed: " + speed + "Bytes per second");

			for (int i=0; i<=repeat; i++)
			{
				// Do this N times
        	                time0 = System.currentTimeMillis();

				SpeedTest t0 = new SpeedTest(url);
				t0.start();
	                        SpeedTest t1 = new SpeedTest(url);
				t1.start();
                	        SpeedTest t2 = new SpeedTest(url);
				t2.start();
	                        SpeedTest t3 = new SpeedTest(url);
				t3.start();
                	        SpeedTest t4 = new SpeedTest(url);
				t4.start();
	                        SpeedTest t5 = new SpeedTest(url);
				t5.start();
                	        SpeedTest t6 = new SpeedTest(url);
				t6.start();
	                        SpeedTest t7 = new SpeedTest(url);
				t7.start();
                	        SpeedTest t8 = new SpeedTest(url);
				t8.start();
	                        SpeedTest t9 = new SpeedTest(url);
				t9.start();
			
				try
				{
					t0.join();
					t1.join();
					t2.join();
					t3.join();
	                                t4.join();
                	                t5.join();
                        	        t6.join();
                                	t7.join();
                                	t8.join();
                                	t9.join();
				} catch (InterruptedException e1)
				{
					e1.printStackTrace();
				}

				time1 = System.currentTimeMillis();
				t = time1 - time0;
				System.out.println(time1 + "\t" + time0 + "\t" + t);
				speed = 10 * (1000 * length / t) / 1024 / 1024;
                        	System.out.println("Speed: " + speed + "MBps per second");
			}
		} catch (Exception e)
		{
			System.out.println(e.getMessage());
			e.printStackTrace();
		}
	}
}

