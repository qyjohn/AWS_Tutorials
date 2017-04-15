import java.net.InetSocketAddress;
import java.util.concurrent.Future;
import net.spy.memcached.MemcachedClient;

public class MemcachedSet
{
	public static void main(String[] args) 
	{
		try
		{
			// Connecting to Memcached server on localhost
			MemcachedClient mcc = new MemcachedClient(new InetSocketAddress("training.rmzfbh.0001.apse2.cache.amazonaws.com", 11211));
			System.out.println("Connection to server sucessful.");

			// now set data into memcached server
			Future fo = mcc.set("TestKey", 900, "This is a test message");

			// print status of set method
			System.out.println("set status:" + fo.get());

			// retrieve and check the value from cache
			System.out.println("TestKey value in cache - " + mcc.get("TestKey"));

			// Shutdowns the memcached client
			mcc.shutdown();
		} catch(Exception ex)
		{
			System.out.println( ex.getMessage() );
		}
	}
}
