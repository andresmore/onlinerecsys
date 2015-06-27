package edu.uniandes.privateRecsys.onlineRecommender;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Random;

import org.apache.commons.math3.distribution.BetaDistribution;
import org.apache.mahout.cf.taste.common.TasteException;
import org.apache.mahout.math.DenseVector;
import org.apache.mahout.math.Vector;
import org.apache.mahout.math.function.Functions;

import edu.uniandes.privateRecsys.onlineRecommender.factorModelRepresentation.FactorUserItemRepresentation;
import edu.uniandes.privateRecsys.onlineRecommender.factorModelRepresentation.UserProfile;
import edu.uniandes.privateRecsys.onlineRecommender.metadata.SlidingWindowCountMinSketch;
import edu.uniandes.privateRecsys.onlineRecommender.utils.PrivateRandomUtils;
import edu.uniandes.privateRecsys.onlineRecommender.vo.Prediction;
import edu.uniandes.privateRecsys.onlineRecommender.vo.UserTrainEvent;
/**
 * Class that trains and samples a metadata model for the user
 * @author Andres M
 *
 */
public class MetadataPredictor implements UserModelTrainerPredictor {

	private  int numRolling=3;
	
	public static final int SEED = PrivateRandomUtils.getSeed();

	public static final int NUMROLLING = 3;
	
	private int limitSize=10;
	
	private FactorUserItemRepresentation model;

	private LearningRateStrategy learningStrategy;

	private int depth;

	private int width;

	private int windowLenght;

	private int numberOfSegments;
	
	private long[] hash_a;
	
	public MetadataPredictor(int limitSize, int depth, int width, int windowLenght, int numberOfSegments, int numRolling){
		this.limitSize=limitSize;
		initSketchProperties(depth, width, windowLenght, numberOfSegments, numRolling);
	}

	
	public void initSketchProperties(int depth, int width, int windowLenght,
			int numberOfSegments,int numRolling) {
		this.depth=depth;
		this.width=width;
		this.windowLenght=windowLenght;
		this.numberOfSegments=numberOfSegments;
		this.numRolling=numRolling;
		hash_a= new long[depth];
		Random r = new Random(SEED);
	       // We're using a linear hash functions
	       // of the form (a*x+b) mod p.
	       // a,b are chosen independently for each hash function.
	       // However we can set b = 0 as all it does is shift the results
	       // without compromising their uniformity or independence with
	       // the other hashes.
	       for (int i = 0; i < SKETCH_DEPTH; ++i)
	       {
	    	   hash_a[i] = r.nextInt(Integer.MAX_VALUE);
	       }
	}
	
	
	/**
	 * Depth of created sketches, 3 -> \delta \aprox 0.05
	 */
	public static int SKETCH_DEPTH=3;
	/**
	 *  for \epsilon=0.006
	 */
	public static int SKETCH_WIDTH=450;
	
	
	public static int WINDOW_LENGHT=100;
	public static int NUMBER_OF_SEGMENTS=3;
	
		
	@Override
	public void setModelRepresentation(FactorUserItemRepresentation model) {
		this.model=model;

	}

	@Override
public  Prediction calculatePrediction(UserTrainEvent event, int minTrains) throws TasteException{
		
		
		long userId=event.getUserId();
		long itemId=event.getItemId();
		
		if(event.getMetadata()==null)
			return Prediction.createNoAblePrediction(userId, itemId);
		
		UserProfile user = model
				.getPrivateUserProfile(userId);
		UserMetadataInfo trainedMetadataProfiles=user.getMetadataInfo();
		LinkedList<Long> profileConcepts=trainedMetadataProfiles.getIncludedConcepts();
		HashMap<String,Vector> profiles=trainedMetadataProfiles.getTrainedProfiles();
		
		
		LinkedList<Long> metadataConcepts=ConceptBreaker.breakConcepts(event.getMetadata());
		Vector itemVector=buildItemVector(profileConcepts,metadataConcepts);
		
		double prediction=0;
		double sumProb=0;
		for (String ratingKey: profiles.keySet()) {
			Vector userVector = profiles
					.get(ratingKey);
			double dot = Functions.SIGMOID.apply(userVector.dot(itemVector)) ;
			sumProb+=dot;
			prediction += dot * Double.parseDouble(ratingKey);
			
		}
		
		return Prediction.createPrediction(userId, itemId, prediction/sumProb);
	}

