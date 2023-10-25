package com.amazonaws.lambda.mihai.tagpicture.test.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.amazonaws.lambda.mihai.tagpicture.service.NoSQLDatabaseService;
import com.amazonaws.lambda.mihai.tagscommons.model.PictureTagsMap;

public class DynamoService implements NoSQLDatabaseService {

	private Logger logger = LogManager.getLogger(DynamoService.class);
    
	private Map<String, List<PictureTagsMap>> database = new HashMap<String, List<PictureTagsMap>>();
	
	{
		
		ArrayList<PictureTagsMap> records = new ArrayList<PictureTagsMap>();
    	
//    	PictureTagsMap rec = new PictureTagsMap();
//    	rec.setPictureKey("pics-repository#pics#biserici#Bucuresti#BCasin#20190914_105211.jpg");
//    	rec.setTagsTool("aws_rekognition_worker_analyze_face");
//    	Map<String, String> tags = null;
//    	rec.setTags(tags);
//    	records.add(rec);
    	
    	PictureTagsMap rec = new PictureTagsMap();
    	rec.setPictureKey("pics-repository#pics#biserici#Bucuresti#BCasin#20190914_105211.jpg");
    	rec.setTagsTool("aws_rekognition_worker_compare_faces");
    	Map<String, String> tags = new HashMap<String, String>();//{ "personal:data:content:has_faces:is_me" : { "S" : "false" } }
    	tags.put("personal:data:content:has_faces:is_me", "false");
    	rec.setTags(tags);
    	records.add(rec);
    	
    	rec = new PictureTagsMap();
    	rec.setPictureKey("pics-repository#pics#biserici#Bucuresti#BCasin#20190914_105211.jpg");
    	rec.setTagsTool("aws_rekognition_worker_index_faces");
    	tags = new HashMap<String, String>();//{ "personal:data:content:has_faces" : { "S" : "1" } }
    	tags.put("personal:data:content:has_faces", "1");
    	rec.setTags(tags);
    	records.add(rec);
    	
    	rec = new PictureTagsMap();
    	rec.setPictureKey("pics-repository#pics#biserici#Bucuresti#BCasin#20190914_105211.jpg");
    	rec.setTagsTool("aws_rekognition_worker_labels_no_params");
    	tags = new HashMap<String, String>();//{ "personal:data:content:labels:dominant_label" : { "S" : "Person" }, "personal:data:content:labels:dominant_category" : { "S" : "Person Description" } }
    	tags.put("personal:data:content:labels:dominant_label", "Person");
    	tags.put("personal:data:content:labels:dominant_category", "Person Description");
    	rec.setTags(tags);
    	records.add(rec);
    	
    	rec = new PictureTagsMap();
    	rec.setPictureKey("pics-repository#pics#biserici#Bucuresti#BCasin#20190914_105211.jpg");
    	rec.setTagsTool("aws_rekognition_worker_labels_with_params");
    	tags = new HashMap<String, String>();//{ "personal:data:content:labels:has_persons" : { "S" : "17" }, "personal:data:content:labels:has_landmarks" : { "S" : "2" } }
    	tags.put("personal:data:content:labels:has_persons", "17");
    	tags.put("personal:data:content:labels:has_landmarks", "2");
    	rec.setTags(tags);
    	records.add(rec);
    	
    	rec = new PictureTagsMap();
    	rec.setPictureKey("pics-repository#pics#biserici#Bucuresti#BCasin#20190914_105211.jpg");
    	rec.setTagsTool("aws_rekognition_worker_text");
    	tags = new HashMap<String, String>();//{ "personal:data:content:has_text" : { "S" : "false" } }
    	tags.put("personal:data:content:has_text", "false");
    	rec.setTags(tags);
    	records.add(rec);
    	
    	database.put("pics-repository#pics#biserici#Bucuresti#BCasin#20190914_105211.jpg", records);
	}
	
	public void addToDatabase(String pictureKey, List<PictureTagsMap> tags) {
		database.put(pictureKey, tags);
	}
	
	public List<PictureTagsMap> getDefaultTags () {
		
		return database.get("pics-repository#pics#biserici#Bucuresti#BCasin#20190914_105211.jpg");
	}

    /**
     * @param imageDynamoPK
     * @param allWorkers
     * @return
     */
    public List<PictureTagsMap> getAllPictureRecords (String imageDynamoPK, Integer allWorkers) {
    	
    	List<PictureTagsMap> defaultTestTags = database.get("pics-repository#pics#biserici#Bucuresti#BCasin#20190914_105211.jpg");
    	List<PictureTagsMap> tags = database.get(imageDynamoPK);
    	return (tags==null) ? defaultTestTags : tags;
    }

}
