package edu.uniandes.privateRecsys.onlineRecommender.Evaluationtesters;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.logging.Logger;

import org.apache.mahout.cf.taste.common.TasteException;

import edu.uniandes.privateRecsys.onlineRecommender.BaseModelPredictorWithItemRegularizationUpdate;
import edu.uniandes.privateRecsys.onlineRecommender.BlackListedMetadataItemProfileUpdater;
import edu.uniandes.privateRecsys.onlineRecommender.DifferentialPrivacyMaskingStrategy;
import edu.uniandes.privateRecsys.onlineRecommender.IItemProfileUpdater;
import edu.uniandes.privateRecsys.onlineRecommender.IUserMaskingStrategy;
import edu.uniandes.privateRecsys.onlineRecommender.IUserProfileUpdater;
import edu.uniandes.privateRecsys.onlineRecommender.ItemProfileUpdater;
import edu.uniandes.privateRecsys.onlineRecommender.LearningRateStrategy;
import edu.uniandes.privateRecsys.onlineRecommender.MetadataPredictor;
import edu.uniandes.privateRecsys.onlineRecommender.ModelEvaluator;
import edu.uniandes.privateRecsys.onlineRecommender.ProbabilityMetadataModelPredictor;
import edu.uniandes.privateRecsys.onlineRecommender.UserProfileUpdater;
import edu.uniandes.privateRecsys.onlineRecommender.exception.PrivateRecsysException;
import edu.uniandes.privateRecsys.onlineRecommender.factorModelRepresentation.FactorUserItemRepresentation;
import edu.uniandes.privateRecsys.onlineRecommender.factorModelRepresentation.IncrementalFactorUserItemRepresentation;
import edu.uniandes.privateRecsys.onlineRecommender.ratingScale.OrdinalRatingScale;
import edu.uniandes.privateRecsys.onlineRecommender.ratingScale.RatingScale;
import edu.uniandes.privateRecsys.onlineRecommender.vo.ErrorReport;

public class DifferentialPrivacyOnlineRecommenderTester extends AbstractRecommenderTester {


	
	private final static Logger LOG = Logger.getLogger(DifferentialPrivacyOnlineRecommenderTester.class
		      .getName());

