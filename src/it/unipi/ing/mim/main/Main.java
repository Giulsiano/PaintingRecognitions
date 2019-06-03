package it.unipi.ing.mim.main;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.stream.Stream;

import org.bytedeco.javacpp.indexer.FloatRawIndexer;
import org.bytedeco.opencv.opencv_core.KeyPointVector;
import org.bytedeco.opencv.opencv_core.Mat;
import static org.bytedeco.opencv.global.opencv_imgcodecs.imread;
import static org.bytedeco.opencv.global.opencv_features2d.drawKeypoints;
import static org.bytedeco.opencv.global.opencv_highgui.imshow;
import static org.bytedeco.opencv.global.opencv_highgui.waitKey;
import static org.bytedeco.opencv.global.opencv_highgui.destroyAllWindows;

import com.mathworks.engine.*;

import it.unipi.ing.mim.features.FeaturesExtraction;
import it.unipi.ing.mim.features.KeyPointsDetector;

public class Main {
	
	public static void main(String[] args) throws Exception {
		// example for running Matlab code
		Main m = new Main();
		System.out.println("Scanning image directory");
		List<Mat> descriptors = m.scanImgDirectory();
		m.runMatlabCode(new File(Parameters.DESCRIPTOR_FILE));
		System.out.println("Ended program");
	}
	
	private void runMatlabCode (File keypointFile) throws Exception {
		MatlabEngine eng;
		try {
			eng = MatlabEngine.startMatlab();
		    eng.eval("M = csvread(\"" + keypointFile.getAbsolutePath() +"\");");
		    eng.eval("[idx, C] = kmeans(M, " + 1000 + ");");
		    double[][] C = eng.getVariable("C");
		    
		    // Put keypoints into file line by line
    		StringBuilder fileRow = new StringBuilder();
    		PrintWriter file = new PrintWriter(Parameters.CLUSTER_FILE);
    		for (int j = 0; j < C.length; j++) {
    			for (int k = 0; k < C[j].length; k++) {
    				fileRow.append(C[j][k]+ ((k < (C[j].length - 1)) ? "," : "\n"));
    			}
    			file.append(fileRow.toString());

    			// Reset the string buffer
    			fileRow.setLength(0);
    		}
    		file.close();
		} catch (EngineException | IllegalArgumentException | IllegalStateException | InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ExecutionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	private List<Mat> scanImgDirectory() throws IOException {
		List<Mat> descriptors = new LinkedList<Mat>();
		long i = 0, dirCounter = 0;
		PrintWriter descFile = new PrintWriter(Parameters.DESCRIPTOR_FILE);
		KeyPointsDetector detector = new KeyPointsDetector();		
		FeaturesExtraction extractor = new FeaturesExtraction();
		long dirNum = new File(Parameters.imgDir.toString()).list().length;
		try (DirectoryStream<Path> ds = Files.newDirectoryStream(Parameters.imgDir)) {
	        for (Path directory : ds) {
	        	++dirCounter;
	        	DirectoryStream<Path> childs = Files.newDirectoryStream(directory);
	        	for (Path file : childs) {
	        		// In case the file is an image compute its keypoint and put them into a file
	        		if (!Files.isDirectory(file) && file.toString().toLowerCase().endsWith(".jpg")) {
	        			System.out.println("("+ dirCounter + "/" + dirNum + ") Processing" + file.toString());
	            		Mat image = imread(file.toString());
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
