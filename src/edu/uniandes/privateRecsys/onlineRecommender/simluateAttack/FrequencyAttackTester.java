package edu.uniandes.privateRecsys.onlineRecommender.simluateAttack;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map.Entry;
import java.util.Observable;
import java.util.Observer;
import java.util.logging.Logger;

import org.apache.mahout.cf.taste.common.TasteException;
import org.apache.mahout.cf.taste.impl.common.FullRunningAverage;
import org.apache.mahout.math.DenseVector;
import org.apache.mahout.math.Vector;

import edu.uniandes.privateRecsys.onlineRecommender.FileEventCreator;
import edu.uniandes.privateRecsys.onlineRecommender.Evaluationtesters.RSDataset;
import edu.uniandes.privateRecsys.onlineRecommender.exception.PrivateRecsysException;
import edu.uniandes.privateRecsys.onlineRecommender.factorModelRepresentation.VectorProjector;
import edu.uniandes.privateRecsys.onlineRecommender.ratingScale.OrdinalRatingScale;
import edu.uniandes.privateRecsys.onlineRecommender.ratingScale.RatingScale;
import edu.uniandes.privateRecsys.onlineRecommender.vo.ErrorReport;
import edu.uniandes.privateRecsys.onlineRecommender.vo.FileEvent;
import edu.uniandes.privateRecsys.onlineRecommender.vo.RMSE_ErrorReport;
import edu.uniandes.privateRecsys.onlineRecommender.vo.UserTrainEvent;

public class FrequencyAttackTester implements Observer {

	
	
	private final static Logger LOG = Logger.getLogger(FrequencyAttackTester.class
		      .getName());
	private static final String TRAINING = "train";
	private static final String TESTING = "test";
	
	private static final double NOT_ENOUGH_RATINGS = -1;
	private RSDataset dataset;
	private String state=TRAINING;
	private int[] distributionValues;
	
	private LinkedList<Double> orderedKnownPreferences= new LinkedList<Double>();
	
	//private Vector knownDistribution= VectorProjector.projectVectorIntoSimplex(new DenseVector(knownDistributionValues));
	
	private HashMap<Long,HashMap<Double,Integer>> userFrequencyMap= new HashMap<Long, HashMap<Double,Integer>>();
	
	private FullRunningAverage avg= new FullRunningAverage();
	private int minRatings = 0;
	private int numPredictions;
	
	public FrequencyAttackTester(RSDataset dataset)
			throws IOException {
		this.dataset=dataset;
		this.distributionValues=new int[dataset.getScale().getScale().length];
	
		
	}

