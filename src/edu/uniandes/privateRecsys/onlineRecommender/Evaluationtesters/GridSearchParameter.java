package edu.uniandes.privateRecsys.onlineRecommender.Evaluationtesters;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.logging.Logger;

import org.apache.commons.math.util.MathUtils;
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
import edu.uniandes.privateRecsys.onlineRecommender.factorModelRepresentation.IncrementalFactorUserItemRepresentation;
import edu.uniandes.privateRecsys.onlineRecommender.ratingScale.OrdinalRatingScale;
import edu.uniandes.privateRecsys.onlineRecommender.ratingScale.RatingScale;
import edu.uniandes.privateRecsys.onlineRecommender.vo.ErrorReport;

public class GridSearchParameter {
	private final static Logger LOG = Logger.getLogger(GridSearchParameter.class
		      .getName());
	private final static int NUM_PARAMS_VECTOR=4;
	
	private double[] alphaVect=new double[NUM_PARAMS_VECTOR];
	private double[] alphaLimits= new double[2];
	private double[] initGammaVect= new double[NUM_PARAMS_VECTOR];
	private double[] initGammaLimits= new double[2];
	private double[] dimensionsVect= new double[NUM_PARAMS_VECTOR];
	private double[] dimensionLimits= new double[2];
	
	
	
	private double[] bestParamsSoFar= {0,0,0};
	private double bestRMSESoFar=Double.POSITIVE_INFINITY;
	private HashMap<String, Double> results= new HashMap<String, Double>();
	private RSDataset data;
	private LearningRateStrategy tsCreator=null;
	
	
	//(long) (1000*24*60*60*2); 


	
	
	
	public GridSearchParameter(RSDataset data) throws IOException{
		
		setAlphaLimits(1e-6,1);
		setInitialGammaLimits(0.01,0.5);
		setDimensionLimits(5,15);
		
		updateParamVectors();
		this.data=data;
		
		
			tsCreator=LearningRateStrategy.createDecreasingRate(this.alphaVect[0], this.initGammaVect[0]);
		
	}
	
	
	


	private void updateParamVectors() {
		
		updateParameterVector(this.alphaLimits,this.alphaVect);
		updateParameterVector(this.initGammaLimits,this.initGammaVect);
		updateParameterVector(this.dimensionLimits,this.dimensionsVect);
		
		
	}


	





	private void updateParameterVector(double[] limits, double[] vector) {
		double range=limits[1]-limits[0];
		vector[0]=limits[0];
		vector[vector.length-1]=limits[1];
		double incrementUnit=range/vector.length;
		if(vector.equals(this.dimensionsVect))
			incrementUnit=Math.floor(incrementUnit);
		for (int i = 1; i < vector.length-1; i++) {
			
			vector[i]=MathUtils.round(vector[0]+i*(incrementUnit), 2, BigDecimal.ROUND_HALF_DOWN);;
		}
	}


	private void setDimensionLimits(int min, int max) {
		dimensionLimits[0]=min;
		dimensionLimits[1]=max;
		
	}


	private void setInitialGammaLimits(double min, double max) {
		initGammaLimits[0]=min;
		initGammaLimits[1]=max;
		
	}


	private void setAlphaLimits(double min, double max) {
		alphaLimits[0]=min;
		alphaLimits[1]=max;
		
	}
	
	

	public void startSearch(int numIters) throws IOException, TasteException, PrivateRecsysException{
		
		
		
		for (int iter = 0; iter < numIters; iter++) {
			
		
		
		for (int i = 0; i < alphaVect.length; i++) {
			for (int j = 0; j < initGammaVect.length; j++) {
				for (int k = 0; k < dimensionsVect.length; k++) {
					try {
						String key=createKeyForParams(alphaVect[i],initGammaVect[j],dimensionsVect[k]);
							if (results.get(key) == null) {
								double rmse =trainAndTestWithData(
										alphaVect[i], initGammaVect[j],
										dimensionsVect[k]);
								results.put(key, rmse);
								if (rmse < this.bestRMSESoFar) {
									this.bestRMSESoFar = rmse;
									this.bestParamsSoFar[0] = alphaVect[i];
									this.bestParamsSoFar[1] = initGammaVect[j];
									this.bestParamsSoFar[2] = dimensionsVect[k];
									LOG.info("Found better error, is "
											+ this.bestRMSESoFar
											+ " parameters are: alpha:"
											+ this.bestParamsSoFar[0]
											+ " initGamma:"
											+ this.bestParamsSoFar[1]
											+ " dimensions:"
											+ this.bestParamsSoFar[2]);
								}
						}
					} catch (Exception e) {
						LOG.severe("Problem calculating "+e.getMessage());
						throw e;
					} 
					
				}
			}
		}
		
		calculateNewLimits();
		LOG.info("Finished iteration, final error is "+this.bestRMSESoFar+
				" parameters are: alpha:"+this.bestParamsSoFar[0]+" initGamma:"+this.bestParamsSoFar[1]+" dimensions:"+this.bestParamsSoFar[2]);
		
		printResults();
		
		}
		
		
	}


