package edu.uniandes.privateRecsys.onlineRecommender.exception;

import org.apache.mahout.cf.taste.common.TasteException;

public class StopTrainingException extends TasteException {

	public StopTrainingException(String string) {
		super(string);	
	}

}
