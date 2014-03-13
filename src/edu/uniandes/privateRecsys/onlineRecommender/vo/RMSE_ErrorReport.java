package edu.uniandes.privateRecsys.onlineRecommender.vo;

import java.util.LinkedList;

public class RMSE_ErrorReport implements ErrorReport {
	
	private double errorTrain;
	private double errorTest;
	private double errorCV;
	private LinkedList<Double> partialErrors;
	
	public RMSE_ErrorReport(double errorTrain, double errorTest, double errorCV, LinkedList<Double> partialErrors) {
		super();
		this.errorTrain = errorTrain;
		this.errorTest = errorTest;
		this.errorCV = errorCV;
		this.partialErrors=partialErrors;
	}
	
	public double getErrorTrain() {
		return errorTrain;
	}

	/* (non-Javadoc)
	 * @see edu.uniandes.privateRecsys.onlineRecommender.vo.ErrorReport#getErrorTest()
	 */
	@Override
	public double getErrorTest() {
		return errorTest;
	}

	public double getErrorCV() {
		return errorCV;
	}

	public LinkedList<Double> getPartialErrors() {
		return partialErrors;
	}

	
	
	
	
	@Override
	public String toString() {
		
		return ""+errorTrain+'\t'+errorCV+'\t'+errorTest;
	}
	

}
