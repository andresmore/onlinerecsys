package edu.uniandes.privateRecsys.onlineRecommender.Evaluationtesters;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Iterator;
import java.util.Observable;
import java.util.Observer;
import java.util.logging.Logger;

import org.apache.mahout.cf.taste.common.TasteException;
import org.apache.mahout.cf.taste.impl.common.FullRunningAverage;
import org.apache.mahout.cf.taste.impl.common.RunningAverage;
import org.jfree.util.Log;

import edu.uniandes.privateRecsys.onlineRecommender.FileEventCreator;
import edu.uniandes.privateRecsys.onlineRecommender.exception.PrivateRecsysException;
import edu.uniandes.privateRecsys.onlineRecommender.factorModelRepresentation.AverageDataModel;
import edu.uniandes.privateRecsys.onlineRecommender.vo.UserTrainEvent;

public class AveragePredictorTester implements Observer {
	
	
	private RSDataset dataset;
	private RunningAverage globalAvg= new FullRunningAverage();
	private RunningAverage userAvg= new FullRunningAverage();
	private RunningAverage itemAvg= new FullRunningAverage();
	private AverageDataModel model;
	private final static Logger LOG = Logger.getLogger(AveragePredictorTester.class
		      .getName());

	private int numPredictions=0;
	public AveragePredictorTester(RSDataset dataset) {
		this.dataset=dataset;
		
	}

	public static void main(String[] args) throws FileNotFoundException, IOException {
		
		
		RSDataset rsDat=RSDataset.fromPropertyFile("config/yMusic.properties");
		AveragePredictorTester tester= new AveragePredictorTester(rsDat);
		try {
			double rmse=tester.startExperiment();
			System.out.println("RMSE is "+rmse);
		} catch (IOException e) {
			
			e.printStackTrace();
		} catch (PrivateRecsysException e) {
			
			e.printStackTrace();
		}
		
		
	}

	private  double startExperiment() throws IOException, PrivateRecsysException {
		this.model= new AverageDataModel(new File(dataset.getTrainSet()));
		
		FileEventCreator cec = new FileEventCreator(new File(dataset.getTestSet()), -1,-1);
		
		cec.addObserver(this);
		cec.startEvents();
		
		LOG.info("Errors are average "+getRMSEGlobal()+" userAverage"+getRMSEUserAverage()+" itemAverage"+getRMSEItemAverage());
		return getRMSEGlobal();
	}
	
	private double getRMSEItemAverage() {
		
		 return Math.sqrt(this.itemAvg.getAverage());
	}

	private double getRMSEUserAverage() {
		
		return Math.sqrt(this.userAvg.getAverage());
	}

	public double getRMSEGlobal(){
		return Math.sqrt(this.globalAvg.getAverage());
	}

	@Override
	public void update(Observable o, Object arg) {
		UserTrainEvent event = (UserTrainEvent) arg;
		long itemId = event.getItemId();
		long userId = event.getUserId();
		String rating = event.getRating();
	
		
			double globalPrediction = this.model.getGlobalAverage(); 
			
			float globalDiff = (float) (Double.parseDouble(rating) - globalPrediction);
			globalAvg.addDatum(globalDiff * globalDiff);
			
			double itemPrediction=globalPrediction;
			try {
				itemPrediction = getAverageForItem(itemId);
			} catch (PrivateRecsysException e) {
				LOG.severe(e.getMessage());
			}
			float itemDiff = (float) (Double.parseDouble(rating) - itemPrediction);
			itemAvg.addDatum(itemDiff*itemDiff);
			
			double userPrediction=globalPrediction;
			try {
				userPrediction = getAverageForUser(userId);
			} catch (PrivateRecsysException e) {
				LOG.severe(e.getMessage());
			}
			float userDiff = (float) (Double.parseDouble(rating) - userPrediction);
			userAvg.addDatum(userDiff*userDiff);
			
			
			numPredictions++;
				if(numPredictions%1000==0)
					System.out.println("Making prediction #"+numPredictions);
		
		
		
		
		
	}

	private double getAverageForUser(long userId) throws PrivateRecsysException {
		
		double average = this.model.getAverageForUserId(userId);
		if(Double.isNaN(average))
			throw new PrivateRecsysException("Error");
		return average;
	}

	private double getAverageForItem(long itemId) throws PrivateRecsysException {
		
		double average = this.model.getAverageForItemId(itemId);
		if(Double.isNaN(average))
			throw new PrivateRecsysException("Error");
		return average;
	}

	

}