	private void printResults() {
		for (String key : this.results.keySet()) {
			LOG.info(key+" "+this.results.get(key));
		}
		
	}


	private String createKeyForParams(double l, double m, double n) {
		
		return ""+l+" "+m+" "+n;
	}


	private void calculateNewLimits() {
	
			updateLimitBasedOnBestValue(bestParamsSoFar[0],alphaLimits);
			updateLimitBasedOnBestValue(bestParamsSoFar[1],initGammaLimits);
			updateLimitBasedOnBestValue(bestParamsSoFar[2],dimensionLimits);
			updateParamVectors();
		
	}


	private void updateLimitBasedOnBestValue(double bestValue, double[] limits) {
		double range=limits[1]-limits[0];
		double newRange=range/2;
		
		if(bestValue-(newRange/2)>=limits[0] &&  bestValue+(newRange/2)<=limits[1]){
			limits[0]=bestValue-(newRange/2);
			limits[1]=bestValue+(newRange/2);
		}else if(bestValue-(newRange/2)<limits[0]){
			limits[1]=limits[0]+newRange;
		}else{
			limits[0]=limits[1]-newRange;
		}
		
	}
	
	


	private double trainAndTestWithData(double alpha, double initialGamma, double dim) throws IOException, TasteException, PrivateRecsysException {
		
		
		this.tsCreator= LearningRateStrategy.createDecreasingRate(alpha, initialGamma);
		int dimensions=(int) dim;
		UserModelTrainerPredictor modelTrainerPredictor= new BaseModelPredictor();
		IncrementalFactorUserItemRepresentation denseModel= new IncrementalFactorUserItemRepresentation( data.getScale(), dimensions, false,modelTrainerPredictor.getHyperParametersSize());
		modelTrainerPredictor.setModelRepresentation(denseModel);
		
		OnlineRecommenderTester rest=new OnlineRecommenderTester(data, dimensions, tsCreator);
		
		UserProfileUpdater userUp= new UserProfileUpdater(modelTrainerPredictor);
		IUserItemAggregator agregator= new NoPrivacyAggregator();
		IItemProfileUpdater itemUpdater= new ItemProfileUpdater();
		rest.setModelAndUpdaters(denseModel, userUp, agregator, itemUpdater);
		rest.setModelPredictor(modelTrainerPredictor);
		ErrorReport result=rest.startExperiment(1);
		denseModel=null;
		return result.getErrorTest();
	}
	
	public static void main(String[] args) {
		String trainSetMovieLens="data/ml-10M100K/rb.train.sorted";
		String testSetMovielens="data/ml-10M100K/rb.test.test";
		String testCVMovielens="data/ml-10M100K/rb.test.cv";
		 HashMap<String,String> translations=new HashMap<String,String>();
		 translations.put("0.5", "1");
		 translations.put("1.5", "2");
		 translations.put("2.5", "3");
		 translations.put("3.5", "4");
		 translations.put("4.5", "5");
		RatingScale scale= new OrdinalRatingScale(new String[] {"0.5","1","1.5","2","2.5","3","3.5","4","4.5","5"},translations);
		RSDataset datasetMovielens= new RSDataset(trainSetMovieLens,testSetMovielens,testCVMovielens,scale);
		
		String trainSetNetflix="data/netflix/rb.train.sorted";
		String testSetNetflix="data/netflix/rb.test.test";
		String testCVNetflix="data/netflix/rb.test.CV";
		RatingScale scaleNetflix= new OrdinalRatingScale(new String[] {"1","2","3","4","5"}, new HashMap<String,String>());
		RSDataset datasetNetflix= new RSDataset(trainSetNetflix,testSetNetflix,testCVNetflix,scaleNetflix);
		try {
			new GridSearchParameter(datasetMovielens).startSearch(1);
		} catch (IOException | TasteException | PrivateRecsysException e) {
			
			e.printStackTrace();
		}
	}
	
	
	
	

}
