package edu.uniandes.privateRecsys.onlineRecommender;

import java.util.LinkedList;

import org.apache.commons.math3.distribution.BetaDistribution;
import org.apache.mahout.cf.taste.common.TasteException;
import org.apache.mahout.math.Vector;

import edu.uniandes.privateRecsys.onlineRecommender.factorModelRepresentation.FactorUserItemRepresentation;
import edu.uniandes.privateRecsys.onlineRecommender.factorModelRepresentation.UserProfile;
import edu.uniandes.privateRecsys.onlineRecommender.utils.PrivateRandomUtils;
import edu.uniandes.privateRecsys.onlineRecommender.vo.UserTrainEvent;

public class DifferentialPrivacyMaskingStrategy implements IUserMaskingStrategy {

	
	private double sigma=0;
	

	public DifferentialPrivacyMaskingStrategy( double epsilon) {
		if(epsilon==0){
			this.sigma=0;
		}
		else{
			this.sigma=1/epsilon;
		}
		
	}
	

	/* (non-Javadoc)
	 * @see edu.uniandes.privateRecsys.onlineRecommender.IUserItemAggregator#aggregateEvent(edu.uniandes.privateRecsys.onlineRecommender.vo.EventVO, edu.uniandes.privateRecsys.onlineRecommender.factorModelRepresentation.FactorUserItemRepresentation)
	 */
	@Override
	public UserProfile maskProfile(UserProfile privateUserProfile,UserTrainEvent event,
			FactorUserItemRepresentation userItemRep) throws TasteException {
		
		if(sigma==0){
			return privateUserProfile;
		}
	
		
		String rating=event.getRating();
		
					
		
		String[] ratingScale=userItemRep.getRatingScale().getScale();
		LinkedList<Vector> trainedProfiles= new LinkedList<>();
		LinkedList<BetaDistribution> distributions= new LinkedList<BetaDistribution>();
		LinkedList<Vector> trainedMetadataProfiles= new LinkedList<Vector>();
		UserMetadataInfo metadataInfo = privateUserProfile.getMetadataInfo();
		for (int i = 0; i < ratingScale.length; i++) {
			String scale = ratingScale[i];
			Vector privateVector=privateUserProfile.getProfileForScale(scale);
			
			if(privateUserProfile.getUserBias()!=null)
				distributions.add(privateUserProfile.getUserBias().get(rating));
			
			if(metadataInfo!=null)
				trainedMetadataProfiles.add(metadataInfo.getTrainedProfiles().get(rating));
			if(scale.equals(event.getRating())){
					Vector randVector=PrivateRandomUtils.laplaceRandom(0, sigma, privateVector.size());
					privateVector=privateVector.plus(randVector);
					
			}
			
			trainedProfiles.add(privateVector);
			
		} 
		
		
		return UserProfile.buildDenseProfile(trainedProfiles, userItemRep.getRatingScale(), 
				distributions, privateUserProfile.getHyperParameters(), metadataInfo==null?null: trainedMetadataProfiles, 
						metadataInfo==null?null:metadataInfo.getIncludedConcepts(), metadataInfo==null?null:metadataInfo.getUserSketch(), privateUserProfile.getUserHistory(), 0);
		
	}

}
