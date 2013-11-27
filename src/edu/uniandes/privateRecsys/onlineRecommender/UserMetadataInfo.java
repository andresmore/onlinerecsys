package edu.uniandes.privateRecsys.onlineRecommender;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;

import org.apache.mahout.math.Vector;

public class UserMetadataInfo {
	
	
	private HashMap<String, Vector> trainedProfiles= new HashMap<String, Vector>();
	private LinkedList<String> includedConcepts;
	
	
	public HashMap<String, Vector> getTrainedProfiles() {
		return trainedProfiles;
	}
	public LinkedList<String> getIncludedConcepts() {
		return includedConcepts;
	}
	public UserMetadataInfo(
			LinkedList<String> includedConcepts) {
		
		
		this.includedConcepts = includedConcepts;
	}
	public void addMetadataVector(Vector vector, String scale) {
		this.trainedProfiles.put(scale, vector);
		
	}
	
	

}
