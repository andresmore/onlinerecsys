package edu.uniandes.privateRecsys.onlineRecommender.Evaluationtesters;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Properties;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.collections.map.HashedMap;
import org.jfree.util.Log;

import com.google.common.base.CharMatcher;
import com.google.common.base.Splitter;
import com.google.common.collect.Iterables;

import edu.uniandes.privateRecsys.onlineRecommender.ratingScale.OrdinalRatingScale;
import edu.uniandes.privateRecsys.onlineRecommender.ratingScale.RatingScale;

public class RSDataset {
	private final static Logger LOG = Logger.getLogger(RSDataset.class
		      .getName());

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
	public static RSDataset fromPropertyFile(String file) throws IOException{
		Properties prop= new Properties();
		FileInputStream fileInputStream = new FileInputStream(file);
		try {
			prop.load(fileInputStream);
		} catch (IOException e) {
			throw e;
		}
		finally{
			if(fileInputStream!=null)
				try {
					fileInputStream.close();
				} catch (IOException e) {}
		}
		String[] scaleStr=prop.getProperty("scale").split(",");
		
		
		HashMap<String, String> translations = new HashMap<String, String>();
		String mapping=prop.getProperty("translations") == null?"":prop.getProperty("translations").replaceAll("\\s+","");
		
		String regex="\\((\\d+(\\.\\d+)*),(\\d+(\\.\\d+)*)\\)";
		Pattern pa=Pattern.compile(regex);
		Matcher ns=pa.matcher(mapping);
		while(ns.find()){
			String key=ns.group(1);
			
			String value=ns.group(3);
			
			translations.put(key, value);
		}
		OrdinalRatingScale scale= new OrdinalRatingScale(scaleStr, translations);
		
		
		RSDataset rsDataset = new RSDataset(prop.getProperty("trainSet"), prop.getProperty("testSet"), prop.getProperty("testCV"),scale);
		LOG.info("Dataset created: "+rsDataset.toString());
		return rsDataset;
	}
	
	@Override
	public String toString() {
		// TODO Auto-generated method stub
		return "train "+this.trainSet+" cv "+this.testCV+" test"+this.testSet+" scale"+Arrays.toString(this.scale.getScale())+" translations"+this.scale.getTranslations();
	}

}
