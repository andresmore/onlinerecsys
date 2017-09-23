package edu.uniandes.privateRecsys.onlineRecommender;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Observable;
import java.util.Observer;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import org.apache.mahout.cf.taste.common.TasteException;
import org.jfree.util.Log;

import edu.uniandes.privateRecsys.onlineRecommender.Evaluationtesters.PlistaDataset;
import edu.uniandes.privateRecsys.onlineRecommender.exception.PrivateRecsysException;
import edu.uniandes.privateRecsys.onlineRecommender.factorModelRepresentation.FactorUserItemRepresentation;
import edu.uniandes.privateRecsys.onlineRecommender.factorModelRepresentation.IncrementalFactorUserItemRepresentation;
import edu.uniandes.privateRecsys.onlineRecommender.jsonParserEvents.PListaEventCollector;
import edu.uniandes.privateRecsys.onlineRecommender.jsonParserEvents.PlistaJsonEventCreator;
import edu.uniandes.privateRecsys.onlineRecommender.ratingScale.RatingScale;
import edu.uniandes.privateRecsys.onlineRecommender.vo.FileEvent;
import edu.uniandes.privateRecsys.onlineRecommender.vo.IRPrecisionError;
import edu.uniandes.privateRecsys.onlineRecommender.vo.UserTrainEvent;


public class ModelEvaluator {
	
	
	private final static Logger LOG = Logger.getLogger(PrivateRecommenderParallelTrainer.class
		      .getName()); 
	
	public static double evaluateModel(File testSet, RatingScale scale,
			UserModelTrainerPredictor predictor,
			int minTraining) throws IOException, PrivateRecsysException,
			TasteException {
		Log.info("Evaluating error for model with file " + testSet);
		FileEventCreator testEv = new FileEventCreator(testSet, -1, -1);
        PredictionProfiler.reset();
		RMSE_Evaluator rmseEval = new RMSE_Evaluator(predictor, scale, minTraining);
		testEv.addObserver(rmseEval);
		testEv.startEvents();
		rmseEval.shutdownThread();
		boolean finished;
		try {
			finished = rmseEval.forceShutdown();
			if (!finished) {
				throw new TasteException(
						"Evaluation failed - not completed Executed tasks: "
								+ rmseEval.numExecutedTasks());
			}
			
			
			if((rmseEval.getNumEvals()+rmseEval.getRandEvals())!=rmseEval.getNumSubmitedTasks()){
				throw new TasteException(
						"Evaluation failed -  numEvaluated not as  numSubmitedTasks: "
								+ (rmseEval.getNumEvals()+rmseEval.getRandEvals())+"!="+rmseEval.getNumSubmitedTasks());
			}
		} catch (InterruptedException e) {
			throw new TasteException(
					"Evaluation failed - not completed Executed tasks: "
							+ rmseEval.numExecutedTasks());
		}

	
		PredictionProfiler.getInstance().printStats();
LOG.info("MAE Error for model with file "+testSet+" is "+rmseEval.getMAE()+" with "+rmseEval.getNumEvals()+" predictions (rand: "+rmseEval.getRandEvals()+", hybrid: "+rmseEval.getNumHybridEvals()+")");
LOG.info("RMSE Error for model with file "+testSet+" is "+rmseEval.getRMSE()+" with "+rmseEval.getNumEvals()+" predictions (rand: "+rmseEval.getRandEvals()+", hybrid: "+rmseEval.getNumHybridEvals()+")");
return rmseEval.getRMSE();
}
	
	
	
	public static IRPrecisionError evaluateModelIR(String testSet, RatingScale scale,
			
			
			FactorUserItemRepresentation userItemRep, TopNRecommender topNRecommender, int numMinTrains, int N, boolean preloadTest) throws IOException,
			TasteException {
		
		Log.info("Evaluating IR errors ");
		

		Set<Long> users = userItemRep.getUsersId();
		if (users != null) {
			
		
			
			TopNPredictorParallelCalculator parallel= new TopNPredictorParallelCalculator(users,userItemRep,topNRecommender,testSet);
			
			return parallel.calculateIRMetrics(numMinTrains, N,preloadTest);
		}
		return null;
	}

}
