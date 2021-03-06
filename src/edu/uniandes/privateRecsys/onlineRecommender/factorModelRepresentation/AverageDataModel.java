package edu.uniandes.privateRecsys.onlineRecommender.factorModelRepresentation;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.logging.Logger;

import org.apache.mahout.cf.taste.impl.common.FullRunningAverage;
import org.apache.mahout.common.iterator.FileLineIterator;

import com.google.common.base.Splitter;

import edu.uniandes.privateRecsys.onlineRecommender.exception.PrivateRecsysException;

/**
 * A model that reads from a Mahout/Taste model file and only keeps information about the averages of each item and user in the file
 * @author Andres M
 *
 */
public class AverageDataModel {

	private final static Logger LOG = Logger.getLogger(AverageDataModel.class
		      .getName());
	private FullRunningAverage globalAverage= new FullRunningAverage();
	private HashMap<Long, FullRunningAverage> itemAverages= new HashMap<Long, FullRunningAverage>();
	private HashMap<Long, FullRunningAverage> userAverages= new HashMap<Long, FullRunningAverage>();
	
	public AverageDataModel(File model) throws IOException {
		FileLineIterator iterator = null;
		long numlines = 0;
		try {
			iterator = new FileLineIterator(model);
			Splitter spt = Splitter.on(',');

			while (iterator.hasNext()) {

				String line = iterator.next();
				Iterator<String> tokens = spt.split(line).iterator();

				String userIDString = tokens.next();
				String itemIDString = tokens.next();
				String preferenceValueString = tokens.next();
				boolean hasTimestamp = tokens.hasNext();
				String timestampString = hasTimestamp ? tokens.next() : null;
				long timeStampAct = Long.parseLong(timestampString);
				long itemId = Long.parseLong(itemIDString);
				long userId = Long.parseLong(userIDString);
				double preferenceValue = Double
						.parseDouble(preferenceValueString);
				
				this.globalAverage.addDatum(preferenceValue);
				if (itemAverages.get(itemId) == null)
					itemAverages.put(itemId, new FullRunningAverage());

				if (userAverages.get(userId) == null)
					userAverages.put(userId, new FullRunningAverage());

				itemAverages.get(itemId).addDatum(preferenceValue);
				userAverages.get(userId).addDatum(preferenceValue);
				numlines++;
				if (numlines % 10000 == 0)
					LOG.info("Loaded into average model " + numlines);

			}
		} catch (Exception e) {
			throw e;
		} finally {
			if (iterator != null)
				iterator.close();
		}

		LOG.info("Loaded into model " + numlines);
	}

	public double getAverageForItemId(long itemId) throws PrivateRecsysException {
		if(itemAverages.get(itemId)==null)
			throw new PrivateRecsysException("Could not find item "+itemId+" in model");
		return itemAverages.get(itemId).getAverage();
	}
	
	public double getAverageForUserId(long userId) throws PrivateRecsysException {
		if(userAverages.get(userId)==null)
			throw new PrivateRecsysException("Could not find user "+userId+" in model");
		return userAverages.get(userId).getAverage();
	}

	public Iterator<Long> getUserIDs() {
		
		return userAverages.keySet().iterator();
	}
	
	public double getGlobalAverage(){
		return this.globalAverage.getAverage();
	}

	public Iterator<Long> getItemIDs() {
		// TODO Auto-generated method stub
		return itemAverages.keySet().iterator();
	}

	public int getNumUsers() {
		// TODO Auto-generated method stub
		return userAverages.keySet().size();
	}

	public int getNumItems() {
		// TODO Auto-generated method stub
		return itemAverages.keySet().size();
	}

	

}
