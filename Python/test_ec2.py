import boto3
boto3.set_stream_logger(name='botocore')

def print_instances(response):
    reservations = response['Reservations']
    for reservation in reservations:
        print reservation['ReservationId']
        instances = reservation['Instances']
        for instance in instances:
            print "\t", instance['InstanceId'], "\t", instance['State']['Name']
    return;

aws_region='ap-southeast-2'
client = boto3.client('ec2', aws_region)
response = client.describe_instances()
print_instances(response)
