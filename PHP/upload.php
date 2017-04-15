<?php
	$server   = $_SERVER['SERVER_ADDR'];
	session_start();
	$session_id = session_id();
	if (!isset($_SESSION['marker']))
	{
		$_SESSION['marker'] = $server . ' - ' . time();
	}

	require 'aws.phar';
	$s3 = new Aws\S3\S3Client(['version' => 'latest','region'  => 'ap-southeast-2']);
	$bucket = '331982-training';

?>

<HTML>
        <Head>
                <title>Simple S3 Demo</title>
        </Head>
	<body>
		<H1><?php echo $server;?></H1>
		<H3><?php echo 'Session ID: ' . $session_id;?></H3>
		<H3><?php echo 'Session Marker: ' . $_SESSION['marker'];?></H3>
		<HR>

		<form action='upload.php' method='post' enctype='multipart/form-data'>
		<input type='file' id='fileToUpload' name='fileToUpload' id='fileToUpload''>
		<input type='submit' value='Upload' id='submit_button' name='submit_button'>
		</form>

<?php
	if (isset($_FILES["fileToUpload"]))
	{
		save_upload_to_s3($s3, $_FILES["fileToUpload"], $bucket);
	}

	echo "<p>";
	$result = $s3->listObjects(array('Bucket' => $bucket));
	foreach ($result['Contents'] as $object) 
	{
		echo $object['Key'] . "<br>";
	}


	function save_upload_to_s3($s3_client, $uploadedFile, $s3_bucket)
	{
		try 
		{
			// Upload the uploaded file to S3 bucket
			$key = $uploadedFile["name"];
			$s3_client->putObject(array(
				'Bucket' => $s3_bucket,
				'Key'    => $key,
				'SourceFile' => $uploadedFile["tmp_name"],
				'ACL'    => 'public-read'));

			echo "Upload successful<br>";
		} catch (S3Exception $e) 
		{
			echo "There was an error uploading the file.<br>";
			return false;
		}
	}
?>
		<HR>
		<footer>
			<p align='right'>AWS Tutorials prepared by Qingye Jiang (John).</p>
		</footer>
	</body>
</HTML>
