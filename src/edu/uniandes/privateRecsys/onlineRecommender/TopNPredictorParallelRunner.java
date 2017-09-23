package edu.uniandes.privateRecsys.onlineRecommender;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.mahout.cf.taste.common.TasteException;

import edu.uniandes.privateRecsys.onlineRecommender.factorModelRepresentation.FactorUserItemRepresentation;
import edu.uniandes.privateRecsys.onlineRecommender.vo.Prediction;

public class TopNPredictorParallelRunner implements Runnable {

	private TopNPredictorParallelCalculator parent;
	private Long userID;

	private TopNRecommender topRecommender;
	private FactorUserItemRepresentation userItemRep;
	private String testFile;
	private int minTrainsForItem;
	private int N;
	private boolean preloadedTest=false;
	private HashMap<Long, Set<Long>> testPreferences;

	public TopNPredictorParallelRunner(
			TopNPredictorParallelCalculator topNPredictorParallelCalculator,
			Long userID, FactorUserItemRepresentation userItemRep, TopNRecommender topRecommender, String testFile, int minTrainsForItem, int N,HashMap<Long, Set<Long>> testPreferences) {
		this.parent=topNPredictorParallelCalculator;
		this.userItemRep=userItemRep;
		this.userID=userID;
		this.testFile=testFile;
		this.topRecommender=topRecommender;
		this.minTrainsForItem=minTrainsForItem;
		this.N=N;
		this.testPreferences=testPreferences;
		if(testPreferences!=null)
			preloadedTest=true;
		
	}
	
	

	@Override
	public void run() {
		if (userID != 0 && userID != -1) {
			Prediction[] topNPrediction = null;
			try {
				topNPrediction = topRecommender.getTopRecommendationForUsers(userItemRep.getItemsId(minTrainsForItem),userItemRep.getRatedItems(userID),
						userID,  N, this.minTrainsForItem);
			} catch (TasteException e) {

			}
			if (topNPrediction != null) {
				
				Set<Long> positiveRecommendations =null;
				if(!preloadedTest) {
					positiveRecommendations = userItemRep.getPositiveElements(userID, testFile);
				}
				else {
					positiveRecommendations =testPreferences.get(userID);
				}
				
				int truePosLocal = 0;
				int falsePosLocal = 0;
				boolean hit=false;
				int posFirstHit=0;
				LinkedList<Double> precisionsAt = new LinkedList<Double>();
				LinkedList<Integer> aucCurve = new LinkedList<Integer>();
				
				for (Prediction recMovieId : topNPrediction) {
					if (positiveRecommendations
							.contains(recMovieId.getItemId())) {
						truePosLocal++;
						aucCurve.add(1);
						if(!hit) {
							posFirstHit=aucCurve.size();
						}
						hit=true;
					} else {
						falsePosLocal++;
						aucCurve.add(0);
					}

					precisionsAt
							.add((double) ((double) truePosLocal / ((double) truePosLocal + (double) falsePosLocal)));

				}
				// MAP per user
				double averagePR = 0;

				for (Double precsAt : precisionsAt) {
					averagePR += precsAt;
				}
				averagePR = (double) averagePR / (double) precisionsAt.size();
				parent.addNewPrecision(averagePR);
				
				// Precision@5 for user
				if (precisionsAt.size() >= 5)
					parent.addNewPrecisionAt5(precisionsAt.get(4));
				
				// Precision@10 for user
				if (precisionsAt.size() >= 10)
					parent.addNewPrecisionAt10(precisionsAt.get(9));
				
				// AUC curve per user
				double trapezoidSum = 0.0;
				int negativeCount = 0;
				for (int j = 0; j < aucCurve.size(); j++) {
					if (aucCurve.get(j) == 1) {
						trapezoidSum += (1.0 - (double) ((double) negativeCount / (double) aucCurve
								.size()));
					} else {
						negativeCount++;
					}

				}
				trapezoidSum = (double) trapezoidSum / (double) aucCurve.size();
				parent.addNewAUC(trapezoidSum);
				parent.addFirstHit(posFirstHit);
				parent.incrementNumExecutedTasks();
				
			}
		}

	}

}
