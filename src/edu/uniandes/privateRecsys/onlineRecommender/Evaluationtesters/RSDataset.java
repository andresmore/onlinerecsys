package edu.uniandes.privateRecsys.onlineRecommender.Evaluationtesters;

import edu.uniandes.privateRecsys.onlineRecommender.ratingScale.RatingScale;

public class RSDataset {

	private String trainSet;
	public String getTrainSet() {
		return trainSet;
	}

	public String getTestSet() {
		return testSet;
	}

	public String getTestCV() {
		return testCV;
	}

	public RatingScale getScale() {
		return scale;
	}

	private String testSet;
	private String testCV;
	private RatingScale scale;
	

	public RSDataset(String trainSet, String testSet, String testCV,
			RatingScale scale) {
		this.trainSet=trainSet;
		this.testSet=testSet;
		this.testCV=testCV;
		this.scale=scale;
	}
	

	
	

}
