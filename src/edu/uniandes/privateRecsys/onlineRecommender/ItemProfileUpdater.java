package edu.uniandes.privateRecsys.onlineRecommender;

import java.util.HashMap;
import java.util.logging.Logger;

import org.apache.mahout.cf.taste.common.TasteException;
import org.apache.mahout.math.DenseVector;
import org.apache.mahout.math.Vector;

import edu.uniandes.privateRecsys.onlineRecommender.factorModelRepresentation.FactorUserItemRepresentation;
import edu.uniandes.privateRecsys.onlineRecommender.factorModelRepresentation.ItemProfile;
import edu.uniandes.privateRecsys.onlineRecommender.factorModelRepresentation.UserProfile;
import edu.uniandes.privateRecsys.onlineRecommender.factorModelRepresentation.VectorProjector;
import edu.uniandes.privateRecsys.onlineRecommender.vo.UserTrainEvent;

public class ItemProfileUpdater implements IItemProfileUpdater {
	private final static Logger LOG = Logger.getLogger(ItemProfileUpdater.class
		      .getName());
	

	public ItemProfileUpdater() {
		
	}

	/* (non-Javadoc)
	 * @see edu.uniandes.privateRecsys.onlineRecommender.IItemProfileUpdater#processEvent(edu.uniandes.privateRecsys.onlineRecommender.vo.EventVO, edu.uniandes.privateRecsys.onlineRecommender.factorModelRepresentation.FactorUserItemRepresentation)
	 */
	@Override
	public void processEvent(UserTrainEvent event,
			FactorUserItemRepresentation userItemRep, LearningRateStrategy strategy, UserProfile oldUserProfile) throws TasteException {
		
		long itemId=event.getItemId();
		long userId=event.getUserId();
		String rating=event.getRating();
		
		
		
		ItemProfile itemProfile=userItemRep.getPrivateItemProfile(itemId);
		Vector itemVector = itemProfile.getVector();
		//UserProfile user=userItemRep.getPublicUserProfile(userId);
		
		double initPrediction=calculatePrediction(itemVector,oldUserProfile,userItemRep.getRatingScale().getScale());
		
		int numTrains=userItemRep.getNumberTrainsItem(itemId)+1;
		//double lambda=(double)1/(double)numTrains*0.75;
		double lambda=strategy.getGammaForTime(event.getTimestamp());
		
		
		String[] ratingScale=userItemRep.getRatingScale().getScale();
		double sum= 0;
		double sumProb= 0;
		/*for (int i = 0; i < ratingScale.length; i++) {
			Vector userVector = oldUserProfile
					.getProfileForScale(ratingScale[i]);
			int prob = ratingScale[i].equals(rating) ? 1 : 0;

			double dotProd = itemVector.dot(userVector);
			sumProb += dotProd;
			sum += (prob - dotProd);

		}
		*/
		
		Vector userVectorO=oldUserProfile.getProfileForScale(rating);
		sum+=(1-itemVector.dot(userVectorO));
		Vector mult=userVectorO.times(sum).times(lambda);
		
		itemVector = itemVector.plus(mult);
		
		
		itemVector = VectorProjector
				.projectVectorIntoSimplex(itemVector);
		
		userItemRep.updateItemVector(itemId,itemVector);
		
		double endPrediction=calculatePrediction(itemVector,oldUserProfile,userItemRep.getRatingScale().getScale());
		
		
		
		
		LOG.fine("UserUpdater: Train was"+event.getRating()+", initPrediction="+initPrediction+", endPrediction="+endPrediction);
		
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
