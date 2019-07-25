var AWS = require("aws-sdk");
AWS.config.update({
  region: "us-west-2"
});
var client = new AWS.DynamoDB.DocumentClient();
test(client, 10);

function test(client, n) {
    var params = {
        TableName : "PPS",
        KeyConditionExpression: "ID = :pkval",
        ExpressionAttributeValues: {":pkval": "id1"}
    };
    var startTime = new Date().getTime();
    client.query(params, function(err, data) {
        if (err) {
            console.error("Unable to query. Error:", JSON.stringify(err, null, 2));
        } else {
            console.log(data);
            var endTime = new Date().getTime();
            console.log("\tTotal time: " + (endTime - startTime) +  "ms");
            if (n !=0) {
                test(client, n-1);
            }
        }
    });
}
