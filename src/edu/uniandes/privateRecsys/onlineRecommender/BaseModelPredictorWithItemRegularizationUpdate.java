package edu.uniandes.privateRecsys.onlineRecommender;

import org.apache.mahout.cf.taste.common.TasteException;
import org.apache.mahout.math.Vector;

import edu.uniandes.privateRecsys.onlineRecommender.factorModelRepresentation.ItemProfile;
import edu.uniandes.privateRecsys.onlineRecommender.factorModelRepresentation.UserProfile;
import edu.uniandes.privateRecsys.onlineRecommender.factorModelRepresentation.VectorProjector;
import edu.uniandes.privateRecsys.onlineRecommender.vo.UserTrainEvent;

public class BaseModelPredictorWithItemRegularizationUpdate extends
		BaseModelPredictor {
	
	
	private double regulatizationConstant;
	public BaseModelPredictorWithItemRegularizationUpdate(double regularizationConstant){
		super();
		this.regulatizationConstant=regularizationConstant;
	}
	@Override
	public void updateItemProbabilityVector(
			UserTrainEvent event, UserProfile oldUserProfile,
			long itemId, String rating) {

		double gamma = this.learningRateStrategy
				.getGammaFromK(modelRepresentation.getNumberTrainsItem(event.getItemId()));
		try {
			ItemProfile itemProfile = modelRepresentation
					.getPrivateItemProfile(itemId);
			Vector itemVector = itemProfile.getProbabilityVector();


			double sum = 0;

			Vector userVector = oldUserProfile.getProfileForScale(rating);
			int prob = 1;

			double dotProd = itemVector.dot(userVector);

			sum += prob - dotProd;

			Vector userVectorO = oldUserProfile.getProfileForScale(rating);

			Vector mult = userVectorO.times(sum).times(gamma);

			Vector regularized = itemVector.times(
					regulatizationConstant);
			Vector toProject = itemVector.plus(mult).minus(regularized);

			Vector projected = VectorProjector
					.projectVectorIntoSimplex(toProject);

			

			modelRepresentation.updateItemVector(itemId, projected);
		} catch (TasteException e) {

			e.printStackTrace();
		}
			
			
		}
	
	@Override
	public String toString(){
		return "BaseModelPredictorWithItemRegularizationUpdate "+this.learningRateStrategy.toString()+" dim="+modelRepresentation.getfDimensions()+" reg "+this.regulatizationConstant ;
	}

}
