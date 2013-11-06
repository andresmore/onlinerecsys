package edu.uniandes.privateRecsys.onlineRecommender.Evaluationtesters;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.logging.Logger;

import org.apache.mahout.cf.taste.common.TasteException;

import edu.uniandes.privateRecsys.onlineRecommender.IItemProfileUpdater;
import edu.uniandes.privateRecsys.onlineRecommender.IUserItemAggregator;
import edu.uniandes.privateRecsys.onlineRecommender.ItemProfileUpdater;
import edu.uniandes.privateRecsys.onlineRecommender.LearningRateStrategy;
import edu.uniandes.privateRecsys.onlineRecommender.NoPrivacyAggregator;
import edu.uniandes.privateRecsys.onlineRecommender.UserProfileUpdater;
import edu.uniandes.privateRecsys.onlineRecommender.exception.PrivateRecsysException;
import edu.uniandes.privateRecsys.onlineRecommender.factorModelRepresentation.AverageDataModel;
import edu.uniandes.privateRecsys.onlineRecommender.factorModelRepresentation.BaseModelPredictor;
import edu.uniandes.privateRecsys.onlineRecommender.factorModelRepresentation.DenseFactorUserItemRepresentation;
import edu.uniandes.privateRecsys.onlineRecommender.factorModelRepresentation.UserModelTrainerPredictor;
import edu.uniandes.privateRecsys.onlineRecommender.vo.ErrorReport;

public class GridSearchParameterLearningRate {
	
	private final static Logger LOG = Logger.getLogger(GridSearchParameterLearningRate.class
		      .getName());
	private final static int NUM_PARAMS_VECTOR=4;
	
	private double[] learningRateVect=new double[NUM_PARAMS_VECTOR];
	private double[] learningRateLimits= new double[2];

	private double[] dimensionsVect= new double[NUM_PARAMS_VECTOR];
	private double[] dimensionLimits= new double[2];
	
	
	
	
	private double[] bestParamsSoFar= {0,0};
	
	private double bestRMSESoFar=Double.POSITIVE_INFINITY;
	private HashMap<String, Double> results= new HashMap<String, Double>();
	private RSDataset data;
	private LearningRateStrategy tsCreator=null;
	
	//(long) (1000*24*60*60*2); 
	private AverageDataModel model;
	
public GridSearchParameterLearningRate(RSDataset data) throws IOException{
		
		
		setDimensionLimits(5,30);
		setLearningRateLimits(0.01,0.5);
		updateParamVectors();
		this.data=data;
		if(data instanceof RSMetadataDataset)
			this.model= new AverageDataModel(new File(((RSMetadataDataset)data).getAllDataset()));
		else
			this.model= new AverageDataModel(new File(data.getTrainSet()));
		tsCreator=LearningRateStrategy.createWithConstantRate(0.9);
		
	}

	private void updateParamVectors() {
		
		updateParameterVector(this.dimensionLimits,this.dimensionsVect,true);
		updateParameterVector(this.learningRateLimits,this.learningRateVect,false);
	
	}	

	private void setLearningRateLimits(double min, double max) {
		learningRateLimits[0]=min;
		learningRateLimits[1]=max;
	
	}

	private void setDimensionLimits(double min, double max) {
		dimensionLimits[0]=min;
		dimensionLimits[1]=max;
	
	}
	
	private void updateParameterVector(double[] limits, double[] vector, boolean floorIncrement) {
		double range=limits[1]-limits[0];
		vector[0]=limits[0];
		vector[vector.length-1]=limits[1];
		double increment=(double)range/(double)vector.length;
		
		if(floorIncrement)
			increment=Math.floor(increment);
		
		for (int i = 1; i < vector.length-1; i++) {
			vector[i]=vector[0]+i*(increment);
		}
	}

	public void startSearch(int numIters) throws IOException, TasteException, PrivateRecsysException {
		
		
		
		for (int iter = 0; iter < numIters; iter++) {
			
		
		
		for (int i = 0; i < dimensionsVect.length; i++) {
			for (int j = 0; j < learningRateVect.length; j++) {
				
					try {
						String key=createKeyForParams(dimensionsVect[i],learningRateVect[j]);
							if (results.get(key) == null) {
								double rmse = trainAndTestWithData(data,
										dimensionsVect[i],learningRateVect[j]);
								results.put(key, rmse);
								if (rmse < this.bestRMSESoFar) {
									this.bestRMSESoFar = rmse;
									this.bestParamsSoFar[0] = dimensionsVect[i];
									this.bestParamsSoFar[1] = learningRateVect[j];
									
									LOG.info("Found better error, is "
											+ this.bestRMSESoFar
											+ " parameters are: dimensions:"
											+ this.bestParamsSoFar[0]
											+ " learningRate:"
											+ this.bestParamsSoFar[1]
											);
								}
						}
					} catch (TasteException e) {
						LOG.severe("Problem calculating "+e.getMessage());
						throw e;
					} catch (PrivateRecsysException e) {
						LOG.severe("Problem calculating "+e.getMessage());
						throw e;
					}
					
			}
			
		}
		
		calculateNewLimits();
		LOG.info("Finished iteration "+iter+",best error is "
				+ this.bestRMSESoFar
				+ " parameters are: dimensions:"
				+ this.bestParamsSoFar[0]
				+ " learningRate:"
				+ this.bestParamsSoFar[1]
				);
		
		printResults();
		
		}
		
		
	}
	private void printResults() {
		for (String key : this.results.keySet()) {
			LOG.info(key+" "+this.results.get(key));
		}
		
	}
	
	
	private void calculateNewLimits() {
		
		
		updateLimitBasedOnBestValue(bestParamsSoFar[0],dimensionLimits,true);
		updateLimitBasedOnBestValue(bestParamsSoFar[1],learningRateLimits,false);
		
		updateParamVectors();
	
	}
	
	private void updateLimitBasedOnBestValue(double bestValue, double[] limits, boolean floorValues) {
		double range=limits[1]-limits[0];
		double newRange=range/(double)2.0;
		
		if(bestValue-(newRange/2)>=limits[0] &&  bestValue+(newRange/2)<=limits[1]){
			limits[0]=bestValue-(newRange/2);
			limits[1]=bestValue+(newRange/2);
		}else if(bestValue-(newRange/2)<limits[0]){
			limits[1]=limits[0]+newRange;
		}else{
			limits[0]=limits[1]-newRange;
		}
		
		if(floorValues){
			limits[0]=Math.floor(limits[0]);
			limits[1]=Math.floor(limits[1]);
		}
			
		
	}

	private double trainAndTestWithData(RSDataset data, double dimension, double learningRate) throws TasteException, IOException, PrivateRecsysException {
		this.tsCreator=LearningRateStrategy.createWithConstantRate(learningRate);
		int dimensions=(int) dimension;
		UserModelTrainerPredictor modelTrainerPredictor= new BaseModelPredictor();
		DenseFactorUserItemRepresentation denseModel= new DenseFactorUserItemRepresentation(this.model, data.getScale(), dimensions,modelTrainerPredictor.getHyperParametersSize());
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

	private String createKeyForParams(double d, double e) {
		
		return d+" "+e;
	}
	
	
}
	
	
	



