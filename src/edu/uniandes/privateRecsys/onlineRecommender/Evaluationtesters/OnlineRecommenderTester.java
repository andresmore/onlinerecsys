package edu.uniandes.privateRecsys.onlineRecommender.Evaluationtesters;

import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.logging.Logger;

import org.apache.mahout.cf.taste.common.TasteException;

import edu.uniandes.privateRecsys.onlineRecommender.BaseModelPredictor;
import edu.uniandes.privateRecsys.onlineRecommender.BaseModelPredictorWithItemRegularizationUpdate;
import edu.uniandes.privateRecsys.onlineRecommender.IItemProfileUpdater;
import edu.uniandes.privateRecsys.onlineRecommender.IUserItemAggregator;
import edu.uniandes.privateRecsys.onlineRecommender.ItemProfileUpdater;
import edu.uniandes.privateRecsys.onlineRecommender.ItemTrainLimitPrivacyAggregator;
import edu.uniandes.privateRecsys.onlineRecommender.LearningRateStrategy;
import edu.uniandes.privateRecsys.onlineRecommender.MetadataPredictor;
import edu.uniandes.privateRecsys.onlineRecommender.NoPrivacyAggregator;
import edu.uniandes.privateRecsys.onlineRecommender.ProbabilityBiasMetadataModelPredictor;
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

public class OnlineRecommenderTester extends AbstractRecommenderTester {

	
	
	private final static Logger LOG = Logger.getLogger(OnlineRecommenderTester.class
		      .getName());

	public OnlineRecommenderTester(RSDataset dataset, int fDimensions
			)
			throws IOException {
		super(dataset, fDimensions);
		
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		try {
			
			LinkedList<String> results= new LinkedList<>();
			String trainSet=new String("data/dbBook/ra.train.meta");
			//String trainSet=new String("data/ml-10M100K/rb.train.sorted");
			//String trainSet=new String("data/ml-10M100K/rb.train.meta.sorted");
			//String trainSet=new String("data/ml-1m/rb.train.sorted");
			//String trainSet=new String("data/ml-1m/rb.train.meta.sorted");
			//String trainSet="data/netflix/rb.train.sorted";
			
			String testSet=new String("data/dbBook/ra.test.meta");
			//String testSet=new String("data/ml-10M100K/rb.test.test");
			//String testSet=new String("data/ml-10M100K/rb.test.meta.test");
			//String testSet=new String("data/ml-1m/rb.test.test");
			//String testSet=new String("data/ml-1m/rb.test.meta.test");
			//String testSet="data/netflix/rb.test.test";
			
			String testCV=new String("data/dbBook/ra.test.meta");
			//String testCV=new String("data/ml-10M100K/rb.test.cv");
			//String testCV=new String("data/ml-10M100K/rb.test.meta.cv");
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
			RSDataset data= new RSDataset(trainSet,testSet,testCV,scale);
			
			
			
			
			
			
			int[] limitSizes={5,10,25,50,75,100};
			double[] learningRates={0.01,0.05,0.15,0.25,0.3,0.4,0.5,0.6,0.75,0.9};
			int[] trainLimits={5,10,25,50,75,100,150,200,-1};
			LinkedList<UserModelTrainerPredictor> predictorsLinked= new LinkedList<UserModelTrainerPredictor>();
			//predictorsLinked.add(new BayesAveragePredictor());	
			BaseModelPredictorWithItemRegularizationUpdate baseModelPredictor = new BaseModelPredictorWithItemRegularizationUpdate(0);
			predictorsLinked.add(baseModelPredictor);
			//BaseModelPredictor basemodel= new BaseModelPredictor();
			//predictorsLinked.add(basemodel);
			//predictorsLinked.add(new BlendedModelPredictor());
			//predictorsLinked.add(new  MetadataSimilarityPredictor());
			MetadataPredictor metadataModel = new MetadataPredictor(50);
			predictorsLinked.add(metadataModel);
			
			predictorsLinked.add(new ProbabilityMetadataModelPredictor(baseModelPredictor,metadataModel));
			//predictorsLinked.add(new BaseModelPredictorWithItemRegularizationUpdate(0));
			
			
			Object[] predictors=  predictorsLinked.toArray();
				
			for (int i = 0; i < predictors.length; i++) {
				
				for (int j = 0; j < learningRates.length; j++) {
					
					int dimensions=limitSizes[0];
						
						UserModelTrainerPredictor trainerPredictor = (UserModelTrainerPredictor) predictors[i];
						FactorUserItemRepresentation denseModel = new IncrementalFactorUserItemRepresentation(
								scale, dimensions, false, trainerPredictor);
						
						trainerPredictor.setModelRepresentation(denseModel);
						if(trainerPredictor instanceof ProbabilityMetadataModelPredictor){
							LearningRateStrategy learningRateStrategy = LearningRateStrategy.createDecreasingRate(1e-6, learningRates[j]);
							baseModelPredictor.setLearningRateStrategy(learningRateStrategy);
							LearningRateStrategy tsCreator = LearningRateStrategy.createDecreasingRate(1e-6, 0.75);
							metadataModel.setLearningRateStrategy(tsCreator);
							
						}
						else{
							trainerPredictor.setLearningRateStrategy( LearningRateStrategy.createDecreasingRate(1e-6, learningRates[j]));
						}
						

						OnlineRecommenderTester rest = new OnlineRecommenderTester(
								data, dimensions);
						// rest.setEventsReport(1000000);
						UserProfileUpdater userUp = new UserProfileUpdater(
								trainerPredictor);
						//int limit=trainLimits[j];
						IUserItemAggregator agregator = new NoPrivacyAggregator();
						IItemProfileUpdater itemUpdater = new ItemProfileUpdater(
								trainerPredictor);
						rest.setModelAndUpdaters(denseModel, userUp, agregator,
								itemUpdater);
						rest.setModelPredictor(trainerPredictor);
						ErrorReport result = rest.startExperiment(50);
						String resultLine=predictors[i] + "" + '\t'
								+ learningRates[j] + "" + '\t'
								+ result.toString();
						if(trainerPredictor instanceof ProbabilityMetadataModelPredictor){
							ProbabilityMetadataModelPredictor blender=(ProbabilityMetadataModelPredictor) trainerPredictor;
							resultLine=resultLine+'\t'+blender.calculateRegretBaseExpert().toString(); 
						}
						results.add(resultLine);
						denseModel = null;

				
				}

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
	


	

}
