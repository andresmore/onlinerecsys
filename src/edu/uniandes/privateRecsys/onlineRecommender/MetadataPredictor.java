package edu.uniandes.privateRecsys.onlineRecommender;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;

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

	private static int NUM_INCLUSION_ROLLING=3;
	
	private int limitSize=10;
	
	private FactorUserItemRepresentation model;
	
	public MetadataPredictor(int limitSize){
		this.limitSize=limitSize;
	}
	
	@Override
	public void setModelRepresentation(FactorUserItemRepresentation model) {
		this.model=model;

	}

	@Override
public  Prediction calculatePrediction(UserTrainEvent event, int minTrains) throws TasteException{
		
		long userId=event.getUserId();
		long itemId=event.getItemId();
		
		UserProfile user = model
				.getPrivateUserProfile(userId);
		UserMetadataInfo trainedMetadataProfiles=user.getMetadataInfo();
		LinkedList<Long> profileConcepts=trainedMetadataProfiles.getIncludedConcepts();
		HashMap<String,Vector> profiles=trainedMetadataProfiles.getTrainedProfiles();
		
		
		HashSet<Long> metadataConcepts=event.getMetadata();
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
	public HashMap<String, Vector> calculateProbabilityUpdate(double gamma,
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
	public Vector calculatehyperParamsUpdate(double gamma,UserTrainEvent event,Vector itemVector,
			HashMap<String, Vector> trainedProfiles,
			HashMap<String, BetaDistribution> biasVector,
			Vector oldHyperparameters, int numTrains) {
	
		return oldHyperparameters;
	}
	@Override
	public UserMetadataInfo calculateMetadataUpdate(UserTrainEvent event,
			double gamma, UserMetadataInfo trainedMetadataProfiles,int numTrains) {
		String rating=event.getRating();
		
		LinkedList<Long> profileConcepts=trainedMetadataProfiles.getIncludedConcepts();
		HashMap<String,Vector> profiles=trainedMetadataProfiles.getTrainedProfiles();
		SlidingWindowCountMinSketch sketch=trainedMetadataProfiles.getUserSketch();
		
		HashSet<Long> metadataConcepts=event.getMetadata();
		
		
		LinkedList<Long> endProfileConcepts=addMetadataConceptsFromDistribution(profileConcepts,metadataConcepts,sketch,numTrains);
		
		
		HashMap<String, Vector> extendedProfiles=modifyMetadataVectors(profiles,profileConcepts,endProfileConcepts);
		Vector itemVector=buildItemVector(endProfileConcepts,metadataConcepts);
		UserMetadataInfo profile=new UserMetadataInfo(endProfileConcepts,sketch);
		
		for (String ratingKey : extendedProfiles.keySet()) {	
		
			Vector privateVector=extendedProfiles.get(ratingKey);
			if (privateVector.size() > 0 && itemVector.size() > 0) {
				int prob = ratingKey.equals(rating) ? 1 : 0;

				double dotProb = Functions.SIGMOID.apply(privateVector.dot(itemVector));

				double loss = prob - dotProb;

				double multiplicator = gamma * (loss);
				Vector privateVectorMult = itemVector.times(multiplicator);
				Vector result = privateVector.plus(privateVectorMult);

				double dotProbEnd = Functions.SIGMOID.apply(result.dot(itemVector));

				profile.addMetadataVector(result, ratingKey);
			}
			else{
				profile.addMetadataVector(privateVector, ratingKey);
			}
		}
		
		
		return profile;
		
		
	}

	private HashMap<String, Vector> modifyMetadataVectors(HashMap<String, Vector> profiles,
			LinkedList<Long> profileConcepts,
			LinkedList<Long> endProfileConcepts) {
		
		HashMap<String, Vector> ret=  new HashMap<String, Vector>();
		
		//if(!endProfileConcepts.isEmpty())
			//System.out.println("paus");
		for (String key : profiles.keySet()) {
			Vector metadataVector=profiles.get(key);
			Vector extendedVector= new DenseVector(endProfileConcepts.size());
			
			Iterator<Long> iterator = profileConcepts.iterator();
			Iterator<Long> iterator2 = endProfileConcepts.iterator();
			int pos1=0;
			int pos2=0;
			boolean finished=false;
			while(!finished){
				
				if(iterator2.hasNext()){
					
					Long concept2=iterator2.next();
					boolean concept2Found=false;
					
					Long concept1=null;
					
					while(!concept2Found && iterator.hasNext()){
						concept1=iterator.next();
						if(concept1.equals(concept2)){
							extendedVector.set(pos2, metadataVector.get(pos1));
							concept2Found=true;
							pos2++;
						}	
						pos1++;
					}
					if(!iterator.hasNext()){
						finished=true;
					}
					
					
				}
				else{
					finished=true;
				}
				
					
			}
			
			
			extendedVector.viewPart(pos2, extendedVector.size()-pos2).assign(PrivateRandomUtils.normalRandom(0, 1, extendedVector.size()-pos2));
			ret.put(key, extendedVector);
		}
		return ret;
	}

	private Vector buildItemVector(LinkedList<Long> profileConcepts, HashSet<Long> metadataConcepts) {
		
		Vector denseVector= new DenseVector(profileConcepts.size());
		for (int i = 0; i < profileConcepts.size(); i++) {
			if(metadataConcepts.contains(profileConcepts.get(i)))
				denseVector.setQuick(i, 1);
		}
		return denseVector;
	}

//TODO: LIMIT SIZE OF INCLUDED CONCEPTS
	private LinkedList<Long>  addMetadataConceptsFromDistribution(
			LinkedList<Long> profileConcepts, HashSet<Long> metadataConcepts, SlidingWindowCountMinSketch sketch, int numTrains) {
		
		
		LinkedList<Long> endConcepts= new LinkedList<Long>();
		
		sketch.updateCounter(numTrains+1);
		
		for (Long concept : metadataConcepts) {
			sketch.add(concept);
		}
		
		endConcepts.addAll(profileConcepts);
		metadataConcepts.removeAll(profileConcepts);
		for (Iterator<Long> iterator = metadataConcepts.iterator(); iterator
				.hasNext()&&endConcepts.size()<limitSize;) {
			endConcepts.add(iterator.next());
		}
		
		
		//endConcepts.addAll(metadataConcepts);
		
		for (Iterator<Long> iterator = endConcepts.iterator(); iterator
				.hasNext();) {
			
			
			Long concept = (Long) iterator.next();
			
			if(sketch.estimateCount(concept)<NUM_INCLUSION_ROLLING)
				iterator.remove();
			
		}
		
		
		return endConcepts;
		
		
		
	}

	

	@Override
	public int getHyperParametersSize() {
		
		return 0;
	}
	

	@Override
	public String toString(){
		return "MetadataPredictor";
	}

	

	

	

}
