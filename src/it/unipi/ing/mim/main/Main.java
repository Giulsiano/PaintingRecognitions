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

import it.unipi.ing.mim.deep.ImgDescriptor;
import it.unipi.ing.mim.deep.Parameters;
import it.unipi.ing.mim.deep.seq.SeqImageStorage;
import it.unipi.ing.mim.deep.tools.FeaturesStorage;
import it.unipi.ing.mim.features.FeaturesExtraction;
import it.unipi.ing.mim.features.KeyPointsDetector;

public class Main {
	
	public static void main(String[] args) throws Exception {
		// example for running Matlab code
		Main m = new Main();
		SeqImageStorage indexing = new SeqImageStorage();
		System.out.println("Scanning image directory");
		List<ImgDescriptor> descriptors = indexing.extractFeatures(Parameters.imgDir);
		m.runMatlabCode(new File(Parameters.DESCRIPTOR_FILE));
		System.out.println("Ended program");
	}
	
	private void runMatlabCode (List<ImgDescriptor> descriptors) {
		MatlabEngine eng;
		try {
			eng = MatlabEngine.startMatlab();
			for (ImgDescriptor descriptor : descriptors) {
				eng.putVariable("f", descriptor.getFeatures());
				eng.eval("");
			}
			eng = MatlabEngine.startMatlab();
		    eng.eval("[idx, C] = kmeans(M, " + 1000 + ");");
		    double[][] C = eng.getVariable("C");
		}
		catch (EngineException | IllegalArgumentException | IllegalStateException | InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ExecutionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
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
