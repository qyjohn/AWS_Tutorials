#!/bin/bash
uuid=$(uuidgen)
instance_id=$(curl http://169.254.169.254/latest/meta-data/instance-id)
image_id=$(aws ec2 create-image --instance-id $instance_id --no-reboot --name Image-$uuid --region ap-southeast-2 | grep ImageId | awk '{print $2}' | sed -e 's/^"//' -e 's/"$//')
state=$(aws ec2 describe-images --image-ids ami-da0a1db9 --region ap-southeast-2 | grep State | awk '{print $2}' | sed -e 's/^"//' -e 's/.$//' -e 's/.$//')
while [ "$state" != "available" ]
do
  sleep 5
  state=$(aws ec2 describe-images --image-ids ami-da0a1db9 --region ap-southeast-2 | grep State | awk '{print $2}' | sed -e 's/^"//' -e 's/.$//' -e 's/.$//')
done
echo "$image_id is now $state"

