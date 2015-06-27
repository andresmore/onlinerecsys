package edu.uniandes.privateRecsys.onlineRecommender;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

import org.apache.mahout.cf.taste.common.TasteException;
import org.apache.mahout.math.Vector;

import edu.uniandes.privateRecsys.onlineRecommender.exception.PrivateRecsysException;
import edu.uniandes.privateRecsys.onlineRecommender.factorModelRepresentation.FactorUserItemRepresentation;
import edu.uniandes.privateRecsys.onlineRecommender.factorModelRepresentation.ItemProfile;
import edu.uniandes.privateRecsys.onlineRecommender.factorModelRepresentation.UserProfile;
import edu.uniandes.privateRecsys.onlineRecommender.factorModelRepresentation.VectorProjector;
import edu.uniandes.privateRecsys.onlineRecommender.vo.UserTrainEvent;

public class ThresholdItemProfileUpdater implements IItemProfileUpdater {
	private final static Logger LOG = Logger.getLogger(ThresholdItemProfileUpdater.class
		      .getName());
	


	private UserModelTrainerPredictor predictor;



	private double threshold;



	private AtomicInteger numEventsThreshold;

	public ThresholdItemProfileUpdater(UserModelTrainerPredictor predictor, double threshold) throws PrivateRecsysException{
		this.predictor=predictor;
		
		if(!this.predictor.hasProbabilityPrediction()){
			throw new PrivateRecsysException("Predictor is invalid with no probability predictor");
		}
		if(threshold<0|| threshold >1){
			throw new PrivateRecsysException("Predictor is invalid with no probability predictor");
		}
		this.threshold=threshold;
		this.numEventsThreshold=new AtomicInteger(0);
	}
	/* (non-Javadoc)
	 * @see edu.uniandes.privateRecsys.onlineRecommender.IItemProfileUpdater#processEvent(edu.uniandes.privateRecsys.onlineRecommender.vo.EventVO, edu.uniandes.privateRecsys.onlineRecommender.factorModelRepresentation.FactorUserItemRepresentation)
	 */
	@Override
	public void processEvent(UserTrainEvent event,
			FactorUserItemRepresentation userItemRep,  UserProfile oldUserProfile) throws TasteException {
	
		long userId = event.getUserId();
		long itemId = event.getItemId();

		String rating = event.getRating();
		
		UserProfile userProfile=userItemRep.getPrivateUserProfile(userId);
		ItemProfile itemProfile=userItemRep.getPrivateItemProfile(itemId);
		
		if(userProfile.getProfileForScale(rating).dot(itemProfile.getProbabilityVector())>threshold){
			this.numEventsThreshold.incrementAndGet();
			return;
		}
		if (predictor.saveItemMetadata())
			userItemRep.saveItemMetadata(itemId, event.getMetadata());

		if (predictor.hasProbabilityPrediction() ) {

			predictor.updateItemProbabilityVector(event, oldUserProfile,
					itemId, rating);

		}
	}
	
	public int getNumEventsThreshold(){
		return numEventsThreshold.get();
	}
	
	
	
	

}
