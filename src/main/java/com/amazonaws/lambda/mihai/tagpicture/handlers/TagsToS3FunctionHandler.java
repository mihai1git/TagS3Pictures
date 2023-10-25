package com.amazonaws.lambda.mihai.tagpicture.handlers;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.amazonaws.lambda.mihai.tagpicture.model.Constants;
import com.amazonaws.lambda.mihai.tagpicture.model.Response;
import com.amazonaws.lambda.mihai.tagpicture.model.ResponseBatch;
import com.amazonaws.lambda.mihai.tagpicture.model.Utils;
import com.amazonaws.lambda.mihai.tagpicture.service.DynamoService;
import com.amazonaws.lambda.mihai.tagpicture.service.FileSystemService;
import com.amazonaws.lambda.mihai.tagpicture.service.NoSQLDatabaseService;
import com.amazonaws.lambda.mihai.tagpicture.service.S3Service;
import com.amazonaws.lambda.mihai.tagscommons.model.PictureTagsMap;
import com.amazonaws.lambda.mihai.tagscommons.model.TagSchema;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestStreamHandler;
import com.amazonaws.services.lambda.runtime.events.DynamodbEvent;
import com.amazonaws.services.lambda.runtime.events.DynamodbEvent.DynamodbStreamRecord;
import com.amazonaws.services.lambda.runtime.events.models.dynamodb.Record;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Lambda Handler that receive events from DynamoDB Stream Service; 
 * entry point of the package
 * @author Mihai ADAM
 *
 */
public class TagsToS3FunctionHandler implements RequestStreamHandler {
	
	//service to AWS DynamoDB
	private NoSQLDatabaseService dynamoService;
	//service to AWS S3
	private FileSystemService s3Service;
	
	//Lambda Parameters from Management Console
	private Map<String, String> lambdaParams;
	
	private Logger logger = LogManager.getLogger(TagsToS3FunctionHandler.class);
		  
	/**
	 * for Cloud Environment
	 */
    public TagsToS3FunctionHandler() {
    	s3Service = S3Service.build();
    	dynamoService = DynamoService.build();
    	lambdaParams = new HashMap<String, String>();
    	lambdaParams.put("max_workers", System.getenv("max_workers"));
    }

    /**
     * for Test Environment
     * @param dynamoService mock of DynamoDB Service
     * @param s3Service mock of S3 Service
     * @param lambdaParams Lambda Parameters from Test Environment
     */
    public TagsToS3FunctionHandler(NoSQLDatabaseService dynamoService, FileSystemService s3Service, Map<String, String> lambdaParams) {
    	this.dynamoService = dynamoService;
    	this.s3Service = s3Service;
    	this.lambdaParams = lambdaParams;
    }

    /**
     * entry point of the handler
     * @param input - JSON structure with DynamoDB event, with multiple records
     * @param output - JSON structure with Responses for each record from the event
     * @param context - holds data from the environment where Lambda runs: AWS Cloud Lambda Service
     */
    @Override
    public void handleRequest(InputStream input, OutputStream output, Context context) throws IOException {
    	
    	ObjectMapper OBJECT_MAPPER = new ObjectMapper();
            	
    	List<ResponseBatch> returnMsg = new ArrayList<ResponseBatch>();
    	
    	for (DynamodbEvent event: Utils.extractDynamoEvents(input)) {
    		
    		returnMsg.add(handleRequest(event, context));
    	}
    	
    	logger.debug("Batch response: " + OBJECT_MAPPER.setSerializationInclusion(Include.NON_NULL).writer().writeValueAsString(returnMsg));
		
		OBJECT_MAPPER.writeValue(output, returnMsg);
    }
    
