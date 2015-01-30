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
			FactorUserItemRepresentation userItemRep,  UserProfile oldUserProfile) throws TasteException {
	
		
		long itemId = event.getItemId();

		String rating = event.getRating();

		if (predictor.saveItemMetadata())
			userItemRep.saveItemMetadata(itemId, event.getMetadata());

		if (predictor.hasProbabilityPrediction()) {

			predictor.updateItemProbabilityVector(event, oldUserProfile,
					itemId, rating);

		}
	}
	
	
	
	

}
