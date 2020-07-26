import boto3

dynamodb = boto3.resource('dynamodb', region_name='ap-southeast-2')
table = dynamodb.Table('test-table')

response = table.put_item(
    Item = {'id': "ABC", 'boolean-attribute': True}
)

response = table.put_item(
    Item = {'id': "XYZ", 'boolean-attribute': False}
)

