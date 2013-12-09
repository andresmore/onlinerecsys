package edu.uniandes.privateRecsys.onlineRecommender;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;

import org.apache.commons.math3.distribution.BetaDistribution;
import org.apache.commons.math3.distribution.PoissonDistribution;
import org.apache.mahout.cf.taste.common.TasteException;
import org.apache.mahout.math.DenseVector;
import org.apache.mahout.math.Vector;
import org.apache.mahout.math.VectorBinaryAggregate;
import org.apache.mahout.math.function.Functions;
import org.uncommons.maths.random.PoissonGenerator;

import edu.uniandes.privateRecsys.onlineRecommender.factorModelRepresentation.FactorUserItemRepresentation;
import edu.uniandes.privateRecsys.onlineRecommender.factorModelRepresentation.UserProfile;
import edu.uniandes.privateRecsys.onlineRecommender.utils.PrivateRandomUtils;
import edu.uniandes.privateRecsys.onlineRecommender.vo.Prediction;
import edu.uniandes.privateRecsys.onlineRecommender.vo.UserTrainEvent;
/**
 * Class that trains and samples a metadata model for the user
 * @author Andres M
 *
 */
public class MetadataPredictor implements UserModelTrainerPredictor {

	private FactorUserItemRepresentation model;
	
	
	//private PoissonDistribution samplingDistribution= new PoissonDistribution(PrivateRandomUtils.getCurrentRandomGenerator(), 0.03, PoissonDistribution.DEFAULT_EPSILON, PoissonDistribution.DEFAULT_MAX_ITERATIONS);
	private PoissonGenerator generator= new PoissonGenerator(0.03, PrivateRandomUtils.getCurrentRandomGenerator());

	private HashSet<Character> separators;

	public MetadataPredictor(){
		this.separators= new HashSet<>(4);
		separators.add('{');
		separators.add('}');
		separators.add(',');
		separators.add(':');
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
		LinkedList<String> profileConcepts=trainedMetadataProfiles.getIncludedConcepts();
		HashMap<String,Vector> profiles=trainedMetadataProfiles.getTrainedProfiles();
		
		
		HashSet<String> metadataConcepts=event.getMetadata();
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
		
		return Prediction.createPrediction(userId, itemId, prediction);
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
			double gamma, UserMetadataInfo trainedMetadataProfiles) {
		String rating=event.getRating();
		
		LinkedList<String> profileConcepts=trainedMetadataProfiles.getIncludedConcepts();
		HashMap<String,Vector> profiles=trainedMetadataProfiles.getTrainedProfiles();
		
		HashSet<String> metadataConcepts=event.getMetadata();
		int initSize=profileConcepts.size();
		addMetadataConceptsFromDistribution(profileConcepts,metadataConcepts);
		int endSize=profileConcepts.size();
		HashMap<String, Vector> extendedProfiles=increaseSizeMetadataConcepts(profiles, endSize-initSize);
		Vector itemVector=buildItemVector(profileConcepts,metadataConcepts);
		UserMetadataInfo profile=new UserMetadataInfo(profileConcepts);
		
		for (String ratingKey : extendedProfiles.keySet()) {	
			Vector privateVector=extendedProfiles.get(ratingKey);
			int prob=ratingKey.equals(rating)?1:0;
			
			double dotProb=privateVector.dot(itemVector);
			
			double loss=prob-dotProb;
			
			double multiplicator=gamma*(loss);
			Vector privateVectorMult=itemVector.times(multiplicator);
			Vector result=privateVector.plus(privateVectorMult);
			
			
			profile.addMetadataVector(result, ratingKey);
		}
		
		
		return profile;
		
		
	}

	private Vector buildItemVector(LinkedList<String> profileConcepts, HashSet<String> metadataConcepts) {
		
		Vector denseVector= new DenseVector(profileConcepts.size());
		for (int i = 0; i < profileConcepts.size(); i++) {
			if(metadataConcepts.contains(profileConcepts.get(i)))
				denseVector.setQuick(i, 1);
		}
		return denseVector;
	}

	private HashMap<String, Vector> increaseSizeMetadataConcepts(HashMap<String, Vector> profiles,
			int howMany) {
		HashMap<String, Vector> ret=  new HashMap<String, Vector>();
		if(howMany==0)
			return profiles;
		for (String key : profiles.keySet()) {
			Vector metadataVector=profiles.get(key);
			Vector extendedVector= new DenseVector(metadataVector.size()+howMany);
			extendedVector.viewPart(0, metadataVector.size()).assign(metadataVector);
			extendedVector.viewPart(metadataVector.size(), howMany).assign(PrivateRandomUtils.normalRandom(0, 1, howMany));
			ret.put(key, extendedVector);
		}
		return ret;
		
	}

	private void addMetadataConceptsFromDistribution(
			LinkedList<String> profileConcepts, HashSet<String> metadataConcepts) {
		metadataConcepts.removeAll(profileConcepts);
		
		int[] add=new int[metadataConcepts.size()];
		for (int i = 0; i < add.length; i++) {
			add[i]=this.generator.nextValue();
		}
		int i=0;
		for (String concept : metadataConcepts) {
		 if(add[i]>0)	 
			profileConcepts.add(concept);
		 i++;
		}
		
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
