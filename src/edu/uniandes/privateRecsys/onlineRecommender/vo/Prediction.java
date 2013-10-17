package edu.uniandes.privateRecsys.onlineRecommender.vo;

public class Prediction implements Comparable<Prediction> {
	
	private long itemId;
	private long userId;
	private double predictionValue;
	private boolean noPrediction;
	private boolean isHybrid;
	private Prediction(long userId, long itemId, double predictionValue, boolean noPrediction,
			boolean isHybrid) {
		this.userId=userId;
		this.itemId=itemId;
		this.predictionValue = predictionValue;
		this.noPrediction = noPrediction;
		this.isHybrid = isHybrid;
	}
	
	public static Prediction createNormalPrediction(long userId, long itemId,double predictionValue){
		return new Prediction(userId, itemId, predictionValue, false, false);	
		
		
	}
	
	public double getPredictionValue() {
		return predictionValue;
	}

	public long getItemId() {
		return itemId;
	}

	public long getUserId() {
		return userId;
	}

	public boolean isNoPrediction() {
		return noPrediction;
	}

	public boolean isHybrid() {
		return isHybrid;
	}

	public static Prediction createHyrbidPrediction(long userId, long itemId,double predictionValue){
		return new Prediction(userId,itemId,predictionValue, false, true);	
		
		
	}
	
	public static Prediction createNoAblePrediction(long userId, long itemId){
		return new Prediction(userId,itemId,0, true, false);	
		
		
	}
	
	public static void main(String[] args) {
	System.out.println("prueba");	
	}

	@Override
	public int compareTo(Prediction o) {
		// TODO Auto-generated method stub
		return Double.compare(this.predictionValue, o.getPredictionValue());
	}
	
	@Override
	public String toString() {
		// TODO Auto-generated method stub
		return userId+" "+itemId+" "+predictionValue;
	}
	

}