	/**
	 * iterate through dynamo events
	 * @param event one dynamoDB event that could hold multiple records
	 * @param context Lambda Context
	 * @return list of Responses for each record from the event 
	 */
    public ResponseBatch handleRequest(DynamodbEvent event, Context context) {
    	
        logger.debug("Received event: " + event);
        
        ListIterator<DynamodbStreamRecord> iter = event.getRecords().listIterator();
        List<Response> returnMsg = new ArrayList<Response>();
        while (iter.hasNext()) {
        	DynamodbStreamRecord rec = iter.next();
        	returnMsg.add(handleRequest(rec, context));
        }  
        ResponseBatch res = new ResponseBatch();
        res.setBatchResponses(returnMsg);
        return res;
    }
    
    /**
     * main handler of each event=record
     * @param event one DynamoDB record
     * @param context - Lambda Context
     * @return output for one record structured as object that will be converted to JSON by Lambda Service
     */
   public Response handleRequest(Record event, Context context) {  
	            
	   logger.debug("START event record: " + event);
	   	     
       Response res = new Response();
       res.setEventId(event.getEventID());
       res.setRequestId(context.getAwsRequestId());
       res.setSource(Constants.LAMBDA_REQUEST_SOURCE_DYNAMODB);
       
       String message = validateEvent(event);
       if (message != null) {
    	   res.setMessage(message);
    	   return res; 
       }
       
        String imageDynPK = event.getDynamodb().getKeys().get("PictureS3BucketKeyPK").getS();
        String imageDynSK = event.getDynamodb().getKeys().get("TagsTool").getS();
        String imageBucket = imageDynPK.substring(0, imageDynPK.indexOf("#"));
        String imageKey = imageDynPK.substring(imageDynPK.indexOf("#") + 1).replace('#', '/');
        Integer maxWorkers = Integer.decode(lambdaParams.get("max_workers"));
        
        logger.debug("imageDynPK: " + imageDynPK + " imageDynSK: " + imageDynSK + " imageBucket: " + imageBucket + " imageKey: " + imageKey);
        
        logger.debug("lambdaParams: " + getLambdaParamsString());
        
        //get all records for same picture
        List<PictureTagsMap> tags = dynamoService.getAllPictureRecords (imageDynPK, maxWorkers);
        
        //test if are for all workers; if not stop
        if (tags != null) {
        	
        	Map<String, String> newS3Tags = buildPictureTagsOverwrite (tags);
        	
        	optimizePictureTags(newS3Tags);
        	
            if (newS3Tags.keySet().size() > 0 
            		//&& !s3Service.isImageTagged(imageBucket, imageKey)
            		) {
            	
            	s3Service.tagImage(imageBucket, imageKey, newS3Tags);
            	message = "Succesfully TAGGED picture: " + imageDynPK;
            	
            } else {
            	message = "NOT TAGGED picture: "  + imageDynPK;
            }
        } else {
        	message = "NOT TAGGED picture: "  + imageDynPK;
        }
        
        res.setMessage(message);

        return res;                                                                                                                                                                        
    }
   
   /**
    * validate event that came from AWS Cloud - DynamoDB Stream: only INSERT events are handled
    * @param event
    * @throws RuntimeException if event not an image
    */
   private String validateEvent (Record event) {	
	   
	   String invalidMessage = null;
       //jump over non INSERT events
       if (!"INSERT".equals(event.getEventName())) {
    	   invalidMessage = "InvalidEventException, expected only INSERT type, but found: "+ event.getEventName();
       }
       
       return invalidMessage;
   }
    
