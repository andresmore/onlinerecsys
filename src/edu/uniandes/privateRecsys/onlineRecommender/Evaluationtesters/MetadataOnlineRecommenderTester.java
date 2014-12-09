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

public class MetadataOnlineRecommenderTester extends AbstractRecommenderTester {

	
	
	private final static Logger LOG = Logger.getLogger(MetadataOnlineRecommenderTester.class
		      .getName());

	public MetadataOnlineRecommenderTester(RSDataset dataset
			)
			throws IOException {
		super(dataset, 1);
		
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		try {
			LOG.info(""+System.nanoTime());
			LinkedList<String> results= new LinkedList<>();
			//String trainSet=new String("data/dbBook/ra.train.meta");
			//String trainSet=new String("data/ml-10M100K/rb.train.sorted");
			String trainSet=new String("data/ml-10M100K/rb.train.meta.sorted");
			//String trainSet=new String("data/ml-1m/rb.train.sorted");
			//String trainSet=new String("data/ml-1m/rb.train.meta.sorted");
			//String trainSet="data/netflix/rb.train.sorted";
			
			//String testSet=new String("data/dbBook/ra.test.meta");
			//String testSet=new String("data/ml-10M100K/rb.test.test");
			String testSet=new String("data/ml-10M100K/rb.test.meta.test");
			//String testSet=new String("data/ml-1m/rb.test.test");
			//String testSet=new String("data/ml-1m/rb.test.meta.test");
			//String testSet="data/netflix/rb.test.test";
			
			//String testCV=new String("data/dbBook/ra.test.meta");
			//String testCV=new String("data/ml-10M100K/rb.test.cv");
			String testCV=new String("data/ml-10M100K/rb.test.meta.cv");
			//String testCV=new String("data/ml-1m/rb.test.cv");
			//String testCV=new String("data/ml-1m/rb.test.meta.cv");
			//String testCV="data/netflix/rb.test.CV";
			LOG.info("Loading model");
			
			 HashMap<String,String> translations=new HashMap<String,String>();
			 translations.put(new String("0"), new String("1"));
			 translations.put(new String("0.5"), new String("1"));
			 translations.put(new String("1.5"), new String("2"));
			 translations.put(new String("2.5"), new String("3"));
			 translations.put(new String("3.5"), new String("4"));
			 translations.put(new String("4.5"), new String("5"));
			RatingScale scale= new OrdinalRatingScale(new String[] {new String("0"),new String("0.5"),new String("1"),new String("1.5"),new String("2"),new String("2.5"),new String("3"),new String("3.5"),new String("4"),new String("4.5"),new String("5")},translations);
			
			
			//RSDataset data=RSDataset.fromPropertyFile("config/yMusic.properties"); 
			//RSDataset data=RSDataset.fromPropertyFile("config/dbbooklocation.properties");
			RSDataset data=new RSDataset(trainSet,testSet,testCV,scale);
			
			
			
			
			
			
		
			double[] learningRates={0.25,0.5,0.75,0.85};
			int[] window_lenght={60}; 
			int[] segment_size={5};
			int[] numRollingVec={5,7,10};
			LinkedList<UserModelTrainerPredictor> predictorsLinked= new LinkedList<UserModelTrainerPredictor>();
			
			
		
			int skips=0;
			int currIter=0;
			
		
			MetadataPredictor metadataModel = new MetadataPredictor(-1,MetadataPredictor.SKETCH_DEPTH,MetadataPredictor.SKETCH_WIDTH, MetadataPredictor.WINDOW_LENGHT, MetadataPredictor.NUMBER_OF_SEGMENTS, MetadataPredictor.NUMROLLING);
			predictorsLinked.add(metadataModel);
			
		
			
			Object[] predictors=  predictorsLinked.toArray();
				
			

				for (int j = 0; j < learningRates.length; j++) {

					for (int d = 0; d < window_lenght.length; d++) {

						int windowLenght = window_lenght[d];
						for (int k = 0; k < segment_size.length; k++) {
							int numberOfSegments = segment_size[k];
							for (int i = 0; i < numRollingVec.length; i++) {
							
								int numRolling=numRollingVec[i];
							
						
						FactorUserItemRepresentation denseModel = new IncrementalFactorUserItemRepresentation(
								data.getScale(), 5, false, metadataModel);

						metadataModel.setModelRepresentation(denseModel);
					
						metadataModel
									.setLearningRateStrategy(LearningRateStrategy
											.createDecreasingRate(1e-6,
													learningRates[j]));
							metadataModel.initSketchProperties(MetadataPredictor.SKETCH_DEPTH, MetadataPredictor.SKETCH_WIDTH, windowLenght, numberOfSegments,numRolling);
							
						MetadataOnlineRecommenderTester rest = new MetadataOnlineRecommenderTester(
								data);
						// rest.setEventsReport(1000000);
						UserProfileUpdater userUp = new UserProfileUpdater(
								metadataModel);
						// int limit=trainLimits[j];
						IUserMaskingStrategy agregator = new NoMaskingStrategy();
						IItemProfileUpdater itemUpdater = new ItemProfileUpdater(
								metadataModel);
						rest.setModelAndUpdaters(denseModel, userUp, agregator,
								itemUpdater);
						rest.setModelPredictor(metadataModel);
						currIter++;
						if(currIter<skips){
							LOG.info("Skipped "+metadataModel.toString() + "" + '\t'
									+ learningRates[j] + "" + '\t'+ window_lenght[d] + "" + '\t'+ segment_size[k]+ "" + '\t'+ numRollingVec[i]+ "" + '\t');
								
						}
						else{
						LOG.info("Start "+metadataModel.toString() + "" + '\t'
								+ learningRates[j] + "" + '\t'+ window_lenght[d] + "" + '\t'+ segment_size[k]+ "" + '\t'+ numRollingVec[i]+ "" + '\t');
						
						ErrorReport result = rest.startExperiment(1);
						String resultLine = metadataModel.toString() + "" + '\t'
								+ learningRates[j] + "" + '\t'+ window_lenght[d] + "" + '\t'+ segment_size[k]+ "" + '\t'+ numRollingVec[i]+ "" + '\t'
								+ result.toString();
						LOG.info(resultLine);
						results.add(resultLine);
						denseModel = null;
						}
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
