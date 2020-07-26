import java.io.*;
import java.util.*;

public class GenData
{
	public static void main(String[] args)
	{
		try
		{
			int count = Integer.parseInt(args[0]);
			String file = args[1];
			Random rand = new Random();

			StringBuffer buffer = new StringBuffer();
			for (int i=0; i<count; i++)
			{
				String hash = UUID.randomUUID().toString();
				int sort = rand.nextInt(1000000);
				String val = hash + "-" + sort;
				buffer = buffer.append(hash + "," + sort + "," + val + "\n");
			}

			String output = buffer.toString().trim();
			BufferedWriter writer = new BufferedWriter(new FileWriter(file));
			writer.write(output);
			writer.close();
		} catch (Exception e)
		{
			System.out.println(e.getMessage());
			e.printStackTrace();
		}
	}
}
