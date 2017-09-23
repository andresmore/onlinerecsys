package edu.uniandes.privateRecsys.onlineRecommender.Evaluationtesters;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.logging.Logger;

import org.apache.mahout.cf.taste.common.TasteException;

import edu.uniandes.privateRecsys.onlineRecommender.BaseModelPredictorWithHistory;
import edu.uniandes.privateRecsys.onlineRecommender.BaseModelPredictorWithItemRegularizationUpdate;
import edu.uniandes.privateRecsys.onlineRecommender.BaseModelPredictorWithItemRegularizationUpdateWithHistory;
import edu.uniandes.privateRecsys.onlineRecommender.FileEventCreator;
import edu.uniandes.privateRecsys.onlineRecommender.IItemProfileUpdater;
import edu.uniandes.privateRecsys.onlineRecommender.IUserMaskingStrategy;
import edu.uniandes.privateRecsys.onlineRecommender.ItemProfileUpdater;
import edu.uniandes.privateRecsys.onlineRecommender.LearningRateStrategy;
import edu.uniandes.privateRecsys.onlineRecommender.ModelEvaluator;
import edu.uniandes.privateRecsys.onlineRecommender.NoMaskingStrategy;
import edu.uniandes.privateRecsys.onlineRecommender.PrivateRecommenderParallelTrainer;
import edu.uniandes.privateRecsys.onlineRecommender.TopNRecommenderFactorModel;
import edu.uniandes.privateRecsys.onlineRecommender.UserModelTrainerPredictor;
import edu.uniandes.privateRecsys.onlineRecommender.UserProfileUpdater;
import edu.uniandes.privateRecsys.onlineRecommender.exception.PrivateRecsysException;
import edu.uniandes.privateRecsys.onlineRecommender.factorModelRepresentation.FactorUserItemRepresentation;
import edu.uniandes.privateRecsys.onlineRecommender.factorModelRepresentation.IncrementalFactorUserItemRepresentation;
import edu.uniandes.privateRecsys.onlineRecommender.ratingScale.OrdinalRatingScale;
import edu.uniandes.privateRecsys.onlineRecommender.ratingScale.RatingScale;
import edu.uniandes.privateRecsys.onlineRecommender.vo.ErrorReport;
import edu.uniandes.privateRecsys.onlineRecommender.vo.IRPrecisionError;

public class OnlineRecommenderIRTester extends AbstractRecommenderTester {

	
	
	private final static Logger LOG = Logger.getLogger(OnlineRecommenderIRTester.class
		      .getName());

