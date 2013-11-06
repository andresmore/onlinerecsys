package edu.uniandes.privateRecsys.onlineRecommender.jsonParserEvents;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.logging.Logger;

import org.apache.mahout.cf.taste.common.TasteException;

import edu.uniandes.privateRecsys.onlineRecommender.BaseModelPredictor;
import edu.uniandes.privateRecsys.onlineRecommender.IItemProfileUpdater;
import edu.uniandes.privateRecsys.onlineRecommender.IUserItemAggregator;
import edu.uniandes.privateRecsys.onlineRecommender.ItemProfileUpdater;
import edu.uniandes.privateRecsys.onlineRecommender.LearningRateStrategy;
import edu.uniandes.privateRecsys.onlineRecommender.ModelEvaluator;
import edu.uniandes.privateRecsys.onlineRecommender.NoPrivacyAggregator;
import edu.uniandes.privateRecsys.onlineRecommender.PopularityObserver;
import edu.uniandes.privateRecsys.onlineRecommender.PrivateRecommenderParallelTrainer;
import edu.uniandes.privateRecsys.onlineRecommender.UserModelTrainerPredictor;
import edu.uniandes.privateRecsys.onlineRecommender.UserProfileUpdater;
import edu.uniandes.privateRecsys.onlineRecommender.Evaluationtesters.AbstractRecommenderTester;
import edu.uniandes.privateRecsys.onlineRecommender.Evaluationtesters.PlistaDataset;
import edu.uniandes.privateRecsys.onlineRecommender.Evaluationtesters.RSDataset;
import edu.uniandes.privateRecsys.onlineRecommender.exception.PrivateRecsysException;
import edu.uniandes.privateRecsys.onlineRecommender.factorModelRepresentation.IncrementalFactorUserItemRepresentation;
import edu.uniandes.privateRecsys.onlineRecommender.ratingScale.OrdinalRatingScale;
import edu.uniandes.privateRecsys.onlineRecommender.ratingScale.RatingScale;
import edu.uniandes.privateRecsys.onlineRecommender.vo.ErrorReport;
import edu.uniandes.privateRecsys.onlineRecommender.vo.IRPrecisionError;

public class PlistaMostPopularRecommenderTester extends AbstractRecommenderTester {
	
	private HashSet<Long> restrictedUserIds= new HashSet<Long>();
	private PlistaDataset plistaDataset;
	private boolean useTestEventForPopularity;
	private final static Logger LOG = Logger.getLogger(PlistaMostPopularRecommenderTester.class
		      .getName());
	public PlistaMostPopularRecommenderTester(RSDataset dataset, int fDimensions,
			LearningRateStrategy tsCreator, boolean useTestEventForPopularity) throws IOException {
		super(dataset, fDimensions, tsCreator);
		if(!(dataset instanceof PlistaDataset))
			throw new RuntimeException("Only works with plista dataset");
		
		this.plistaDataset=(PlistaDataset)dataset;
		loadUsers(plistaDataset.getUserFile());
		this.useTestEventForPopularity=useTestEventForPopularity;
		
	}
	
	private void loadUsers(String usersFile) throws  IOException {
		BufferedReader red=null;
		String line= null;
		try{
			red=new BufferedReader(new FileReader(usersFile));
			while((line=red.readLine())!=null){
				String id=line.split(",")[0];
				if(!id.equals("0")&&!id.equals("null")){
					id=id.replaceAll("[\\\"]", "");
					Long longId=Long.parseLong(id);
					restrictedUserIds.add(longId);
				}
			}
		} catch (FileNotFoundException e) {
			throw e;
		} catch (NumberFormatException e) {} 
		catch (IOException e) {
			
			throw e;
		}finally{
			if(red!=null)
				try {
					red.close();
				} catch (IOException e) {}
		}
		
	}

