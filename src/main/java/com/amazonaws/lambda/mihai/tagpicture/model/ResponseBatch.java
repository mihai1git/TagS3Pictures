package com.amazonaws.lambda.mihai.tagpicture.model;

import java.util.List;

/**
 * class that holds multiple responses, one for each record, from one Lambda handler invocation
 * @author Mihai ADAM
 *
 */
public class ResponseBatch {

	/**
	 * aggregated responses, ready to form JSON
	 */
	private List<Response> batchResponses;

	/**
	 * standard getter
	 * @return list of Response
	 */
	public List<Response> getBatchResponses() {
		return batchResponses;
	}

	/**
	 * standard setter
	 * @param batchResponses list of Response
	 */
	public void setBatchResponses(List<Response> batchResponses) {
		this.batchResponses = batchResponses;
	}
	
	
}
