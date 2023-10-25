package com.amazonaws.lambda.mihai.tagpicture.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.ExpectedException;
import org.junit.rules.TestName;

import com.amazonaws.lambda.mihai.tagpicture.handlers.TagsToS3FunctionHandler;
import com.amazonaws.lambda.mihai.tagpicture.service.FileSystemService;
import com.amazonaws.lambda.mihai.tagpicture.service.NoSQLDatabaseService;
import com.amazonaws.lambda.mihai.tagpicture.test.service.DynamoService;
import com.amazonaws.lambda.mihai.tagpicture.test.service.S3Service;
import com.amazonaws.lambda.mihai.tagpicture.test.utils.LogStartStopRule;
import com.amazonaws.lambda.mihai.tagpicture.test.utils.TagSchemaTests;
import com.amazonaws.lambda.mihai.tagpicture.test.utils.TestContext;
import com.amazonaws.lambda.mihai.tagscommons.model.PictureTagsMap;
import com.amazonaws.lambda.mihai.tagscommons.model.TagSchema;
import com.amazonaws.services.lambda.runtime.Context;

@Category(TagSchemaTests.class)
public class LambdaFunctionHandlerTagsTest {
		
	@Rule
	public final TestName name = new TestName();
	@Rule
	public LogStartStopRule myRule = new LogStartStopRule();
	
	private TagsToS3FunctionHandler handler;

    @Before
    public void setUp() throws Exception {
    	NoSQLDatabaseService dynamoService = new DynamoService();
    	FileSystemService s3Service = new S3Service();
    	Map<String, String> lambdaParams = new HashMap<String, String>();
    	lambdaParams.put("max_workers", "6");
    	
        handler = new TagsToS3FunctionHandler(dynamoService, s3Service, lambdaParams);
                
        System.out.println("In test: " + name.getMethodName() + ", SETUP FINISHED !!!");
    }
    
    @Test
    public void testTagsSchemaViolatedNoTag() throws Exception {
    	
    	//add data to dynamo
    	String dynamoPK = "pics-repository#pics#biserici#Bucuresti#BCasin#20190914_105211.jpg";
    	PictureTagsMap rec = new PictureTagsMap();
    	rec.setPictureKey(dynamoPK);
    	rec.setTagsTool("aws_rekognition_worker_analyze_face");
    	Map<String, String> tags = new HashMap<String, String>();//{ "personal:data:content:has_faces:is_me:with_glasses" : { "S" : "false" } }
    	tags.put("personal:data:content:has_faces:is_me:with_glasses", "false");
    	rec.setTags(tags);
    	
    	handler.getDynamoService().getAllPictureRecords(dynamoPK, null).add(rec);
    	    	
		Context ctx = new TestContext();
		((TestContext)ctx).setFunctionName("com.amazonaws.lambda.mihai.tagpicture.handlers.LambdaFunctionHandler.handleRequest");
		
		File initialFile = new File("src/test/resources/dynamodb-insert-event.json");
		FileInputStream file = new FileInputStream(initialFile);
	    InputStream inputStream = new ByteArrayInputStream(file.readAllBytes());
	    file.close();
	    OutputStream outStream = new ByteArrayOutputStream();
	    
    	Exception exception = assertThrows(RuntimeException.class, () -> {
    		handler.handleRequest(inputStream, outStream, ctx);
        });

    	String exMessage = exception.getMessage();
        assertTrue("exception message should be: " + exMessage, exMessage.equals("This tag should not be added: personal:data:content:has_faces:is_me:with_glasses"));
    }
    
    @Test
    public void testTagsSchemaViolatedDuplicatedTag() throws Exception {
    	
    	//add data to dynamo
    	String dynamoPK = "pics-repository#pics#biserici#Bucuresti#BCasin#20190914_105211.jpg";
    	PictureTagsMap rec = new PictureTagsMap();
    	rec.setPictureKey(dynamoPK);
    	rec.setTagsTool("aws_rekognition_worker_text");
    	Map<String, String> tags = new HashMap<String, String>();//{ "personal:data:content:has_text" : { "S" : "false" } }
    	tags.put("personal:data:content:has_text", "false");//already exists and SAME
    	rec.setTags(tags);
    	
    	handler.getDynamoService().getAllPictureRecords(dynamoPK, null).add(rec);
    	    	
		Context ctx = new TestContext();
		((TestContext)ctx).setFunctionName("com.amazonaws.lambda.mihai.tagpicture.handlers.LambdaFunctionHandler.handleRequest");
		
		File initialFile = new File("src/test/resources/dynamodb-insert-event.json");
		FileInputStream file = new FileInputStream(initialFile);
	    InputStream inputStream = new ByteArrayInputStream(file.readAllBytes());
	    file.close();
	    OutputStream outStream = new ByteArrayOutputStream();
	    
	    handler.handleRequest(inputStream, outStream, ctx);
	    
	    String out = outStream.toString();
	    
	    assertTrue( "out should be succesfull ", out.contains("Succesfully"));
    	
    	//add data to dynamo
    	rec = new PictureTagsMap();
    	rec.setPictureKey(dynamoPK);
    	rec.setTagsTool("aws_rekognition_worker_text");
    	tags = new HashMap<String, String>();//{ "personal:data:content:has_text" : { "S" : "false" } }
    	tags.put("personal:data:content:has_text", "true");//already exists and DIFFERENT
    	rec.setTags(tags);
    	
    	handler.getDynamoService().getAllPictureRecords(dynamoPK, null).add(rec);
	    
    	inputStream.reset();
    	
    	Exception exception = assertThrows(RuntimeException.class, () -> {
    		handler.handleRequest(inputStream, outStream, ctx);
        });

    	String exMessage = exception.getMessage();
        assertTrue("exception message should be: " + exMessage, exMessage.equals("Redundant tags from workers logic: personal:data:content:has_text"));
    }
    
