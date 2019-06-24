package it.unipi.ing.mim.utils;

import java.io.File;
import java.io.IOException;
import java.util.AbstractMap;
import java.util.AbstractMap.SimpleEntry;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import org.bytedeco.javacpp.indexer.IntRawIndexer;
import org.bytedeco.opencv.opencv_core.Mat;

import it.unipi.ing.mim.deep.tools.StreamManagement;
import it.unipi.ing.mim.main.Parameters;

public class BOF {
	private static String DELIMITER = " ";
	public static final File POSTING_LIST_FILE =  Parameters.POSTING_LISTS_FILE;
	
	@SuppressWarnings("unchecked")
	public static void getPostingLists (Mat labels, int numClusters, List<Integer> keypointPerImage, List<String> imgIds) throws IOException{
		IntRawIndexer labelIdx = labels.createIndexer();
		int start = 0;
		int end = 0;
		int i = 0;
		
		// For each image compute the posting list associated to it, that is the list
		// (image name, array of couple (clusterId, frequency of cluster)
		for (String imgId : imgIds) {
			SimpleEntry<Integer, Integer>[] clusterFrequencies = (SimpleEntry<Integer, Integer>[]) new SimpleEntry[numClusters];
			int[] frequencies = new int[numClusters];
			Arrays.fill(frequencies, 0);
			
			// Compute histogram/frequencies per cluster
			end += keypointPerImage.get(i++);
			for (int j = start; j < end; ++j) {
				++frequencies[labelIdx.get(j)];
			}
			start = end;
			
			// Ordering (clusterID, frequency of cluster) descendently, so more frequent clusters
			// are the first in the posting list
			for (int j = 0; j < frequencies.length; ++j) 
				clusterFrequencies[j] = new SimpleEntry<Integer, Integer>(j, frequencies[j]);
			Arrays.sort(clusterFrequencies, Comparator.comparing(SimpleEntry::getValue, 
					Comparator.reverseOrder()));

			// Store posting list to disk
			SimpleEntry<String, SimpleEntry<Integer, Integer>[]> postingList =
					new SimpleEntry<String, AbstractMap.SimpleEntry<Integer,Integer>[]>(imgId, clusterFrequencies);
			StreamManagement.append(postingList, POSTING_LIST_FILE, SimpleEntry.class);
		}
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

