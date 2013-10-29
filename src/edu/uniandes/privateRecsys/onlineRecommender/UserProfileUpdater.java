package edu.uniandes.privateRecsys.onlineRecommender;

import java.util.HashMap;
import java.util.logging.Logger;

import org.apache.mahout.cf.taste.common.TasteException;
import org.apache.mahout.math.Vector;
import edu.uniandes.privateRecsys.onlineRecommender.factorModelRepresentation.FactorUserItemRepresentation;
import edu.uniandes.privateRecsys.onlineRecommender.factorModelRepresentation.ItemProfile;
import edu.uniandes.privateRecsys.onlineRecommender.factorModelRepresentation.UserProfile;
import edu.uniandes.privateRecsys.onlineRecommender.factorModelRepresentation.VectorProjector;
import edu.uniandes.privateRecsys.onlineRecommender.vo.UserTrainEvent;

public class UserProfileUpdater implements IUserProfileUpdater {

	private final static Logger LOG = Logger.getLogger(UserProfileUpdater.class
		      .getName());

	/* (non-Javadoc)
	 * @see edu.uniandes.privateRecsys.onlineRecommender.IUserProfileUpdater#processEvent(edu.uniandes.privateRecsys.onlineRecommender.vo.EventVO, edu.uniandes.privateRecsys.onlineRecommender.factorModelRepresentation.FactorUserItemRepresentation)
	 */
	@Override
	public UserProfile processEvent(UserTrainEvent event,
			FactorUserItemRepresentation userItemRep,double gamma) throws TasteException {
		
		long itemId=event.getItemId();
		long userId=event.getUserId();
		String rating=event.getRating();
		
		
		
		ItemProfile itemProfile=userItemRep.getPrivateItemProfile(itemId);
		Vector itemVector = itemProfile.getVector();
		UserProfile oldUserPrivate=userItemRep.getPrivateUserProfile(userId);
		
		Vector biasVector=oldUserPrivate.getUserBias();
		if(oldUserPrivate!=null){
			double initPrediction=calculatePrediction(itemVector,oldUserPrivate.getUserProfiles(),userItemRep.getRatingScale().getScale(),biasVector);
	    int numTrains=userItemRep.getNumberTrainsUser(userId)+1;
	   
		
		
		String[] ratingScale=userItemRep.getRatingScale().getScale();
		HashMap<String, Vector> trainedProfiles= new HashMap<>();
		
		Vector updatedBiasVector=biasVector.clone();
		for (int i = 0; i < ratingScale.length; i++) {
			Vector privateVector=oldUserPrivate.getProfileForScale(ratingScale[i]);
			int prob=ratingScale[i].equals(rating)?1:0;
			
			double dotProb=privateVector.dot(itemVector);
			double modelPrediction=dotProb*biasVector.getQuick(i);
			double error=prob-modelPrediction;
			modelPrediction=gamma*(error);
			Vector privateVectorMult=itemVector.times(modelPrediction);
			privateVector=privateVector.plus(privateVectorMult);
			
			trainedProfiles.put(ratingScale[i], privateVector);
			updatedBiasVector.setQuick(i, gamma*error*dotProb);
			
		}
		updatedBiasVector=VectorProjector.projectVectorIntoSimplex(updatedBiasVector);
		trainedProfiles=VectorProjector.projectUserProfileIntoSimplex(trainedProfiles,ratingScale, itemVector.size());
		userItemRep.updatePrivateTrainedProfile(userId,trainedProfiles,updatedBiasVector);
		double endPrediction=calculatePrediction(itemVector,trainedProfiles,userItemRep.getRatingScale().getScale(),updatedBiasVector);
		//System.out.println("UserUpdater: Train was "+event.getRating()+", initPrediction="+initPrediction+", endPrediction="+endPrediction);
		LOG.fine("UserUpdater: Train was"+event.getRating()+", initPrediction="+initPrediction+", endPrediction="+endPrediction);
		
		}
		return oldUserPrivate; 
		
		
		
		
	}
	public double calculatePrediction(Vector itemVector,HashMap<String, Vector> trainedProfiles, String[] ratingScale, Vector userBias ){
		double prediction=0;
	
		
		double sumProb=0;
		for (int i = 0; i < ratingScale.length; i++) {
			Vector userVector = trainedProfiles.get(ratingScale[i]);
			double dot = userBias.getQuick(i);//userVector.dot(itemVector);//*
			sumProb+=dot;
			prediction += dot * Double.parseDouble(ratingScale[i]);
			
		}
		System.out.println(sumProb);
		return prediction;
	}
}