	private LinkedList<Double> createOrderKnownPreferences(
			int[] distributionValues) {
		double[] scaleAsArray=this.dataset.getScale().scaleAsValues();
		int[] sortedDistrib=distributionValues.clone();
		
		Arrays.sort(sortedDistrib );
		LinkedList<Double> retList= new LinkedList<Double>();
		for (int i = sortedDistrib.length-1; i >= 0; i--) {
			int frequency=sortedDistrib[i];
			
			for (int j = 0; j < distributionValues.length; j++) {
				if(frequency==distributionValues[j]&&!retList.contains(scaleAsArray[j])){
					retList.add(scaleAsArray[j]);
				}
			}
		}
		
		return retList;
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
			
			
			
	
						FrequencyAttackTester rest = new FrequencyAttackTester(
								data);
						
					
						ErrorReport result = rest.startExperiment();
						
						
			
			
		} catch (IOException e) {
		
			e.printStackTrace();
		} catch (TasteException e) {
			
			e.printStackTrace();
		} catch (PrivateRecsysException e) {
			
			e.printStackTrace();
		}

	}
	
	
	/***
	 * 
	 * @return The rmse of the experiment
	 * @throws IOException If the files specified are not accesible
	 * @throws TasteException If the format of the file is not valid
	 * @throws PrivateRecsysException
	 */
	public ErrorReport startExperiment( ) throws IOException, TasteException, PrivateRecsysException {
		
			
		LOG.info("Starting experiment");
		double error=0;
		double errorTrain=0;
		double errorCV=0;
		LinkedList<Double> partialErrors= new LinkedList<>();
	
		
			
			FileEventCreator cec= new FileEventCreator(new File(this.dataset.getTrainSet()),-1,-1);
			cec.addObserver(this);
		
			cec.startEvents();
			
			
		
			
				LOG.info("Finished training, updating  distribution ");

				int[] distributionValues2={0,1,2,4,3};
				//TODO:Generalizar
				distributionValues=distributionValues2;
				this.orderedKnownPreferences=createOrderKnownPreferences(distributionValues);
				
				System.out.println(this.orderedKnownPreferences);
				int[] minRatingsArr={0,10,20,30,40,50,60,70,80,90,100};
				for (int i = 0; i < minRatingsArr.length; i++) {
					errorTrain=evaluateModel(minRatingsArr[i]);
					System.out.println("Error was: "+minRatingsArr[i]+'\t'+errorTrain+'\t'+this.numPredictions);
				}
				
				//System.out.println("Error at iteration "+iteration+" is: Train: "+errorTrain+" CV:"+errorCV+" Test:"+error);
			
		
		return new RMSE_ErrorReport(errorTrain, error, errorCV,partialErrors);//""+errorTrain+'\t'+errorCV+'\t'+error;
		
	}
	
	private double evaluateModel(int minRatings) throws IOException, PrivateRecsysException {
		this.numPredictions=0;
		FileEventCreator cec= new FileEventCreator(new File(this.dataset.getTrainSet()),-1,-1);
		cec.addObserver(this);
		this.state=TESTING;
		cec.startEvents();
		this.minRatings=minRatings;
		
		//return Math.sqrt(this.avg.getAverage());
		return this.avg.getAverage();
	}
	
	public void processEvent(UserTrainEvent event) {
		
		long userId=event.getUserId();
		double rating=Double.parseDouble(this.dataset.getScale().getRatingAlias(event.getRating()));
		incrementFrequency(rating);
		
		
		HashMap<Double, Integer> map=this.userFrequencyMap.get(userId);
		if(map==null){
			map=new HashMap<>();
		}
		int number=1;
		if(map.containsKey(rating)){
			number=map.get(rating)+1;
		}
		map.put(rating, number);
		this.userFrequencyMap.put(userId, map);
		
		
	}
	

	private void incrementFrequency(double rating) {
		double[] scaleValues=this.dataset.getScale().scaleAsValues();
		for (int i = 0; i < scaleValues.length; i++) {
			if(scaleValues[i]==rating)
				this.distributionValues[i]=this.distributionValues[i]+1;
		}
		
	}

	@Override
	public void update(Observable o, Object arg) {
		FileEvent event = (FileEvent) arg;
		try {
			if(event.convertToTrainEvent()!=null&& state.equals(TRAINING)){
				processEvent(event.convertToTrainEvent());
			}
			else if(state.equals(TESTING)){
				calculateError(event.convertToTrainEvent());
			}
		
		} catch (Exception e) {
			
			e.printStackTrace();
		}
		
	}

	private void calculateError(UserTrainEvent event) throws Exception {
		long userId=event.getUserId();
		String rating=this.dataset.getScale().getRatingAlias(event.getRating());
		double realRating=Double.parseDouble(rating);
		HashMap<Double, Integer> map=this.userFrequencyMap.get(userId);
		double guessValue=guessValueFromUserDistribution(this.orderedKnownPreferences, map,realRating);
		if(guessValue!=NOT_ENOUGH_RATINGS){
			double error=realRating-guessValue;
			this.avg.addDatum(Math.abs(error));
			this.numPredictions++;
		}
		
		
	}

	private double guessValueFromUserDistribution(LinkedList<Double> orderedKnownPreferences,
			HashMap<Double, Integer> map, double realRating) {
		
		LinkedList<Entry<Double, Integer> > entryList= new LinkedList<Entry<Double, Integer>> ();
		entryList.addAll(map.entrySet());
		
		
		Collections.sort(entryList, new Comparator<Entry<Double, Integer>>() {
			
			@Override
			public int compare(Entry<Double, Integer> o1, Entry<Double, Integer> o2) {
				// TODO Auto-generated method stub
				return -1*(o1.getValue().compareTo(o2.getValue()));
			}
		});
		
		for (int i = 0; i < entryList.size(); i++) {
			if(entryList.get(i).getKey()==realRating && entryList.get(i).getValue()>this.minRatings )
				return orderedKnownPreferences.get(i);
		}
		
		
		return NOT_ENOUGH_RATINGS;
		
	}


	

}
