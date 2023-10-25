package com.amazonaws.lambda.mihai.tagpicture.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.TestName;

import com.amazonaws.lambda.mihai.tagpicture.handlers.TagsToS3FunctionHandler;
import com.amazonaws.lambda.mihai.tagpicture.model.ResponseBatch;
import com.amazonaws.lambda.mihai.tagpicture.model.Utils;
import com.amazonaws.lambda.mihai.tagpicture.service.FileSystemService;
import com.amazonaws.lambda.mihai.tagpicture.service.NoSQLDatabaseService;
import com.amazonaws.lambda.mihai.tagpicture.test.service.DynamoService;
import com.amazonaws.lambda.mihai.tagpicture.test.service.S3Service;
import com.amazonaws.lambda.mihai.tagpicture.test.utils.EventSourceTests;
import com.amazonaws.lambda.mihai.tagpicture.test.utils.LogStartStopRule;
import com.amazonaws.lambda.mihai.tagpicture.test.utils.TestContext;
import com.amazonaws.services.lambda.runtime.Context;

@Category(EventSourceTests.class)
public class LambdaFunctionHandlerTest {
	
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
    public void tesSourceDynamoStreamRecord() throws Exception {
    	
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
    	
        assertTrue( "Should be succesfull: ", out.contains("Succesfully"));
    }
    
    @Test
    public void tesSourceDynamoStreamRecords() throws Exception {
    	
		Context ctx = new TestContext();
		((TestContext)ctx).setFunctionName("com.amazonaws.lambda.mihai.tagpicture.handlers.LambdaFunctionHandler.handleRequest");
		
		File initialFile = new File("src/test/resources/dynamodb-events.json");
		FileInputStream file = new FileInputStream(initialFile);
	    InputStream inputStream = new ByteArrayInputStream(file.readAllBytes());
	    file.close();
	    OutputStream outStream = new ByteArrayOutputStream();

	    handler.handleRequest(inputStream, outStream, ctx);
	    
	    String out = outStream.toString();
	    
	    System.out.println("In test: " + name.getMethodName() + ", HANDLER OUTPUT: " + out);
	    
	    ResponseBatch[] outObj = Utils.getJsonAsObject(out, ResponseBatch[].class);
    	
	    assertTrue( "Should be succesfull: ", out.contains("Succesfully"));
	    assertEquals("should be 1 nested arrays ", 1, outObj.length);
	    assertTrue( "event 1 should be succesfull ", outObj[0].getBatchResponses().get(0).getMessage().contains("Succesfully TAGGED picture"));
	    assertTrue( "event 2 should be InvalidEventException due to REMOVE type ", outObj[0].getBatchResponses().get(1).getMessage().contains("InvalidEventException"));
	    assertFalse( "event 3 should be NOT TAGGED because of detection of already existing tags in S3  ", outObj[0].getBatchResponses().get(2).getMessage().contains("NOT TAGGED picture"));
    }
    
    @Test
    public void tesSourceDynamoStreamNoInserts() throws Exception {
    	
		Context ctx = new TestContext();
		((TestContext)ctx).setFunctionName("com.amazonaws.lambda.mihai.tagpicture.handlers.LambdaFunctionHandler.handleRequest");
		
		File initialFile = new File("src/test/resources/dynamodb-update-event.json");
		FileInputStream file = new FileInputStream(initialFile);
	    InputStream inputStream = new ByteArrayInputStream(file.readAllBytes());
	    file.close();
	    OutputStream outStream = new ByteArrayOutputStream();

	    handler.handleRequest(inputStream, outStream, ctx);
	    
	    String out = outStream.toString();
	    
	    System.out.println("In test: " + name.getMethodName() + ", HANDLER OUTPUT: " + out);
	    
	    ResponseBatch[] outObj = Utils.getJsonAsObject(out, ResponseBatch[].class);
    	
	    assertFalse( "Should be succesfull: ", out.contains("Succesfully"));
	    assertEquals("should be 1 nested arrays ", 1, outObj.length);
	    assertTrue( "event 2 should be InvalidEventException due to MODIFY type ", outObj[0].getBatchResponses().get(0).getMessage().contains("InvalidEventException"));
	    assertTrue( "event 2 should be InvalidEventException due to REMOVE type ", outObj[0].getBatchResponses().get(1).getMessage().contains("InvalidEventException"));
    }
    
    @Test
    public void tesSourceReplyQueue() throws Exception {
		Context ctx = new TestContext();
		((TestContext)ctx).setFunctionName("com.amazonaws.lambda.mihai.tagpicture.handlers.LambdaFunctionHandler.handleRequest");
		
		File initialFile = new File("src/test/resources/sqs-reply-dynamodb-events.json");
		FileInputStream file = new FileInputStream(initialFile);
	    InputStream inputStream = new ByteArrayInputStream(file.readAllBytes());
	    file.close();
	    OutputStream outStream = new ByteArrayOutputStream();

	    handler.handleRequest(inputStream, outStream, ctx);
	    
	    String out = outStream.toString();
	    
	    System.out.println("In test: " + name.getMethodName() + ", HANDLER OUTPUT: " + out);
	    
	    ResponseBatch[] outObj = Utils.getJsonAsObject(out, ResponseBatch[].class);
    	
	    assertTrue( "Should be succesfull", out.contains("Succesfully"));
	    assertEquals("should be 10 nested arrays ", 10, outObj.length);
    }
}