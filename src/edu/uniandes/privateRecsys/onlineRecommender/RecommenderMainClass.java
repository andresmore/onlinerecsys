package edu.uniandes.privateRecsys.onlineRecommender;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;

import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.mahout.cf.taste.common.TasteException;

import edu.uniandes.privateRecsys.onlineRecommender.Evaluationtesters.AbstractRecommenderTester;
import edu.uniandes.privateRecsys.onlineRecommender.Evaluationtesters.ContinualDifferentialPrivacyOnlineRecommenderTester;
import edu.uniandes.privateRecsys.onlineRecommender.Evaluationtesters.DifferentialPrivacyOnlineRecommenderTester;
import edu.uniandes.privateRecsys.onlineRecommender.Evaluationtesters.GridSearchParameter;
import edu.uniandes.privateRecsys.onlineRecommender.Evaluationtesters.GridSearchParameterLearningRate;
import edu.uniandes.privateRecsys.onlineRecommender.Evaluationtesters.OnlineRecommenderTester;
import edu.uniandes.privateRecsys.onlineRecommender.Evaluationtesters.ProbabilityMetadataBlenderRecommenderTester;
import edu.uniandes.privateRecsys.onlineRecommender.Evaluationtesters.RSDataset;
import edu.uniandes.privateRecsys.onlineRecommender.exception.PrivateRecsysException;
import edu.uniandes.privateRecsys.onlineRecommender.factorModelRepresentation.AverageDataModel;
import edu.uniandes.privateRecsys.onlineRecommender.factorModelRepresentation.DenseFactorUserItemRepresentation;
import edu.uniandes.privateRecsys.onlineRecommender.factorModelRepresentation.FactorUserItemRepresentation;
import edu.uniandes.privateRecsys.onlineRecommender.ratingScale.OrdinalRatingScale;
import edu.uniandes.privateRecsys.onlineRecommender.ratingScale.RatingScale;
import edu.uniandes.privateRecsys.onlineRecommender.vo.RMSE_ErrorReport;

/**
 * Class that parses the arguments to launch a recommender with given parameters
 * @author Andres M
 *
 */
public class RecommenderMainClass {
	
	
	/**
	 * Options received by the system
	 */
	private static final String DATASET_OPTION="dataset"; 
	private static final String RECOMMENDER="recommender";
	private static final String ALPHA_LEARNING_RATE="alphaLearningRate";
	private static final String INITIAL_GAMMA="initialGamma";
	private static final String DIMENSIONS="dimensions";
	private static final String PRIVACY_BUDGET="privacyBudget";
	private static final String PRIVACY_TIME_BUDGET="timeBudget";
	private static final String ERROR_REPORT="numErrorReport";
	private static final String CONSTANT_RATE="learningRate";
	private static final String USER_HYPOTHESIS="userHypothesis";
	
	
	
	/**
	 * Available datasets
	 */
	private static final String MOVIELENS_DATASET="movielens";
	private static final String SEMANTIC_MOVIELENS_DATASET="semanticmovielens";
	private static final String NETFLIX_DATASET="netflix";
	
	/**
	 * Available recommenders
	 */
	private static final String CONTINUAL_DIFFERENTIAL_PRIVACY_RECOMMENDER="ContinualDifferentialPrivacyOnlineRecommenderTester";
	private static final String DIFFERENTIAL_PRIVACY_RECOMMENDER="DifferentialPrivacyOnlineRecommenderTester";
	private static final String NO_PRIVACY_RECOMMENDER="OnlineRecommenderTester";
	private static final String GRIDSEARCH="GridSearchParameter";
	private static final String GRIDSEARCH_FIXEDLEARNINGRATE="GridSearchWLearningRate";
	private static final String PROBABILITYMETADATARECOMENDERTESTER="BlenderTesterLearningRate";
	
	
	/**
	 * Available UserProfileUpdaters
	 */
	private static final String BASE_PROFILE_UPDATER="baseProfileUpdater";
	private static final String AVERAGE_PROFILE_UPDATER="averageProfileUpdater";
	private static final String BLENDED_PROFILE_UPDATER="blendedProfileUpdater";
	
