package edu.uniandes.privateRecsys.onlineRecommender.jsonParserEvents;

import java.util.Comparator;
import java.util.HashMap;

public class MapValueComparator<T1,T2> implements Comparator<T1> {

	private HashMap<T1, T2> hashMap;

	public MapValueComparator(HashMap<T1, T2> itemIdsCount) {
		this.hashMap=itemIdsCount;
	}

	@Override
	public int compare(T1 o1, T1 o2) {
		Comparable oo1=(Comparable) o1;
		Comparable oo2=(Comparable) o2;
		return oo1.compareTo(oo2);
	}
	
	public static void main(String[] args) {
		System.out.println("exito");
	}

}