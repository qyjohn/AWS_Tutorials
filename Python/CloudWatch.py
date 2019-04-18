#
# usage:
# python CloudWatch.py i-0c38708a2a3e99099 vol-0f881ee3173f566e3 20190418T030000 20190418T063000
#
import boto3
import sys
from datetime import datetime, timedelta

client = boto3.client('cloudwatch', region_name='us-east-1')
response = client.get_metric_statistics(
  Namespace='AWS/EC2',
  MetricName='CPUUtilization',
  Dimensions=[{'Name': 'InstanceId','Value': sys.argv[1]}],
  StartTime=datetime.strptime(sys.argv[3], '%Y%m%dT%H%M%S'),
  EndTime=datetime.strptime(sys.argv[4], '%Y%m%dT%H%M%S'),
  Period=300,
  Statistics=['Average'],
  Unit='Percent')
points = response['Datapoints']
cpu = sorted(points, key = lambda x: x['Timestamp'])

response = client.get_metric_statistics(
  Namespace='AWS/EBS',
  MetricName='VolumeWriteOps',
  Dimensions=[{'Name': 'VolumeId','Value': sys.argv[2]}],
  StartTime=datetime.strptime(sys.argv[3], '%Y%m%dT%H%M%S'),
  EndTime=datetime.strptime(sys.argv[4], '%Y%m%dT%H%M%S'),
  Period=300,
  Statistics=['Sum'],
  Unit='Count')
points = response['Datapoints']
dw_iops = sorted(points, key = lambda x: x['Timestamp'])

response = client.get_metric_statistics(
  Namespace='AWS/EBS',
  MetricName='VolumeReadOps',
  Dimensions=[{'Name': 'VolumeId','Value': sys.argv[2]}],
  StartTime=datetime.strptime(sys.argv[3], '%Y%m%dT%H%M%S'),
  EndTime=datetime.strptime(sys.argv[4], '%Y%m%dT%H%M%S'),
  Period=300,
  Statistics=['Sum'],
  Unit='Count')
points = response['Datapoints']
dr_iops = sorted(points, key = lambda x: x['Timestamp'])

response = client.get_metric_statistics(
  Namespace='AWS/EBS',
  MetricName='VolumeWriteBytes',
  Dimensions=[{'Name': 'VolumeId','Value': sys.argv[2]}],
  StartTime=datetime.strptime(sys.argv[3], '%Y%m%dT%H%M%S'),
  EndTime=datetime.strptime(sys.argv[4], '%Y%m%dT%H%M%S'),
  Period=300,
  Statistics=['Sum'],
  Unit='Bytes')
points = response['Datapoints']
dw_bytes = sorted(points, key = lambda x: x['Timestamp'])

response = client.get_metric_statistics(
  Namespace='AWS/EBS',
  MetricName='VolumeReadBytes',
  Dimensions=[{'Name': 'VolumeId','Value': sys.argv[2]}],
  StartTime=datetime.strptime(sys.argv[3], '%Y%m%dT%H%M%S'),
  EndTime=datetime.strptime(sys.argv[4], '%Y%m%dT%H%M%S'),
  Period=300,
  Statistics=['Sum'],
  Unit='Bytes')
points = response['Datapoints']
dr_bytes = sorted(points, key = lambda x: x['Timestamp'])

total = len(dr_bytes)
print total
for x in range(0, total):
  print("%f\t%d\t%d\t%d\t%d" % (cpu[x]['Average'], dw_iops[x]['Sum'], dr_iops[x]['Sum'], dw_bytes[x]['Sum'], dr_bytes[x]['Sum']))