	@Override
	public HashMap<String, Vector> calculateProbabilityUpdate(UserTrainEvent event,
			String rating, Vector itemVector, UserProfile oldUserPrivate,
			String[] ratingScale) {
		
		return oldUserPrivate.getUserProfiles();
	}

	@Override
	public HashMap<String, BetaDistribution> calculatePriorsUpdate(
			UserTrainEvent event, HashMap<String, BetaDistribution> biasVector,
			String[] ratingScale) {
		
		return biasVector;
	}

	@Override
	public Vector calculatehyperParamsUpdate(UserTrainEvent event,Vector itemVector,
			HashMap<String, Vector> trainedProfiles,
			HashMap<String, BetaDistribution> biasVector,
			Vector oldHyperparameters, int numTrains) {
	
		return oldHyperparameters;
	}
	@Override
	public UserMetadataInfo calculateMetadataUpdate(UserTrainEvent event,
			UserMetadataInfo trainedMetadataProfiles,int numTrains) {
		String rating=event.getRating();
		
		if(event.getMetadata()==null)
			return trainedMetadataProfiles;
		
		double gamma= this.learningStrategy.getGammaFromK(this.model.getNumberTrainsUser(event.getUserId()));	
		LinkedList<Long> profileConcepts=trainedMetadataProfiles.getIncludedConcepts();
		HashMap<String,Vector> profiles=trainedMetadataProfiles.getTrainedProfiles();
		SlidingWindowCountMinSketch sketch=trainedMetadataProfiles.getUserSketch();
		
		LinkedList<Long> metadataItemConcepts=ConceptBreaker.breakConcepts(event.getMetadata());
		
		
		HashSet<Long> endProfileConcepts=addMetadataConceptsFromDistribution(profileConcepts,metadataItemConcepts,sketch,numTrains);
		
		
		UserMetadataInfo profile=modifyMetadataVectors(profiles,profileConcepts,endProfileConcepts,sketch);
		Vector itemVector=buildItemVector(profile.getIncludedConcepts(),metadataItemConcepts);
		
		
		for (String ratingKey : profile.getTrainedProfiles().keySet()) {	
		
			Vector privateVector= profile.getTrainedProfiles().get(ratingKey);
			if (privateVector.size() > 0 && itemVector.size() > 0) {
				int prob = ratingKey.equals(rating) ? 1 : 0;

				double dotProb = Functions.SIGMOID.apply(privateVector.dot(itemVector));

				double loss =dotProb  - prob;

				double multiplicator = gamma * (loss);
				Vector privateVectorMult = itemVector.times(multiplicator);
				Vector result = privateVector.minus(privateVectorMult);

				//double dotProbEnd = Functions.SIGMOID.apply(result.dot(itemVector));

				profile.addMetadataVector(result, ratingKey);
			}
			else{
				profile.addMetadataVector(privateVector, ratingKey);
			}
		}
		
		
		return profile;
		
		
	}

	private UserMetadataInfo modifyMetadataVectors(HashMap<String, Vector> profiles,
			LinkedList<Long> profileConcepts,
			HashSet<Long> endProfileConcepts, SlidingWindowCountMinSketch sketch) {
		
		
		LinkedList<Long> finalConceptList= fillFinalConceptList(profileConcepts,endProfileConcepts);
		UserMetadataInfo retUser=new UserMetadataInfo(finalConceptList, sketch);
		
		for (String key : profiles.keySet()) {
			Vector metadataVector=profiles.get(key);
			Vector extendedVector= new DenseVector(endProfileConcepts.size());
			
			Iterator<Long> iterator = profileConcepts.iterator();
			int posNewVector=0;
			int posOriginal=0;
			
				
				while(iterator.hasNext()){
					Long concept=iterator.next();
					if(endProfileConcepts.contains(concept)){
						//
						extendedVector.set(posNewVector, metadataVector.get(posOriginal));
						
						posNewVector++;
						
					}
					posOriginal++;
					
				}
					
			
			
			
			extendedVector.viewPart(posNewVector, extendedVector.size()-posNewVector).assign(PrivateRandomUtils.normalRandom(0, 1, extendedVector.size()-posNewVector));
			retUser.addMetadataVector(extendedVector, key);
		}
		
	
		return retUser;
	}

