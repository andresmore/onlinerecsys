package edu.uniandes.privateRecsys.onlineRecommender;

import java.util.logging.Level;
import java.util.logging.Logger;

import edu.uniandes.privateRecsys.onlineRecommender.factorModelRepresentation.FactorUserItemRepresentation;
import edu.uniandes.privateRecsys.onlineRecommender.vo.Prediction;
import edu.uniandes.privateRecsys.onlineRecommender.vo.UserTrainEvent;

public class PredictionEvaluationRunnable implements Runnable {

	private UserTrainEvent event;
	
	
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
		
		
		long initialTime=System.nanoTime();
		
		try {
			Prediction prediction = predictor.calculatePrediction(event,minTrains);
			rmse_Evaluator.notifyPrediction(event,prediction);
			PredictionProfiler.getInstance().reportTimes(initialTime, System.nanoTime());
			event=null;

		} catch (Exception e) {
			LOG.log(Level.SEVERE, "Error making prediction", e);
			PredictionProfiler.getInstance().reportFailure();
			//e.printStackTrace();
		}
		
		

	}

	

}
