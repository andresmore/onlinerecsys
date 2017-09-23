package edu.uniandes.privateRecsys.onlineRecommender.datasplitter;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import net.recommenders.rival.core.DataModel;
import net.recommenders.rival.split.parser.MovielensParser;
import net.recommenders.rival.split.splitter.Splitter;

/**
 * 
 * @author <a href="https://github.com/andresmore">Andrés Moreno </a>
 *
 */
public class IterativeLeaveOneOutSplitter<U, I>  implements Splitter<U, I> { 
	
	/**
     * An instance of a Random class.
     */
    private Random rnd=new Random(0);
    
    /**
     * Minimum number of preferences of user to include it on train and test set
     */
	private int minPreferences=0;
	
	/**
     * Path out
     */
	private String outPath;

    /**
     * Constructor.
     *
     * @param seed value to initialize a Random class
     * @param minPreferences minimum number of preferences either the user must have to be included in the splits.
     * @param outPath folder where each split (train and test) will be written
     */
    public IterativeLeaveOneOutSplitter(final long seed, final int minPreferences, final String outPath) {
  

        this.rnd = new Random(seed);
        this.minPreferences=minPreferences;
        this.outPath=outPath;
    }

	@Override
	public DataModel<U, I>[] split(DataModel<U, I> data) {
		
		@SuppressWarnings("unchecked")
		final PrintWriter[] splits = new PrintWriter[2];
		
		Map<U, Map<I, Set<Long>>> timestamps=data.getUserItemTimestamps();
		boolean hasTimestamps=!timestamps.isEmpty();
		String trainFile = outPath + "train_0.csv";
		String testFile = outPath + "test_0.csv";
		try {
			splits[0]= new PrintWriter( trainFile);
			splits[1]= new PrintWriter( testFile);
		} catch (FileNotFoundException e) {
			throw new RuntimeException("Error writting: "+e);
		}
		
		 for (U user : data.getUsers()) {
			 
			 
			 List<I> items = new ArrayList<I>(data.getUserItemPreferences().get(user).keySet());
			 
			
			 
			 if(items.size()>=minPreferences) {
				 
				 Collections.shuffle(items, rnd);
				 I crossValidatedItem=items.remove(0);
				 double prefCV = data.getUserItemPreferences().get(user).get(crossValidatedItem);
				 String timestamp=null;
				 
				 if(hasTimestamps) {
					Set<Long> timestamps_user_item=timestamps.get(user).get(crossValidatedItem);
					timestamp=""+Collections.min(timestamps_user_item);
					splits[1].println(user + "\t" + crossValidatedItem + "\t" + prefCV+"\t"+timestamp);
				 }
				 else {
					 splits[1].println(user + "\t" + crossValidatedItem + "\t" + prefCV);
				 }
				 
				 for (I item : items) {
	                    Double pref = data.getUserItemPreferences().get(user).get(item);
	                    if(hasTimestamps) {
	                    	Set<Long> timestamps_user_item=timestamps.get(user).get(crossValidatedItem);
	    					timestamp=""+Collections.min(timestamps_user_item);
	    					splits[0].println(user + "\t" + crossValidatedItem + "\t" + prefCV+"\t"+timestamp);
	                    }
	                    splits[0].println(user + "\t" + item + "\t" + pref.floatValue());
				 } 
				 
				 
			 }
			 	
		 }	
		 for (int i = 0; i < splits.length; i++) {
			splits[i].flush();
			splits[i].close();
		}
		return null;
	}
	
	public static void main(String[] args) throws IOException {
		IterativeLeaveOneOutSplitter<Long,Long> splitter= new IterativeLeaveOneOutSplitter<>(0, 10, "data/ml-10M100K/LOU_SPLIT/");
		MovielensParser parser= new MovielensParser();
		DataModel<Long, Long> model=parser.parseData(new File("data\\ml-10M100K\\ratings.dat"));
		splitter.split(model);
	}

}
