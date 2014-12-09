package edu.uniandes.privateRecsys.onlineRecommender.Evaluationtesters;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.logging.Logger;

import org.apache.mahout.cf.taste.common.TasteException;

import edu.uniandes.privateRecsys.onlineRecommender.BaseModelPredictor;
import edu.uniandes.privateRecsys.onlineRecommender.BaseModelPredictorWithItemRegularizationUpdate;
import edu.uniandes.privateRecsys.onlineRecommender.IItemProfileUpdater;
import edu.uniandes.privateRecsys.onlineRecommender.IUserMaskingStrategy;
import edu.uniandes.privateRecsys.onlineRecommender.ItemProfileUpdater;
import edu.uniandes.privateRecsys.onlineRecommender.ItemTrainLimitPrivacyAggregator;
import edu.uniandes.privateRecsys.onlineRecommender.LearningRateStrategy;
import edu.uniandes.privateRecsys.onlineRecommender.MetadataPredictor;
import edu.uniandes.privateRecsys.onlineRecommender.ModelEvaluator;
import edu.uniandes.privateRecsys.onlineRecommender.NoMaskingStrategy;
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

public class HybridModelRecommenderTester extends AbstractRecommenderTester {

	
	
	private final static Logger LOG = Logger.getLogger(HybridModelRecommenderTester.class
		      .getName());

	public HybridModelRecommenderTester(RSDataset dataset, int fDimensions
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
			RSDataset data= new RSDataset(trainSet,testSet,testCV,scale);
			
			
			
			
			
			
			int[] limitSizes={5};
			int[] windowSizes={60};
			double[] learningRates={0.01,0.15,0.25,0.35,0.5,0.75,0.85};
			//double[] learningRates={0.35};
			
			double[] metaDatalearningRates={0.75};
			
			
			BaseModelPredictorWithItemRegularizationUpdate baseModelPredictor = new BaseModelPredictorWithItemRegularizationUpdate(0.01);
			MetadataPredictor metadataModel = new MetadataPredictor(-1,MetadataPredictor.SKETCH_DEPTH,MetadataPredictor.SKETCH_WIDTH, MetadataPredictor.WINDOW_LENGHT, MetadataPredictor.NUMBER_OF_SEGMENTS, MetadataPredictor.NUMROLLING);
			
			
			ProbabilityMetadataModelPredictor hybrid=(new ProbabilityMetadataModelPredictor(baseModelPredictor,metadataModel));
			//predictorsLinked.add(new BaseModelPredictorWithItemRegularizationUpdate(0));
			
			
			
				
			for (int j = 0; j < learningRates.length; j++) {

				for (int d = 0; d < limitSizes.length; d++) {
					for (int i = 0; i < windowSizes.length; i++) {

						for (int k = 0; k < metaDatalearningRates.length; k++) {

							int dimensions = limitSizes[d];
							int windowSize = windowSizes[i];
							double cfLearningRate = learningRates[j];
							double cbLearningRate = metaDatalearningRates[k];
							//double cbLearningRate = cfLearningRate;

							FactorUserItemRepresentation denseModel = new IncrementalFactorUserItemRepresentation(
									scale, dimensions, false, hybrid);

							hybrid.setModelRepresentation(denseModel);

							LearningRateStrategy learningRateStrategy = LearningRateStrategy
									.createDecreasingRate(1e-6, cfLearningRate);
							baseModelPredictor
									.setLearningRateStrategy(learningRateStrategy);
							LearningRateStrategy tsCreator = LearningRateStrategy
									.createDecreasingRate(1e-6, cbLearningRate);
							metadataModel.setLearningRateStrategy(tsCreator);

							metadataModel.initSketchProperties(
									MetadataPredictor.SKETCH_DEPTH,
									MetadataPredictor.SKETCH_WIDTH, windowSize,
									5, 5);

							HybridModelRecommenderTester rest = new HybridModelRecommenderTester(
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
							
							double cfRMSETest=ModelEvaluator.evaluateModel(new File(data.getTestSet()),data.getScale(),baseModelPredictor,3);
							double cbRMSETest=ModelEvaluator.evaluateModel(new File(data.getTestSet()),data.getScale(),metadataModel,3);
							
							String resultLine = "HybridPredictor"
									+ '\t'
									+cfLearningRate
									+ ""
									+ '\t'
									+ cbLearningRate
									+ ""
									+ '\t'
									+ dimensions
									+ ""
									+ '\t'
									+ windowSize
									+ ""
									+ '\t'
									+ hybrid.calculateRegretBaseExpert()
											.toString() + "" + '\t'
									+ result.toString() + '\t'
									+""+cfRMSETest+'\t'
									+""+cbRMSETest+'\t'
									;

							results.add(resultLine);
							denseModel = null;
						}
					}
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
