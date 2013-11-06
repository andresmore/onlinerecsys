package edu.uniandes.privateRecsys.onlineRecommender.Evaluationtesters;

import java.io.IOException;
import java.util.LinkedList;
import java.util.logging.Logger;

import org.apache.mahout.cf.taste.common.TasteException;

import edu.uniandes.privateRecsys.onlineRecommender.BaseModelPredictor;
import edu.uniandes.privateRecsys.onlineRecommender.IItemProfileUpdater;
import edu.uniandes.privateRecsys.onlineRecommender.IUserItemAggregator;
import edu.uniandes.privateRecsys.onlineRecommender.ItemProfileUpdater;
import edu.uniandes.privateRecsys.onlineRecommender.LearningRateStrategy;
import edu.uniandes.privateRecsys.onlineRecommender.NoPrivacyAggregator;
import edu.uniandes.privateRecsys.onlineRecommender.UserModelTrainerPredictor;
import edu.uniandes.privateRecsys.onlineRecommender.UserProfileUpdater;
import edu.uniandes.privateRecsys.onlineRecommender.exception.PrivateRecsysException;
import edu.uniandes.privateRecsys.onlineRecommender.factorModelRepresentation.FactorUserItemRepresentation;
import edu.uniandes.privateRecsys.onlineRecommender.factorModelRepresentation.IncrementalFactorUserItemRepresentation;
import edu.uniandes.privateRecsys.onlineRecommender.ratingScale.OrdinalRatingScale;
import edu.uniandes.privateRecsys.onlineRecommender.ratingScale.RatingScale;
import edu.uniandes.privateRecsys.onlineRecommender.vo.ErrorReport;

public class OnlineRecommenderTester extends AbstractRecommenderTester {

	
	
	private final static Logger LOG = Logger.getLogger(OnlineRecommenderTester.class
		      .getName());

	public OnlineRecommenderTester(RSDataset dataset, int fDimensions,
			LearningRateStrategy tsCreator)
			throws IOException {
		super(dataset, fDimensions, tsCreator);
		
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		try {
			//new OnlineRecommenderTester("data/ml-100k/ua.base", "data/ml-100k/testingFile", 10).startRecommendations();
			
			LinkedList<String> results= new LinkedList<>();
			String trainSet="data/ml-10M100K/rb.train.sorted";
			//String trainSet="data/netflix/rb.train.sorted";
			String testSet="data/ml-10M100K/rb.test.test";
			//String testSet="data/netflix/rb.test.test";
			String testCV="data/ml-10M100K/rb.test.cv";
			//String testCV="data/netflix/rb.test.CV";
			LOG.info("Loading model");
			//RatingScale scale= new OrdinalRatingScale(new String[] {"1","2","3","4","5"});
			RatingScale scale= new OrdinalRatingScale(new String[] {"0.5","1","1.5","2","2.5","3","3.5","4","4.5","5"});
			RSDataset data= new RSDataset(trainSet,testSet,testCV,scale);
			//AverageDataModel model= new AverageDataModel(new File(trainSet));
			double delta=0.1;
			
			
			//LearningRateStrategy tsCreator=LearningRateStrategy.createWithConstantRate(delta);
			LearningRateStrategy tsCreator=LearningRateStrategy.createDecreasingRate(1e-6, 0.1);
			int dimensions=5;
			
				
				
				UserModelTrainerPredictor trainerPredictor= new BaseModelPredictor();
			
				FactorUserItemRepresentation denseModel= new IncrementalFactorUserItemRepresentation(scale, dimensions, false,trainerPredictor.getHyperParametersSize());
				trainerPredictor.setModelRepresentation(denseModel);
				OnlineRecommenderTester rest=new OnlineRecommenderTester(data, dimensions, tsCreator);
				
				UserProfileUpdater userUp= new UserProfileUpdater(trainerPredictor);
				IUserItemAggregator agregator= new NoPrivacyAggregator();
				IItemProfileUpdater itemUpdater= new ItemProfileUpdater();
				rest.setModelAndUpdaters(denseModel, userUp, agregator, itemUpdater);
				rest.setModelPredictor(trainerPredictor);
				ErrorReport result=rest.startExperiment(10);
				results.add(result.toString());
			
			for (String string : results) {
				System.out.println(string);
			}
			
			
		
			
			//OnlineRecommenderTester rest=new OnlineRecommenderTester("data/ml-100k/ua.base", "data/ml-100k/testingFile", 5, 1*24*60*60);
			//double result=rest.startRecommendations();
			//System.out.println(result);
		} catch (IOException e) {
		
			e.printStackTrace();
		} catch (TasteException e) {
			
			e.printStackTrace();
		} catch (PrivateRecsysException e) {
			
			e.printStackTrace();
		}

	}
	


	

}
