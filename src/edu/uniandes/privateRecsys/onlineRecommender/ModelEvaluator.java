package edu.uniandes.privateRecsys.onlineRecommender;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
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
import edu.uniandes.privateRecsys.onlineRecommender.vo.IRPrecisionError;


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

	
//System.out.println("RMSE is :"+rmseEval.getRMSE());
//System.out.println("NumEvals total "+rmseEval.getNumEvals());
//System.out.println("RandomEvaluations total "+rmseEval.getRandEvals());
		PredictionProfiler.getInstance().printStats();
LOG.info("MAE Error for model with file "+testSet+" is "+rmseEval.getMAE()+" with "+rmseEval.getNumEvals()+" predictions (rand: "+rmseEval.getRandEvals()+", hybrid: "+rmseEval.getNumHybridEvals()+")");
LOG.info("RMSE Error for model with file "+testSet+" is "+rmseEval.getRMSE()+" with "+rmseEval.getNumEvals()+" predictions (rand: "+rmseEval.getRandEvals()+", hybrid: "+rmseEval.getNumHybridEvals()+")");
return rmseEval.getRMSE();
}

	@Deprecated
	public static IRPrecisionError evaluatePlistaModel(
			PlistaDataset plistaDataset, RatingScale scale,
			
			FactorUserItemRepresentation userItemRep, TopNRecommender topNRecommender) throws IOException,
			TasteException {
		
		Log.info("Evaluating IR error for plista files ");
		

		HashSet<Long> users = ((IncrementalFactorUserItemRepresentation) userItemRep)
				.getRestrictedUserIds();
		if (users != null) {
			PlistaJsonEventCreator plistaEventCreator = new PlistaJsonEventCreator(
					plistaDataset.getDirectory(), 26, 30,
					plistaDataset.getPrefixes());
			PListaEventCollector collector = new PListaEventCollector(users,
					plistaEventCreator);
			plistaEventCreator.startEvents();
		
		
			
			TopNPredictorParallelCalculator parallel= new TopNPredictorParallelCalculator(users,userItemRep,topNRecommender,null);
			return parallel.calculateIRMetrics();
		}
		return null;
	}
	
	public static IRPrecisionError evaluateModelIR(String testSet, RatingScale scale,
			
			
			FactorUserItemRepresentation userItemRep, TopNRecommender topNRecommender) throws IOException,
			TasteException {
		
		Log.info("Evaluating IR errors ");
		

		Set<Long> users = userItemRep.getUsersId();
		if (users != null) {
			
		
			
			TopNPredictorParallelCalculator parallel= new TopNPredictorParallelCalculator(users,userItemRep,topNRecommender,testSet);
			return parallel.calculateIRMetrics();
		}
		return null;
	}

}