    /**
     * Optimise from costs perspective (S3 tags have price, without monthly free tier)
     * @param tags - tags with all values
     * @return less tags without obvious values; according to TagSchema 
     */
    private Map<String, String> optimizePictureTags (Map<String, String> tags) {
    	
    	Iterator<Entry<String, String>> tagsIter = tags.entrySet().iterator();
    	while (tagsIter.hasNext()) {
    		
    		Entry<String, String> tag = tagsIter.next();
    		
    		switch (tag.getKey()) {
    		
			case TagSchema.TAG_KEY_HAS_FACES:
				if (TagSchema.TAG_VALUE_FACES_NUMBER_MAX.equals(tag.getValue())) break;
				if (Integer.valueOf(0).equals(Integer.valueOf(tag.getValue()))) tagsIter.remove();
				break;
			case TagSchema.TAG_KEY_IS_ME:
				if (TagSchema.TAG_VALUE_FALSE.equals(tag.getValue())) tagsIter.remove();
				break;
			case TagSchema.TAG_KEY_IS_ME_FACE_OCCLUDED:
				if (TagSchema.TAG_VALUE_FALSE.equals(tag.getValue())) tagsIter.remove();
				break;
			case TagSchema.TAG_KEY_HAS_TEXT:
				if (TagSchema.TAG_VALUE_FALSE.equals(tag.getValue())) tagsIter.remove();			
				break;
			case TagSchema.TAG_KEY_HAS_TEXT_LANGUAGE:
				tagsIter.remove();
				break;
			case TagSchema.TAG_KEY_LABELS_HAS_PERSONS:
				if (Integer.valueOf(0).equals(Integer.valueOf(tag.getValue()))) tagsIter.remove();
				break;
			case TagSchema.TAG_KEY_LABELS_HAS_LANDMARKS:
				if (Integer.valueOf(0).equals(Integer.valueOf(tag.getValue()))) tagsIter.remove();
				break;
			case TagSchema.TAG_KEY_LABELS_DOMINANT_CATEGORY:
				
				break;
			case TagSchema.TAG_KEY_LABELS_DOMINANT_LABEL:
				
				break;
	
			default:
				break;
			}
    		
    	}
    	
    	return tags;
    }
    
    /**
     * compute S3 tags from DynamoDB tags, considering management rules (e.g. TagsSchema, Default tags, ...)
     * @param tags - records with tags from DynamoDB synchronizer table
     * @return map with tags as they should be in S3 system
     */
    private Map<String, String> buildPictureTagsOverwrite (List<PictureTagsMap> tags) {
    	
    	Map<String, String> newS3Tags = new HashMap<String, String>();
    	
    	for (PictureTagsMap picTags : tags) {
    		for (Entry<String, String> tag : picTags.getTags().entrySet()) {
    			if (newS3Tags.containsKey(tag.getKey())) 
    				if ((newS3Tags.get(tag.getKey()) == null && tag.getValue() == null)
    						|| (newS3Tags.get(tag.getKey()).equals(tag.getValue()))) {
    					//do nothing if already exists and equals
    					continue;
    					
    				} else throw new RuntimeException ("Redundant tags from workers logic: " + tag.getKey());//just to check code
    			
    			newS3Tags.put(tag.getKey(), tag.getValue());
    		}
    	}
    	
    	//check only for extra keys / from outside schema
    	newS3Tags.entrySet().forEach(
				item -> {
					if (!TagSchema.TAGS_DEFAULT.containsKey(item.getKey())) throw new RuntimeException ("This tag should not be added: " + item.getKey());
				}
			);
    	
    	logger.debug("new tags keys before defaults: " + Arrays.toString(newS3Tags.keySet().toArray()));
    	
    	// add missing keys (tags from schema) with default values
    	TagSchema.TAGS_DEFAULT.entrySet().forEach(
				item -> newS3Tags.putIfAbsent(item.getKey(), item.getValue())
			);
    	
    	
    	
    	return newS3Tags;
    }
    
    /**
     * 
     * @return Lambda Parameters from Management Console as a string
     */
    private String getLambdaParamsString() {
    	if (lambdaParams == null) return null;
    	StringBuffer sb = new StringBuffer();
    	lambdaParams.entrySet().forEach(item->sb.append(item).append("\n"));
    	return sb.toString();
    }

	/**
	 * only for tests
	 * @return S3 service as File System interface, with S3 client
	 */
	public FileSystemService getS3Service() {
		return s3Service;
	}
	/**
	 * only for tests
	 * @return Dynamo service as NoSQL database interface, with DynamoDB client
	 */
	public NoSQLDatabaseService getDynamoService() {
		return dynamoService;
	}
    
    
}