    @Test
    public void testTagsSchema() throws Exception {
		Context ctx = new TestContext();
		((TestContext)ctx).setFunctionName("com.amazonaws.lambda.mihai.tagpicture.handlers.LambdaFunctionHandler.handleRequest");
		
		File initialFile = new File("src/test/resources/dynamodb-insert-event.json");
		FileInputStream file = new FileInputStream(initialFile);
	    InputStream inputStream = new ByteArrayInputStream(file.readAllBytes());
	    file.close();
	    OutputStream outStream = new ByteArrayOutputStream();

	    handler.handleRequest(inputStream, outStream, ctx);
	    
	    String out = outStream.toString();
	    
	    System.out.println("In test: " + name.getMethodName() + ", HANDLER OUTPUT: " + out);
    	
        assertTrue( "out should be succesfull ", out.contains("Succesfully"));
        
        Map<String, String> tags = handler.getS3Service().getImageTags(null, "pics/biserici/Bucuresti/BCasin/20190914_105211.jpg");
        
        System.out.println("In test: " + name.getMethodName() + ", S3 TAGS: " + Arrays.toString(tags.entrySet().toArray()));
        
        assertTrue("S3 schema should contains maximum 9 tags", tags.size() <= 9);
        assertEquals("this picture should have 6 tags", 6, tags.size());
        assertEquals("current tags schema version is 3_05.08.2023_9tags ", "3_05.08.2023_9tags", tags.get("personal:schema:version"));
        
        tags.forEach((key,value) -> assertNotNull(key + " tag should be not null ", value));
        
        assertNotNull(TagSchema.TAG_KEY_LABELS_DOMINANT_CATEGORY + " tag should always exists ", tags.get(TagSchema.TAG_KEY_LABELS_DOMINANT_CATEGORY));
        assertNotNull(TagSchema.TAG_KEY_LABELS_DOMINANT_LABEL + " tag should always exists ", tags.get(TagSchema.TAG_KEY_LABELS_DOMINANT_LABEL));
        assertNotNull(TagSchema.TAG_KEY_VERSION + " tag should always exists ", tags.get(TagSchema.TAG_KEY_VERSION));
    }
    
    @Test
    public void testTags() throws Exception {
		Context ctx = new TestContext();
		((TestContext)ctx).setFunctionName("com.amazonaws.lambda.mihai.tagpicture.handlers.LambdaFunctionHandler.handleRequest");
		
		File initialFile = new File("src/test/resources/dynamodb-insert-event.json");
		FileInputStream file = new FileInputStream(initialFile);
	    InputStream inputStream = new ByteArrayInputStream(file.readAllBytes());
	    file.close();
	    OutputStream outStream = new ByteArrayOutputStream();

	    handler.handleRequest(inputStream, outStream, ctx);
	    
	    String out = outStream.toString();
	        	
        assertTrue( "out should be succesfull ", out.contains("Succesfully"));
        
        //dynamodb data
        List<PictureTagsMap> structuredData = handler.getDynamoService().getAllPictureRecords("pics-repository#pics#biserici#Bucuresti#BCasin#20190914_105211.jpg", null);
        
        // s3 data
        Map<String, String> tags = handler.getS3Service().getImageTags(null, "pics/biserici/Bucuresti/BCasin/20190914_105211.jpg");
        
        System.out.println("In test: " + name.getMethodName() + ", S3 TAGS: " + Arrays.toString(tags.entrySet().toArray()));
        
        //match dynamodb with s3 data
        for (PictureTagsMap tagsData : structuredData) {
        	for (String tagData : tagsData.getTags().keySet()) {
        		if (TagSchema.TAG_KEY_LABELS_DOMINANT_LABEL.equals(tagData)) {
        			assertEquals("S3 tag personal:data:content:labels:dominant_label should have value Person", "Person", tags.get(TagSchema.TAG_KEY_LABELS_DOMINANT_LABEL));
        		}
        	}
        }
    }
}
