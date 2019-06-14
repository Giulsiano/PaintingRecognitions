package it.unipi.ing.mim.utils;

import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.bytedeco.javacpp.indexer.IntRawIndexer;
import org.bytedeco.opencv.opencv_core.Mat;

import it.unipi.ing.mim.main.Parameters;

import java.util.AbstractMap.SimpleEntry;

public class BOF {
	private static String DELIMITER = " ";
	
	@SuppressWarnings("unchecked")
	public static Map<String, SimpleEntry<Integer, Integer>[]> getPostingLists (Mat labels, int numClusters, List<Integer> keypointPerImage, List<String> imgIds){
		IntRawIndexer labelIdx = labels.createIndexer();
		long labelRows = labelIdx.rows();
		int nimgs = keypointPerImage.size();
		long labelPerImage = labelRows/nimgs; 
		int start = 0;
		int end = 0;
		
		// For each image compute the posting list associated to it, that is the list
		// (image name, array of couple (clusterId, frequency of cluster)
		Map<String, SimpleEntry<Integer, Integer>[]> postingLists = new HashMap<>(nimgs, 1.0f);
		Iterator<String> imgIdsIt = imgIds.iterator();
		for (int i = 0; i < nimgs; ++i) {
			SimpleEntry<Integer, Integer>[] clusterFrequencies = (SimpleEntry<Integer, Integer>[]) new SimpleEntry[numClusters];
			int[] frequencies = new int[numClusters];
			Arrays.fill(frequencies, 0);
			
			// Compute histogram/frequencies per cluster
			end += Math.min(keypointPerImage.get(i), labelPerImage);
			for (int j = start; j < end; ++j) {
				++frequencies[labelIdx.get(j)];
			}
			start = end;
			
			// Ordering (clusterID, frequency of cluster) descendently, so there are first more 
			// frequent clusters 
			for (int j = 0; j < frequencies.length; ++j) 
				clusterFrequencies[j] = new SimpleEntry<Integer, Integer>(j, frequencies[j]);
			Arrays.sort(clusterFrequencies, Comparator.comparing(SimpleEntry::getValue, 
																 Comparator.reverseOrder()));
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
