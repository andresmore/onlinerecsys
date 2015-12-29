package edu.uniandes.privateRecsys.onlineRecommender;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Observable;

import edu.uniandes.privateRecsys.onlineRecommender.exception.PrivateRecsysException;
import edu.uniandes.privateRecsys.onlineRecommender.vo.ReportErrorEvent;
import edu.uniandes.privateRecsys.onlineRecommender.vo.UserTrainEvent;



public class FileEventCreator extends Observable{

	
	
	private static int defaultCharBufferSize = 8192*3;
	private static final int MAX_FIELDS=5;
	private LineParser data ;
	private int numEventReport;
	private int limitEvents;
	private File file;
	
	public FileEventCreator(File file, int numEventReport, int limitEvents) throws IOException {
		this.file=file;
		data= new LineParser();
		this.data.addSeparator(":");
		this.data.addSeparator(",");
		this.data.addSeparator(""+'\t');
		
		
				
		this.numEventReport=numEventReport;
		this.limitEvents=limitEvents;
	}
	
	
	public int startEvents() throws PrivateRecsysException, IOException {

		BufferedReader iterator = null;
		int numlines = 0;
		try {
			iterator = new BufferedReader(new FileReader(file),defaultCharBufferSize);

			if (this.countObservers() == 0) {

				throw new PrivateRecsysException(
						"No observers registered for experiment, cancelling ...");
			}
			String line =null;
			while ((line=iterator.readLine())!=null) {

				 
				UserTrainEvent event = data.processLine(line,MAX_FIELDS);
				setChanged();
				notifyObservers(event);
				numlines++;
				if (limitEvents == numlines) {
					try{
						iterator.close();
						return limitEvents;
					}catch(Exception e){
						
					}
				}
				if ((numlines % this.numEventReport) == 0
						&& this.numEventReport != -1) {
					ReportErrorEvent event2 = new ReportErrorEvent();
					setChanged();
					notifyObservers(event2);
				}

				
			/*	 if(numlines%100000==0)
				  System.out.println(numlines+" events created");*/
				 
			}
		} catch (IOException e) {

		} finally {
			if (iterator != null)
				try{
					iterator.close();
				}catch (Exception e2) {}
		}

		// System.out.println("Number of events created is "+numlines);
		return numlines;
	}


	
	
	

}
