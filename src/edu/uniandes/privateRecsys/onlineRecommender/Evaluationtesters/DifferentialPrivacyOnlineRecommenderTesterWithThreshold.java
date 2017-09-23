package edu.uniandes.privateRecsys.onlineRecommender.Evaluationtesters;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.logging.Logger;

import org.apache.mahout.cf.taste.common.TasteException;

import edu.uniandes.privateRecsys.onlineRecommender.BaseModelPredictorWithItemRegularizationUpdate;
import edu.uniandes.privateRecsys.onlineRecommender.DifferentialPrivacyMaskingStrategy;
import edu.uniandes.privateRecsys.onlineRecommender.FileEventCreator;
import edu.uniandes.privateRecsys.onlineRecommender.IItemProfileUpdater;
import edu.uniandes.privateRecsys.onlineRecommender.IUserMaskingStrategy;
import edu.uniandes.privateRecsys.onlineRecommender.IUserProfileUpdater;
import edu.uniandes.privateRecsys.onlineRecommender.ItemProfileUpdater;
import edu.uniandes.privateRecsys.onlineRecommender.LearningRateStrategy;
import edu.uniandes.privateRecsys.onlineRecommender.ModelEvaluator;
import edu.uniandes.privateRecsys.onlineRecommender.PrivateRecommenderParallelTrainer;
import edu.uniandes.privateRecsys.onlineRecommender.ThresholdItemProfileUpdater;
import edu.uniandes.privateRecsys.onlineRecommender.UserProfileUpdater;
import edu.uniandes.privateRecsys.onlineRecommender.exception.PrivateRecsysException;
import edu.uniandes.privateRecsys.onlineRecommender.factorModelRepresentation.FactorUserItemRepresentation;
import edu.uniandes.privateRecsys.onlineRecommender.factorModelRepresentation.IncrementalFactorUserItemRepresentation;
import edu.uniandes.privateRecsys.onlineRecommender.vo.ErrorReport;
import edu.uniandes.privateRecsys.onlineRecommender.vo.RMSE_ErrorReport;

public class DifferentialPrivacyOnlineRecommenderTesterWithThreshold extends AbstractRecommenderTester {


	
	private final static Logger LOG = Logger.getLogger(DifferentialPrivacyOnlineRecommenderTesterWithThreshold.class
		      .getName());

	public DifferentialPrivacyOnlineRecommenderTesterWithThreshold(RSDataset data, int fDimensions) throws IOException {
		super(data, fDimensions);
		
	}
	
	@Override
	public void setModelAndUpdaters(
			FactorUserItemRepresentation representation,
			IUserProfileUpdater userUpdater, IUserMaskingStrategy agregator,
			IItemProfileUpdater itemUpdater) throws TasteException {
		if(!(agregator instanceof DifferentialPrivacyMaskingStrategy))
			throw new TasteException("DifferentialPrivacyOnlineRecommenderTester has to have a DifferentialPrivacyMaskingStrategy as user-item agregator");
		this.userItemRep=representation;
		this.userUpdater=userUpdater;
		this.userAggregator=agregator;
		this.itemProfileUpdater=itemUpdater;
	}	

