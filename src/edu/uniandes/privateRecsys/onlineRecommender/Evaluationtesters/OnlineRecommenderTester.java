package edu.uniandes.privateRecsys.onlineRecommender.Evaluationtesters;

import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.logging.Logger;

import org.apache.mahout.cf.taste.common.TasteException;

import edu.uniandes.privateRecsys.onlineRecommender.IItemProfileUpdater;
import edu.uniandes.privateRecsys.onlineRecommender.IUserItemAggregator;
import edu.uniandes.privateRecsys.onlineRecommender.ItemProfileUpdater;
import edu.uniandes.privateRecsys.onlineRecommender.LearningRateStrategy;
import edu.uniandes.privateRecsys.onlineRecommender.MetadataPredictor;
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
			
			LinkedList<String> results= new LinkedList<>();
			//String trainSet=new String("data/ml-10M100K/rb.train.sorted");
			String trainSet=new String("data/ml-10M100K/rb.train.meta.sorted");
			//String trainSet="data/netflix/rb.train.sorted";
			//String testSet=new String("data/ml-10M100K/rb.test.test");
			String testSet=new String("data/ml-10M100K/rb.test.meta.test");
			//String testSet="data/netflix/rb.test.test";
			//String testCV=new String("data/ml-10M100K/rb.test.cv");
			String testCV=new String("data/ml-10M100K/rb.test.meta.cv");
			//String testCV="data/netflix/rb.test.CV";
			LOG.info("Loading model");
			
			 HashMap<String,String> translations=new HashMap<String,String>();
			 translations.put(new String("0.5"), new String("1"));
			 translations.put(new String("1.5"), new String("2"));
			 translations.put(new String("2.5"), new String("3"));
			 translations.put(new String("3.5"), new String("4"));
			 translations.put(new String("4.5"), new String("5"));
			RatingScale scale= new OrdinalRatingScale(new String[] {new String("0.5"),new String("1"),new String("1.5"),new String("2"),new String("2.5"),new String("3"),new String("3.5"),new String("4"),new String("4.5"),new String("5")},translations);
			RSDataset data= new RSDataset(trainSet,testSet,testCV,scale);
			
			
			
			
			
			int dimensions=5;
			int[] limitSizes={5,10,50};
			double[] learningRates={0.01,0.05,0.1,0.5};
			for (int j = 0; j < limitSizes.length; j++) {
				for (int i = 0; i < learningRates.length; i++) {
					double delta=learningRates[i];
					LearningRateStrategy tsCreator=LearningRateStrategy.createWithConstantRate(delta);
					//LearningRateStrategy tsCreator=LearningRateStrategy.createDecreasingRate(1e-6, 0.01);
					//UserModelTrainerPredictor trainerPredictor= new ProbabilityBiasMetadataSimilarityModelPredictor();
					//UserModelTrainerPredictor trainerPredictor= new MetadataSimilarityPredictor();
					//UserModelTrainerPredictor trainerPredictor= new BayesAveragePredictor();
					//UserModelTrainerPredictor trainerPredictor= new BlendedModelPredictor();
					//UserModelTrainerPredictor trainerPredictor= new MetadataSimilarityPredictor();
					//UserModelTrainerPredictor trainerPredictor= new BaseModelPredictor();
					UserModelTrainerPredictor trainerPredictor= new MetadataPredictor(limitSizes[j]);
					FactorUserItemRepresentation denseModel= new IncrementalFactorUserItemRepresentation(scale, dimensions, false,trainerPredictor);
					//FactorUserItemRepresentation denseModel= new DenseFactorUserItemRepresentation(new AverageDataModel(new File(data.getTrainSet())), scale, dimensions, trainerPredictor.getHyperParametersSize());
					trainerPredictor.setModelRepresentation(denseModel);
					OnlineRecommenderTester rest=new OnlineRecommenderTester(data, dimensions, tsCreator);
					//rest.setEventsReport(1000000);
					UserProfileUpdater userUp= new UserProfileUpdater(trainerPredictor);
					IUserItemAggregator agregator= new NoPrivacyAggregator();
					IItemProfileUpdater itemUpdater= new ItemProfileUpdater(trainerPredictor);
					rest.setModelAndUpdaters(denseModel, userUp, agregator, itemUpdater);
					rest.setModelPredictor(trainerPredictor);
					ErrorReport result=rest.startExperiment(1);
					results.add(limitSizes[j]+" "+learningRates[i]+" "+result.toString());	
				}
				
			
			}	
				
			
			for (String string : results) {
				System.out.println(string);
			}
			
			
		} catch (IOException e) {
		
			e.printStackTrace();
		} catch (TasteException e) {
			
			e.printStackTrace();
		} catch (PrivateRecsysException e) {
			
			e.printStackTrace();
		}

	}
	


	

}
