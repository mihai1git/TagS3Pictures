package com.amazonaws.lambda.mihai.tagpicture.service;

import java.util.Map;
/**
 * Interface for the layer between the Lambda logic and the FileSystem storage; 
 * There are to implementations, one for each filesystem: AWS S3 and localhost windows NTFS
 * @author Mihai ADAM
 *
 */
public interface FileSystemService {
	
	/**
	 * the name of the S3 Object Metadata Key for initial image bucket
	 */
	public String META_IMAGE_BUCKET = "image-bucket";
	/**
	 * the name of the S3 Object Metadata Key for default value for Metadata Key: image-bucket
	 */
	public String IMAGE_BUCKET_DEFAULT_VALUE = "pics-repository";
	
	/**
	 * the name of the S3 Object Metadata Key for initial image key
	 */
	public String META_IMAGE_NAME = "image-key";
	
	/**
	 * the S3 bucket with pictures
	 */
	public String BUCKET_IMAGES = "pics-repository";
	/**
	 * file that holds S3 keys with errors that will be used in the Replay action 
	 */
	public String ERROR_EVENTS_KEYS_FILE = "db-errror-keys.txt";
	/**
	 * bucket where db-errror-keys.txt exists
	 */
	public String ERROR_EVENTS_BUCKET = "lambda-config-ohio-mihaiadam";
	/**
	 * 
	 * @param bucket the S3 bucket of the Picture
	 * @param key the S3 Key of the Picture
	 * @return S3 tags for one Picture
	 */
	public Map<String, String> getImageTags (String bucket, String key);
	
	/**
	 * 
	 * @param bucketName the S3 bucket of the Picture
	 * @param key the S3 Key of the Picture
	 * @param tags key->value pairs that will be assigned to S3 Picture as tags
	 */
	public void tagImage (String bucketName, String key, Map<String, String> tags);
	
	/**
	 * 
	 * @param bucket the S3 bucket of the Picture
	 * @param key the S3 Key of the Picture
	 * @return TRUE if the Picture has any tag
	 */
	public Boolean isImageTagged (String bucket, String key);
}
