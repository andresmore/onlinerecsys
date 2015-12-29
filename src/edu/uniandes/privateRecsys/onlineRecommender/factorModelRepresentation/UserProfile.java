package edu.uniandes.privateRecsys.onlineRecommender.factorModelRepresentation;

import java.util.HashMap;
import java.util.LinkedList;

import org.apache.commons.math3.distribution.BetaDistribution;
import org.apache.mahout.cf.taste.model.Preference;
import org.apache.mahout.math.Vector;

import edu.uniandes.privateRecsys.onlineRecommender.UserMetadataInfo;
import edu.uniandes.privateRecsys.onlineRecommender.metadata.SlidingWindowCountMinSketch;
import edu.uniandes.privateRecsys.onlineRecommender.ratingScale.RatingScale;

public class UserProfile {

	
	
	
	private HashMap<String, Vector> userProfiles= new HashMap<>();
	private HashMap<String, BetaDistribution> userBias= new HashMap<String, BetaDistribution>();
	private LinkedList<Preference> userHistory= new LinkedList<Preference>();
	
	private Vector hyperParams;
	private int numTrains;
	private UserMetadataInfo metadataInfo;
	
	
	private UserProfile(){
		
		
	}
	
	
	public static UserProfile buildDenseProfile(
			LinkedList<Vector> userVectors, RatingScale ratingScale, LinkedList<BetaDistribution> userBiasVector, Vector userHyperParams, LinkedList<Vector> userMetadataVectors,LinkedList<Long> existingConcepts,SlidingWindowCountMinSketch sketch, LinkedList<Preference> userHistory, int numTrains) {
		UserProfile prof= new UserProfile();
		
		prof.setHyperParams(userHyperParams);
		prof.numTrains=numTrains;
		prof.metadataInfo=new UserMetadataInfo(existingConcepts,sketch);
		prof.userHistory=userHistory;
		
		for (int i = 0; i < ratingScale.getScale().length; i++) {
			if(userVectors!=null&&!userVectors.isEmpty())
				prof.addVector(userVectors.get(i), ratingScale.getScale()[i]);
			
			if(userBiasVector!=null&&!userBiasVector.isEmpty())
				prof.addBias(userBiasVector.get(i), ratingScale.getScale()[i]);
			
			if(userMetadataVectors!=null&&!userMetadataVectors.isEmpty())
				prof.addMetadataVector(userMetadataVectors.get(i),ratingScale.getScale()[i]);
		}
		
		return prof;
	}
	
	
	
	private void addMetadataVector(Vector vector, String scale) {
		this.metadataInfo.addMetadataVector(vector,scale);
		
	}


	private void setHyperParams(Vector userHyperParams) {
		this.hyperParams=userHyperParams;
		
	}


	private void addBias(BetaDistribution betaDistribution, String key) {
		userBias.put(key, betaDistribution);
		
	}


	public HashMap<String, Vector> getUserProfiles() {
		return userProfiles;
	}

	private void addVector(Vector vector, String key) {
		userProfiles.put(key, vector);
		
	}

	public Vector getProfileForScale(String key) {
		
		if(userProfiles!=null)
			return userProfiles.get(key);
		
		return null;
	}
	
	public HashMap<String, BetaDistribution> getUserBias(){
		return userBias;
	}


	public Vector getHyperParameters() {
		
		return this.hyperParams;
	}


	public int getNumTrains() {
		
		return this.numTrains;
	}

	public UserMetadataInfo getMetadataInfo(){
		return this.metadataInfo;
	}


	public LinkedList<Preference> getUserHistory() {
		return userHistory;
	}
	
	

	
}
