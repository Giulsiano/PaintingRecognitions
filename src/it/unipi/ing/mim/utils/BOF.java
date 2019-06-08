package it.unipi.ing.mim.utils;

import java.util.Arrays;
import java.util.AbstractMap.SimpleEntry;

public class BOF {
	private static String DELIMITER = " ";
	
	public static String features2Text(SimpleEntry<Integer, Integer>[] imgPostingList, int topK) {
		StringBuilder sb = new StringBuilder();
		
		SimpleEntry<Integer, Integer>[] topKPivot = Arrays.copyOf(imgPostingList, topK);
		int topkTemp = topK;
		for(int i = 0; i < topKPivot.length; i++) {
			String id = topKPivot[i].getKey().toString();
			
			for (int j = topkTemp; j > 0; j--) {
				sb.append(id + DELIMITER);
			}
			topkTemp--;
			sb.append('\n');
		}
		return sb.toString();
	}
}
