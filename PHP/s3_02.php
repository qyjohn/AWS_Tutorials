<?php
        require 'aws.phar';
        $s3 = new Aws\S3\S3Client(['version' => 'latest','region'  => 'us-east-1']);
        $result = $s3->listBuckets();
        foreach ($result['Buckets'] as $bucket) 
        {
                echo $bucket['Name'] . "<br>";
        }
?>
