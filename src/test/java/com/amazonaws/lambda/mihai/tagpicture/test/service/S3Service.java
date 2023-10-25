package com.amazonaws.lambda.mihai.tagpicture.test.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.amazonaws.lambda.mihai.tagpicture.service.FileSystemService;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * layer between lambda logic and S3 filesystem; files are treated as objects
 * @author Mihai ADAM
 *
 */
public class S3Service implements FileSystemService {
    
    private Logger logger = LogManager.getLogger(DynamoService.class);
    
    private Map<String, Map<String, String>> tagSystem = new HashMap<String, Map<String,String>>();

    
    public Map<String, String> getImageTags (String bucket, String key) {

    	return tagSystem.get(key);
    }
    
    public Boolean isImageTagged (String bucket, String key) {
    	return tagSystem.containsKey(key);
    }
    
    public void tagImage (String bucketName, String key, Map<String, String> tags) {
    	tagSystem.put(key, tags);

    	logger.debug("TAGGED: " + key + " with tags: " + Arrays.toString(tags.entrySet().toArray()) );
    }

	public void removeImage (String bucket, String key) {
		logger.debug("image removed: " + key);
	}
    
    public List<String> getS3ObjectsInfo (String bucketName, String prefix, String suffix, Integer maxKeys) {
    	
    	logger.debug("start getS3ObjectsInfo");

    	List<String> info = new ArrayList<String>();

    	return info;
    }
    
    
    /**
     * convert key from JSON name to initial image name (jpg file extension)
     * @param s3ObjectName
     * @return
     */
    public String getJpgFileName (String s3ObjectName) {
    	return getNoExtFileName(s3ObjectName) + "jpg";
    }
    
    /**
     * extract file name without extension
     * @param s3ObjectName
     * @return
     */
    private String getNoExtFileName (String s3ObjectName) {
		 Matcher matcher = Pattern.compile(".*\\.([^\\.]*)").matcher(s3ObjectName);
         if (!matcher.matches()) {
        	 System.out.println("Unable to infer image type for key " + s3ObjectName);
             throw new RuntimeException("Unable to infer image type for key "+ s3ObjectName);
         }
         String imageType = matcher.group(1);
         return s3ObjectName.substring(0, matcher.start(1));
    }

     /**
      * 
      * @param jsonObject
      * @return
      */
     public Object getJsonAsObject (String jsonObject, Class objectClass) {
     	
     	Object result = null;
     	
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

}
