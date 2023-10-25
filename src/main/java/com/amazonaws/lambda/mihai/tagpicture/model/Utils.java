package com.amazonaws.lambda.mihai.tagpicture.model;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.amazonaws.services.lambda.runtime.events.DynamodbEvent;
import com.amazonaws.services.lambda.runtime.events.S3Event;
import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import com.amazonaws.services.lambda.runtime.events.SQSEvent.SQSMessage;
import com.amazonaws.services.lambda.runtime.serialization.PojoSerializer;
import com.amazonaws.services.lambda.runtime.serialization.events.LambdaEventSerializers;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;

/**
 * class with utilities
 * @author Mihai ADAM
 *
 */
public class Utils {
	
	private static Logger logger = LogManager.getLogger(Utils.class);
	
    /**
     * the source of the event is extracted from JSON structure
     * @param prototypeNode Lambda event as JSON string, as it arrive in the Lambda input stream
     * @return the source of the event
     * @throws IOException when JSON deserialization to objects fails
     */
	public static String getEventSource (JsonNode prototypeNode)  throws IOException {
    	
    	
    	ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    	
    	@SuppressWarnings(value = { "" })
    	JsonNode lambdaRequest = prototypeNode;
    	String eventString = OBJECT_MAPPER.setSerializationInclusion(Include.NON_NULL).writer().writeValueAsString(lambdaRequest);
    	logger.debug("Received event: " + eventString);
    	JsonNode lambdaResponse = null;
    	
    	String eventSource = lambdaRequest.at("/Records/0/eventSource").asText();
    	
    	if (eventSource == null || eventSource.isEmpty()) {
    		 eventSource = lambdaRequest.at("/Records/0/EventSource").asText();
    	}
    	
    	if (Constants.LAMBDA_REQUEST_SOURCE_SNS.equals(eventSource)) {
    		lambdaResponse = OBJECT_MAPPER.readTree(lambdaRequest.at("/Records/0/Sns/Message").asText());
    		String innerEventSource = lambdaResponse.at("/requestContext/functionArn").asText();
    		logger.debug("innerEventSource: " + innerEventSource);
    		if (innerEventSource == null || innerEventSource.isEmpty()) {
    			throw new RuntimeException("Unexpected structure for Event Source ! Expected: /Records/0/Sns/Message/requestContext/functionArn");
    		} else {
    			
    			if (innerEventSource.startsWith(Constants.LAMBDA_REQUEST_SOURCE_LAMBDA_RESPONSE)) {
    				eventSource = Constants.LAMBDA_REQUEST_SOURCE_LAMBDA_RESPONSE;
    			}
    		}
    	}
    	
    	logger.debug("eventSource: " + eventSource);
    	
    	return eventSource;
    }
	
	/**
	 * extracts DynamodbEvents from JSON structure
	 * @param input the Lambda event as JSON with all the DynamodbEvents; could be multiple when comming from SQS source
	 * @return list with all the DynamodbEvent deserialized from JSON
	 * @throws IOException from serialization processes
	 */
	public static List<DynamodbEvent> extractDynamoEvents(InputStream input) throws IOException {
		
		ObjectMapper OBJECT_MAPPER = new ObjectMapper();
		List<DynamodbEvent> events = new ArrayList<DynamodbEvent>();
		
    	@SuppressWarnings(value = { "" })
    	JsonNode lambdaRequest = OBJECT_MAPPER.readTree(input);
    	
    	String eventSource = getEventSource (lambdaRequest);
    	
    	logger.debug("eventSource: " + eventSource);
 	
    	PojoSerializer<DynamodbEvent> dynamoEventSerializer = LambdaEventSerializers.serializerFor(DynamodbEvent.class, DynamodbEvent.class.getClassLoader());
    	
    	// normal flow
    	if (Constants.LAMBDA_REQUEST_SOURCE_DYNAMODB.equals(eventSource)) {
        	input.reset();
        	DynamodbEvent event = dynamoEventSerializer.fromJson(input);
        	events.add(event);
    	}
    	// used for Reply of ERRORS
    	if (Constants.LAMBDA_REQUEST_SOURCE_SQS.equals(eventSource)) {
    		
    		PojoSerializer<SQSEvent> sqsEventSerializer = LambdaEventSerializers.serializerFor(SQSEvent.class, SQSEvent.class.getClassLoader());
    		input.reset();
    		
    		SQSEvent event = sqsEventSerializer.fromJson(input);    		
    		for (SQSMessage msg : event.getRecords()) {
    			DynamodbEvent dynEvent = dynamoEventSerializer.fromJson(msg.getBody());
    			events.add(dynEvent);
    		}
    	}
    	
    	return events;
	}

