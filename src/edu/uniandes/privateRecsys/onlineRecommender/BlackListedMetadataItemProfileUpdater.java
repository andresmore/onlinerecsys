package edu.uniandes.privateRecsys.onlineRecommender;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.logging.Logger;

import org.apache.mahout.cf.taste.common.TasteException;
import org.apache.mahout.math.Vector;

import edu.uniandes.privateRecsys.onlineRecommender.exception.PrivateRecsysException;
import edu.uniandes.privateRecsys.onlineRecommender.factorModelRepresentation.FactorUserItemRepresentation;
import edu.uniandes.privateRecsys.onlineRecommender.factorModelRepresentation.ItemProfile;
import edu.uniandes.privateRecsys.onlineRecommender.factorModelRepresentation.UserProfile;
import edu.uniandes.privateRecsys.onlineRecommender.factorModelRepresentation.VectorProjector;
import edu.uniandes.privateRecsys.onlineRecommender.vo.UserTrainEvent;

public class BlackListedMetadataItemProfileUpdater implements IItemProfileUpdater {
	private final static Logger LOG = Logger.getLogger(BlackListedMetadataItemProfileUpdater.class
		      .getName());
	


	private UserModelTrainerPredictor predictor;
	private HashSet<Long> blackListedContepts;
	public BlackListedMetadataItemProfileUpdater(UserModelTrainerPredictor predictor, HashSet<Long> blacklistedConcepts) throws PrivateRecsysException{
		this.predictor=predictor;
		
		if(!this.predictor.hasMetadataPredictor()){
			throw new PrivateRecsysException("Predictor is invalid with metadata blacklist");
		}
		this.blackListedContepts=blacklistedConcepts;
	}
	/* (non-Javadoc)
	 * @see edu.uniandes.privateRecsys.onlineRecommender.IItemProfileUpdater#processEvent(edu.uniandes.privateRecsys.onlineRecommender.vo.EventVO, edu.uniandes.privateRecsys.onlineRecommender.factorModelRepresentation.FactorUserItemRepresentation)
	 */
	@Override
	public void processEvent(UserTrainEvent event,
			FactorUserItemRepresentation userItemRep,  UserProfile oldUserProfile) throws TasteException {
	
		
		long itemId = event.getItemId();

		String rating = event.getRating();
		
		LinkedList<Long> metadataConcepts=ConceptBreaker.breakConcepts(event.getMetadata());
		for (Long conceptId : metadataConcepts) {
			if(this.blackListedContepts.contains(conceptId))
				return;
		}
		
		if (predictor.saveItemMetadata())
			userItemRep.saveItemMetadata(itemId, event.getMetadata());

		if (predictor.hasProbabilityPrediction() ) {

			predictor.updateItemProbabilityVector(event, oldUserProfile,
					itemId, rating);

		}
	}
	
	
	
	

}
