package edu.uniandes.privateRecsys.onlineRecommender.factorModelRepresentation;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;

import org.apache.commons.math3.distribution.BetaDistribution;
import org.apache.mahout.math.Vector;

import edu.uniandes.privateRecsys.onlineRecommender.UserMetadataInfo;
import edu.uniandes.privateRecsys.onlineRecommender.ratingScale.RatingScale;

public class UserProfile {

	private HashMap<String, Vector> userProfiles= new HashMap<>();
	private HashMap<String, BetaDistribution> userBias= new HashMap<String, BetaDistribution>();
	private Vector hyperParams;
	private int numTrains;
	private UserMetadataInfo metadataInfo;
	
	private UserProfile(){
		
		
	}
	
	
	public static UserProfile buildDenseProfile(
			LinkedList<Vector> userVectors, RatingScale ratingScale, LinkedList<BetaDistribution> userBiasVector, Vector userHyperParams, LinkedList<Vector> userMetadataVectors,LinkedList<String> existingConcepts,int numTrains) {
		UserProfile prof= new UserProfile();
		
		prof.setHyperParams(userHyperParams);
		prof.numTrains=numTrains;
		prof.metadataInfo=new UserMetadataInfo(existingConcepts);
		for (int i = 0; i < userVectors.size(); i++) {
			prof.addVector(userVectors.get(i), ratingScale.getScale()[i]);
			prof.addBias(userBiasVector.get(i), ratingScale.getScale()[i]);
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
		
		return userProfiles.get(key);
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

	
}