	/**
	 * extracts S3Events from JSON structure
	 * @param input input the Lambda event as JSON with all the DynamodbEvents; the structure is different, depending on the source: SNS, S3, other Lambda
	 * @return list with all the S3Events deserialized from JSON
	 * @throws IOException from serialization processes
	 */
    public static S3Event extractS3Event(InputStream input) throws IOException {
    	
    	ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    	S3Event event = null;
    	
    	@SuppressWarnings(value = { "" })
    	JsonNode lambdaRequest = OBJECT_MAPPER.readTree(input);
    	
    	String eventSource = getEventSource (lambdaRequest);
    	
    	PojoSerializer<S3Event> s3EventSerializer = LambdaEventSerializers.serializerFor(S3Event.class, S3Event.class.getClassLoader());
    	
    	//if SNS request look for S3 bucket&key
    	if (Constants.LAMBDA_REQUEST_SOURCE_SNS.equals(eventSource)) {
    		String message = lambdaRequest.at("/Records/0/Sns/Message").asText();
    		event = s3EventSerializer.fromJson(message);
    	}
		//if S3 request look for S3 bucket&key
    	if (Constants.LAMBDA_REQUEST_SOURCE_S3.equals(eventSource)) {
        	input.reset();
    		event = s3EventSerializer.fromJson(input);
    	}
    	if (Constants.LAMBDA_REQUEST_SOURCE_LAMBDA_RESPONSE.equals(eventSource)) {
    		JsonNode lambdaResponse = OBJECT_MAPPER.readTree(lambdaRequest.at("/Records/0/Sns/Message").asText());
    		event = s3EventSerializer.fromJson(lambdaResponse.at("/requestPayload/Records/0/Sns/Message").asText());
            
    	}

    	input.reset();
    	
    	return event;
    }

    /**
     * 
     * @param <T> the class of the object to be deserialized
     * @param jsonObject data as JSON for the object
     * @param objectClass the class of the object
     * @return the object
     */
   public static <T> T getJsonAsObject(String jsonObject, Class<T> objectClass) {
    	
    	T result = null;
    	
    	ObjectMapper om = new ObjectMapper().setSerializationInclusion(Include.NON_NULL);
    	
    	om.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    	om.configure(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES, false);
    	om.configure(DeserializationFeature.FAIL_ON_NUMBERS_FOR_ENUMS, false);
    	
    	try {
    		result = om.readValue(jsonObject.getBytes(), objectClass);
    		
    	} catch (Exception ex) {
    		ex.printStackTrace();
    		throw new RuntimeException(ex);
    	}
    	
    	
    	return result;
    }
    	
   /**
    * 
    * @param detectedLabels results from the AWS services
    * @return JSON representation of the object
    */
    public static String getDetectionAsJson (Object detectedLabels) {
    	StringBuffer sb = new StringBuffer();
    	    	
    	ObjectMapper om = new ObjectMapper().setSerializationInclusion(Include.NON_NULL);
    	//ObjectWriter ow = om.writer().withDefaultPrettyPrinter();
		ObjectWriter ow = om.writer();
    	
    	try {
    		sb.append(ow.writeValueAsString(detectedLabels));
    		
    	} catch (JsonProcessingException ex) {
    		ex.printStackTrace();
    	}
    	
    	return sb.toString();
    }
   
   /**
    * 
    * convert file from image type to file of type: extension
    * @param s3ObjectName name of the S3 object / file
    * @param extension the new extension of the S3 object
    * @return file name with new extension
    */
   public static String getImgFileNameWithExtension (String s3ObjectName, String extension) {
   	return getNoExtFileName(s3ObjectName) + extension;
   }
    
    /**
     * extract file name without extension
     * @param s3ObjectName name of the S3 object / file
     * @return file name without extension
     */
    private static String getNoExtFileName (String s3ObjectName) {
		 Matcher matcher = Pattern.compile(".*\\.([^\\.]*)").matcher(s3ObjectName);
         if (!matcher.matches()) {
        	 System.out.println("Unable to infer image type for key " + s3ObjectName);
             throw new RuntimeException("Unable to infer image type for key "+ s3ObjectName);
         }
         String imageType = matcher.group(1);
         return s3ObjectName.substring(0, matcher.start(1));
    }
}
