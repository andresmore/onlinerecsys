package edu.uniandes.privateRecsys.onlineRecommender;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.Observable;

import org.apache.mahout.common.iterator.FileLineIterator;

import com.google.common.base.Splitter;

import edu.uniandes.privateRecsys.onlineRecommender.exception.PrivateRecsysException;
import edu.uniandes.privateRecsys.onlineRecommender.vo.ReportErrorEvent;
import edu.uniandes.privateRecsys.onlineRecommender.vo.UserTrainEvent;



public class FileEventCreator extends Observable{

	
	private Splitter splitter;
	
	private int numEventReport;
	private int limitEvents;
	private File file;
	
	public FileEventCreator(File file, int numEventReport, int limitEvents) throws IOException {
		this.file=file;
		
		this.splitter= Splitter.on(',');
		
		this.numEventReport=numEventReport;
		this.limitEvents=limitEvents;
	}
	
	
	public int startEvents() throws PrivateRecsysException, IOException {

		FileLineIterator iterator = null;
		int numlines = 0;
		try {
			iterator = new FileLineIterator(file);

			if (this.countObservers() == 0) {

				throw new PrivateRecsysException(
						"No observers registered for experiment, cancelling ...");
			}

			while (iterator.hasNext()) {

				String line = iterator.next();
				UserTrainEvent event = processLine(line);
				setChanged();
				notifyObservers(event);
				numlines++;
				if (limitEvents == numlines) {

					return limitEvents;
				}
				if ((numlines % this.numEventReport) == 0
						&& this.numEventReport != -1) {
					ReportErrorEvent event2 = new ReportErrorEvent();
					setChanged();
					notifyObservers(event2);
				}

				/*
				 * if(numlines%100000==0)
				 * System.out.println(numlines+" events created");
				 */
			}
		} catch (IOException e) {

		} finally {
			if (iterator != null)
				iterator.close();
		}

		// System.out.println("Number of events created is "+numlines);
		return numlines;
	}


	private UserTrainEvent processLine(String line) {
		Iterator<String> tokens=splitter.split(line).iterator();
		
	
			 String userIDString = tokens.next();
			 String itemIDString = tokens.next();
			 String preferenceValueString = tokens.next();
			 boolean hasTimestamp = tokens.hasNext();
			 String timestampString = hasTimestamp ? tokens.next() : null;
	
		
		return new UserTrainEvent(Long.parseLong(userIDString), Long.parseLong(itemIDString), preferenceValueString, Long.parseLong(timestampString));
	}
	
	

}
