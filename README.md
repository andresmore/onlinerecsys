onlinerecsys description
============

A java library for creating an online recommender system as seen in 

Moreno, A., Castro, H., & Riveill, M. (2014). Client-Side hybrid rating prediction for recommendation. 
In V. Dimitrova, T. Kuflik, D. Chin, F. Ricci, P. Dolog, & G.-J. Houben (Eds.) User Modeling, Adaptation, and Personalization, vol. 8538 of Lecture Notes in Computer Science , (pp. 369-380). Springer International Publishing.
URL http://dx.doi.org/10.1007/978-3-319-08786-3_33

Instalation instructions
============

The project uses maven for dependency management

Install first into your local repository the stream-lib project
```
git clone https://github.com/addthis/stream-lib.git
cd stream-lib
mvn install 
```
Then clone and install this project 
```
git clone https://github.com/andresmore/onlinerecsys.git
cd onlinerecsys
mvn install
```
In the folder target the required libs for running the project are downloaded.

Configure datasets
============

This project uses csv Mahout file style data for the datasets, example:
```
#userId,itemId,rating,timestamp
1549,1580,3,974741755
...
```

Datasets with metadata should be added to each line with the following format
```
#userId,itemId,rating,timestamp,metadataInfo
1549,1580,3,974741755,{12194:1.0,148:1.0,1519:1.0}
...
```

Metadata info is a hash representation of a conceptId (must be int) followed by a weight (in this case all are 1.0)


Running an experiment
============

The class edu.uniandes.privateRecsys.onlineRecommender.Evaluationtesters.AbstractRecommenderTester runs an experiment, create a concrete class that extends AbstractRecommenderTester and in a method of that class do the following:

```Java
//Create a dataset object, limiting the number of ratings in the set 1,2,3,4,5
  HashMap<String,String> translations=new HashMap<String,String>();
	  translations.put(new String("0"), new String("1"));
	  translations.put(new String("0.5"), new String("1"));
	  translations.put(new String("1.5"), new String("2"));
	  translations.put(new String("2.5"), new String("3"));
	  translations.put(new String("3.5"), new String("4"));
	  translations.put(new String("4.5"), new String("5"));
  
  RatingScale scale= new OrdinalRatingScale(new String[] {new String("0"),new String("0.5"),new String("1"),new String("1.5"),new String("2"),new String("2.5"),new String("3"),new String("3.5"),new String("4"),new String("4.5"),new String("5")},translations);
	
  RSDataset data= new RSDataset("path to train","path to test ","path to cv",scale);
	
//Choose a model predictor
  BaseModelPredictorWithItemRegularizationUpdate baseModelPredictor = new BaseModelPredictorWithItemRegularizationUpdate(0.01);
  
//Instantiate the profile manager, choose 5 as the latent vector length
  FactorUserItemRepresentation denseModel = new IncrementalFactorUserItemRepresentation(scale, 5, false, baseModelPredictor);
  
//Create a learning rate scheduling and add it to the predictor
  LearningRateStrategy learningRateStrategy = LearningRateStrategy.createDecreasingRate(1e-6, 0.25);
  baseModelPredictor.setLearningRateStrategy(learningRateStrategy);
  
// Set the update strategy for the user and item profile and add them to the recommender tester
  UserProfileUpdater userUp = new UserProfileUpdater(baseModelPredictor);
	IUserMaskingStrategy agregator = new NoMaskingStrategy();
	IItemProfileUpdater itemUpdater = new ItemProfileUpdater(baseModelPredictor);
	setModelAndUpdaters(denseModel, userUp,agregator, itemUpdater);
	setModelPredictor(baseModelPredictor);

//Run the experiment	
	ErrorReport result = rest.startExperiment(1);
```
