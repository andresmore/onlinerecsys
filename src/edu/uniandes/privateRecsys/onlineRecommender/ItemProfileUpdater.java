package edu.uniandes.privateRecsys.onlineRecommender;

import java.util.logging.Logger;

import org.apache.mahout.cf.taste.common.TasteException;
import org.apache.mahout.math.Vector;

import edu.uniandes.privateRecsys.onlineRecommender.factorModelRepresentation.FactorUserItemRepresentation;
import edu.uniandes.privateRecsys.onlineRecommender.factorModelRepresentation.ItemProfile;
import edu.uniandes.privateRecsys.onlineRecommender.factorModelRepresentation.UserProfile;
import edu.uniandes.privateRecsys.onlineRecommender.factorModelRepresentation.VectorProjector;
import edu.uniandes.privateRecsys.onlineRecommender.vo.UserTrainEvent;

public class ItemProfileUpdater implements IItemProfileUpdater {
	private final static Logger LOG = Logger.getLogger(ItemProfileUpdater.class
		      .getName());
	


	private UserModelTrainerPredictor predictor;

	public ItemProfileUpdater(UserModelTrainerPredictor predictor){
		this.predictor=predictor;
	}
	/* (non-Javadoc)
	 * @see edu.uniandes.privateRecsys.onlineRecommender.IItemProfileUpdater#processEvent(edu.uniandes.privateRecsys.onlineRecommender.vo.EventVO, edu.uniandes.privateRecsys.onlineRecommender.factorModelRepresentation.FactorUserItemRepresentation)
	 */
	@Override
	public void processEvent(UserTrainEvent event,
			FactorUserItemRepresentation userItemRep, double gamma, UserProfile oldUserProfile) throws TasteException {
	
		
		
		long itemId=event.getItemId();
		
		String rating=event.getRating();
		
		if(predictor.saveItemMetadata())
			userItemRep.saveItemMetadata(itemId,event.getMetadata());
		
		if(predictor.hasProbabilityPrediction()){
		ItemProfile itemProfile=userItemRep.getPrivateItemProfile(itemId);
		
		
		Vector itemVector = itemProfile.getProbabilityVector();
		
		
		double initPrediction=calculatePrediction(itemVector,oldUserProfile,userItemRep.getRatingScale().getScale());
		//TODO: just checking, not a good idea if want to keep privacy
		double loss=Double.parseDouble(rating)-initPrediction;
		
		
		
		String[] ratingScale=userItemRep.getRatingScale().getScale();
		double sum= 0;
		
		for (int i = 0; i < ratingScale.length; i++) {
			Vector userVector = oldUserProfile
					.getProfileForScale(ratingScale[i]);
			int prob = ratingScale[i].equals(rating) ? 1 : 0;

			double dotProd = itemVector.dot(userVector);
			
			sum += prob - dotProd;

		}
		
		
		Vector userVectorO=oldUserProfile.getProfileForScale(rating);
	
		Vector mult=userVectorO.times(sum).times(gamma);
		
		Vector toProject = itemVector.plus(mult);
		
		
		Vector projected = VectorProjector
				.projectVectorIntoSimplex(toProject);
		
		double endPrediction=calculatePrediction(projected,oldUserProfile,userItemRep.getRatingScale().getScale());
		
		double stepLoss=Double.parseDouble(rating)-endPrediction;
		
		userItemRep.updateItemVector(itemId,projected);
			
		
		
		
		
		
		
		}
	}
	
	public double calculatePrediction(Vector itemVector,UserProfile oldUserProfile, String[] ratingScale ){
		double prediction=0;
	
		
		
		for (int i = 0; i < ratingScale.length; i++) {
			Vector userVector = oldUserProfile
					.getProfileForScale(ratingScale[i]);
			double dot = userVector.dot(itemVector);
			
			prediction += dot * Double.parseDouble(ratingScale[i]);
			
		}
		return prediction;
	}

}
