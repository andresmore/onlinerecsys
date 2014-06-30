package edu.uniandes.privateRecsys.onlineRecommender.Evaluationtesters;

import edu.uniandes.privateRecsys.onlineRecommender.ratingScale.RatingScale;

public class RSMetadataDataset extends RSDataset {

	

	private String spectralDataFile;
	private String allDataset;

	public RSMetadataDataset(String trainSet, String testSet, String testCV,
			RatingScale scale, String spectralData, String allDataset) {
		super(trainSet, testSet, testCV, scale);
		this.spectralDataFile= spectralData;
		this.allDataset=allDataset;
		
	}
	
	public String getSpectralDataFile() {
		return spectralDataFile;
	}

	public String getAllDataset() {
		return allDataset;
	}

		
}
