package edu.uniandes.privateRecsys.onlineRecommender.jsonParserEvents;

public class BorrarLoad {
	
	
	public static void main(String[] args) {
		
		for (int i = 1; i <= 30; i++) {
			String pad=String.format("%02d", i);
			System.out.println("mongoimport -db plista --collection click --file click_2013-06-"+pad+".log");
			System.out.println("mongoimport -db plista --collection create --file create_2013-06-"+pad+".log");
			System.out.println("mongoimport -db plista --collection impression --file impression_2013-06-"+pad+".log");
			System.out.println("mongoimport -db plista --collection update --file update_2013-06-"+pad+".log");
		}
	}

}
