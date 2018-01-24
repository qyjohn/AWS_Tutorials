    /**
     * 
     * The DynamoDB service does not have an SLA on request latency. When the customer's application
     * nees to meet a certain latency requirements, we recommend the customer to set a small request
     * time out in the client configuration, so that the AWS SDK automatically retries the same
     * request after the request timeout. In most cases, this technique can achieve the customer's
     * latency requirements. However, the same technique can also lead to undesired behavior.
     *
     * Let's consider the follow scenario:
     * 
     * (1) The customer makes an API call to DynamoDB. The request was not finished on the server 
     *     side within the request timeout period. The request was still in-flight on the server
     *     side.
     *
     * (2) The AWS SDK retries the request from the client side, and the client receives a 200 OK.
     *
     * (3) The previous request in (1) eventually succeeds after (2). The same API call was executed
     *     twice on the server side, but the customer's applicaiton is totally not aware of it, 
     *     unless the customer enables DEBUG for the AWS SDK.
     *
     * This might not seem to be an issue for simple API calls such as PutItem and GetItem. But it
     * does make a difference for UpdateItem API calls with UpdateExpression. For example, the
     * UpdateExpression increments a particular attribute by 1. When the above-mentioned scenario 
     * occurs, the result would be indeterministic, depending on the number of retries made by the
     * client side, and the number of successful executions on the server side. 
     *
     * This demo program demonstrates the above-mentioned behavior by intentionally setting the
     * request timeout to a small value. You can run this demo program with the following steps:
     *
     * (1) Set up a test DynamoDB table with a partition key 'hash' (String). The test table does
     *     not have a sort key.
     *
     * (2) Create a property file ddb.properties, with the following line:
     *
     *     tableName=<test table name>
     *
     * (3) Add the following line to your log4j.properties:
     *
     *     log4j.logger.com.amazonaws.request=DEBUG
     *
     * (4) Run the program with the following parameters. The first parameter is the partition key 
     *     for the item, while the second parameter is the desired request timeout. In this example, 
     *     the partition key is 1234 and the desired request timeout is 500 ms. 
     *
     *     java DDBRetry 1234 500
     *
     *     When you run the program for the first time, it will create a new item with the following 
     *     value:
     *
     *     {'hash': '1234', 'val': 1}
     *
     *     When you run the program for the second time, it will update the value of attribute 'val'
     *     with val = val + 1, so the item now becomes:
     *
     *     {'hash': '1234', 'val': 2}
     *
     * (5) Now, run the program with a smaller request timeout such as 50 ms:
     *
     *     java DDBRetry 1234 50
     *
     *     Depending on the network condition between your client and the DynamoDB service endpoint,
     *     the demo program might be successful after a certain number of retries, or might fail 
     *     after the maximum number of retries. When the client receives a 200 OK, the value of 
     *     attribute 'val' might get updated multiple times on the server side. When the client
     *     receives a 4xx error, the value of attribute 'val' might also get updated multiple times
     *     on the server side. 
     *
     *     From the SYD12 office network, a 50 ms request timeout is sufficient to reproduce the
     *     above-mentioned indeterministic results.
     *
     * The same issue exists in other AWS services as well. For example, a single PutRecord API 
     * call to the Kinesis service might produces multiple records in the Kinesis stream. 
     *
     * The demo program also provides a runEC2() method, which shows that a RunInstances API call
     * might seem to be failing on the client side but is actually successful on the server side.
     * However, the EC2 service seems to be able to avoid creating multiple EC2 instances for the
     * same API call. In our tests, regardless of the number of MaxErrorRetry we only get one EC2
     * instance for the API call. 
     * 
     * By looking at the DEBUG output, API calls to the EC2 service includes a ClientToken and an
     * amz-sdk-invocation-id, both remain the same for the multiple retries of the same API call.
     * This is probably how the EC2 service performs deduplication on the server side. 
     *
     * API calls to the DynamoDB service also include an amz-sdk-invocation-id, which remains the
     * same for the multiple retries of the same API call. It might be worthwhile to take advantage
     * of this amz-sdk-invocation-id to perform deduplication on the server side to avoid the
     * issue described in this demo program.
     *
     */
     
    import java.io.*;
    import java.util.*;
    import com.amazonaws.*;
    import com.amazonaws.auth.*;
    import com.amazonaws.auth.profile.*;
    import com.amazonaws.regions.*;
    import com.amazonaws.services.dynamodbv2.*;
    import com.amazonaws.services.dynamodbv2.model.*;
    import com.amazonaws.services.dynamodbv2.document.*;
    import com.amazonaws.services.dynamodbv2.datamodeling.*;
     
    import com.amazonaws.services.ec2.*;
    import com.amazonaws.services.ec2.model.*;
     
    public class DDBRetry
    {
            public AmazonDynamoDBClient client;
    	public String tableName;
    	public int timeout;
     
    	public DDBRetry(int timeout)
    	{
    		this.timeout = timeout;
     
    		ClientConfiguration clientConfig = new ClientConfiguration();
    		clientConfig.setRequestTimeout(timeout);
    //		clientConfig.setMaxErrorRetry(10);
                    client = new AmazonDynamoDBClient(clientConfig);
                    client.configureRegion(Regions.AP_SOUTHEAST_2);
     
    		try
    		{
    			Properties prop = new Properties();
    			InputStream input = new FileInputStream("ddb.properties");
    			prop.load(input);
    			tableName = prop.getProperty("tableName");
    		}catch (Exception e)
    		{
    			System.out.println(e.getMessage());
    			e.printStackTrace();
    		}
    	}
     
    	public void update(String hash)
    	{
    		try
    		{
    			Map<String, AttributeValue> key = new HashMap<String, AttributeValue>();
    			key.put("hash", new AttributeValue(hash));
    			String expression = "ADD val :val1";
    			Map<String, AttributeValue> expressionAttributeValues = new HashMap<String, AttributeValue>();
    			expressionAttributeValues.put(":val1", new AttributeValue().withN("1"));  // val = val + 1
     
    			UpdateItemRequest request = new UpdateItemRequest().withTableName(tableName)
    				.withKey(key)
    				.withUpdateExpression(expression)
    				.withExpressionAttributeValues(expressionAttributeValues);
     
    			client.updateItem(request);
    		} catch (Exception e)
    		{
    			System.out.print(e.getMessage());
    			e.printStackTrace();
    		}
    	}
     
    	public void runEC2()
    	{
    		try
    		{
    			ClientConfiguration config = new ClientConfiguration();
    			config.setRequestTimeout(timeout);
    //			config.setMaxErrorRetry(10);
    		        AmazonEC2Client ec2Client = new AmazonEC2Client(config);
    		        ec2Client.configureRegion(Regions.AP_SOUTHEAST_2);
     
    			RunInstancesRequest request = new RunInstancesRequest();
    			request.withImageId("ami-41c12e23")
    				.withInstanceType("t2.micro")
    				.withMinCount(1)
    				.withMaxCount(1)
    				.withKeyName("desktop")
    				.withSecurityGroups("default");
    			ec2Client.runInstances(request);
    		} catch (Exception e)
    		{
    			System.out.print(e.getMessage());
    			e.printStackTrace();
    		}
    	}
     
    	public static void main(String[] args)
    	{
    		try
    		{
    			String hash = args[0];
    			int timeout = Integer.parseInt(args[1]);
    			DDBRetry test = new DDBRetry(timeout);
    			test.update(hash);
    //			test.runEC2();
    		} catch (Exception e)
    		{
    			System.out.print(e.getMessage());
    			e.printStackTrace();
    		}
    	}
    }


