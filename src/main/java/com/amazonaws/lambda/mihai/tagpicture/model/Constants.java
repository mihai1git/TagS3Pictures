package com.amazonaws.lambda.mihai.tagpicture.model;

/**
 * regular class that holds only constant fields
 * @author Mihai ADAM
 *
 */
public class Constants {

	//the source of the event that enters Lambda handler
	/**
	 * JSON string for S3 service as source
	 */
	public static final String LAMBDA_REQUEST_SOURCE_S3 = "aws:s3";
	/**
	 * JSON string for SNS service as source
	 */
	public static final String LAMBDA_REQUEST_SOURCE_SNS = "aws:sns";
	/**
	 * JSON string for Lambda service as source
	 */
	public static final String LAMBDA_REQUEST_SOURCE_LAMBDA_RESPONSE = "arn:aws:lambda";
	/**
	 * JSON string for SQS service as source
	 */
	public static final String LAMBDA_REQUEST_SOURCE_SQS = "aws:sqs";
	/**
	 * JSON string for DynamoDB Stream service as source
	 */
	public static final String LAMBDA_REQUEST_SOURCE_DYNAMODB = "aws:dynamodb";
}
