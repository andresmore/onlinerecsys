package edu.uniandes.privateRecsys.onlineRecommender;

import org.apache.mahout.cf.taste.common.TasteException;

import edu.uniandes.privateRecsys.onlineRecommender.exception.StopTrainingException;
import edu.uniandes.privateRecsys.onlineRecommender.factorModelRepresentation.FactorUserItemRepresentation;
import edu.uniandes.privateRecsys.onlineRecommender.factorModelRepresentation.UserProfile;
import edu.uniandes.privateRecsys.onlineRecommender.vo.UserTrainEvent;

public class ItemTrainLimitPrivacyAggregator implements IUserMaskingStrategy {


	private int T;
	
	public ItemTrainLimitPrivacyAggregator(int T) {
		
		this.T=T;
	  
	}
	

	/* (non-Javadoc)
	 * @see edu.uniandes.privateRecsys.onlineRecommender.IUserItemAggregator#aggregateEvent(edu.uniandes.privateRecsys.onlineRecommender.vo.EventVO, edu.uniandes.privateRecsys.onlineRecommender.factorModelRepresentation.FactorUserItemRepresentation)
	 */
	@Override
	public UserProfile maskProfile(UserProfile oldProfile,UserTrainEvent event,
			FactorUserItemRepresentation userItemRep) throws TasteException {
		
		long itemId=event.getItemId();
		
		
		int numTrains=userItemRep.getNumberTrainsItem(itemId)+1;
		if(numTrains>=T)
			throw new StopTrainingException("Item trains exeeded");
		
		return oldProfile;
		
	}

}
