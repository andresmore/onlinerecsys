package edu.uniandes.privateRecsys.onlineRecommender.Evaluationtesters;

import java.io.IOException;
import java.util.LinkedList;
import java.util.logging.Logger;

import org.apache.mahout.cf.taste.common.TasteException;

import edu.uniandes.privateRecsys.onlineRecommender.IItemProfileUpdater;
import edu.uniandes.privateRecsys.onlineRecommender.IUserMaskingStrategy;
import edu.uniandes.privateRecsys.onlineRecommender.ItemProfileUpdater;
import edu.uniandes.privateRecsys.onlineRecommender.LearningRateStrategy;
import edu.uniandes.privateRecsys.onlineRecommender.NoMaskingStrategy;
import edu.uniandes.privateRecsys.onlineRecommender.ProbabilityMetadataModelPredictor;
import edu.uniandes.privateRecsys.onlineRecommender.UserProfileUpdater;
import edu.uniandes.privateRecsys.onlineRecommender.exception.PrivateRecsysException;
import edu.uniandes.privateRecsys.onlineRecommender.factorModelRepresentation.FactorUserItemRepresentation;
import edu.uniandes.privateRecsys.onlineRecommender.factorModelRepresentation.IncrementalFactorUserItemRepresentation;
import edu.uniandes.privateRecsys.onlineRecommender.vo.ErrorReport;

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
					dataset, dimensions, false, trainerPredictor);
			
			this.trainerPredictor.setBaseModelLearningRateStrategy(learningRateStrategy);
			
			
			LearningRateStrategy tsCreator = LearningRateStrategy.createDecreasingRate(1e-6, initialGammaBaseModel);
			this.trainerPredictor.setMetadataModelModelLearningRateStrategy(tsCreator);
			
			UserProfileUpdater userUp = new UserProfileUpdater(
					trainerPredictor);
			IUserMaskingStrategy agregator = new NoMaskingStrategy();
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
