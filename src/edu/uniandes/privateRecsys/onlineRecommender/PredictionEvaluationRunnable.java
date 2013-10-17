package edu.uniandes.privateRecsys.onlineRecommender;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.lang.math.RandomUtils;
import org.apache.mahout.cf.taste.common.TasteException;
import org.apache.mahout.math.DenseVector;
import org.apache.mahout.math.Vector;

import edu.uniandes.privateRecsys.onlineRecommender.factorModelRepresentation.FactorUserItemRepresentation;
import edu.uniandes.privateRecsys.onlineRecommender.factorModelRepresentation.ItemProfile;
import edu.uniandes.privateRecsys.onlineRecommender.factorModelRepresentation.UserProfile;
import edu.uniandes.privateRecsys.onlineRecommender.ratingScale.RatingScale;
import edu.uniandes.privateRecsys.onlineRecommender.vo.Prediction;
import edu.uniandes.privateRecsys.onlineRecommender.vo.UserTrainEvent;

public class PredictionEvaluationRunnable implements Runnable {

	private UserTrainEvent event;
	private FactorUserItemRepresentation userItemRepresentation;
	private RatingScale ratingScale;
	private int minTrains;
	private RMSE_Evaluator rmse_Evaluator;
	private final static Logger LOG = Logger.getLogger(PredictionEvaluationRunnable.class
		      .getName());

	public PredictionEvaluationRunnable(UserTrainEvent event,
			FactorUserItemRepresentation userItemRepresentation,
			RatingScale scale, int minTrains, RMSE_Evaluator rmse_Evaluator) {
		this.event=event;
		this.userItemRepresentation=userItemRepresentation;
		this.ratingScale=scale;
		this.minTrains=minTrains;
		this.rmse_Evaluator=rmse_Evaluator;
	}

	@Override
	public void run() {
		long itemId = event.getItemId();
		long userId = event.getUserId();
		String rating = event.getRating();
		long time = event.getTime();
		
		try {
			Prediction prediction = userItemRepresentation.calculatePrediction(itemId, userId,minTrains);
			rmse_Evaluator.notifyPrediction(event,prediction);

		} catch (Exception e) {
			LOG.log(Level.SEVERE, "Error making prediction", e);
			
			//e.printStackTrace();
		}
		

	}

	

}
