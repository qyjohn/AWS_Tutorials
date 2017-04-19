import java.util.*;
import com.amazonaws.regions.*;
import com.amazonaws.services.dynamodbv2.*;
import com.amazonaws.services.dynamodbv2.model.*;
import com.amazonaws.services.dynamodbv2.datamodeling.*;

public class TestDDBMapper 
{
	public AmazonDynamoDBClient client;
	public DynamoDBMapper mapper;
	
	public TestDDBMapper()
	{
		try
		{
			client = new AmazonDynamoDBClient();
			client.configureRegion(Regions.AP_SOUTHEAST_2);
			mapper = new DynamoDBMapper(client);
		} catch (Exception e)
		{
			System.out.print(e.getMessage());
			e.printStackTrace();
		}
	}

	/**
	 *
	 * Conditional update example.
	 *
	 * For example, you have a test table with "id" as the hash key, and another non-key attribute "total". 
	 * The following code updates the item when (a) the hash key exists, and (b) the existing value "total" 
	 * is less than (LT) the new value "total".
	 *
	 */

	public void test(int id, int total)
	{
		try
		{			
			TestTable table = new TestTable();
			table.setId(id);
			table.setTotal(total);

			DynamoDBSaveExpression saveExpression = new DynamoDBSaveExpression();
			Map expected = new HashMap();
			expected.put("id", new ExpectedAttributeValue(new AttributeValue().withN(""+id)).withExists(true));
			expected.put("total", new ExpectedAttributeValue(new AttributeValue().withN(""+total)).withComparisonOperator("LT"));
			saveExpression.setExpected(expected);

			mapper.save(table, saveExpression);
		} catch (Exception e)
		{
			System.out.print(e.getMessage());
			e.printStackTrace();
		}
	}

	public static void main(String[] args)
	{
		TestDDBMapper t = new TestDDBMapper();
		t.test(Integer.parseInt(args[0]), Integer.parseInt(args[1]));
	}

	@DynamoDBTable(tableName="test")
	public static class TestTable
	{
		private int id;
		private int total;

		@DynamoDBHashKey(attributeName="id")
		public int getId() 
		{
			return id;
		}
		public void setId(int id) 
		{
			this.id = id;
		}

		@DynamoDBAttribute(attributeName="total")
		public int getTotal() 
		{
			return total;
		}
		public void setTotal(int total) 
		{
			this.total = total;
		}	
	}
}