	public DifferentialPrivacyOnlineRecommenderTester(RSDataset data, int fDimensions) throws IOException {
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
			//new OnlineRecommenderTester("data/ml-100k/ua.base", "data/ml-100k/testingFile", 10).startRecommendations();
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
			 HashMap<String,String> translations=new HashMap<String,String>();
			 translations.put("0.5", "1");
			 translations.put("1.5", "2");
			 translations.put("2.5", "3");
			 translations.put("3.5", "4");
			 translations.put("4.5", "5");
			RatingScale scale= new OrdinalRatingScale(new String[] {"0.5","1","1.5","2","2.5","3","3.5","4","4.5","5"},translations);
			//RatingScale scale= new OrdinalRatingScale(new String[] {"1","2","3","4","5"});
			LOG.info("Loading model");
			RSDataset data= new RSDataset(trainSet,testCV,testSet,scale);
			double[] cfLearningRate={0.15};
			double[] cbRates={0.75};
			int[] dimensionsArr={5};
			int[] windowSizeArr={60};
			double[] epsilonArr={0,0.01,0.1,0.25,0.5,0.75,1,1.25};
			//double[] epsilonArr={0};
			String[] itemUpdaters={"ItemProfileUpdater","BlackListedMetadata"};  
	
			
			BaseModelPredictorWithItemRegularizationUpdate baseModelPredictor = new BaseModelPredictorWithItemRegularizationUpdate(0.01);
			MetadataPredictor metadataModel = new MetadataPredictor(-1,MetadataPredictor.SKETCH_DEPTH,MetadataPredictor.SKETCH_WIDTH, MetadataPredictor.WINDOW_LENGHT, MetadataPredictor.NUMBER_OF_SEGMENTS, MetadataPredictor.NUMROLLING);
			
			
			ProbabilityMetadataModelPredictor hybrid=(new ProbabilityMetadataModelPredictor(baseModelPredictor,metadataModel));
			HashSet<Long> blacklistedConcepts = new HashSet<Long>();
			
			//Concept genre:Horror count: 1013, id: 461
			blacklistedConcepts.add(461L);
			
			for (int i = 0; i < cfLearningRate.length; i++) {
				for (int j = 0; j < cbRates.length; j++) {
					for (int k = 0; k < dimensionsArr.length; k++) {
						for (int l = 0; l < windowSizeArr.length; l++) {
							for (int m = 0; m < epsilonArr.length; m++) {
								for (int n = 0; n < itemUpdaters.length; n++) {
									double cflearn=cfLearningRate[i];
									double cblearn=cbRates[j];
									int dimensions=dimensionsArr[k];
									int windowSize=windowSizeArr[l];
									double epsilon=epsilonArr[m];
									String itemUpdModel=itemUpdaters[n];
									FactorUserItemRepresentation denseModel = new IncrementalFactorUserItemRepresentation(
											data, dimensions, false, hybrid);

									hybrid.setModelRepresentation(denseModel);

									
									LearningRateStrategy cfLearningRateStrategy = LearningRateStrategy
											.createDecreasingRate(1e-6,cflearn);	
									baseModelPredictor
									.setLearningRateStrategy(cfLearningRateStrategy);
									
									LearningRateStrategy cbLearningRateStrategy = LearningRateStrategy
											.createDecreasingRate(1e-6,cblearn);
									
									metadataModel.setLearningRateStrategy(cbLearningRateStrategy);
									metadataModel.initSketchProperties(
											MetadataPredictor.SKETCH_DEPTH,
											MetadataPredictor.SKETCH_WIDTH, windowSize,
											5, 5);
									
									DifferentialPrivacyOnlineRecommenderTester rest=new DifferentialPrivacyOnlineRecommenderTester(data, dimensions);
									UserProfileUpdater userUp = new UserProfileUpdater(hybrid);
									IUserMaskingStrategy agregator= new DifferentialPrivacyMaskingStrategy(epsilon);
									IItemProfileUpdater itemUpdater= null;
									if(itemUpdModel.equals("ItemProfileUpdater")){
										itemUpdater= new ItemProfileUpdater(hybrid);
									}else{
										
										itemUpdater= new BlackListedMetadataItemProfileUpdater(hybrid, blacklistedConcepts);
									}
									
									rest.setModelAndUpdaters(denseModel, userUp,
											agregator, itemUpdater);
									rest.setModelPredictor(hybrid);
									ErrorReport result = rest.startExperiment(1);
									
									double cfRMSETest=ModelEvaluator.evaluateModel(new File(data.getTestSet()),data.getScale(),baseModelPredictor,0);
									double cbRMSETest=ModelEvaluator.evaluateModel(new File(data.getTestSet()),data.getScale(),metadataModel,0);
									String resultLine = "DiffPrivate predictor"
											+ '\t'
											+ cflearn
											+ ""
											+ '\t'
											+ cblearn
											+ ""
											+ '\t'
											+ dimensions
											+ ""
											+ '\t'
											+ windowSize
											+ ""
											+ '\t'
											+ epsilon
											+ ""
											+ '\t'
											+ itemUpdModel
											+ ""
											+ '\t'
											+ hybrid.calculateRegretBaseExpert()
													.toString() + "" + '\t'
											+ result.toString() + '\t'
											+""+cfRMSETest+'\t'
											+""+cbRMSETest+'\t'
											;
									LOG.info("Finished: "+resultLine);
									results.add(resultLine);
									denseModel = null;
								}
								
							}
		
						}
					}
	
				}		
			}

			
			
			//int eventsReport=100000;
				
				
			for (String string : results) {
				LOG.info(string);
				
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
