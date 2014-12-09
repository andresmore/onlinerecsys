package edu.uniandes.privateRecsys.onlineRecommender.Evaluationtesters;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.logging.Logger;

import org.apache.mahout.cf.taste.common.TasteException;

import edu.uniandes.privateRecsys.onlineRecommender.BaseModelPredictorWithItemRegularizationUpdate;
import edu.uniandes.privateRecsys.onlineRecommender.FileEventCreator;
import edu.uniandes.privateRecsys.onlineRecommender.IItemProfileUpdater;
import edu.uniandes.privateRecsys.onlineRecommender.IUserMaskingStrategy;
import edu.uniandes.privateRecsys.onlineRecommender.ItemProfileUpdater;
import edu.uniandes.privateRecsys.onlineRecommender.LearningRateStrategy;
import edu.uniandes.privateRecsys.onlineRecommender.MetadataPredictor;
import edu.uniandes.privateRecsys.onlineRecommender.ModelEvaluator;
import edu.uniandes.privateRecsys.onlineRecommender.NoMaskingStrategy;
import edu.uniandes.privateRecsys.onlineRecommender.PrivateRecommenderParallelTrainer;
import edu.uniandes.privateRecsys.onlineRecommender.ProbabilityMetadataModelPredictor;
import edu.uniandes.privateRecsys.onlineRecommender.RatingsFileSplitter;
import edu.uniandes.privateRecsys.onlineRecommender.UserProfileUpdater;
import edu.uniandes.privateRecsys.onlineRecommender.exception.PrivateRecsysException;
import edu.uniandes.privateRecsys.onlineRecommender.factorModelRepresentation.FactorUserItemRepresentation;
import edu.uniandes.privateRecsys.onlineRecommender.factorModelRepresentation.IncrementalFactorUserItemRepresentation;
import edu.uniandes.privateRecsys.onlineRecommender.ratingScale.OrdinalRatingScale;
import edu.uniandes.privateRecsys.onlineRecommender.ratingScale.RatingScale;
import edu.uniandes.privateRecsys.onlineRecommender.vo.ErrorReport;
import edu.uniandes.privateRecsys.onlineRecommender.vo.RMSE_ErrorReport;

public class ColdStartItemModelRecommenderTester extends AbstractRecommenderTester {

	
	
	private final static Logger LOG = Logger.getLogger(ColdStartItemModelRecommenderTester.class
		      .getName());
	

	
	public ColdStartItemModelRecommenderTester(RSDataset data, int fDimensions)
			throws IOException {
		super(data, fDimensions);
		
	}

	private static RSDataset createDatasetFromFile(
			String mainFile, RatingScale scale, double percentage) throws IOException, TasteException, PrivateRecsysException {
		File trainFile=File.createTempFile("tempFileTrain"+percentage+"_", ".tmp");
		File testFile=File.createTempFile("tempFileTest"+percentage+"_", ".tmp");
		File cvFile=File.createTempFile("tempFileCV"+percentage+"_", ".tmp");
		RatingsFileSplitter rfs= new RatingsFileSplitter(mainFile, testFile, cvFile, trainFile, 0.8, percentage);
		rfs.split();
		return new RSDataset(trainFile.getAbsolutePath(), testFile.getAbsolutePath(), cvFile.getAbsolutePath(), scale);
		
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		try {
			
			LinkedList<String> results= new LinkedList<>();
			
			LOG.info("Loading model");
			 HashMap<String,String> translations=new HashMap<String,String>();
			 translations.put(new String("0"), new String("1"));
			 translations.put(new String("0.5"), new String("1"));
			 translations.put(new String("1.5"), new String("2"));
			 translations.put(new String("2.5"), new String("3"));
			 translations.put(new String("3.5"), new String("4"));
			 translations.put(new String("4.5"), new String("5"));
			RatingScale scale= new OrdinalRatingScale(new String[] {new String("0"),new String("0.5"),new String("1"),new String("1.5"),new String("2"),new String("2.5"),new String("3"),new String("3.5"),new String("4"),new String("4.5"),new String("5")},translations);
			
			
			
			
			
			double[] percentagesContent={0.05,0.1,0.15,0.2,0.25,0.30,0.35};
			
			
			BaseModelPredictorWithItemRegularizationUpdate baseModelPredictor = new BaseModelPredictorWithItemRegularizationUpdate(0.01);
			MetadataPredictor metadataModel = new MetadataPredictor(-1,MetadataPredictor.SKETCH_DEPTH,MetadataPredictor.SKETCH_WIDTH, 60, MetadataPredictor.NUMBER_OF_SEGMENTS, MetadataPredictor.NUMROLLING);
			
			
			ProbabilityMetadataModelPredictor hybrid=(new ProbabilityMetadataModelPredictor(baseModelPredictor,metadataModel));
			String mainFile="data/ml-10M100K/orderedRatings.dat.meta2";
			int skips=6;
			int iters=0;
			int dimensions=5;
			double cfLearningRate=0.15;
			double cbLearningRate=0.75;
			
				
			for (int i = 0; i < percentagesContent.length; i++) {

									
						if(iters>=skips){
		
							
							
							FactorUserItemRepresentation denseModel = new IncrementalFactorUserItemRepresentation(
									scale, dimensions, false,
									hybrid);

							hybrid.setModelRepresentation(denseModel);

							baseModelPredictor
									.setLearningRateStrategy(LearningRateStrategy
											.createDecreasingRate(1e-6,
													cfLearningRate));
							metadataModel.setLearningRateStrategy(LearningRateStrategy.createDecreasingRate(1e-6, cbLearningRate));

							RSDataset data=createDatasetFromFile(mainFile, scale, percentagesContent[i]);
							ColdStartItemModelRecommenderTester rest = new ColdStartItemModelRecommenderTester(
									data, dimensions);
							// rest.setEventsReport(1000000);
							UserProfileUpdater userUp = new UserProfileUpdater(
									hybrid);
							// int limit=trainLimits[j];
							IUserMaskingStrategy agregator = new NoMaskingStrategy();
							IItemProfileUpdater itemUpdater = new ItemProfileUpdater(
									hybrid);
							rest.setModelAndUpdaters(denseModel, userUp,
									agregator, itemUpdater);
							rest.setModelPredictor(hybrid);
							ErrorReport result = rest.startExperiment(1);
							double errorCF=ModelEvaluator.evaluateModel(new File(data.getTestSet()),data.getScale(),baseModelPredictor,0);
							double errorCB=ModelEvaluator.evaluateModel(new File(data.getTestSet()),data.getScale(),metadataModel,0);
							
							String resultLine = hybrid + "" + '\t'
									+ percentagesContent[i] + "" + '\t'
									+ errorCF + "" + '\t'
									+ errorCB + "" + '\t'
									+ result.toString();

							LOG.info(resultLine);
							results.add(resultLine);
							denseModel = null;
						}
						iters++;


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
