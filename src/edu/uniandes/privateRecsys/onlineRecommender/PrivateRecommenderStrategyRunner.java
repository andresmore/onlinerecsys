package edu.uniandes.privateRecsys.onlineRecommender;

import java.util.logging.Level;
import java.util.logging.Logger;

import edu.uniandes.privateRecsys.onlineRecommender.factorModelRepresentation.FactorUserItemRepresentation;
import edu.uniandes.privateRecsys.onlineRecommender.factorModelRepresentation.UserProfile;
import edu.uniandes.privateRecsys.onlineRecommender.vo.UserTrainEvent;

public class PrivateRecommenderStrategyRunner implements Runnable {

	private IUserProfileUpdater userUpdater;
	private IUserItemAggregator userAggregator;
	private IItemProfileUpdater itemProfileUpdater;
	private UserTrainEvent event;
	private FactorUserItemRepresentation userItemRep;
	private PrivateRecommenderParallelTrainer trainer;
	//private double gamma;

	
	private final static Logger LOG = Logger.getLogger(PrivateRecommenderStrategyRunner.class
		      .getName());

	public PrivateRecommenderStrategyRunner(FactorUserItemRepresentation userItemRep, IUserProfileUpdater userUpdater,
			IUserItemAggregator userAggregator,
			IItemProfileUpdater itemProfileUpdater, UserTrainEvent event, PrivateRecommenderParallelTrainer privateRecommenderParallelTrainer) {
		this.userItemRep=userItemRep;
		this.userUpdater=userUpdater;
		this.userAggregator=userAggregator;
		this.itemProfileUpdater=itemProfileUpdater;
		this.event=event;
		this.trainer=privateRecommenderParallelTrainer;
		
		
	}

	@Override
	public void run() {
		
		
		
		LOG.finest(Thread.currentThread()+" started event "+event.getUserId()+","+event.getItemId());
		
		//trainer.updateState(Thread.currentThread().getId(), event,"WAIT");
		
			UserProfile user=null;
			long initialTime=System.nanoTime();
			long userTime=0;
			long userAggregation=0;
			boolean ok=true;
			//double gamma=gammaStrategy.getGammaFromK(userItemRep.getNumberTrainsItems());
			synchronized (userItemRep.blockUser(event.getUserId())) {
				
				
				//trainer.updateState(Thread.currentThread().getId(), event,"LOCK");
				
				
				
				try {
					user = userUpdater.processEvent(event,userItemRep);
					if(user==null)
						ok=false;
					trainer.updateState(Thread.currentThread().getId(), event,"LOCK-USERUPDATED");
				}catch (Exception e) 
				{
					
					LOG.log(Level.SEVERE,"ERROR",e);
					LOG.severe(Thread.currentThread()+" ended event with error "+event.getUserId()+","+event.getItemId()+" "+e.getMessage()+"" );
					e.printStackTrace();
					ok=false;
				}
				//System.out.println(Thread.currentThread()+" ended event "+event.getUserId()+","+event.getItemId());
				
				//System.out.println(Thread.currentThread()+" ended user update "+event.getUserId()+","+event.getItemId());
				if(ok){
				userTime=System.nanoTime();
				try {
					 
						user=userAggregator.aggregateEvent(user,event,userItemRep);
						trainer.updateState(Thread.currentThread().getId(), event,"LOCK-AGGREGATED");
				} catch (Exception e) {
					LOG.log(Level.SEVERE,"ERROR",e);
					LOG.severe(Thread.currentThread()+" ended event with error "+event.getUserId()+","+event.getItemId()+" "+e.getMessage()+"" );
					e.printStackTrace();
					ok=false;
				
				}
				//System.out.println(Thread.currentThread()+" ended user agg "+event.getUserId()+","+event.getItemId());
				userAggregation=System.nanoTime();
				}
			}
			//trainer.updateState(Thread.currentThread().getId(), event,"WAIT-ITEM");
			synchronized (userItemRep.blockItem(event.getItemId())) {
				
			
			try {
				if(ok){
					
					itemProfileUpdater.processEvent(event,userItemRep,user);
					//trainer.updateState(Thread.currentThread().getId(), event,"ITEM-UPDATED");
				}
			} catch (Exception e) {
				LOG.log(Level.SEVERE,"ERROR",e);
				LOG.severe(Thread.currentThread()+" ended event with error "+event.getUserId()+","+event.getItemId()+" "+e.getMessage()+"" );
				e.printStackTrace();
				ok=false;
			}
			}
			//trainer.updateState(Thread.currentThread().getId(), event,"LOCK-RELEASE");
			LOG.fine(Thread.currentThread()+" ended item update "+event.getUserId()+","+event.getItemId());
			if(ok){
				long itemUpdater=System.nanoTime();
				Profiler.getInstance().reportTimes(initialTime,userTime,userAggregation,itemUpdater);
			}else{
				Profiler.getInstance().reportFailure();
			}
			
			
		
		
		

	}

}
