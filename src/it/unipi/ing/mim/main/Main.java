package it.unipi.ing.mim.main;
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
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.ExecutionException;

import org.bytedeco.javacpp.indexer.FloatRawIndexer;
import org.bytedeco.opencv.opencv_core.KeyPointVector;
import org.bytedeco.opencv.opencv_core.Mat;

import com.mathworks.engine.EngineException;
import com.mathworks.engine.MatlabEngine;

import it.unipi.ing.mim.deep.ImgDescriptor;
import it.unipi.ing.mim.deep.Parameters;
import it.unipi.ing.mim.deep.seq.SeqImageStorage;
import it.unipi.ing.mim.features.FeaturesExtraction;
import it.unipi.ing.mim.features.KeyPointsDetector;
import javafx.util.Pair;

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
		File pivotFile =  Parameters.PIVOTS_FILE;
		float[][] centroids = null;
		List<Centroid> centroidList = new LinkedList<Centroid>();
		if (!pivotFile.exists()) {
			System.out.println("Running kmeans by using Matlab");
			centroids = m.computeKMeans(descFile);
		    // Put keypoints into file line by line
			System.out.println("Storing centroids to disk");
    		try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(pivotFile))) { 
    			for (int i = 0; i < centroids.length; i ++) {
    				Centroid c = new Centroid(centroids[i], i);
    				oos.writeObject(c);	
    			}
    		}
		}
		else {
			System.out.println("Loading centroid from file");
			while (true) {
				try (ObjectInputStream oos = new ObjectInputStream(new FileInputStream(pivotFile))) { 
    				centroidList.add((Centroid) oos.readObject());
	    		}
				catch(EOFException e) {
					// File is ended, so we finish to read 
					break;
				}
			}
		}
		if (centroidList.isEmpty()) System.out.println("AAAAAAAA lista vuota AAAAAAAAH!");
		while (true) {
			try (ObjectInputStream oos = new ObjectInputStream(new FileInputStream(descFile))) {
				List<Pair<Integer, Float[]>> distances = new LinkedList<Pair<Integer, Float[]>>();
				ImgDescriptor imgDesc = (ImgDescriptor) oos.readObject();
				for (Centroid c : centroidList) {
					distances.add(new Pair<Integer, Float[]>(c.getId(), c.distancesTo(imgDesc)));
				}
				
				List<Pair<Integer, Float>> postingLists = new LinkedList<Pair<Integer, Float>>();
				for (int i = 0; i < centroidList.size(); i++) {
					Pair<Integer, Float> postingList  = null;
					for (Pair<Integer, Float[]> distId : distances) {
						postingList = new Pair<Integer, Float>(distId.getKey(), distId.getValue()[i]);
					}
					postingLists.add(postingList);
					// TODO Implementare il comparator
					Collections.sort(list, new Comparator<T>() {
					});
				}
				postingLists.forEach((postList) -> {Arrays.parallelSort(postList);});
				
    		}
			catch(EOFException e) {
				// File is ended, so we finish to read 
				break;
			}
		}
		System.out.println("Program ended");
	}
	
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
	
	private float[][] computeKMeans (File descriptorFile) throws Exception {
		MatlabEngine eng;
		try {
			eng = MatlabEngine.startMatlab();
			eng.putVariable("M", null);
			ObjectInputStream ois = new ObjectInputStream(new FileInputStream(descriptorFile));
			ImgDescriptor desc = null;
			while (true){
				try {
					desc = (ImgDescriptor) ois.readObject();
					if (!(desc instanceof ImgDescriptor)) break;
					float[][] feat = desc.getFeatures();
					eng.putVariable("temp", feat);
					eng.eval("M = [M; temp];");
				}
				catch (IOException e) { 
					break;
				}
			}
		    eng.eval("[idx, C] = kmeans(M, " + 1000 + ");");
		    return eng.getVariable("C");
		} catch (EngineException | IllegalArgumentException | IllegalStateException | InterruptedException e) {
			e.printStackTrace();
		} catch (ExecutionException e) {
			e.printStackTrace();
		} 
		return null;
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