	public OnlineRecommenderIRTester(RSDataset dataset, int fDimensions
			)
			throws IOException {
		super(dataset, fDimensions);
		
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		try {
			
			LinkedList<String> results= new LinkedList<>();
			
			
			LOG.info("Loading model");
			
			RSDataset data= RSDataset.fromPropertyFile("config/dbbookLocation.properties");
			
			
			
			
			
			
			//int[] limitSizes={5,10,15,20,25,30,35,40,45,50};
			int[] limitSizes={5};
			
			double[] learningRates={0.001,0.01,0.05,0.15,0.25};
			
			LinkedList<UserModelTrainerPredictor> predictorsLinked= new LinkedList<UserModelTrainerPredictor>();
			//predictorsLinked.add(new BayesAveragePredictor());	
			BaseModelPredictorWithHistory basemodel= new BaseModelPredictorWithHistory();
			predictorsLinked.add(basemodel);
			BaseModelPredictorWithItemRegularizationUpdateWithHistory baseModelPredictor = new BaseModelPredictorWithItemRegularizationUpdateWithHistory(0.001);
			predictorsLinked.add(baseModelPredictor);
			BaseModelPredictorWithItemRegularizationUpdateWithHistory baseModelPredictor2 = new BaseModelPredictorWithItemRegularizationUpdateWithHistory(0.01);
			predictorsLinked.add(baseModelPredictor2);
			BaseModelPredictorWithItemRegularizationUpdateWithHistory baseModelPredictor3 = new BaseModelPredictorWithItemRegularizationUpdateWithHistory(0.1);
			predictorsLinked.add(baseModelPredictor3);
			//BaseModelPredictorWithItemRegularizationUpdate baseModelPredictor4 = new BaseModelPredictorWithItemRegularizationUpdate(0);
			//predictorsLinked.add(baseModelPredictor4);
			
			
			//predictorsLinked.add(new BlendedModelPredictor());
			//predictorsLinked.add(new  MetadataSimilarityPredictor());
			
			//predictorsLinked.add(metadataModel);
			
			//predictorsLinked.add(new ProbabilityMetadataModelPredictor(baseModelPredictor,metadataModel));
			//predictorsLinked.add(new BaseModelPredictorWithItemRegularizationUpdate(0));
			
			int skips=0;
			int iters=0;
			Object[] predictors=  predictorsLinked.toArray();
				
			for (int i = 0; i < predictors.length; i++) {

				for (int j = 0; j < learningRates.length; j++) {

					for (int d = 0; d < limitSizes.length  ; d++) {
						
						if(iters>=skips){
							int dimensions = limitSizes[d];

							UserModelTrainerPredictor trainerPredictor = (UserModelTrainerPredictor) predictors[i];
							FactorUserItemRepresentation denseModel = new IncrementalFactorUserItemRepresentation(
									data, dimensions, false,
									trainerPredictor);

							trainerPredictor.setModelRepresentation(denseModel);

							trainerPredictor
									.setLearningRateStrategy(LearningRateStrategy
											.createDecreasingRate(1e-6,
													learningRates[j]));

							OnlineRecommenderIRTester rest = new OnlineRecommenderIRTester(
									data, dimensions);
							// rest.setEventsReport(1000000);
							UserProfileUpdater userUp = new UserProfileUpdater(
									trainerPredictor);
							// int limit=trainLimits[j];
							IUserMaskingStrategy agregator = new NoMaskingStrategy();
							IItemProfileUpdater itemUpdater = new ItemProfileUpdater(
									trainerPredictor);
							rest.setModelAndUpdaters(denseModel, userUp,
									agregator, itemUpdater);
							rest.setModelPredictor(trainerPredictor);
							ErrorReport result = rest.startExperiment(1);
							String resultLine = predictors[i] + "" + '\t'
									+ learningRates[j] + "" + '\t'
									+ limitSizes[d] + "" + '\t'
									+ result.toString();

							LOG.info(resultLine);
							results.add(resultLine);
							denseModel = null;
						}
						iters++;

					}

				}

			}		
				
			
			for (String string : results) {
				LOG.info(string);
				
			}
			
			
		} catch (IOException e) {
		
			e.printStackTrace();
		} catch (TasteException e) {
			
			e.printStackTrace();
		} catch (PrivateRecsysException e) {
			
			e.printStackTrace();
		}

	}
	/***
	 * 
	 * @return The rmse of the experiment
	 * @throws IOException If the files specified are not accesible
	 * @throws TasteException If the format of the file is not valid
	 * @throws PrivateRecsysException
	 */
	public ErrorReport startExperiment(int numIterations) throws IOException, TasteException, PrivateRecsysException {
		
		if(userItemRep==null || userUpdater==null || userAggregator==null || itemProfileUpdater==null||predictor==null ){
			LOG.severe("Could not start experiment: Model and iterator not set");
			throw new TasteException("Model and iterator not set");
		}	
		
		
		LOG.info("Starting experiment with predictor ="+predictor.toString()+" numIterations training "+numIterations );
		IRPrecisionError error=null;
		IRPrecisionError errorCV=null;
		LinkedList<Double> partialErrors= new LinkedList<>();
		for (int iteration = 1; iteration <= numIterations; iteration++) {
			
			
			PrivateRecommenderParallelTrainer pstr= new PrivateRecommenderParallelTrainer(this.userItemRep,this.predictor, this.userUpdater, this.userAggregator,this.itemProfileUpdater,this.rsDataset);
			
			FileEventCreator cec= new FileEventCreator(new File(rsDataset.getTrainSet()),this.eventsReport,this.numLimitEvents);
			cec.addObserver(pstr);
		
			cec.startEvents();
			
			
			pstr.shutdownThread();
			try {
				
				boolean finished=pstr.forceShutdown();
				if(!finished){
					throw new TasteException("Training failed - Timeout exception - not completed - Executed tasks: "+pstr.numExecutedTasks());
				}
				if(pstr.getNumSubmitedTasks()!=pstr.numExecutedTasks()){
					throw new TasteException("Training failed - not all tasks executed - Sumbited tasks: "+pstr.getNumSubmitedTasks()+" Executed tasks: "+pstr.numExecutedTasks());
				}
				LOG.info(System.nanoTime()+" Finished training, measuring errors ");
				TopNRecommenderFactorModel topNRecommender= new TopNRecommenderFactorModel(this.predictor);
				error=ModelEvaluator.evaluateModelIR(rsDataset.getTestSet(),rsDataset.getScale(),this.userItemRep, topNRecommender,0,10, true);
				errorCV=ModelEvaluator.evaluateModelIR(rsDataset.getTestCV(),rsDataset.getScale(),this.userItemRep, topNRecommender,0,10, true);
				
				LOG.info(System.nanoTime()+" Iteration "+iteration+" errors: "+error.toString());
				partialErrors.addAll(pstr.getPartialEvaluations());
			} catch (InterruptedException e) {
				throw new TasteException("Training failed - not completed Executed tasks: "+pstr.numExecutedTasks());
			}
		}
		LOG.info("Final error for experiment with with predictor ="+predictor.toString()+" numIterations training "+numIterations+" UserProfiler: "+userUpdater.toString()+" numIterations training "+numIterations +" is: cv="+errorCV+" test="+error);
		return error;
		
		
		
	}
	
	


	

}
