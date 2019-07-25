const AmazonDaxClient = require('amazon-dax-client');
var AWS = require("aws-sdk");
var region = "us-west-2";
AWS.config.update({
  region: region
});

var dax = new AmazonDaxClient({endpoints: ['dax-cluster.mrn1ea.clustercfg.dax.usw2.cache.amazonaws.com:8111'], region: region});
var daxClient = new AWS.DynamoDB.DocumentClient({service: dax });
test(daxClient, 10);

function test(client, n) {
    var tableName = "PPS";
    var pk = 'id1';
    var params = {
        TableName: tableName,
        KeyConditionExpression: "ID = :pkval",
        ExpressionAttributeValues: {":pkval":pk}
    };
    var startTime = new Date().getTime();

    client.query(params, function(err, data) {
        if (err) {
            console.error("Unable to read item. Error JSON:", JSON.stringify(err, null, 2));
        } else {
           console.log(data);
	   var endTime = new Date().getTime();
	   console.log("\tTotal time: " + (endTime - startTime) +  "ms");
           if (n !=0) {
              test(client, n-1);
           }
        }
    })
}

