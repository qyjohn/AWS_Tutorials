<?php
        require 'aws.phar';
        $s3 = new Aws\S3\S3Client(['version' => 'latest','region'  => 'us-east-1']);
        $bucket = '331982-training';
        $result = $s3->listObjects(array('Bucket' => $bucket));
        foreach ($result['Contents'] as $object) 
        {
                echo $object['Key'] . "<br>";
        }
?>
