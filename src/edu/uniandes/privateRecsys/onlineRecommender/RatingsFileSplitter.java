package edu.uniandes.privateRecsys.onlineRecommender;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.LineNumberReader;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Observable;
import java.util.Observer;
import java.util.Random;
import java.util.Set;
import java.util.logging.Logger;

import org.apache.mahout.cf.taste.common.TasteException;
import org.apache.mahout.cf.taste.impl.common.LongPrimitiveIterator;
import org.apache.mahout.cf.taste.impl.model.file.FileDataModel;
import org.apache.mahout.cf.taste.model.Preference;

import edu.uniandes.privateRecsys.onlineRecommender.exception.PrivateRecsysException;
import edu.uniandes.privateRecsys.onlineRecommender.vo.UserTrainEvent;


/**
 * Splits a rating file into train/test files
 * @author Andres M
 *
 */
public class RatingsFileSplitter implements Observer {
	private final static Logger LOG = Logger.getLogger(RatingsFileSplitter.class
		      .getName());
	private String sourceFile;
	private File testFile;
	private File trainFile;
	private double trainPercentage;
	private double contentBasedPercentage;
	private PrintWriter trainFileWriter= null;
	private PrintWriter testFileWriter= null;
	private HashMap<Long, Integer> numTrainsUser= new HashMap<>();
	private int countTrain=0;
	private int countTest=0;
	private int countCV=0;
	private int forcedContent=0;
	private HashSet<Long> candidateItems;
	private Random randTrainTest;
	private Random randTestCV;
	private int numTestRatings;
	private File cvFile;
	private PrintWriter cvFileWriter;
	
	
	/**
	 * Splits a rating file into train/test sets with trainPercentage% ratings in the train file and forces 
	 * that contentBasedPercentage% of ratings in the test file are from items not in the train set.  
	 * @param sourceFile 
	 * @param testFile
	 * @param trainFile
	 * @param trainPercentage
	 * @param contentBasedPercentage
	 */
	public RatingsFileSplitter(String sourceFile, File testFile, File cvFile,
			File trainFile, double trainPercentage, double contentBasedPercentage) {
		super();
		this.sourceFile = sourceFile;
		this.testFile = testFile;
		this.trainFile = trainFile;
		this.cvFile=cvFile;
		this.trainPercentage=trainPercentage;
		this.contentBasedPercentage=contentBasedPercentage;
	}
	
	public void split() throws  TasteException, IOException, PrivateRecsysException{
		
		File sourceF= new File(sourceFile);
		
		
		LineNumberReader lnr=null;
		this.randTrainTest= new Random();
		this.randTestCV= new Random();
		
		try {
			
			lnr = new LineNumberReader(new FileReader(sourceF));
			lnr.skip(Long.MAX_VALUE);
			int numLines=lnr.getLineNumber();
			lnr.close();
			int numTrainratings=(int) Math.ceil(numLines*trainPercentage);
			this.numTestRatings=numLines-numTrainratings;
			int numTestContent=(int) Math.ceil(numTestRatings*contentBasedPercentage);
			
			
			LOG.info("trying to split file into files with trainSize:"+numTrainratings+" testSize:"+numTestRatings+"("+numTestContent+")");
			FileDataModel model= new FileDataModel(new File(sourceFile));
			this.candidateItems=getItemstoCount(model,numTestContent);
			if(candidateItems!=null){
				System.out.println("Solution found! "+candidateItems.size());
				
				trainFileWriter=new PrintWriter(this.trainFile);
				testFileWriter=new PrintWriter(this.testFile);
				cvFileWriter= new PrintWriter(this.cvFile);
				FileEventCreator fec = new FileEventCreator(new File(this.sourceFile), -1, -1);
				fec.addObserver(this);
				fec.startEvents();
				//LongPrimitiveIterator iter=model.getItemIDs();
				
				LOG.info("Created file "+this.trainFile+" ("+countTrain+") "+this.testFile+"("+countTest+") "+this.cvFile+"("+countCV+") pct:"+ forcedContent);
				
			}
			model=null;
			
			
		} catch (IOException e) {
			throw e;
		}finally{
			if(lnr!=null)
				try {
					lnr.close();
				} catch (IOException e) {}
			if(trainFileWriter!=null)
				trainFileWriter.close();
			if(testFileWriter!=null)
				testFileWriter.close();
			if(cvFileWriter!=null)
				cvFileWriter.close();
		}
		
		
		
		
		
	}
	
	
	private HashSet<Long> getItemstoCount(FileDataModel model,
			int numTestContent) throws TasteException {
		Random rand= new Random();
		int countRatings=0;
		LinkedList<Long> items=new LinkedList<>();
		LinkedList<Long> allItems=new LinkedList<>();
		LongPrimitiveIterator iter=model.getItemIDs();
		
		while(iter.hasNext()){
			allItems.add(iter.next());
		}
		boolean solutionFound=false;
		int numTries=0;
		//random walk into solutions
		while(!solutionFound){
			long itemCandidate=allItems.remove(rand.nextInt(allItems.size()));
			int weight=model.getNumUsersWithPreferenceFor(itemCandidate);
			items.add(itemCandidate);
			countRatings+=weight;
			if(Math.abs(countRatings-numTestContent)<=10){
				//solution found +-10 of desired weight
				HashSet<Long> elements=new HashSet<Long>();
				elements.addAll(items);
				return elements;
				
			}
			if(countRatings>numTestContent){
				System.out.println("countRatings is "+countRatings+", desired is"+numTestContent+" starting over ...");
				allItems.addAll(items);
				items.clear();
				countRatings=0;
				numTries++;
				
			}
			if(numTries==100)
				throw new TasteException("Exception Creating Datasets");
				
			
		}
			
		 
		
		return null;
	}

	
	@Override
	public void update(Observable o, Object arg) {
		
		UserTrainEvent event = (UserTrainEvent) arg;
		
		
			long itemId=event.getItemId();
			
			if(candidateItems.contains(itemId)){
				//"All item "+itemId+" goes to test"
			
					
					testFileWriter.println(event.getUserId()+","+event.getItemId()+","+event.getRating()+","+event.getTimestamp()+","+event.getMetadata());
					countTest++;
					forcedContent++;
				
				
			}
			else{
				
					
					long userId=event.getUserId();
					
					int numTrains=1;
					
					if(numTrainsUser.get(userId)!=null)
						numTrains=numTrainsUser.get(userId)+1;
						
					numTrainsUser.put(userId, numTrains);
					
					if(numTrains>10 && countTest<=numTestRatings && randTrainTest.nextBoolean()){
						if(randTestCV.nextBoolean()){
							testFileWriter.println(event.getUserId()+","+event.getItemId()+","+event.getRating()+","+event.getTimestamp()+","+event.getMetadata());
							countTest++;
						}
						else{
							cvFileWriter.println(event.getUserId()+","+event.getItemId()+","+event.getRating()+","+event.getTimestamp()+","+event.getMetadata());
							countCV++;
						}
						
					}else{
						trainFileWriter.println(event.getUserId()+","+event.getItemId()+","+event.getRating()+","+event.getTimestamp()+","+event.getMetadata());
						countTrain++;
					}
					
				
				
				
			}
			
			
			
		
		
	}
	
	

}
