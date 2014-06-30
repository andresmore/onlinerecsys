package edu.uniandes.privateRecsys.onlineRecommender;

import java.util.HashMap;
import java.util.LinkedList;

import org.apache.mahout.math.Vector;

import edu.uniandes.privateRecsys.onlineRecommender.metadata.ResetableCountMinSketch;
import edu.uniandes.privateRecsys.onlineRecommender.metadata.SlidingWindowCountMinSketch;

public class UserMetadataInfo {
	
	
	private HashMap<String, Vector> trainedProfiles= new HashMap<String, Vector>();
	private LinkedList<Long> includedConcepts;
	
	
	private  SlidingWindowCountMinSketch countMinSketch;
	
	public UserMetadataInfo(LinkedList<Long> includedConcepts, SlidingWindowCountMinSketch sketch) {
		
		
		this.includedConcepts = includedConcepts;
		this.countMinSketch=sketch;
	}
	
	public HashMap<String, Vector> getTrainedProfiles() {
		return trainedProfiles;
	}
	public LinkedList<Long> getIncludedConcepts() {
		return includedConcepts;
	}
	
	public void addMetadataVector(Vector vector, String scale) {
		this.trainedProfiles.put(scale, vector);
		
	}

	public SlidingWindowCountMinSketch getUserSketch() {
		return countMinSketch;
	}
	
	

}
