package edu.uniandes.privateRecsys.onlineRecommender.utils;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.logging.Logger;

public class RandomGeneratorThread extends Thread {

	private BlockingQueue<Double> randNumbers;
	private UncommonsRandomGeneratorAdaptor generator;
	
	private final static int MAX_QUEUE_SIZE=500000;
	private final static int MIN_QUEUE_SIZE=200000;
	
	private final static Logger LOG = Logger.getLogger(RandomGeneratorThread.class
		      .getName());
	

	public RandomGeneratorThread(BlockingQueue<Double> randNumbers,
			UncommonsRandomGeneratorAdaptor randomGenerator) {
		this.randNumbers=randNumbers;
		this.generator=randomGenerator;
	}

	@Override
	public void run() {
		while(true){
			if(randNumbers.size()<MIN_QUEUE_SIZE){
				LOG.fine("Generating random, exhausted");
				for (int i = 0; i < MAX_QUEUE_SIZE; i++) {
					double next = generator.nextGaussian();
					randNumbers.add(next);
				}
				
				//LOG.fine("Random numbers generated, size is: "+randNumbers.size());
			}
			try {
				Thread.sleep(10);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		
	}

}