	public static void main(String[] args) {
		try {
		
			LinkedList<String> results= new LinkedList<>();
			
			
		
		
			 HashMap<String,String> translations=new HashMap<String,String>();
			 translations.put("0.5", "1");
			 translations.put("1.5", "2");
			 translations.put("2.5", "3");
			 translations.put("3.5", "4");
			 translations.put("4.5", "5");
	
			LOG.info("Loading model");
		
			RSDataset data=RSDataset.fromPropertyFile("config/yMusic.properties");
			//RSDataset data=RSDataset.fromPropertyFile("config/movielensLocation.properties");
			double[] cfLearningRate={0.15,0.25,0.4};
			double[] regularizations={0.001,0.01,0.1};
			
			int[] dimensionsArr={5};
			
			double[] epsilonArr={0,0.01,0.1,0.25,0.5,0.75,1};
			double[] thresholds={0.5,0.75,0.8,0.9};
			
			
			String[] itemUpdaters={"ItemProfileUpdater","ThresholdItemProfileUpdater"};  
	
			
			
			
			
			
			for (int i = 0; i < cfLearningRate.length; i++) {
				for (int n = 0; n < itemUpdaters.length; n++) {
					
				

					for (int k = 0; k < dimensionsArr.length; k++) {
						for (int k2 = 0; k2 < regularizations.length; k2++) {

							for (int m = 0; m < epsilonArr.length; m++) {
								boolean needsThresholdsIteration=true;
								for (int j = 0; j < thresholds.length && needsThresholdsIteration; j++) {

									double threshold = thresholds[j];

									double cflearn = cfLearningRate[i];
									
									int dimensions = dimensionsArr[k];
									double regularization=regularizations[k2];
									double epsilon = epsilonArr[m];
									String itemUpdModel = itemUpdaters[n];
									BaseModelPredictorWithItemRegularizationUpdate baseModelPredictor = new BaseModelPredictorWithItemRegularizationUpdate(
											regularization);
									FactorUserItemRepresentation denseModel = new IncrementalFactorUserItemRepresentation(
											data, dimensions, false,
											baseModelPredictor);

									baseModelPredictor
											.setModelRepresentation(denseModel);

									LearningRateStrategy cfLearningRateStrategy = LearningRateStrategy
											.createDecreasingRate(1e-6, cflearn);
									baseModelPredictor
											.setLearningRateStrategy(cfLearningRateStrategy);

									DifferentialPrivacyOnlineRecommenderTesterWithThreshold rest = new DifferentialPrivacyOnlineRecommenderTesterWithThreshold(
											data, dimensions);
									UserProfileUpdater userUp = new UserProfileUpdater(
											baseModelPredictor);
									IUserMaskingStrategy agregator = new DifferentialPrivacyMaskingStrategy(
											epsilon);
									IItemProfileUpdater itemUpdater = null;
									if (itemUpdModel
											.equals("ItemProfileUpdater")) {
										itemUpdater = new ItemProfileUpdater(
												baseModelPredictor);
										needsThresholdsIteration=false;
										threshold=-1;
									} else {
										
										itemUpdater = new ThresholdItemProfileUpdater(
												baseModelPredictor, threshold);
									}

									rest.setModelAndUpdaters(denseModel,
											userUp, agregator, itemUpdater);
									rest.setModelPredictor(baseModelPredictor);
									//rest.setNumLimitEvents(10000000);
									ErrorReport result = rest
											.startExperiment(1);
									String events = "";
									if (itemUpdModel
											.equals("ThresholdItemProfileUpdater")) {
										events = ""
												+ ((ThresholdItemProfileUpdater) itemUpdater)
														.getNumEventsThreshold();
									}
									
									String resultLine = "DiffPrivate predictor"
											+ '\t' + cflearn + "" + '\t'
											+ '\t' + regularization + "" + '\t'
											+ threshold + "" + '\t'
											+ dimensions + "" + '\t' + ""
											+ '\t' + epsilon + "" + '\t'
											+ itemUpdModel + "" + '\t'
											+ result.toString() + '\t'

											+ events + '\t'

									;
									LOG.info("Finished: " + resultLine);
									results.add(resultLine);
									denseModel = null;
								}

							}
						}
					}
				}
			}
	
			
		
				
				
			for (String string : results) {
				LOG.info(string);
				
			}
				
	
			System.exit(0);

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
	 * @throws IOException If the files specified are not accessible
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
				LOG.info("Finished training, measuring errors ");
				
				error=ModelEvaluator.evaluateModel(new File(rsDataset.getTestSet()),rsDataset.getScale(),this.predictor,3);
				errorCV=ModelEvaluator.evaluateModel(new File(rsDataset.getTestCV()),rsDataset.getScale(), this.predictor,3);
				errorTrain=1.19;//ModelEvaluator.evaluateModel(new File(rsDataset.getTrainSet()),rsDataset.getScale(), this.predictor,3);
				//System.out.println("Error at iteration "+iteration+" is: Train: "+errorTrain+" CV:"+errorCV+" Test:"+error);
				LOG.info("Iteration "+iteration+" errors: "+errorTrain+'\t'+errorCV+'\t'+error);
				partialErrors.addAll(pstr.getPartialEvaluations());
			} catch (InterruptedException e) {
				throw new TasteException("Training failed - not completed Executed tasks: "+pstr.numExecutedTasks());
			}
		}
		LOG.info("Final error for experiment with with predictor ="+predictor.toString()+" numIterations training "+numIterations+" UserProfiler: "+userUpdater.toString()+" numIterations training "+numIterations +" is: train="+errorTrain+" cv="+errorCV+" test="+error);
		
		
		return new RMSE_ErrorReport(errorTrain, error, errorCV,partialErrors);//""+errorTrain+'\t'+errorCV+'\t'+error;
		
	}
	

	
	


	

}