	private LinkedList<Long> fillFinalConceptList(
			LinkedList<Long> profileConcepts, HashSet<Long> endProfileConcepts) {
		HashSet<Long> endProfileConceptsClone= (HashSet<Long>) endProfileConcepts.clone();
		LinkedList<Long> finalConceptList= new LinkedList<>();
		for (Long concept : profileConcepts) {
			if(endProfileConceptsClone.contains(concept)){
				finalConceptList.add(concept);
				endProfileConceptsClone.remove(concept);
			}
		}
		finalConceptList.addAll(endProfileConceptsClone);
		return finalConceptList;
	}

	private Vector buildItemVector(LinkedList<Long> profileConcepts, LinkedList<Long> metadataConcepts) {
		HashSet<Long> metadataConceptsHashSet=new HashSet<>();
		metadataConceptsHashSet.addAll(metadataConcepts);
		Vector denseVector= new DenseVector(profileConcepts.size());
		Iterator<Long> profileConceptIter=profileConcepts.iterator();
		for (int i = 0; i < profileConcepts.size(); i++) {
			if(metadataConceptsHashSet.contains(profileConceptIter.next()))
				denseVector.setQuick(i, 1);
		}
		return denseVector;
	}


	private HashSet<Long>  addMetadataConceptsFromDistribution(
			LinkedList<Long> oldProfileConcepts, LinkedList<Long> itemConcepts, SlidingWindowCountMinSketch sketch, int numTrains) {
		
		
		
		HashSet<Long> finalConcepts =new  HashSet<>();
		finalConcepts.addAll(oldProfileConcepts);
		finalConcepts.addAll(itemConcepts);
		sketch.updateCounter(numTrains+1);
		
		for (Long concept : itemConcepts) {
			sketch.add(concept);
		}
		
		
		for (Iterator<Long> iterator = finalConcepts.iterator(); iterator
				.hasNext();) {
			
			
			Long concept = (Long) iterator.next();
			
			if(sketch.estimateCount(concept)<this.numRolling)
				iterator.remove();
			
		}
		
		
		return finalConcepts;
		
		
		
	}

	

	@Override
	public int getHyperParametersSize() {
		
		return 0;
	}
	

	@Override
	public String toString(){
		return "MetadataPredictor "+this.learningStrategy.toString();
	}

	@Override
	public boolean hasHyperParameters() {
		
		return false;
	}

	@Override
	public boolean hasProbabilityPrediction() {
		return false;
	}

	@Override
	public boolean hasMetadataPredictor() {
		return true;
	}

	@Override
	public boolean hasBiasPredictor() {
		return false;
	}

	@Override
	public boolean hasUserHistory() {
		
		return false;
	}

	@Override
	public boolean saveItemMetadata() {
		
		return false;
	}

	@Override
	public void updateItemProbabilityVector(
			UserTrainEvent gamma, UserProfile oldUserProfile,
			long itemId, String rating) {
		
		
	}

	@Override
	public void setLearningRateStrategy(LearningRateStrategy strategy) {
		this.learningStrategy=strategy;
		
	}

	@Override
	public SlidingWindowCountMinSketch buildMetadataSketch() {
		return  new SlidingWindowCountMinSketch(
					this.depth, this.width,
					SEED, this.numberOfSegments,
					this.windowLenght, this.hash_a);
		
	}

	

	

	

}
