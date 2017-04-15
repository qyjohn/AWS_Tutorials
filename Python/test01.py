import boto3

aws_region='ap-southeast-2'
client = boto3.client('ec2', aws_region)
response = client.describe_instances()
reservations = response['Reservations']
for reservation in reservations:
	print reservation['ReservationId']

