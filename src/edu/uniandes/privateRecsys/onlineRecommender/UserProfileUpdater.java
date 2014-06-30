package edu.uniandes.privateRecsys.onlineRecommender;

import java.util.HashMap;
import java.util.logging.Logger;

import org.apache.commons.math3.distribution.BetaDistribution;
import org.apache.hadoop.hdfs.server.datanode.FSDatasetInterface.MetaDataInputStream;
import org.apache.mahout.cf.taste.common.TasteException;
import org.apache.mahout.math.Vector;

import edu.uniandes.privateRecsys.onlineRecommender.factorModelRepresentation.FactorUserItemRepresentation;
import edu.uniandes.privateRecsys.onlineRecommender.factorModelRepresentation.ItemProfile;
import edu.uniandes.privateRecsys.onlineRecommender.factorModelRepresentation.UserProfile;
import edu.uniandes.privateRecsys.onlineRecommender.metadata.MetadataDatasetConverter;
import edu.uniandes.privateRecsys.onlineRecommender.vo.UserTrainEvent;

public class UserProfileUpdater implements IUserProfileUpdater {

	private final static Logger LOG = Logger.getLogger(UserProfileUpdater.class
		      .getName());
	private UserModelTrainerPredictor predictor;
	
	
	public UserProfileUpdater(UserModelTrainerPredictor predictor){
		this.predictor=predictor;
	}
	
	/* (non-Javadoc)
	 * @see edu.uniandes.privateRecsys.onlineRecommender.IUserProfileUpdater#processEvent(edu.uniandes.privateRecsys.onlineRecommender.vo.EventVO, edu.uniandes.privateRecsys.onlineRecommender.factorModelRepresentation.FactorUserItemRepresentation)
	 */
	@Override
	public UserProfile processEvent(UserTrainEvent event,
			FactorUserItemRepresentation userItemRep) throws TasteException {
		
		long itemId=event.getItemId();
		long userId=event.getUserId();
		String rating=event.getRating();
		
		if(predictor.saveItemMetadata())
			userItemRep.saveItemMetadata(itemId,event.getMetadata());
		
		ItemProfile itemProfile=userItemRep.getPrivateItemProfile(itemId);
		Vector itemVector =null;
		if(itemProfile!=null)
			itemVector = itemProfile.getProbabilityVector();
		
		UserProfile oldUserPrivate=userItemRep.getPrivateUserProfile(userId);
		HashMap<String, BetaDistribution> biasVector=oldUserPrivate.getUserBias();
		Vector hyperParameterVector= oldUserPrivate.getHyperParameters();
		UserMetadataInfo metaInfo=oldUserPrivate.getMetadataInfo();
		
		if (oldUserPrivate != null) {
			
			
			
			
			
			

			String[] ratingScale = userItemRep.getRatingScale().getScale();
			
			Vector newHyperParams=predictor.calculatehyperParamsUpdate(event,itemVector, oldUserPrivate.getUserProfiles(),biasVector,hyperParameterVector,oldUserPrivate.getNumTrains()+1);
			
			
			HashMap<String, Vector> trainedProfiles = predictor.calculateProbabilityUpdate(
					event, rating, itemVector, oldUserPrivate, ratingScale);
			HashMap<String, BetaDistribution> biasVectorUpdate = predictor.calculatePriorsUpdate(event, biasVector, ratingScale);
			
			UserMetadataInfo metadataInfo=predictor.calculateMetadataUpdate(event, metaInfo,oldUserPrivate.getNumTrains()+1);
			
			userItemRep.updatePrivateTrainedProfile(userId, trainedProfiles,
					biasVectorUpdate,newHyperParams,metadataInfo);
		
			userItemRep.addUserEvent(userId,itemId,rating);
			
		}
		return oldUserPrivate;
		
		
		
		
	}
	
	@Override
	public String toString() {

		return predictor.toString();
	}
}