	/***
	 * 
	 * @return The ErrorReport of the experiment
	 * @throws IOException If the files specified are not accesible
	 * @throws TasteException If the format of the file is not valid
	 * @throws PrivateRecsysException
	 */
	public ErrorReport startExperiment(int numIterations) throws IOException, TasteException, PrivateRecsysException {
		
		if(userItemRep==null || userUpdater==null || userAggregator==null || itemProfileUpdater==null || predictor==null){
			LOG.severe("Could not start experiment: Model and iterator not set");
			throw new TasteException("Model and aggregator not set");
		}	
		userItemRep.setRestrictUsers(this.restrictedUserIds);
		
		LOG.info("Starting experiment with params dim="+fDimensions+" learningRate= "+learningRateStrategy.toString()+" numIterations training "+numIterations );
		
		LinkedList<Double> partialErrors= new LinkedList<>();
		for (int iteration = 1; iteration <= numIterations; iteration++) {
			
			
			PrivateRecommenderParallelTrainer pstr= new PrivateRecommenderParallelTrainer(this.userItemRep, predictor, this.userUpdater, this.userAggregator,this.itemProfileUpdater,this.rsDataset,this.learningRateStrategy);
			PlistaJsonEventCreator plistaEventCreator=new PlistaJsonEventCreator(this.plistaDataset.getDirectory(), 1, 25, this.plistaDataset.getPrefixes());
			plistaEventCreator.addObserver(pstr);
			PopularityObserver popObserver= new PopularityObserver();
			plistaEventCreator.addObserver(popObserver);
			//plistaEventCreator.startEvents();
			
		
			
			pstr.shutdownThread();
			try {
				
				boolean finished=pstr.forceShutdown();
				if(!finished){
					throw new TasteException("Training failed - not completed Executed tasks: "+pstr.numExecutedTasks());
				}
				if(pstr.getNumSubmitedTasks()!=pstr.numExecutedTasks()){
					throw new TasteException("Training failed - not completed Executed tasks: "+pstr.numExecutedTasks());
				}
				
				
				LOG.info("Finished training, measuring errors ");
				//this.plistaDataset.getPrefixes().add("create");
				//this.plistaDataset.getPrefixes().add("update");
				if(this.useTestEventForPopularity){
					PlistaJsonEventCreator plistaTestEventCreator=new PlistaJsonEventCreator(this.plistaDataset.getDirectory(), 26, 30, this.plistaDataset.getPrefixes());
					plistaTestEventCreator.addObserver(popObserver);
					plistaTestEventCreator.startEvents();
					
				}
				
				IRPrecisionError error=ModelEvaluator.evaluatePlistaModel(this.plistaDataset,rsDataset.getScale(),this.learningRateStrategy, this.userItemRep, new TopNPopularityRecommender(popObserver));
				return error;
			} catch (InterruptedException e) {
				throw new TasteException("Training failed - not completed Executed tasks: "+pstr.numExecutedTasks());
			}
		}
		
		
		return null;
		//return new ErrorReport(errorTrain, error, errorCV,partialErrors);//""+errorTrain+'\t'+errorCV+'\t'+error;
		
	}
	
	public static void main(String[] args) {
		LinkedList<String> trainPrefixes= new LinkedList<>();
		trainPrefixes.add("click");
		trainPrefixes.add("impression");
		RatingScale scale= new OrdinalRatingScale(new String[] {"0","1","2"});
		PlistaDataset dataset= new PlistaDataset("data/plista/usersCount.csv", "data/plista/filtered", trainPrefixes, scale);
		try {
			PlistaMostPopularRecommenderTester tester= new PlistaMostPopularRecommenderTester(dataset, 10, LearningRateStrategy.createWithConstantRate(0.2),true);
			UserModelTrainerPredictor modelTrainerPredictor= new BaseModelPredictor();
			IncrementalFactorUserItemRepresentation representation = new IncrementalFactorUserItemRepresentation(scale, 10, false, modelTrainerPredictor.getHyperParametersSize());
			modelTrainerPredictor.setModelRepresentation(representation);
			
			UserProfileUpdater userUpdater= new UserProfileUpdater(modelTrainerPredictor);
			IUserItemAggregator agregator= new NoPrivacyAggregator();
			IItemProfileUpdater itemUpdater= new ItemProfileUpdater();
			tester.setModelAndUpdaters(representation, userUpdater, agregator, itemUpdater);
			tester.setModelPredictor(modelTrainerPredictor);
			IRPrecisionError rr=(IRPrecisionError) tester.startExperiment(1);
			long numUsers=representation.getNumUsers();
			long numItems=representation.getNumItems();
			System.out.println("num Users: "+numUsers);
			System.out.println("num Items: "+numItems);
			System.out.println("PRecision "+rr.getAveragePrecision());
			System.out.println("P@5 "+rr.getAveragePrecisionsAt5());
			System.out.println("P@10 "+rr.getAveragePrecisionsAt10());
			System.out.println("AUC "+rr.getAverageAUC());
			
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (TasteException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (PrivateRecsysException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
