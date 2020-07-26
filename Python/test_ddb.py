import sys
import boto3
from configobj import ConfigObj
import uuid
import random

# Reading configurations
config = ConfigObj('ddb.properties')
tableName = config.get('tableName')
region = config.get('region')
# Create the client
dynamodb = boto3.resource('dynamodb', region_name=region)
table = dynamodb.Table(tableName)
# Total number of items
total = int(sys.argv[1])
# Do the work till completion
for x in range(total):
    hash = str(uuid.uuid1())
    sort = random.randint(0, 100)
    rand = random.randint(0, 100)
    val = hash + "-" + str(sort)
    table.put_item(Item={ 'hash': hash, 'sort': sort, 'random': rand, 'val': val })
