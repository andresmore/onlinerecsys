package edu.uniandes.privateRecsys.onlineRecommender.Evaluationtesters;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.logging.Logger;

import org.apache.mahout.cf.taste.common.TasteException;

import edu.uniandes.privateRecsys.onlineRecommender.BaseModelPredictor;
import edu.uniandes.privateRecsys.onlineRecommender.BaseModelPredictorWithItemRegularizationUpdate;
import edu.uniandes.privateRecsys.onlineRecommender.FileEventCreator;
import edu.uniandes.privateRecsys.onlineRecommender.IItemProfileUpdater;
import edu.uniandes.privateRecsys.onlineRecommender.IUserMaskingStrategy;
import edu.uniandes.privateRecsys.onlineRecommender.ItemProfileUpdater;
import edu.uniandes.privateRecsys.onlineRecommender.LearningRateStrategy;
import edu.uniandes.privateRecsys.onlineRecommender.LogCombinationPredictor;
import edu.uniandes.privateRecsys.onlineRecommender.LogProbabilityMetadataModelPredictor;
import edu.uniandes.privateRecsys.onlineRecommender.MetadataPredictor;
import edu.uniandes.privateRecsys.onlineRecommender.ModelEvaluator;
import edu.uniandes.privateRecsys.onlineRecommender.NoMaskingStrategy;
import edu.uniandes.privateRecsys.onlineRecommender.PrivateRecommenderParallelTrainer;
import edu.uniandes.privateRecsys.onlineRecommender.ProbabilityCombinationWithRegressionPredictor;
import edu.uniandes.privateRecsys.onlineRecommender.ProbabilityMetadataModelPredictor;
import edu.uniandes.privateRecsys.onlineRecommender.SimpleAveragePredictor;
import edu.uniandes.privateRecsys.onlineRecommender.UserModelTrainerPredictor;
import edu.uniandes.privateRecsys.onlineRecommender.UserProfileUpdater;
import edu.uniandes.privateRecsys.onlineRecommender.exception.PrivateRecsysException;
import edu.uniandes.privateRecsys.onlineRecommender.factorModelRepresentation.FactorUserItemRepresentation;
import edu.uniandes.privateRecsys.onlineRecommender.factorModelRepresentation.IncrementalFactorUserItemRepresentation;
import edu.uniandes.privateRecsys.onlineRecommender.ratingScale.OrdinalRatingScale;
import edu.uniandes.privateRecsys.onlineRecommender.ratingScale.RatingScale;
import edu.uniandes.privateRecsys.onlineRecommender.vo.ErrorReport;
import edu.uniandes.privateRecsys.onlineRecommender.vo.RMSE_ErrorReport;

public class CompareOnlineRecommenderTester extends AbstractRecommenderTester {

	
	
	private final static Logger LOG = Logger.getLogger(CompareOnlineRecommenderTester.class
		      .getName());

