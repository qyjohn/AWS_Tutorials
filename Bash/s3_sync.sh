#
# s3_sync.sh
# Copy files to S3 using sync
#
date
aws s3 sync many_files s3://331982-training/folder004/
date

