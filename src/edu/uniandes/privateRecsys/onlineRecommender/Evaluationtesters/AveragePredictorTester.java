package edu.uniandes.privateRecsys.onlineRecommender.Evaluationtesters;

import java.io.File;
import java.io.IOException;
import java.util.Observable;
import java.util.Observer;

import org.apache.mahout.cf.taste.common.TasteException;
import org.apache.mahout.cf.taste.impl.common.FullRunningAverage;
import org.apache.mahout.cf.taste.impl.common.RunningAverage;
import org.apache.mahout.cf.taste.impl.model.file.FileDataModel;
import org.apache.mahout.cf.taste.model.Preference;
import org.apache.mahout.cf.taste.model.PreferenceArray;

import edu.uniandes.privateRecsys.onlineRecommender.FileEventCreator;
import edu.uniandes.privateRecsys.onlineRecommender.exception.PrivateRecsysException;
import edu.uniandes.privateRecsys.onlineRecommender.vo.UserTrainEvent;

public class AveragePredictorTester implements Observer {
	
	
	private String trainSet;
	private String testSet;
	private RunningAverage runAvg= new FullRunningAverage();
	private FileDataModel model;

	private int numPredictions=0;
	public AveragePredictorTester(String trainSet, String testSet) {
		this.trainSet=trainSet;
		this.testSet=testSet;
		
	}

	public static void main(String[] args) {
		
		String trainSet="data/netflix/rb.train.sorted";
		String testSet="data/netflix/rb.test.test";
		
		AveragePredictorTester tester= new AveragePredictorTester(trainSet, testSet);
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
		this.model= new FileDataModel(new File(trainSet));
		
		FileEventCreator cec = new FileEventCreator(new File(this.testSet), -1,-1);
		
		cec.addObserver(this);
		cec.startEvents();
		
		
		return Math.sqrt(this.runAvg.getAverage());
	}

	@Override
	public void update(Observable o, Object arg) {
		UserTrainEvent event = (UserTrainEvent) arg;
		long itemId = event.getItemId();
		long userId = event.getUserId();
		String rating = event.getRating();
		long time = event.getTime();

		
		try {
			double prediction = getAverageForItem(itemId);
			float diff = (float) (Double.parseDouble(rating) - prediction);
			runAvg.addDatum(diff * diff);
			numPredictions++;
				if(numPredictions%1000==0)
					System.out.println("Making prediction #"+numPredictions);
		} catch (TasteException e) {
			
			e.printStackTrace();
		}
		
		
		
		
	}

	private double getAverageForItem(long itemId) throws TasteException {
		RunningAverage avg= new FullRunningAverage();
		PreferenceArray prefArr=this.model.getPreferencesForItem(itemId);
		for (Preference preference : prefArr) {
			avg.addDatum(preference.getValue());
		}
		return avg.getAverage();
	}

}
