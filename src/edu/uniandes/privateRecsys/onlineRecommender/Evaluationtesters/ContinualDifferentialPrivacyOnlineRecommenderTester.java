package edu.uniandes.privateRecsys.onlineRecommender.Evaluationtesters;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.logging.Logger;

import org.apache.mahout.cf.taste.common.TasteException;

import edu.uniandes.privateRecsys.onlineRecommender.BaseModelPredictor;
import edu.uniandes.privateRecsys.onlineRecommender.ContinualDifferentialPrivacyAggregator;
import edu.uniandes.privateRecsys.onlineRecommender.IItemProfileUpdater;
import edu.uniandes.privateRecsys.onlineRecommender.IUserMaskingStrategy;
import edu.uniandes.privateRecsys.onlineRecommender.IUserProfileUpdater;
import edu.uniandes.privateRecsys.onlineRecommender.ItemProfileUpdater;
import edu.uniandes.privateRecsys.onlineRecommender.LearningRateStrategy;
import edu.uniandes.privateRecsys.onlineRecommender.UserModelTrainerPredictor;
import edu.uniandes.privateRecsys.onlineRecommender.UserProfileUpdater;
import edu.uniandes.privateRecsys.onlineRecommender.exception.PrivateRecsysException;
import edu.uniandes.privateRecsys.onlineRecommender.factorModelRepresentation.AverageDataModel;
import edu.uniandes.privateRecsys.onlineRecommender.factorModelRepresentation.FactorUserItemRepresentation;
import edu.uniandes.privateRecsys.onlineRecommender.factorModelRepresentation.IncrementalFactorUserItemRepresentation;
import edu.uniandes.privateRecsys.onlineRecommender.ratingScale.OrdinalRatingScale;
import edu.uniandes.privateRecsys.onlineRecommender.ratingScale.RatingScale;
import edu.uniandes.privateRecsys.onlineRecommender.vo.RMSE_ErrorReport;

public class ContinualDifferentialPrivacyOnlineRecommenderTester extends AbstractRecommenderTester  {

	
	private final static Logger LOG = Logger.getLogger(ContinualDifferentialPrivacyOnlineRecommenderTester.class
		      .getName());

	public ContinualDifferentialPrivacyOnlineRecommenderTester(RSDataset data, int fDimensions) throws IOException {
		super(data, fDimensions);
	}

	@Override
	public void setModelAndUpdaters(
			FactorUserItemRepresentation representation,
			IUserProfileUpdater userUpdater, IUserMaskingStrategy agregator,
			IItemProfileUpdater itemUpdater) throws TasteException {
		if(!(agregator instanceof ContinualDifferentialPrivacyAggregator))
			throw new TasteException("ContinualDifferentialPrivacyOnlineRecommenderTester has to have a ContinualDifferentialPrivacyAggregator as user-item agregator");
		this.userItemRep=representation;
		this.userUpdater=userUpdater;
		this.userAggregator=agregator;
		this.itemProfileUpdater=itemUpdater;
	}	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		try {
			//new OnlineRecommenderTester("data/ml-100k/ua.base", "data/ml-100k/testingFile", 10).startRecommendations();
			
			LinkedList<RMSE_ErrorReport> results= new LinkedList<>();
			
			String trainSet="data/ml-10M100K/rb.train.sorted";
			//String trainSet="data/netflix/rb.train.sorted";
			String testSet="data/ml-10M100K/rb.test.test";
			//String testSet="data/netflix/rb.test.test";
			String testCV="data/ml-10M100K/rb.test.cv";
			//String testCV="data/netflix/rb.test.CV";
			 HashMap<String,String> translations=new HashMap<String,String>();
			 translations.put("0.5", "1");
			 translations.put("1.5", "2");
			 translations.put("2.5", "3");
			 translations.put("3.5", "4");
			 translations.put("4.5", "5");
			RatingScale scale= new OrdinalRatingScale(new String[] {"0.5","1","1.5","2","2.5","3","3.5","4","4.5","5"},translations);
			//RatingScale scale= new OrdinalRatingScale(new String[] {"1","2","3","4","5"});
			LOG.info("Loading model");
			RSDataset data= new RSDataset(trainSet,testCV,testCV,scale);
			AverageDataModel model= new AverageDataModel(new File(trainSet));
			double delta=0.1;
			
			
			LearningRateStrategy tsCreator=LearningRateStrategy.createWithConstantRate(delta);
			int dimensions=5;
			int eventsReport=100000;
			
				
				
				
				ContinualDifferentialPrivacyOnlineRecommenderTester rest=new ContinualDifferentialPrivacyOnlineRecommenderTester(data, dimensions);
				UserModelTrainerPredictor modelTrainer=new BaseModelPredictor();
				IncrementalFactorUserItemRepresentation representation = new IncrementalFactorUserItemRepresentation(data,dimensions,true, modelTrainer);
				modelTrainer.setModelRepresentation(representation);
				UserProfileUpdater userUpdater= new UserProfileUpdater(modelTrainer);
				IUserMaskingStrategy agregator= new ContinualDifferentialPrivacyAggregator(0.69,1000);
				IItemProfileUpdater itemUpdater= new ItemProfileUpdater(modelTrainer);
				rest.setModelAndUpdaters(representation, userUpdater, agregator, itemUpdater);
				rest.setModelPredictor(modelTrainer);
				rest.setEventsReport(eventsReport);
				LOG.info("Before Experiment");
				RMSE_ErrorReport result=(RMSE_ErrorReport) rest.startExperiment(1);
				results.add(result);
			
			for (RMSE_ErrorReport error : results) {

				System.out.println(error);
				
				LinkedList<Double> partialErrorsWhere=error.getPartialErrors();
				int numIter=1;
				for (Double errorPartial : partialErrorsWhere) {
					System.out.println((numIter*eventsReport)+","+errorPartial);
				}
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
