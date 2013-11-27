package edu.uniandes.privateRecsys.onlineRecommender;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;

import org.apache.commons.math3.distribution.BetaDistribution;
import org.apache.commons.math3.distribution.PoissonDistribution;
import org.apache.mahout.cf.taste.common.TasteException;
import org.apache.mahout.math.Vector;

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
	
	
	private PoissonDistribution samplingDistribution= new PoissonDistribution(PrivateRandomUtils.getCurrentRandomGenerator(), 0.03, PoissonDistribution.DEFAULT_EPSILON, PoissonDistribution.DEFAULT_MAX_ITERATIONS);


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
	public Prediction calculatePrediction(long itemId, long userId,
			int minTrains) throws TasteException {
		//TODO
		
		return Prediction.createPrediction(userId, itemId, 1);
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
		
		String metadataVector=event.getMetadata();
		LinkedList<String> profileConcepts=trainedMetadataProfiles.getIncludedConcepts();
		
		HashSet<String> metadataConcepts=breakConcepts(metadataVector);
		int initSize=profileConcepts.size();
		addMetadataConceptsFromDistribution(profileConcepts,metadataConcepts);
		
		
		
		
		 return trainedMetadataProfiles;
		
		
	}

	private void addMetadataConceptsFromDistribution(
			LinkedList<String> profileConcepts, HashSet<String> metadataConcepts) {
		metadataConcepts.removeAll(profileConcepts);
		int[] add=samplingDistribution.sample(metadataConcepts.size());
		int i=0;
		for (String concept : metadataConcepts) {
		 if(add[i]>0)	 
			profileConcepts.add(concept);
		 i++;
		}
		
	}

	private HashSet<String> breakConcepts(String metadataVector) {
		HashSet<String> concepts= new HashSet<String>();
		
		StringBuilder builder= new StringBuilder();
		
		for (int i = 0; i < metadataVector.length(); i++) {
			char at= metadataVector.charAt(i);
			if( this.separators.contains(at) ){
				if(builder.length()>0)
					concepts.add(builder.toString());
				
					builder= new StringBuilder();
				
			}
			else{
				builder.append(at);
			}
			
		}
		if(builder.length()>0)
			concepts.add(builder.toString());
		
		return concepts;
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