	/**
	 * Options recognized by main
	 */
	private Options options;
	private RSDataset datasetMovielens;
	private RSDataset datasetNetflix;
	private RSDataset datasetSemanticMovielens;
	
	
	@SuppressWarnings("static-access")
	public RecommenderMainClass() throws FileNotFoundException{
		
		
		String trainSetMovieLens="data/ml-10M100K/rb.train.sorted";
		String testSetMovielens="data/ml-10M100K/rb.test.test";
		String testCVMovielens="data/ml-10M100K/rb.test.cv";
		 HashMap<String,String> translations=new HashMap<String,String>();
		 translations.put("0.5", "1");
		 translations.put("1.5", "2");
		 translations.put("2.5", "3");
		 translations.put("3.5", "4");
		 translations.put("4.5", "5");
		RatingScale scaleMovielens= new OrdinalRatingScale(new String[] {"0.5","1","1.5","2","2.5","3","3.5","4","4.5","5"},translations);
		this.datasetMovielens= new RSDataset(trainSetMovieLens,testSetMovielens,testCVMovielens,scaleMovielens);
		
		String trainSet=new String("data/ml-10M100K/rb.train.meta.sorted");
		String testSet=new String("data/ml-10M100K/rb.test.meta.test");
		String testCV=new String("data/ml-10M100K/rb.test.meta.cv");
		
		
		
		
		this.datasetSemanticMovielens= new RSDataset(trainSet,testSet,testCV,scaleMovielens);
		
		String trainSetNetflix="data/netflix/rb.train.sorted";
		String testSetNetflix="data/netflix/rb.test.test";
		String testCVNetflix="data/netflix/rb.test.CV";
		RatingScale scaleNetflix= new OrdinalRatingScale(new String[] {"1","2","3","4","5"}, new HashMap<String,String>());
		this.datasetNetflix= new RSDataset(trainSetNetflix,testSetNetflix,testCVNetflix,scaleNetflix);
		
		
		Option datasetOption= OptionBuilder.withArgName(DATASET_OPTION)
				.hasArg(true).withDescription("Dataset used for options").isRequired().create(DATASET_OPTION);
		Option recomenderOption=OptionBuilder.withArgName(RECOMMENDER)
				.hasArg(true).withDescription("Recommender to be tested").isRequired().create(RECOMMENDER);
		Option timeslotOption=OptionBuilder.withArgName(ALPHA_LEARNING_RATE)
				.hasArg(true).withDescription("Alpha learning rate").isRequired(false).create(ALPHA_LEARNING_RATE);
		Option lambdaDenominatorOption=OptionBuilder.withArgName(INITIAL_GAMMA)
				.hasArg(true).withDescription("initial gamma for learning rate").isRequired(false).create(INITIAL_GAMMA);
		Option dimensionsOption=OptionBuilder.withArgName(DIMENSIONS)
				.hasArg(true).withDescription("number of dimensions of model").create(DIMENSIONS);
		Option privacyBudgetOption=OptionBuilder.withArgName(PRIVACY_BUDGET)
				.hasArg(true).withDescription("privacy budget (epsilon)").isRequired(false).create(PRIVACY_BUDGET);
		Option privacyTimeBudgetOption=OptionBuilder.withArgName(PRIVACY_TIME_BUDGET)
				.hasArg(true).withDescription("privacy time budget (T)").isRequired(false).create(PRIVACY_TIME_BUDGET);
		Option errorReportOption=OptionBuilder.withArgName(ERROR_REPORT)
				.hasArg(true).withDescription("Report error every numTrainings ").isRequired(false).create(ERROR_REPORT);
		
		Option constantRateOption=OptionBuilder.withArgName(CONSTANT_RATE)
				.hasArg(true).withDescription("provide a fixed learning rate for the experiment").isRequired(false).create(CONSTANT_RATE);
		Option userProfileUpdater=OptionBuilder.withArgName(USER_HYPOTHESIS)
				.hasArg(true).withDescription("User hypothesis for the experiment").isRequired(false).create(USER_HYPOTHESIS);
		
		
		
		this.options= new Options();
		options.addOption(datasetOption);
		options.addOption(recomenderOption);
		options.addOption(timeslotOption);
		options.addOption(lambdaDenominatorOption);
		options.addOption(dimensionsOption);
		options.addOption(privacyBudgetOption);
		options.addOption(privacyTimeBudgetOption);
		options.addOption(errorReportOption);
	
		options.addOption(constantRateOption);
		options.addOption(userProfileUpdater);
		
		
	}
	
	
	
