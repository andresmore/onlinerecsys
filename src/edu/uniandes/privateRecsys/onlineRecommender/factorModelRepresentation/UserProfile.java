package edu.uniandes.privateRecsys.onlineRecommender.factorModelRepresentation;

import java.util.HashMap;
import java.util.LinkedList;

import org.apache.mahout.math.Vector;

import edu.uniandes.privateRecsys.onlineRecommender.ratingScale.RatingScale;

public class UserProfile {

	private HashMap<String, Vector> userProfiles= new HashMap<>();
	private Vector userBias;
	
	private UserProfile(){
		
		
	}
	
	
	public static UserProfile buildDenseProfile(
			LinkedList<Vector> userVectors, RatingScale ratingScale, Vector userBiasVector) {
		UserProfile prof= new UserProfile();
		
		for (int i = 0; i < userVectors.size(); i++) {
			prof.addVector(userVectors.get(i), ratingScale.getScale()[i]);
			
		}
		prof.serBias(userBiasVector);
		return prof;
	}
	
	
	
	private void serBias(Vector bias) {
		userBias=bias;
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
	
	public Vector getUserBias(){
		return userBias;
	}

	

	
}
