package it.unipi.ing.mim.main;
import static org.bytedeco.opencv.global.opencv_core.CV_32F;
import static org.bytedeco.opencv.global.opencv_core.kmeans;
import static org.bytedeco.opencv.global.opencv_imgcodecs.imread;

import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.AbstractMap.SimpleEntry;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import org.bytedeco.javacpp.indexer.FloatRawIndexer;
import org.bytedeco.javacpp.indexer.IntRawIndexer;
import org.bytedeco.opencv.opencv_core.KeyPointVector;
import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.opencv.opencv_core.TermCriteria;
import org.opencv.core.Core;

import it.unipi.ing.mim.deep.ImgDescriptor;
import it.unipi.ing.mim.deep.Parameters;
import it.unipi.ing.mim.deep.seq.SeqImageStorage;
import it.unipi.ing.mim.features.FeaturesExtraction;
import it.unipi.ing.mim.features.KeyPointsDetector;
import it.unipi.ing.mim.img.elasticsearch.ElasticImgIndexing;
import it.unipi.ing.mim.utils.MatConverter;

public class Main {
	
	public static void main(String[] args) throws Exception {
		// example for running Matlab code
		Main m = new Main();
		SeqImageStorage indexing = new SeqImageStorage();
		System.out.println("Scanning image directory");
		File descFile = new File(Parameters.DESCRIPTOR_FILE);
		if (!descFile.exists()) {
			indexing.extractFeatures(Parameters.imgDir);
		}
		
		// Compute centroids of the database
		Mat labels = null;
		File pivotFile =  Parameters.PIVOTS_FILE;
		File labelFile = Parameters.LABEL_FILE;
		List<Centroid> centroidList = new LinkedList<Centroid>();
		if (!pivotFile.exists()) {
			System.out.println("Running kmeans by using OpenCV");
			Mat[] kmeansResults = m.computeKMeans(descFile);
			Mat centroids = kmeansResults[0];
			labels = kmeansResults[1];
			
		    // Put keypoints into file line by line
			System.out.println("Storing centroids to disk");
			int rows = centroids.rows();
    		try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(pivotFile))) { 
    			for (int i = 0; i < rows; i ++) {
    				Centroid c = new Centroid(centroids.row(i), i);
    				centroidList.add(c);
    				oos.writeObject(c);
    				System.out.println("Stored centroid " + c.getId());
    			}
    		}
    		try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(labelFile))) { 
    			oos.writeObject(MatConverter.mat2int(labels));
    		}
		}
		else {
			System.out.println("Loading centroid from file");
			ObjectInputStream ois = new ObjectInputStream(new FileInputStream(pivotFile));
			while (true) {
				try  {
					Centroid c = (Centroid) ois.readObject();
    				centroidList.add(c);
	    		}
				catch (EOFException e) {
					break;
				}
			}
			System.out.println("Loaded centroids");
			ois.close();
			ois = new ObjectInputStream(new FileInputStream(labelFile));
			labels = MatConverter.int2Mat((int[][]) ois.readObject());
			System.out.println("Loaded labels");
			ois.close();
		}
		// Start indexing images from directories
		if (centroidList.isEmpty()) {
			System.err.println("No centroid has been found. Exiting");
			System.exit(1);
		}
		
		// Read all image names from disk
		ObjectInputStream ois = new ObjectInputStream(new FileInputStream(descFile));
		List<String> imgIds = new LinkedList<String>();
		while (true) {
			try  {
				ImgDescriptor c = (ImgDescriptor) ois.readObject();
				imgIds.add(c.getId());
    		}
			catch (EOFException e) {
				break;
			}
		}
		ois.close();
		
		// Create posting lists by counting frequencies of cluster per image
		Map<String, SimpleEntry<Integer, Integer>[]> postingLists = new HashMap<>(labels.rows(), 1.0f);
		IntRawIndexer labelIdx = labels.createIndexer();
		int numClusters = centroidList.size();
		int start = 0;
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
			for (int j = 0; j < frequencies.length; ++j) {
				clusterFrequencies[j] = new SimpleEntry<Integer, Integer>(j, frequencies[j]);
			}
			Arrays.parallelSort(clusterFrequencies, Comparator.comparing(SimpleEntry::getValue, 
																		 Comparator.reverseOrder()));
			
			// Put ordered posting lists into the map
			if (imgIdsIt.hasNext()) postingLists.put(imgIdsIt.next(), clusterFrequencies);
		}
		
		// Save posting lists to file
		File postingFile = Parameters.POSTING_LISTS_FILE;
		try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(postingFile))) { 
				oos.writeObject(postingLists);
		}

		try(ElasticImgIndexing esIndex = new ElasticImgIndexing(postingFile, Parameters.TOP_K_IDX)){
			esIndex.createIndex();
			esIndex.index();
		}
		System.out.println("Program ended");
	}
	
	/// REALLY BEAUTIFUL CODEEEEEE!
