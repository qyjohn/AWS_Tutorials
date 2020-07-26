import boto3
import logging
boto3.set_stream_logger(name='boto3')
boto3.set_stream_logger(name='botocore')

client = boto3.client('dynamodb')

response = client.update_item(
	TableName='test',
	Key={
		'invoiceId':{'N':'66453606444'},
		'sourceTime':{'S':'DB:1574002800433631224'}
	},
	UpdateExpression = 'SET #attrName =:attrValue',
        ExpressionAttributeNames = {'#attrName' : 'isAnomaly'},
        ExpressionAttributeValues = {':attrValue' : {'S': 'true'}})

print(response)
