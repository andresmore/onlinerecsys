package edu.uniandes.privateRecsys.onlineRecommender;

import java.util.logging.Logger;

import org.apache.mahout.cf.taste.common.TasteException;
import org.apache.mahout.math.Vector;

import edu.uniandes.privateRecsys.onlineRecommender.factorModelRepresentation.FactorUserItemRepresentation;
import edu.uniandes.privateRecsys.onlineRecommender.factorModelRepresentation.ItemProfile;
import edu.uniandes.privateRecsys.onlineRecommender.factorModelRepresentation.UserProfile;
import edu.uniandes.privateRecsys.onlineRecommender.factorModelRepresentation.VectorProjector;
import edu.uniandes.privateRecsys.onlineRecommender.vo.UserTrainEvent;

public class SingleVectorItemProfileUpdaterWithRegularization implements IItemProfileUpdater {
	private final static Logger LOG = Logger.getLogger(SingleVectorItemProfileUpdaterWithRegularization.class
		      .getName());
	
	//private static double REGULARIZATION_CONSTANT=0.02;
	
	

	public SingleVectorItemProfileUpdaterWithRegularization() {
		
	}

	/* (non-Javadoc)
	 * @see edu.uniandes.privateRecsys.onlineRecommender.IItemProfileUpdater#processEvent(edu.uniandes.privateRecsys.onlineRecommender.vo.EventVO, edu.uniandes.privateRecsys.onlineRecommender.factorModelRepresentation.FactorUserItemRepresentation)
	 */
	@Override
	public void processEvent(UserTrainEvent event,
			FactorUserItemRepresentation userItemRep, double gamma, UserProfile oldUserProfile) throws TasteException {
		
		long itemId = event.getItemId();
		long userId = event.getUserId();
		String rating = event.getRating();

		ItemProfile itemProfile = userItemRep.getPrivateItemProfile(itemId);
		Vector itemVector = itemProfile.getVector();

		double initPrediction = calculatePrediction(itemVector, oldUserProfile,
				userItemRep.getRatingScale().getScale());

		double loss = Double.parseDouble(rating) - initPrediction;

	

			Vector userVector = oldUserProfile.getProfileForScale(event
					.getRating());
			int prob = 1;

			double dotProd = itemVector.dot(userVector);

			double error = prob - dotProd;

			Vector mult = userVector.times(error);
			
			Vector toAdd=mult.times(gamma);

			Vector toProject = itemVector.plus(toAdd);
			
			Vector projected =VectorProjector
					.projectVectorIntoSimplex(toProject);
		
			double errorStep=prob-itemVector.dot(projected);
			double endPrediction = calculatePrediction(projected,
					oldUserProfile, userItemRep.getRatingScale().getScale());
			double stepLoss = Double.parseDouble(rating) - endPrediction;
			
				userItemRep.updateItemVector(itemId, projected);
			

	
		
		
		
		
		
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
