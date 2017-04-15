#
# s3_cp.sh
# Copy files to S3 one by one
#
date
for f in {0000..1999}
do 
    aws s3 cp many_files/test.$f s3://331982-training/folder00/
done
date

