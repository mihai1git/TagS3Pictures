package com.amazonaws.lambda.mihai.tagpicture.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.TestName;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import com.amazonaws.lambda.mihai.tagpicture.handlers.TagsToS3FunctionHandler;
import com.amazonaws.lambda.mihai.tagpicture.service.FileSystemService;
import com.amazonaws.lambda.mihai.tagpicture.service.NoSQLDatabaseService;
import com.amazonaws.lambda.mihai.tagpicture.test.service.DynamoService;
import com.amazonaws.lambda.mihai.tagpicture.test.service.S3Service;
import com.amazonaws.lambda.mihai.tagpicture.test.utils.LogStartStopRule;
import com.amazonaws.lambda.mihai.tagpicture.test.utils.TagSchemaTests;
import com.amazonaws.lambda.mihai.tagpicture.test.utils.TestContext;
import com.amazonaws.lambda.mihai.tagscommons.model.PictureTagsMap;
import com.amazonaws.services.lambda.runtime.Context;

@RunWith(Parameterized.class)
public class LambdaFunctionHandlerParamTest {
	
	@Rule
	public final TestName name = new TestName();
	@Rule
	public LogStartStopRule myRule = new LogStartStopRule();
	
    // fields used together with @Parameter must be public
    @Parameter(0)
    public String eventFile;
    @Parameter(1)
    public PictureTagsMap dynamoRecord;
    @Parameter(2)
    public String s3TagToTest;
	
	private TagsToS3FunctionHandler handler;

    // creates the test data
    @Parameters
    public static Collection<Object[]> data() {
    	
    	PictureTagsMap rec1 = new PictureTagsMap();
    	rec1.setPictureKey("pics-repository#pics#biserici#Bucuresti#BCasin#654321.jpg");
    	rec1.setTagsTool("aws_rekognition_worker_labels_with_params");
    	Map<String, String> tags = new HashMap<String, String>();//{ "personal:data:content:labels:has_persons" : { "S" : "17" }, "personal:data:content:labels:has_landmarks" : { "S" : "2" } }
    	tags.put("personal:data:content:labels:has_persons", "175");
    	tags.put("personal:data:content:labels:has_landmarks", "2");
    	rec1.setTags(tags);
    	
    	PictureTagsMap rec2 = new PictureTagsMap();
    	rec2.setPictureKey("pics-repository#pics#biserici#Bucuresti#BCasin#123456.jpg");
    	rec2.setTagsTool("aws_rekognition_worker_labels_with_params");
    	tags = new HashMap<String, String>();//{ "personal:data:content:labels:has_persons" : { "S" : "17" }, "personal:data:content:labels:has_landmarks" : { "S" : "2" } }
    	tags.put("personal:data:content:labels:has_persons", "176");
    	tags.put("personal:data:content:labels:has_landmarks", "2");
    	rec2.setTags(tags);
    	
    	PictureTagsMap rec3 = new PictureTagsMap();
    	rec3.setPictureKey("pics-repository#pics#biserici#Bucuresti#BCasin#test_picture.jpg");
    	rec3.setTagsTool("aws_rekognition_worker_index_faces");
    	tags = new HashMap<String, String>();//{ "personal:data:content:has_faces" : { "S" : "1" } }
    	tags.put("personal:data:content:has_faces", "100");
    	rec3.setTags(tags);
    	
        Object[][] data = new Object[][] { 
        	{ "src/test/resources/dynamodb-insert-event-1.json", rec1, "personal:data:content:labels:has_persons" }, 
        	{ "src/test/resources/dynamodb-insert-event-2.json", rec2, "personal:data:content:labels:has_landmarks" }, 
        	{ "src/test/resources/dynamodb-insert-event-3.json", rec3, "personal:data:content:has_faces" } 
        };
        
        return Arrays.asList(data);
    }
    
    public LambdaFunctionHandlerParamTest() {}
    
    @Before
    public void setUp() throws Exception {
    	NoSQLDatabaseService dynamoService = new DynamoService();
    	FileSystemService s3Service = new S3Service();
    	Map<String, String> lambdaParams = new HashMap<String, String>();
    	lambdaParams.put("max_workers", "6");
    	
        handler = new TagsToS3FunctionHandler(dynamoService, s3Service, lambdaParams);
        
      //add data to dynamo
    	DynamoService dynSrv = ((DynamoService)handler.getDynamoService());
    	
    	// get copy of default data
    	List<PictureTagsMap> data = dynSrv.getDefaultTags();
    	
    	// change default data with test data
    	// change all dynamo PK
    	data.forEach(item->item.setPictureKey(dynamoRecord.getPictureKey()));
    	
    	//replace in default data with test data
    	ListIterator<PictureTagsMap> dataIter = data.listIterator();
    	while (dataIter.hasNext()) {
    		PictureTagsMap rec = dataIter.next();
    		if (rec.getTagsTool().equals(dynamoRecord.getTagsTool())) {
    			dataIter.remove();// in next setUp this object will be there
    			data.add(dynamoRecord);
    			break;
    		}
    	}
    	
    	dynSrv.addToDatabase(dynamoRecord.getPictureKey(), data);
                
        System.out.println("In test: " + name.getMethodName() + ", SETUP FINISHED !!!");
    }
    
    @Test
    @Category(TagSchemaTests.class)
    public void testTagsFlow() throws Exception {

		Context ctx = new TestContext();
		((TestContext)ctx).setFunctionName("com.amazonaws.lambda.mihai.tagpicture.handlers.LambdaFunctionHandler.handleRequest");
		
		File initialFile = new File(eventFile);
		FileInputStream file = new FileInputStream(initialFile);
	    InputStream inputStream = new ByteArrayInputStream(file.readAllBytes());
	    file.close();
	    OutputStream outStream = new ByteArrayOutputStream();

	    handler.handleRequest(inputStream, outStream, ctx);
	    
	    String out = outStream.toString();
	        	
        assertTrue("In test: " + name.getMethodName() + ", out should be succesfull ", out.contains("Succesfully"));
        
        String dynamoPKey = dynamoRecord.getPictureKey();
        
        //dynamodb data
        List<PictureTagsMap> structuredData = handler.getDynamoService().getAllPictureRecords(dynamoPKey, null);
        
        // s3 data
        String s3Key = dynamoPKey.substring("pics-repository#".length()).replace('#', '/');//"pics/biserici/Bucuresti/BCasin/123456.jpg"
        Map<String, String> tags = handler.getS3Service().getImageTags(null, s3Key);
        
        System.out.println("In test: " + name.getMethodName() + ", S3 TAGS: " + Arrays.toString(tags.entrySet().toArray()));
        
        //match dynamodb with s3 data
        for (PictureTagsMap tagsData : structuredData) {
        	for (String tagData : tagsData.getTags().keySet()) {
        		if (s3TagToTest.equals(tagData)) {
        			assertEquals("In test: " + name.getMethodName() + ", S3 tag " + s3TagToTest + " should have value " + dynamoRecord.getTags().get(s3TagToTest), dynamoRecord.getTags().get(s3TagToTest), tags.get(s3TagToTest));
        		}
        	}
        }
    }
}
