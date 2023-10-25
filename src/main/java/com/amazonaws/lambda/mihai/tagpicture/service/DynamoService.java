package com.amazonaws.lambda.mihai.tagpicture.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.amazonaws.lambda.mihai.tagscommons.model.PictureTagsMap;

import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.PageIterable;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryEnhancedRequest;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

/**
 * layer between lambda logic and AWS DynamoDB service, a NoSQL database
 * @author Mihai ADAM
 *
 */
public class DynamoService implements NoSQLDatabaseService {

	private Logger logger = LogManager.getLogger(DynamoService.class);
    
	// one of the interfaces with DynamoDB: object to database
    private static DynamoDbEnhancedClient dynamoEnhancedClient;
    
    static {
    	//AWS clients are thread safe and can be shared across multiple objects
    	dynamoEnhancedClient = buildClient();
    }
    
    /**
     * 
     * @return AWS client for DynamoDB service
     */
    private static DynamoDbEnhancedClient buildClient() {
    	return DynamoDbEnhancedClient.builder()
                .dynamoDbClient(
                        // Configure an instance of the standard client.
                        DynamoDbClient.builder()
                                .region(Region.US_EAST_2)
                                .build())
                .build();
    }
    
    /**
     * 
     * @return an instance of this class
     */
    public static DynamoService build() {
    	
    	DynamoService srv = new DynamoService();    	
    	return srv;
    }
    
    @Override
    public List<PictureTagsMap> getAllPictureRecords (String imageDynamoPK, Integer allWorkers) {
    	logger.debug("START getAllPictureRecords with: imageDynamoPK: " + imageDynamoPK + " max: " + allWorkers.intValue());
    	
    	DynamoDbTable<PictureTagsMap> pictureTagsTable = 
    			dynamoEnhancedClient.table("PicturesTagging", TableSchema.fromBean(PictureTagsMap.class));
    	
    	List<PictureTagsMap> records = new ArrayList<PictureTagsMap>();
    	
        // 1. Define a QueryConditional instance to return items matching a partition value.
        QueryConditional keyEqual = QueryConditional.keyEqualTo(b -> b.partitionValue(imageDynamoPK));

        // 3. Build the query request.
        QueryEnhancedRequest tableQuery = QueryEnhancedRequest.builder()
                .queryConditional(keyEqual)
                .build();
        // 4. Perform the query.
        PageIterable<PictureTagsMap> pagedResults = pictureTagsTable.query(tableQuery);

        
        pagedResults.items().stream()
                .forEach(
                        item -> records.add(item)
                );
        
        logger.debug("dynamo tag tools: " + Arrays.toString(records.toArray()));
        
        if (records.size() == allWorkers.intValue()) {
        	
        	//consider only workers with keys; do not consider DUMMY workers used to trigger the tags manager
        	Iterator<PictureTagsMap> iter = records.iterator();
        	while(iter.hasNext()) {
        		PictureTagsMap item = iter.next();
            	if (item.getTags() == null) {
            		iter.remove();
            	}
        	}
        	
        } else {
        	return null;
        }
        
        return records;
    }
}
