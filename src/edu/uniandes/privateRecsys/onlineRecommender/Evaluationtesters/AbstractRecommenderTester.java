package edu.uniandes.privateRecsys.onlineRecommender.Evaluationtesters;

import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.logging.Logger;

import org.apache.mahout.cf.taste.common.TasteException;

import edu.uniandes.privateRecsys.onlineRecommender.FileEventCreator;
import edu.uniandes.privateRecsys.onlineRecommender.IItemProfileUpdater;
import edu.uniandes.privateRecsys.onlineRecommender.IUserItemAggregator;
import edu.uniandes.privateRecsys.onlineRecommender.IUserProfileUpdater;
import edu.uniandes.privateRecsys.onlineRecommender.LearningRateStrategy;
import edu.uniandes.privateRecsys.onlineRecommender.ModelEvaluator;
import edu.uniandes.privateRecsys.onlineRecommender.PrivateRecommenderParallelTrainer;
import edu.uniandes.privateRecsys.onlineRecommender.UserModelTrainerPredictor;
import edu.uniandes.privateRecsys.onlineRecommender.exception.PrivateRecsysException;
import edu.uniandes.privateRecsys.onlineRecommender.factorModelRepresentation.FactorUserItemRepresentation;
import edu.uniandes.privateRecsys.onlineRecommender.vo.ErrorReport;
import edu.uniandes.privateRecsys.onlineRecommender.vo.RMSE_ErrorReport;

public abstract class AbstractRecommenderTester  {

	protected int fDimensions;
	protected FactorUserItemRepresentation userItemRep;
	protected IUserProfileUpdater userUpdater;
	protected IUserItemAggregator userAggregator;
	protected IItemProfileUpdater itemProfileUpdater;
	protected LearningRateStrategy learningRateStrategy;
	protected int numLimitEvents=-1;
	protected int eventsReport=-1;
	
	//protected AverageDataModel model;
	protected RSDataset rsDataset;
	protected UserModelTrainerPredictor predictor;
	private final static Logger LOG = Logger.getLogger(AbstractRecommenderTester.class
		      .getName());

	public AbstractRecommenderTester(RSDataset dataset, int fDimensions, LearningRateStrategy learningRateStrategy) throws IOException {
		this.rsDataset=dataset;
		this.fDimensions=fDimensions;
		this.learningRateStrategy=learningRateStrategy;
		
		
		
	}
	
	public void setModelAndUpdaters(FactorUserItemRepresentation representation, IUserProfileUpdater userUpdater, 
			IUserItemAggregator agregator,IItemProfileUpdater profileUpdater ) throws TasteException{
	
		this.userItemRep= representation;
		this.userUpdater= userUpdater;
		this.userAggregator= agregator;
		this.itemProfileUpdater=profileUpdater;
	}
	
	public void setModelPredictor(UserModelTrainerPredictor pred){
		this.predictor=pred;
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
		
		
		LOG.info("Starting experiment with params dim="+fDimensions+" learningrateStrategy="+learningRateStrategy.toString()+" UserProfiler: "+userUpdater.toString()+" numIterations training "+numIterations );
		double error=0;
		double errorTrain=0;
		double errorCV=0;
		LinkedList<Double> partialErrors= new LinkedList<>();
		for (int iteration = 1; iteration <= numIterations; iteration++) {
			
			
			PrivateRecommenderParallelTrainer pstr= new PrivateRecommenderParallelTrainer(this.userItemRep,this.predictor, this.userUpdater, this.userAggregator,this.itemProfileUpdater,this.rsDataset,this.learningRateStrategy);
			
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
				error=ModelEvaluator.evaluateModel(new File(rsDataset.getTestSet()),rsDataset.getScale(),this.predictor,10);
				errorCV=ModelEvaluator.evaluateModel(new File(rsDataset.getTestCV()),rsDataset.getScale(), this.predictor,10);
				errorTrain=ModelEvaluator.evaluateModel(new File(rsDataset.getTrainSet()),rsDataset.getScale(), this.predictor,10);
				//System.out.println("Error at iteration "+iteration+" is: Train: "+errorTrain+" CV:"+errorCV+" Test:"+error);
				LOG.info("Iteration "+iteration+" errors: "+errorTrain+'\t'+errorCV+'\t'+error);
				partialErrors.addAll(pstr.getPartialEvaluations());
			} catch (InterruptedException e) {
				throw new TasteException("Training failed - not completed Executed tasks: "+pstr.numExecutedTasks());
			}
		}
		LOG.info("Final error for experiment with params dim="+fDimensions+" learningrateStrategy="+learningRateStrategy.toString()+" UserProfiler: "+userUpdater.toString()+" numIterations training "+numIterations +" is: train="+errorTrain+" cv="+errorCV+" test="+error);
		
		
		return new RMSE_ErrorReport(errorTrain, error, errorCV,partialErrors);//""+errorTrain+'\t'+errorCV+'\t'+error;
		
	}
	
	public int getNumLimitEvents() {
		return numLimitEvents;
	}

	public void setNumLimitEvents(int numLimitEvents) {
		this.numLimitEvents = numLimitEvents;
	}

	public int getEventsReport() {
		return eventsReport;
	}

	public void setEventsReport(int eventsReport) {
		this.eventsReport = eventsReport;
	}


	

}
