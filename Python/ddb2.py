import boto3
import logging
boto3.set_stream_logger(name='boto3')
boto3.set_stream_logger(name='botocore')
dynamodb = boto3.resource('dynamodb')
table = dynamodb.Table('test')
response = table.update_item(
	Key={
		'sourceTime':'DB:1574002800433631224',
		'invoiceId':66453606444
	},
	UpdateExpression='SET #attrName =:attrValue',
	ExpressionAttributeNames={'#attrName' : 'isAnomaly'},
	ExpressionAttributeValues={':attrValue' : 'false'})
print(response)
