import java.io.*;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import com.amazonaws.*;
import com.amazonaws.auth.*;
import com.amazonaws.auth.profile.*;
import com.amazonaws.regions.*;
import com.amazonaws.services.glue.*;
import com.amazonaws.services.glue.model.*;

public class TestGlue
{
	public static void main(String[] args)
	{
		try
		{
			String tableName = args[0];
			AWSGlue client = AWSGlueClient.builder().defaultClient();

			LinkedList<Column> columns = new LinkedList<Column>();
			Column id = new Column().withName("id").withType("int");
			Column name = new Column().withName("name").withType("string");
			Column address = new Column().withName("address").withType("struct<city:string,state:string>");
			columns.add(id);
			columns.add(name);
			columns.add(address);

			String schema = new String(Files.readAllBytes(Paths.get("test.avsc")), StandardCharsets.UTF_8);
			HashMap<String, String> param = new HashMap<String, String>();
			param.put("avro.schema.literal", schema);
			SerDeInfo serde = new SerDeInfo()
				.withName("AVRO")
				.withSerializationLibrary("org.apache.hadoop.hive.serde2.avro.AvroSerDe")
				.withParameters(param);
			StorageDescriptor storage = new StorageDescriptor()
				.withColumns(columns)
				.withInputFormat("org.apache.hadoop.hive.ql.io.avro.AvroContainerInputFormat")
				.withOutputFormat("org.apache.hadoop.hive.ql.io.avro.AvroContainerInputFormat")
				.withLocation("s3://331982-syd/avro/")
				.withSerdeInfo(serde);

			TableInput input = new TableInput()
				.withName(tableName)
				.withStorageDescriptor(storage);
			CreateTableRequest request = new CreateTableRequest()
				.withDatabaseName("default")
				.withTableInput(input);
			client.createTable(request);
		} catch (Exception e)
		{
			System.out.println(e.getMessage());
			e.printStackTrace();
		}
        }
}
