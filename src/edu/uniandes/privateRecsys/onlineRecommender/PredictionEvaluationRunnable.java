package edu.uniandes.privateRecsys.onlineRecommender;

import java.util.logging.Level;
import java.util.logging.Logger;

import edu.uniandes.privateRecsys.onlineRecommender.factorModelRepresentation.FactorUserItemRepresentation;
import edu.uniandes.privateRecsys.onlineRecommender.factorModelRepresentation.UserModelTrainerPredictor;
import edu.uniandes.privateRecsys.onlineRecommender.vo.Prediction;
import edu.uniandes.privateRecsys.onlineRecommender.vo.UserTrainEvent;

public class PredictionEvaluationRunnable implements Runnable {

	private UserTrainEvent event;
	private FactorUserItemRepresentation userItemRepresentation;
	
	private int minTrains;
	private RMSE_Evaluator rmse_Evaluator;
	private UserModelTrainerPredictor predictor;
	private final static Logger LOG = Logger.getLogger(PredictionEvaluationRunnable.class
		      .getName());

	public PredictionEvaluationRunnable(UserTrainEvent event,
			UserModelTrainerPredictor predictor, int minTrains, RMSE_Evaluator rmse_Evaluator) {
		this.event=event;
		this.predictor=predictor;
		
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
			Prediction prediction = predictor.calculatePrediction(itemId, userId,minTrains);
			rmse_Evaluator.notifyPrediction(event,prediction);

		} catch (Exception e) {
			LOG.log(Level.SEVERE, "Error making prediction", e);
			
			//e.printStackTrace();
		}
		

	}

	

}