	private AbstractRecommenderTester processParameters(String[] args) throws ParseException, IOException, TasteException, PrivateRecsysException{
		
		
		CommandLineParser parser= new BasicParser();
		CommandLine line=parser.parse(options, args);
		
		//Checking if dataset parameters are ok
		RSDataset dataset=null;
		if(line.hasOption(DATASET_OPTION)){
			String chosenDataset=line.getOptionValue(DATASET_OPTION);
			if(chosenDataset.equals(MOVIELENS_DATASET)){
				
				dataset=this.datasetMovielens;
			}	
			else if(chosenDataset.equals(NETFLIX_DATASET)){
				
				dataset=this.datasetNetflix;	
				
			}
			else if(chosenDataset.equals(SEMANTIC_MOVIELENS_DATASET)){
				dataset=this.datasetSemanticMovielens;
			}
			else{
				throw new ParseException("Dataset "+chosenDataset+" not found");
			}
		}
		//Check recommender dimensions
		int dimensions=5;
		if(line.hasOption(DIMENSIONS)){
			String dimSize=line.getOptionValue(DIMENSIONS);
			try{
				dimensions=Integer.parseInt(dimSize);
			}catch(NumberFormatException e){
				throw new ParseException("Could not convert dimension "+dimSize+" to Integer");
			}
			
		}
		
		if(line.hasOption(CONSTANT_RATE)&& (line.hasOption(ALPHA_LEARNING_RATE)||line.hasOption(INITIAL_GAMMA))){
			throw new ParseException(CONSTANT_RATE+" option is invalid if used with "+ALPHA_LEARNING_RATE+" or "+INITIAL_GAMMA+" options" );
		}
		
		//Calculate learning rate
		double constantLearningRate=-1.0;
		
		if(line.hasOption(CONSTANT_RATE)){
			String cRateString=line.getOptionValue(CONSTANT_RATE);
			try{
				constantLearningRate=Double.parseDouble(cRateString);
				if(constantLearningRate>1 || constantLearningRate<=0){
					throw new ParseException("Learning rate must be in (0,1]");
				}
			}catch(NumberFormatException e){
				throw new ParseException("Could not convert dimension "+cRateString+" to Double");
			}
			
		}
	
		
		
		//Convert alpha duration (default 0.5)
		double alpha=0.5d;
		if(line.hasOption(ALPHA_LEARNING_RATE)){
			String alphaStr=line.getOptionValue(ALPHA_LEARNING_RATE);
			try{
				alpha=Double.parseDouble(alphaStr);
			}catch(NumberFormatException e){
				throw new ParseException("Could not convert alpha "+alphaStr+" to double");
			}
			
		}		
		
		//Convert initial gamma (default 0.9)
		double initialGamma=0.9d;
		if(line.hasOption(INITIAL_GAMMA)){
			String lambdaStr=line.getOptionValue(INITIAL_GAMMA);
					try{
						initialGamma=Double.parseDouble(lambdaStr);
					}catch(NumberFormatException e){
						throw new ParseException("Could not convert initial gamma "+lambdaStr+" to double");
					}
					
		}
		
		
		
		//Convert privacy budget 
		double privacyBudget=-1;
		if(line.hasOption(PRIVACY_BUDGET)){
			String privacyBudgetStr=line.getOptionValue(PRIVACY_BUDGET);
					try{
						privacyBudget=Double.parseDouble(privacyBudgetStr);
					}catch(NumberFormatException e){
						throw new ParseException("Could not convert privacy budget "+privacyBudgetStr+" to Double");
					}
					
		}
		// Convert privacy time budget
		int privacyTimeBudget = -1;
		if (line.hasOption(PRIVACY_TIME_BUDGET)) {
			String privacyTimeBudgetStr = line.getOptionValue(PRIVACY_TIME_BUDGET);
			try {
				privacyTimeBudget = Integer.parseInt(privacyTimeBudgetStr);
			} catch (NumberFormatException e) {
				throw new ParseException("Could not convert privacy time budget "
						+ privacyTimeBudgetStr + " to Integer");
			}

		}

		// Convert numErrorReport
		int numErrorReport = -1;
		if (line.hasOption(ERROR_REPORT)) {
			String errorReportStr = line
					.getOptionValue(ERROR_REPORT);
			try {
				numErrorReport = Integer.parseInt(errorReportStr);
			} catch (NumberFormatException e) {
				throw new ParseException(
						"Could not convert number error report "
								+ errorReportStr + " to Integer");
			}

		}
		
		
		
			
		
		//Check if recommender is ok
		String chosenRecommender=null;
		AbstractRecommenderTester tester=null;
		LearningRateStrategy tsCreator=null;
		
		
		UserModelTrainerPredictor modelTrainerPredictor=null; 
		
		if(line.hasOption(USER_HYPOTHESIS)){
			String chosenProfileUpdater=line.getOptionValue(USER_HYPOTHESIS);
			if(chosenProfileUpdater.equals(BASE_PROFILE_UPDATER)){
				
				modelTrainerPredictor= new BaseModelPredictor();
			}	
			else if(chosenProfileUpdater.equals(AVERAGE_PROFILE_UPDATER)){
				
				modelTrainerPredictor= new BaseModelPredictor();
			}
			else if(chosenProfileUpdater.equals(BLENDED_PROFILE_UPDATER)){
				
				modelTrainerPredictor= new BlendedModelPredictor(new BaseModelPredictor(), new SimpleAveragePredictor());
			}
			else{
				throw new ParseException("User profileUpdater "+chosenProfileUpdater+" not found");
			}
		}else{
			//Default trainer predictor
			modelTrainerPredictor= new BaseModelPredictor();
		}
		
		if(constantLearningRate==-1)
			tsCreator=LearningRateStrategy.createDecreasingRate(alpha, initialGamma);
		else
			tsCreator= LearningRateStrategy.createWithConstantRate(constantLearningRate);
		
		if(line.hasOption(RECOMMENDER)){
			chosenRecommender=line.getOptionValue(RECOMMENDER);
			if(chosenRecommender.equals(GRIDSEARCH)){
				GridSearchParameter search= new GridSearchParameter(dataset,modelTrainerPredictor);
				
				search.startSearch(4);
				return null;
			}
			else if(chosenRecommender.equals(GRIDSEARCH_FIXEDLEARNINGRATE)){
				GridSearchParameterLearningRate search= new GridSearchParameterLearningRate(dataset);
				//Iterations should be a param...
				search.startSearch(4);
				return null;
			}
			else if(chosenRecommender.equals(PROBABILITYMETADATARECOMENDERTESTER)){
				ProbabilityMetadataBlenderRecommenderTester blenderTester= new ProbabilityMetadataBlenderRecommenderTester(dataset, dimensions, new ProbabilityMetadataModelPredictor(new BaseModelPredictorWithItemRegularizationUpdate(0), new MetadataPredictor(-1,MetadataPredictor.SKETCH_DEPTH,MetadataPredictor.SKETCH_WIDTH, MetadataPredictor.WINDOW_LENGHT, MetadataPredictor.NUMBER_OF_SEGMENTS, MetadataPredictor.NUMROLLING)),0.75);
				blenderTester.startTraining();
				return null;
			}
			
			
			else if(chosenRecommender.equals(CONTINUAL_DIFFERENTIAL_PRIVACY_RECOMMENDER)){
				
				if(privacyBudget==-1 || privacyTimeBudget==-1){
					throw new ParseException("Continual differential privacy recommender needs privacy budget and privacy time budget");
				}
				AverageDataModel averageModel= new AverageDataModel(new File(dataset.getTrainSet()));
				tester= new ContinualDifferentialPrivacyOnlineRecommenderTester(dataset, dimensions);
				FactorUserItemRepresentation representation = new DenseFactorUserItemRepresentation(averageModel, dataset.getScale(), dimensions,modelTrainerPredictor.getHyperParametersSize(),true);
				modelTrainerPredictor.setModelRepresentation(representation);
				modelTrainerPredictor.setLearningRateStrategy(tsCreator);
				
				IUserProfileUpdater userUpdater= new UserProfileUpdater(modelTrainerPredictor);
				IUserMaskingStrategy agregator= new ContinualDifferentialPrivacyAggregator(privacyBudget,privacyTimeBudget);
				IItemProfileUpdater itemUpdater= new ItemProfileUpdater(modelTrainerPredictor);
				tester.setModelAndUpdaters(representation, userUpdater, agregator, itemUpdater);
				tester.setEventsReport(numErrorReport);
				
			}
			else if(chosenRecommender.equals(DIFFERENTIAL_PRIVACY_RECOMMENDER)){
				
				if(privacyBudget==-1){
					throw new ParseException("Differential privacy recommender needs privacy budget");
				}
				AverageDataModel averageModel= new AverageDataModel(new File(dataset.getTrainSet()));
				tester= new DifferentialPrivacyOnlineRecommenderTester(dataset, dimensions);
				FactorUserItemRepresentation representation = new DenseFactorUserItemRepresentation(averageModel, dataset.getScale(), dimensions,modelTrainerPredictor.getHyperParametersSize(),true);
				modelTrainerPredictor.setModelRepresentation(representation);
				modelTrainerPredictor.setLearningRateStrategy(tsCreator);
				IUserProfileUpdater userUpdater= new UserProfileUpdater(modelTrainerPredictor);
				IUserMaskingStrategy agregator= new DifferentialPrivacyMaskingStrategy(privacyBudget);
				IItemProfileUpdater itemUpdater= new ItemProfileUpdater(modelTrainerPredictor);
				tester.setModelAndUpdaters(representation, userUpdater, agregator, itemUpdater);
				tester.setEventsReport(numErrorReport);
			}
			else if(chosenRecommender.equals(NO_PRIVACY_RECOMMENDER)){
				AverageDataModel averageModel= null;
				
				averageModel=new AverageDataModel(new File(dataset.getTrainSet()));
				
				tester= new OnlineRecommenderTester(dataset, dimensions);
				FactorUserItemRepresentation representation = new DenseFactorUserItemRepresentation(averageModel, dataset.getScale(), dimensions,modelTrainerPredictor.getHyperParametersSize(),true);
				modelTrainerPredictor.setModelRepresentation(representation);
				modelTrainerPredictor.setLearningRateStrategy(tsCreator);
				IUserProfileUpdater userUpdater= new UserProfileUpdater(modelTrainerPredictor);
				IUserMaskingStrategy agregator= new NoMaskingStrategy();
				IItemProfileUpdater itemUpdater= new ItemProfileUpdater(modelTrainerPredictor);
				tester.setModelAndUpdaters(representation, userUpdater, agregator, itemUpdater);
				tester.setEventsReport(numErrorReport);
			
			}
			
			else{
				throw new ParseException("Recommender "+chosenRecommender+" not found");
			}
			
		}
		
		return tester;
		 
		
	}
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		try {
			new RecommenderMainClass().startRecommender(args);
		} catch (ParseException e) {
			
			e.printStackTrace();
		} catch (IOException e) {
			
			e.printStackTrace();
		} catch (TasteException e) {
			
			e.printStackTrace();
		} catch (PrivateRecsysException e) {
			
			e.printStackTrace();
		}

	}



	private void startRecommender(String[] args) throws ParseException, IOException, TasteException, PrivateRecsysException {
		try {
			AbstractRecommenderTester recommender=this.processParameters(args);
			
			if(recommender!=null){
				RMSE_ErrorReport errRep=(RMSE_ErrorReport) recommender.startExperiment(1);
				
				LinkedList<Double> errors=errRep.getPartialErrors();
				
				System.out.println(errors);
			}
		} catch (ParseException e) {
			HelpFormatter formatter = new HelpFormatter();
			formatter.printHelp( "Recommender", options );
			
			throw e;
		} catch (IOException e) {
			
			throw e;
		} catch (TasteException e) {
			
			
			throw e;
		}
		
		
	}

}
