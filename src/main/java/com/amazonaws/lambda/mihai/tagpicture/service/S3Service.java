package com.amazonaws.lambda.mihai.tagpicture.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.GetObjectTaggingRequest;
import com.amazonaws.services.s3.model.GetObjectTaggingResult;
import com.amazonaws.services.s3.model.ObjectTagging;
import com.amazonaws.services.s3.model.SetObjectTaggingRequest;
import com.amazonaws.services.s3.model.SetObjectTaggingResult;
import com.amazonaws.services.s3.model.Tag;

/**
 * layer between lambda logic and AWS S3 filesystem; files are treated as objects
 * @author Mihai ADAM
 *
 */
public class S3Service implements FileSystemService {

    private static AmazonS3 s3Client;
    
    static {
    	//AWS clients are thread safe and can be shared across multiple objects
    	s3Client =  buildClient();
    }
    
    private Logger logger = LogManager.getLogger(DynamoService.class);
    
    /**
     * 
     * @return AWS client for S3 service
     */
    private static AmazonS3 buildClient() {
    	return AmazonS3ClientBuilder.standard()
    	    	.withRegion(Regions.US_EAST_2)
    			.build();
    }
    
    /**
     * 
     * @return an instance of this class
     */
    public static S3Service build() {
    	S3Service s3Srv = new S3Service();
    	return s3Srv;
    }
    

    public Map<String, String> getImageTags (String bucket, String key) {

    	GetObjectTaggingRequest request = new GetObjectTaggingRequest(bucket, key);
    	GetObjectTaggingResult result = s3Client.getObjectTagging(request);
    	List<Tag> tagSet = result.getTagSet();
    	
    	Map<String, String> tags = new HashMap<String, String>();
    	for (Tag tag : tagSet) {
    		tags.put(tag.getKey(), tag.getValue());
    	}
    	return tags;
    }

    public Boolean isImageTagged (String bucket, String key) {
    	GetObjectTaggingRequest request = new GetObjectTaggingRequest(bucket, key);
    	GetObjectTaggingResult result = s3Client.getObjectTagging(request);
    	return (result.getTagSet() != null && !result.getTagSet().isEmpty());
    }

    public void tagImage (String bucketName, String key, Map<String, String> tags) {
    	List<Tag> tagSet = new ArrayList<Tag>();
    	for (Entry<String, String> entry : tags.entrySet()) {
    		tagSet.add(new Tag(entry.getKey(), entry.getValue()));
    	}
    	ObjectTagging s3Tags = new ObjectTagging(tagSet);
    	SetObjectTaggingRequest setObjectTaggingRequest = new SetObjectTaggingRequest(bucketName, key, s3Tags);
    	SetObjectTaggingResult result = s3Client.setObjectTagging(setObjectTaggingRequest);
    	logger.debug("TAGGED: " + key);
    }
}
