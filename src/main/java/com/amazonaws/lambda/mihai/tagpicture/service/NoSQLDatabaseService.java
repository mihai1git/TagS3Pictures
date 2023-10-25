package com.amazonaws.lambda.mihai.tagpicture.service;

import java.util.List;

import com.amazonaws.lambda.mihai.tagscommons.model.PictureTagsMap;
/**
 * Interface for the layer between the Lambda logic and the No SQL database; 
 * There are two implementations, one for each environments: cloud, local
 * @author Mihai ADAM
 *
 */
public interface NoSQLDatabaseService {

    /**
     * Synchronise the tags for one picture;
     * returns the tags generated by all workers only if results from all workers are finished, otherwise null
     * @param imageDynamoPK Partition Key from PicturesTagging table: 
     * @param allWorkers the maximum number of workers for each Picture 
     * @return all table records (one for each worker) for PK parameter; null if number of records different from allWorkers value
     */
	public List<PictureTagsMap> getAllPictureRecords (String imageDynamoPK, Integer allWorkers);
}