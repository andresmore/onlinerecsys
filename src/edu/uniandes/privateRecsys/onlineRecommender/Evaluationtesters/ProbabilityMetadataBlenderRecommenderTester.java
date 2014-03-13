package edu.uniandes.privateRecsys.onlineRecommender.Evaluationtesters;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.logging.Logger;

import org.apache.mahout.cf.taste.common.TasteException;

import edu.uniandes.privateRecsys.onlineRecommender.BaseModelPredictor;
import edu.uniandes.privateRecsys.onlineRecommender.BaseModelPredictorWithItemRegularizationUpdate;
import edu.uniandes.privateRecsys.onlineRecommender.FileEventCreator;
import edu.uniandes.privateRecsys.onlineRecommender.IItemProfileUpdater;
import edu.uniandes.privateRecsys.onlineRecommender.IUserItemAggregator;
import edu.uniandes.privateRecsys.onlineRecommender.ItemProfileUpdater;
import edu.uniandes.privateRecsys.onlineRecommender.LearningRateStrategy;
import edu.uniandes.privateRecsys.onlineRecommender.MetadataPredictor;
import edu.uniandes.privateRecsys.onlineRecommender.ModelEvaluator;
import edu.uniandes.privateRecsys.onlineRecommender.NoPrivacyAggregator;
import edu.uniandes.privateRecsys.onlineRecommender.PrivateRecommenderParallelTrainer;
import edu.uniandes.privateRecsys.onlineRecommender.ProbabilityMetadataModelPredictor;
import edu.uniandes.privateRecsys.onlineRecommender.UserModelTrainerPredictor;
import edu.uniandes.privateRecsys.onlineRecommender.UserProfileUpdater;
import edu.uniandes.privateRecsys.onlineRecommender.exception.PrivateRecsysException;
import edu.uniandes.privateRecsys.onlineRecommender.factorModelRepresentation.FactorUserItemRepresentation;
import edu.uniandes.privateRecsys.onlineRecommender.factorModelRepresentation.IncrementalFactorUserItemRepresentation;
import edu.uniandes.privateRecsys.onlineRecommender.ratingScale.OrdinalRatingScale;
import edu.uniandes.privateRecsys.onlineRecommender.ratingScale.RatingScale;
import edu.uniandes.privateRecsys.onlineRecommender.vo.ErrorReport;
import edu.uniandes.privateRecsys.onlineRecommender.vo.RMSE_ErrorReport;

public class ProbabilityMetadataBlenderRecommenderTester extends AbstractRecommenderTester {

	
	
	private final static Logger LOG = Logger.getLogger(ProbabilityMetadataBlenderRecommenderTester.class
		      .getName());
	private RSDataset dataset;
	private int dimensions;
	private ProbabilityMetadataModelPredictor trainerPredictor;
	
	private double[] learningRates={0.01,0.05,0.15,0.25,0.5,0.75,0.9};
	private double initialGammaBaseModel;
	public ProbabilityMetadataBlenderRecommenderTester(RSDataset dataset, int fDimensions,
			ProbabilityMetadataModelPredictor modelPredictor, double initialGammaBaseModel)
			throws IOException {
		super(dataset, fDimensions);
		this.trainerPredictor=modelPredictor;
		this.initialGammaBaseModel=initialGammaBaseModel;
		
	}
	
	
	public void startTraining() throws TasteException, IOException, PrivateRecsysException{
		LinkedList<String> results= new LinkedList<String>();
		for (int j = 0; j < learningRates.length; j++) {
			LearningRateStrategy learningRateStrategy = LearningRateStrategy.createDecreasingRate(1e-6, learningRates[j]);
			FactorUserItemRepresentation denseModel = new IncrementalFactorUserItemRepresentation(
					dataset.getScale(), dimensions, false, trainerPredictor);
			
			this.trainerPredictor.setBaseModelLearningRateStrategy(learningRateStrategy);
			
			
			LearningRateStrategy tsCreator = LearningRateStrategy.createDecreasingRate(1e-6, initialGammaBaseModel);
			this.trainerPredictor.setMetadataModelModelLearningRateStrategy(tsCreator);
			
			UserProfileUpdater userUp = new UserProfileUpdater(
					trainerPredictor);
			IUserItemAggregator agregator = new NoPrivacyAggregator();
			IItemProfileUpdater itemUpdater = new ItemProfileUpdater(
					trainerPredictor);
			setModelAndUpdaters(denseModel, userUp, agregator,
					itemUpdater);
			setModelPredictor(trainerPredictor);
			ErrorReport result = startExperiment(1);
			String resultLine=this.predictor.toString()+ "" + '\t'
					+ learningRates[j] + "" + '\t'
					+ result.toString()+'\t'+trainerPredictor.calculateRegretBaseExpert().toString();
			
			results.add(resultLine);
			denseModel = null;
			
			
		}
		for (String string : results) {
			LOG.info(string);
			
		}
		
	}	
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		
		
	}
	
	

	

}
