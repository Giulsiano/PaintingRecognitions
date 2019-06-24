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

/**
 * Class used for computing posting lists and translate image features to text for indexing with 
 * ElasticSearch
 * @author Giuliano Peraz
 * @author Valerio Tanferna
 * @author Maria Taibi
 *
 */
public class BOF {
	private static String DELIMITER = " ";
	public static final File POSTING_LIST_FILE =  Parameters.POSTING_LISTS_FILE;
	
	/**
	 * Compute the posting list of each image contained into imgIds list. It takes labels from
	 * kmeans to compute the frequencies of each cluster. Each posting list is a pair (cluster id,
	 * cluster frequencies) that is stored into disk into a file called {@link Parameters.POSTING_LISTS_FILE}
	 * @param labels Mat containing cluster where that sample belongs to (sample is a feature of the image).
	 * @param numClusters number of cluster get by kmeans algorithm
	 * @param keypointPerImage Number of keypoint extracted from each image
	 * @param imgIds Image name IDS, usually the path of the image
	 * @throws IOException in case it is not possible to read an image or to store a posting list into
	 * disk 
	 */
	@SuppressWarnings("unchecked")
	public static void getPostingLists (Mat labels, int numClusters, List<Integer> keypointPerImage, List<String> imgIds) throws IOException{
		IntRawIndexer labelIdx = labels.createIndexer();
		int start = 0;
		int end = 0;
		int i = 0;
		int plCount = 0;
		int totalImg = imgIds.size();
		
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
			System.out.println("Posting list computed " + (++plCount) + "/" + totalImg);
			SimpleEntry<String, SimpleEntry<Integer, Integer>[]> postingList =
					new SimpleEntry<String, AbstractMap.SimpleEntry<Integer,Integer>[]>(imgId, clusterFrequencies);
			StreamManagement.append(postingList, POSTING_LIST_FILE, SimpleEntry.class);
		}
	}
	
	/**
	 * Translate posting lists to text string that can be indexed by ElasticSearch. The output is like:<br/>
	 * 0 0 0 0 0 0 0
	 * 1 1 1 1 1 1
	 * 2 2 2 2 2
	 * 3 3 3 3 
	 * 4 4 4
	 * 5 5
	 * 6
	 * <br/>
	 * where each number is the cluster ID with higher frequency starting from cluster 0
	 * @param imgPostingList the posting list of the image
	 * @param topK how many cluster to consider for indexing
	 * @return the posting list ready to be indexed
	 */
	public static String features2Text(SimpleEntry<Integer, Integer>[] imgPostingList, int topK) {
		// Compute how many clusters have frequency bigger than 0 in order to avoid indexing clusters
		// that image doesn't belong to
		int topkTemp;
		for (topkTemp = 0; imgPostingList[topkTemp].getValue() > 0; topkTemp++);
		
		// Create the text document to be indexed
		StringBuilder sb = new StringBuilder();
		SimpleEntry<Integer, Integer>[] topKClusters = Arrays.copyOf(imgPostingList, topK);
		for(int i = 0; i < topKClusters.length; i++) {
			String id = topKClusters[i].getKey().toString();
			
			for (int j = topkTemp; j > 0; j--) {
				sb.append(id + DELIMITER);
			}
			sb.append('\n');
			topkTemp--;
		}
		return sb.toString();
	}
}

