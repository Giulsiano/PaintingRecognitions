package it.unipi.ing.mim.utils;

import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.bytedeco.javacpp.indexer.IntRawIndexer;
import org.bytedeco.opencv.opencv_core.Mat;

import it.unipi.ing.mim.deep.Parameters;

import java.util.AbstractMap.SimpleEntry;

public class BOF {
	private static String DELIMITER = " ";
	
	public static Map<String, SimpleEntry<Integer, Integer>[]> getPostingLists (Mat labels, int numClusters, List<String> imgIds){
		Map<String, SimpleEntry<Integer, Integer>[]> postingLists = new HashMap<>(labels.rows(), 1.0f);
		IntRawIndexer labelIdx = labels.createIndexer();
		int start = 0;
		
		// TODO C'è da vedere di non usare qui Parameters
		int end = start + Parameters.RANDOM_KEYPOINT_NUM;
		int nimgs = labels.rows() / Parameters.RANDOM_KEYPOINT_NUM;
		Iterator<String> imgIdsIt = imgIds.iterator();
		for (int i = 0; i < nimgs; ++i) {
			SimpleEntry<Integer, Integer>[] clusterFrequencies = (SimpleEntry<Integer, Integer>[]) new SimpleEntry[numClusters];
			int[] frequencies = new int[numClusters];
			Arrays.fill(frequencies, 0);
			for (int j = start; j < end; ++j) {
				++frequencies[labelIdx.get(j)];
			}
			start = end;
			end += Parameters.RANDOM_KEYPOINT_NUM;
			
			// Ordering couples (clusterID, frequency of cluster)
			for (int j = 0; j < frequencies.length; ++j) 
				clusterFrequencies[j] = new SimpleEntry<Integer, Integer>(j, frequencies[j]);
			
			// Order them decrescently
			Arrays.parallelSort(clusterFrequencies, 
					    		Comparator.comparing(SimpleEntry::getValue, 
													 Comparator.reverseOrder())
					    		);
			
			// Put ordered posting lists into the map
			if (imgIdsIt.hasNext()) postingLists.put(imgIdsIt.next(), clusterFrequencies);
		}
		return postingLists;
	}
	
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
