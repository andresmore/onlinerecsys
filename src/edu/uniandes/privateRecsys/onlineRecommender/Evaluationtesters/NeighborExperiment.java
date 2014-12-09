
package edu.uniandes.privateRecsys.onlineRecommender.Evaluationtesters;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;

import org.apache.mahout.cf.taste.common.TasteException;

import edu.uniandes.privateRecsys.onlineRecommender.BaseModelPredictor;
import edu.uniandes.privateRecsys.onlineRecommender.IItemProfileUpdater;
import edu.uniandes.privateRecsys.onlineRecommender.IUserMaskingStrategy;
import edu.uniandes.privateRecsys.onlineRecommender.ItemProfileUpdater;
import edu.uniandes.privateRecsys.onlineRecommender.LearningRateStrategy;
import edu.uniandes.privateRecsys.onlineRecommender.NoMaskingStrategy;
import edu.uniandes.privateRecsys.onlineRecommender.UserModelTrainerPredictor;
import edu.uniandes.privateRecsys.onlineRecommender.UserProfileUpdater;
import edu.uniandes.privateRecsys.onlineRecommender.exception.PrivateRecsysException;
import edu.uniandes.privateRecsys.onlineRecommender.factorModelRepresentation.AverageDataModel;
import edu.uniandes.privateRecsys.onlineRecommender.factorModelRepresentation.DenseFactorUserItemRepresentationWithMetadata;
import edu.uniandes.privateRecsys.onlineRecommender.factorModelRepresentation.FactorUserItemRepresentation;
import edu.uniandes.privateRecsys.onlineRecommender.ratingScale.OrdinalRatingScale;
import edu.uniandes.privateRecsys.onlineRecommender.ratingScale.RatingScale;
import edu.uniandes.privateRecsys.onlineRecommender.vo.ErrorReport;

public class NeighborExperiment {

	
	
	public static void main(String[] args) throws IOException, TasteException, PrivateRecsysException {
		
		int numNeighbors[]= {1,5,10,15,20,25,30,35,40,45,50};
		
		String trainSetSemanticMovieLens="data/ml-10M100K/metadata/trainSemantic.txt.sorted";
		String testSetSemanticMovielens="data/ml-10M100K/metadata/testSemantic.txt";
		//String trainSetSemanticMovieLens="data/ml-10M100K/metadata/ra.train.sorted";
		//String testSetSemanticMovielens="data/ml-10M100K/metadata/ra.test";
		String metadataSemanticMovielens="data/ml-10M100K/metadata/unitvectors/spectral-15";
		//String metadataSemanticMovielens="data/ml-10M100K/metadata/mapFile.data";
		String allSemanticMovielens="data/ml-10M100K/metadata/allSemantic.txt";
		 HashMap<String,String> translations=new HashMap<String,String>();
		 translations.put("0.5", "1");
		 translations.put("1.5", "2");
		 translations.put("2.5", "3");
		 translations.put("3.5", "4");
		 translations.put("4.5", "5");
		RatingScale scale= new OrdinalRatingScale(new String[] {"0.5","1","1.5","2","2.5","3","3.5","4","4.5","5"},translations);
		RSMetadataDataset dataset= new RSMetadataDataset(trainSetSemanticMovieLens,testSetSemanticMovielens,testSetSemanticMovielens,scale,metadataSemanticMovielens,allSemanticMovielens);
		
		AbstractRecommenderTester tester=null;
		double delta=0.1;
		
		
		LearningRateStrategy tsCreator=LearningRateStrategy.createWithConstantRate(delta);
			RSMetadataDataset dataset2=(RSMetadataDataset) dataset;
			AverageDataModel averageModel= new AverageDataModel(new File(dataset2.getAllDataset()));
		for (int i = 0; i < numNeighbors.length; i++) {
			
			tester= new OnlineRecommenderTester(dataset2, 10);
			UserModelTrainerPredictor modelTrainerPredictor= new BaseModelPredictor();
			//FactorUserItemRepresentation representation= new DenseFactorUserItemRepresentationWithMetadata(averageModel, dataset.getScale(), 10,dataset2.getSpectralDataFile(),numNeighbors[i],false);
			FactorUserItemRepresentation representation= new DenseFactorUserItemRepresentationWithMetadata(averageModel, dataset.getScale(), 10,dataset2.getSpectralDataFile(),numNeighbors[i],true,modelTrainerPredictor.getHyperParametersSize());
			modelTrainerPredictor.setModelRepresentation(representation);
			modelTrainerPredictor.setLearningRateStrategy(tsCreator);
			UserProfileUpdater userUpdater= new UserProfileUpdater(modelTrainerPredictor);
			IUserMaskingStrategy agregator= new NoMaskingStrategy();
			IItemProfileUpdater itemUpdater= new ItemProfileUpdater(modelTrainerPredictor);
			tester.setModelAndUpdaters(representation, userUpdater, agregator, itemUpdater);
			tester.setModelPredictor(modelTrainerPredictor);
			//tester.setEventsReport(100000);
			ErrorReport rep=tester.startExperiment(1);
			System.out.println(rep.getErrorTest());
			//System.out.println(rep.getPartialErrors());
			
		}
		
		
		
	}
}