//		while (true) {
//			try (ObjectInputStream oos = new ObjectInputStream(new FileInputStream(descFile))) {
//				// Compute distances from every keypoint to each pivot
//				System.err.println("Compute distances from keypoints to pivots");
//				List<Pair<Integer, Float[]>> distances = new LinkedList<Pair<Integer, Float[]>>();
//				ImgDescriptor imgDesc = (ImgDescriptor) oos.readObject();
//				for (Centroid c : centroidList) {
//					distances.add(new Pair<Integer, Float[]>(c.getId(), c.distancesTo(imgDesc)));
//				}
//				int keypointNumber = imgDesc.getFeatures().length;
//				
//				// Compute frequencies of pivots given by kmeans
//				System.err.println("Compute frequencies");
//				List<Vector<Pair<Integer, Float>>> postingLists = new Vector<>(keypointNumber);
//				for (int i = 0; i < centroidList.size(); i++) {
//					Vector<Pair<Integer, Float>> postingList  = new Vector<>();
//					for (Pair<Integer, Float[]> distId : distances) {
//						postingList.add(new Pair<Integer, Float>(distId.getKey(), distId.getValue()[i]));
//					}
//					postingList.sort(Comparator.comparing(Pair::getValue));
//					postingLists.add(postingList);
//				}
//    		}
//			catch(EOFException e) {
//				// File is ended, so we finish to read 
//				break;
//			}
//		}
	
	private float[] getRecordFromLine(String line) {
	    float[] values = null;
	    try (Scanner rowScanner = new Scanner(line)) {
	        rowScanner.useDelimiter(",");
	        int i = 0;
	        values = new float[128];	// TODO put the constant into the parameters class
	        while (rowScanner.hasNext()) {
	        	values[i] = rowScanner.nextFloat();
	        }
	    }
	    return values;
	}
	
	private Mat[] computeKMeans (File descriptorFile) throws Exception {
		// Get features randomly from each image
		ObjectInputStream ois = new ObjectInputStream(new FileInputStream(descriptorFile));
		Mat bigmat = new Mat();
		int count = 0;
		while (true){
			try {
				float[][] feat = ((ImgDescriptor) ois.readObject()).getFeatures();
				if (feat == null || feat.length == 0) {
					System.err.println("Image #" + (++count) + " empty feature matrix loaded!");
					continue;
				}
				
				Mat featMat = MatConverter.float2Mat(feat);
				int featRows = featMat.rows();
				for (int i = 0; i < Parameters.RANDOM_KEYPOINT_NUM; ++i) {
					bigmat.push_back(featMat.row((int)(Math.random() * featRows)));
				}
			}
			catch (EOFException e) { 
				break;
			}
		}
		ois.close();
		
		// Compute kmeans' centroids
		Mat labels = new Mat();
		Mat centroids = new Mat();
		TermCriteria criteria = new TermCriteria(CV_32F, 100, 1.0d);
		kmeans(bigmat, Parameters.NUM_KMEANS_CLUSTER, labels, criteria, 1, Core.KMEANS_PP_CENTERS, centroids);
		
		// Put results of kmeans into an array and return it
		Mat[] results = new Mat[2];
		results[0] = centroids;
		results[1] = labels;
		return results;
	}
	
	private List<Mat> scanImgDirectory() throws IOException {
		List<Mat> descriptors = new LinkedList<Mat>();
		long i = 0, dirCounter = 0;
		PrintWriter descFile = new PrintWriter(Parameters.DESCRIPTOR_FILE);
		KeyPointsDetector detector = new KeyPointsDetector();		
		FeaturesExtraction extractor = new FeaturesExtraction();
		long dirNum = new File(Parameters.imgDir.toString()).list().length;
		
		// For each directory
		try (DirectoryStream<Path> ds = Files.newDirectoryStream(Parameters.imgDir)) {
	        for (Path directory : ds) {
	        	// Counter for better user interface
	        	++dirCounter;
	        	
	        	// For each file in the directory
	        	for (Path file : Files.newDirectoryStream(directory)) {
	        		String filename = file.toString();
	        		
	        		// In case the file is an image compute its keypoint and put them into a file
	        		if (!Files.isDirectory(file) && filename.toLowerCase().endsWith(".jpg")) {
	        			System.out.printf("(%d, %d) Processing %s\n", dirCounter, dirNum, filename);
	            		Mat image = imread(filename);
	            		KeyPointVector keypoints = detector.detectKeypoints(image);
	            		Mat descriptor = extractor.extractDescriptor(image, keypoints);
	            		
	            		// Put keypoints into file line by line
	            		StringBuilder fileRow = new StringBuilder();
	    				FloatRawIndexer idx = descriptor.createIndexer();
	    				
	    	    		int rows = (int) idx.rows();
	    	    		int cols = (int) idx.cols();
	    	    		for (int j = 0; j < rows; j++) {
	    	    			for (int k = 0; k < cols; k++) {
	    	    				fileRow.append(idx.get(j, k) + ((k < (cols - 1)) ? "," : "\n"));
	    	    			}
	    	    			descFile.append(fileRow.toString());

	    	    			// Reset the string buffer
	    	    			fileRow.setLength(0);
	    	    		}
	    	    		i += keypoints.size();
	        		}
	        	}
	        }
	        System.out.println("Total keypoints = " + i);
	        descFile.close();
	        return descriptors;
		}
	}
}
