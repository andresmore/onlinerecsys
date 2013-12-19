package edu.uniandes.privateRecsys.onlineRecommender.factorModelRepresentation;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Random;

import org.apache.commons.math3.distribution.BetaDistribution;
import org.apache.mahout.math.Vector;

import edu.uniandes.privateRecsys.onlineRecommender.UserMetadataInfo;
import edu.uniandes.privateRecsys.onlineRecommender.metadata.SlidingWindowCountMinSketch;
import edu.uniandes.privateRecsys.onlineRecommender.ratingScale.RatingScale;

public class UserProfile {

	
	/**
	 * Depth of created sketches, 3 -> \delta \aprox 0.05
	 */
	public static int SKETCH_DEPTH=3;
	/**
	 * given t \aprox 500, for a expected distance of 3 between real and predicted count, E=0.006
	 */
	public static int SKETCH_WIDTH=455;
	public static int WINDOW_LENGHT=100;
	public static int NUMBER_OF_SEGMENTS=3;
	public static long[] HASH_A;
	public static final int SEED = new BigInteger(SecureRandom.getSeed(4)).intValue();
	
	
	
	private HashMap<String, Vector> userProfiles= new HashMap<>();
	private HashMap<String, BetaDistribution> userBias= new HashMap<String, BetaDistribution>();
	private Vector hyperParams;
	private int numTrains;
	private UserMetadataInfo metadataInfo;
	
	
	static{
		HASH_A= new long[SKETCH_DEPTH];
		Random r = new Random(SEED);
        // We're using a linear hash functions
        // of the form (a*x+b) mod p.
        // a,b are chosen independently for each hash function.
        // However we can set b = 0 as all it does is shift the results
        // without compromising their uniformity or independence with
        // the other hashes.
        for (int i = 0; i < SKETCH_DEPTH; ++i)
        {
            HASH_A[i] = r.nextInt(Integer.MAX_VALUE);
        }
	}
	private UserProfile(){
		
		
	}
	
	
	public static UserProfile buildDenseProfile(
			LinkedList<Vector> userVectors, RatingScale ratingScale, LinkedList<BetaDistribution> userBiasVector, Vector userHyperParams, LinkedList<Vector> userMetadataVectors,LinkedList<Long> existingConcepts,SlidingWindowCountMinSketch sketch, int numTrains) {
		UserProfile prof= new UserProfile();
		
		prof.setHyperParams(userHyperParams);
		prof.numTrains=numTrains;
		prof.metadataInfo=new UserMetadataInfo(existingConcepts,sketch);
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
