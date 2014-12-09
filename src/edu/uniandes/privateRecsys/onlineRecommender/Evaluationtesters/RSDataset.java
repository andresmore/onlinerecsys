package edu.uniandes.privateRecsys.onlineRecommender.Evaluationtesters;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Properties;

import edu.uniandes.privateRecsys.onlineRecommender.ratingScale.OrdinalRatingScale;
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
	public static RSDataset fromPropertyFile(String file) throws FileNotFoundException, IOException {
		Properties prop= new Properties();
		prop.load(new FileInputStream(file));
		String[] scaleStr=prop.getProperty("scale").split(",");
		OrdinalRatingScale scale= new OrdinalRatingScale(scaleStr, new HashMap<String, String>());
	
		return new RSDataset(prop.getProperty("trainSet"), prop.getProperty("testSet"), prop.getProperty("testCV"),scale);
	}
	

	
	

}
