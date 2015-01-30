package edu.uniandes.privateRecsys.onlineRecommender.Evaluationtesters;

import java.io.File;
import java.io.IOException;
import java.util.Observable;
import java.util.Observer;

import org.apache.mahout.cf.taste.impl.common.FullRunningAverage;
import org.apache.mahout.cf.taste.impl.common.RunningAverage;

import edu.uniandes.privateRecsys.onlineRecommender.FileEventCreator;
import edu.uniandes.privateRecsys.onlineRecommender.exception.PrivateRecsysException;
import edu.uniandes.privateRecsys.onlineRecommender.factorModelRepresentation.AverageDataModel;
import edu.uniandes.privateRecsys.onlineRecommender.vo.UserTrainEvent;

public class AveragePredictorTesterWithAvgModel implements Observer {
	
	
	private String trainSet;
	private RunningAverage runAvg= new FullRunningAverage();
	private AverageDataModel model;

	private int numPredictions=0;
	public AveragePredictorTesterWithAvgModel(String trainSet) throws IOException {
		this.trainSet=trainSet;
		this.model= new AverageDataModel(new File(trainSet));
		
	}

	public static void main(String[] args) {
		
		String trainSet="data/netflix/rb.train.sorted";
		String testSet="data/netflix/rb.test.test";
		String cvSet="data/netflix/rb.test.CV";
		
		
		try {
			AveragePredictorTesterWithAvgModel tester= new AveragePredictorTesterWithAvgModel(trainSet);
			double rmse=tester.startExperiment(trainSet);
			double rmse2=tester.startExperiment(testSet);
			double rmse3=tester.startExperiment(cvSet);
			System.out.println("RMSE trainset is "+rmse);
			System.out.println("RMSE testset is "+rmse2);
			System.out.println("RMSE cv is "+rmse3);
		} catch (IOException e) {
			
			e.printStackTrace();
		} catch (PrivateRecsysException e) {
			
			e.printStackTrace();
		}
		
		
	}

	private  double startExperiment(String testSet) throws IOException, PrivateRecsysException {
		
		this.runAvg= new FullRunningAverage();
		FileEventCreator cec = new FileEventCreator(new File(testSet), -1,-1);
		
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

	
			double prediction;
			try {
				prediction = getAverageForItem(itemId);
				float diff = (float) (Double.parseDouble(rating) - prediction);
				runAvg.addDatum(diff * diff);
				numPredictions++;
					if(numPredictions%1000==0)
						System.out.println("Making prediction #"+numPredictions);
			} catch (PrivateRecsysException e) {
				
				e.printStackTrace();
			}
			
		
		
		
		
	}

	private double getAverageForItem(long itemId) throws PrivateRecsysException  {
		return model.getAverageForItemId(itemId);
	}

}
