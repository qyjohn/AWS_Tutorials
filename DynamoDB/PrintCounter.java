import java.util.concurrent.atomic.AtomicInteger;

public class PrintCounter extends Thread
{
	public AtomicInteger counter;

        public PrintCounter(AtomicInteger counter)
        {
		this.counter = counter;
        }

	public void run()
	{
		while (true)
		{
			try
			{
				System.out.println(counter.get());
				sleep(10000);
			} catch (Exception e){}
		}
	}
}
