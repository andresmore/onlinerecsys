package edu.uniandes.privateRecsys.onlineRecommender.Evaluationtesters;

import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.logging.Logger;

import org.apache.mahout.cf.taste.common.TasteException;
import org.jfree.util.Log;
import org.mortbay.servlet.UserAgentFilter;

import edu.uniandes.privateRecsys.onlineRecommender.FileEventCreator;
import edu.uniandes.privateRecsys.onlineRecommender.IItemProfileUpdater;
import edu.uniandes.privateRecsys.onlineRecommender.IUserItemAggregator;
import edu.uniandes.privateRecsys.onlineRecommender.IUserProfileUpdater;
import edu.uniandes.privateRecsys.onlineRecommender.ItemProfileUpdater;
import edu.uniandes.privateRecsys.onlineRecommender.LearningRateStrategy;
import edu.uniandes.privateRecsys.onlineRecommender.ModelEvaluator;
import edu.uniandes.privateRecsys.onlineRecommender.NoPrivacyAggregator;
import edu.uniandes.privateRecsys.onlineRecommender.PrivateRecommenderParallelTrainer;
import edu.uniandes.privateRecsys.onlineRecommender.RMSE_Evaluator;
import edu.uniandes.privateRecsys.onlineRecommender.UserProfileUpdater;
import edu.uniandes.privateRecsys.onlineRecommender.exception.PrivateRecsysException;
import edu.uniandes.privateRecsys.onlineRecommender.factorModelRepresentation.AverageDataModel;
import edu.uniandes.privateRecsys.onlineRecommender.factorModelRepresentation.DenseFactorUserItemRepresentation;
import edu.uniandes.privateRecsys.onlineRecommender.factorModelRepresentation.FactorUserItemRepresentation;
import edu.uniandes.privateRecsys.onlineRecommender.factorModelRepresentation.IncrementalDBFactorUserItemRepresentation;
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
			//String trainSet="data/ml-10M100K/rb.train.sorted";
			String trainSet="data/netflix/rb.train.sorted";
			//String testSet="data/ml-10M100K/rb.test.test";
			String testSet="data/netflix/rb.test.test";
			//String testCV="data/ml-10M100K/rb.test.cv";
			String testCV="data/netflix/rb.test.CV";
			LOG.info("Loading model");
			RatingScale scale= new OrdinalRatingScale(new String[] {"1","2","3","4","5"});
			//RatingScale scale= new OrdinalRatingScale(new String[] {"0.5","1","1.5","2","2.5","3","3.5","4","4.5","5"});
			RSDataset data= new RSDataset(trainSet,testSet,testCV,scale);
			//AverageDataModel model= new AverageDataModel(new File(trainSet));
			double delta=0.1;
			
			
			LearningRateStrategy tsCreator=LearningRateStrategy.createWithConstantRate(delta);
			int dimensions=5;
			
				
				
				
			
				FactorUserItemRepresentation denseModel= new IncrementalDBFactorUserItemRepresentation(scale, dimensions, false);
				OnlineRecommenderTester rest=new OnlineRecommenderTester(data, dimensions, tsCreator);
				UserProfileUpdater userUp= new UserProfileUpdater();
				IUserItemAggregator agregator= new NoPrivacyAggregator();
				IItemProfileUpdater itemUpdater= new ItemProfileUpdater();
				rest.setModelAndUpdaters(denseModel, userUp, agregator, itemUpdater);
				ErrorReport result=rest.startExperiment(1);
				results.add(result.toString());
			
			for (String string : results) {
				System.out.println(string);
			}
			
			
		
			
			//OnlineRecommenderTester rest=new OnlineRecommenderTester("data/ml-100k/ua.base", "data/ml-100k/testingFile", 5, 1*24*60*60);
			//double result=rest.startRecommendations();
			//System.out.println(result);
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