	public CompareOnlineRecommenderTester(RSDataset dataset, int fDimensions
			)
			throws IOException {
		super(dataset, fDimensions);
		
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		try {
			LOG.info(""+System.nanoTime());
			LinkedList<String> results= new LinkedList<>();
			
			
			RSDataset data=RSDataset.fromPropertyFile("config/yMusic.properties"); 
			//RSDataset data=RSDataset.fromPropertyFile("config/dbbooklocation.properties");
			//RSDataset data=new RSDataset(trainSet,testSet,testCV,scale);
			
			
			
			
			
			
			int[] limitSizes={5};
			double[] learningRates={0.01};
			//double[] learningRates={0.6,0.75,0.8,0.9};
			//int[] trainLimits={5,10,25,50,75,100,150,200,-1};
			LinkedList<UserModelTrainerPredictor> predictorsLinked= new LinkedList<UserModelTrainerPredictor>();
			//predictorsLinked.add(new BayesAveragePredictor());	
			
			SimpleAveragePredictor average = new SimpleAveragePredictor();
			
			
			//predictorsLinked.add(average);
			
			
			
			
			
			
			BaseModelPredictorWithItemRegularizationUpdate baseModelPredictor = new BaseModelPredictorWithItemRegularizationUpdate(0);
			predictorsLinked.add(baseModelPredictor);
			BaseModelPredictor basemodel= new BaseModelPredictor();
			//predictorsLinked.add(basemodel);
			ProbabilityCombinationWithRegressionPredictor regLogPredictor= new ProbabilityCombinationWithRegressionPredictor(new BaseModelPredictorWithItemRegularizationUpdate(0), average);
			//predictorsLinked.add(regLogPredictor);
			LogCombinationPredictor logPredictor= new LogCombinationPredictor(new BaseModelPredictorWithItemRegularizationUpdate(0), average);
			//predictorsLinked.add(logPredictor);
			
			
			//predictorsLinked.add(new BlendedModelPredictor());
			//predictorsLinked.add(new  MetadataSimilarityPredictor());
			MetadataPredictor metadataModel = new MetadataPredictor(-1,MetadataPredictor.SKETCH_DEPTH,MetadataPredictor.SKETCH_WIDTH, MetadataPredictor.WINDOW_LENGHT, MetadataPredictor.NUMBER_OF_SEGMENTS,MetadataPredictor.NUMROLLING);
			//predictorsLinked.add(metadataModel);
			
			//predictorsLinked.add(new LogProbabilityMetadataModelPredictor(logPredictor,metadataModel));
			//predictorsLinked.add(new BaseModelPredictorWithItemRegularizationUpdate(0));
			
			
			Object[] predictors=  predictorsLinked.toArray();
				
			for (int i = 0; i < predictors.length; i++) {

				for (int j = 0; j < learningRates.length; j++) {

					for (int d = 0; d < limitSizes.length; d++) {

						int dimensions = limitSizes[d];

						UserModelTrainerPredictor trainerPredictor = (UserModelTrainerPredictor) predictors[i];
						FactorUserItemRepresentation denseModel = new IncrementalFactorUserItemRepresentation(
								data.getScale(), dimensions, false, trainerPredictor);

						trainerPredictor.setModelRepresentation(denseModel);
						if (trainerPredictor instanceof ProbabilityMetadataModelPredictor) {
							LearningRateStrategy learningRateStrategy = LearningRateStrategy
									.createDecreasingRate(1e-6,
											learningRates[j]);
							baseModelPredictor
									.setLearningRateStrategy(learningRateStrategy);
							LearningRateStrategy tsCreator = LearningRateStrategy
									.createDecreasingRate(1e-6, 0.75);
							metadataModel.setLearningRateStrategy(tsCreator);

						} else {
							trainerPredictor
									.setLearningRateStrategy(LearningRateStrategy
											.createDecreasingRate(1e-6,
													learningRates[j]));
						}

						CompareOnlineRecommenderTester rest = new CompareOnlineRecommenderTester(
								data, dimensions);
						// rest.setEventsReport(1000000);
						UserProfileUpdater userUp = new UserProfileUpdater(
								trainerPredictor);
						// int limit=trainLimits[j];
						IUserMaskingStrategy agregator = new NoMaskingStrategy();
						IItemProfileUpdater itemUpdater = new ItemProfileUpdater(
								trainerPredictor);
						rest.setModelAndUpdaters(denseModel, userUp, agregator,
								itemUpdater);
						rest.setModelPredictor(trainerPredictor);
						ErrorReport result = rest.startExperiment(1);
						String resultLine = predictors[i] + "" + '\t'
								+ learningRates[j] + "" + '\t'+ limitSizes[d] + "" + '\t'
								+ result.toString();
						if (trainerPredictor instanceof ProbabilityMetadataModelPredictor) {
							ProbabilityMetadataModelPredictor blender = (ProbabilityMetadataModelPredictor) trainerPredictor;
							resultLine = resultLine
									+ '\t'
									+ blender.calculateRegretBaseExpert()
											.toString();
						}
						results.add(resultLine);
						denseModel = null;
						if(trainerPredictor instanceof SimpleAveragePredictor){
							j=learningRates.length;
							d=limitSizes.length;
						}

					}

				}

			}		
				
			
			for (String string : results) {
				LOG.info(string);
				
			}
			LOG.info(""+System.nanoTime());
			
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
		double error=0;
		double errorTrain=0;
		double errorCV=0;
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
				
				error=ModelEvaluator.evaluateModel(new File(rsDataset.getTestSet()),rsDataset.getScale(),this.predictor,0);
				errorCV=ModelEvaluator.evaluateModel(new File(rsDataset.getTestCV()),rsDataset.getScale(), this.predictor,0);
				errorTrain=ModelEvaluator.evaluateModel(new File(rsDataset.getTrainSet()),rsDataset.getScale(), this.predictor,0);
				//System.out.println("Error at iteration "+iteration+" is: Train: "+errorTrain+" CV:"+errorCV+" Test:"+error);
				LOG.info(System.nanoTime()+" Iteration "+iteration+" errors: "+errorTrain+'\t'+errorCV+'\t'+error);
				partialErrors.addAll(pstr.getPartialEvaluations());
			} catch (InterruptedException e) {
				throw new TasteException("Training failed - not completed Executed tasks: "+pstr.numExecutedTasks());
			}
		}
		LOG.info("Final error for experiment with with predictor ="+predictor.toString()+" numIterations training "+numIterations+" UserProfiler: "+userUpdater.toString()+" numIterations training "+numIterations +" is: train="+errorTrain+" cv="+errorCV+" test="+error);
		
		
		return new RMSE_ErrorReport(errorTrain, error, errorCV,partialErrors);//""+errorTrain+'\t'+errorCV+'\t'+error;
		
	}
	


	

}
