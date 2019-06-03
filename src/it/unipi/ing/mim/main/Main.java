package it.unipi.ing.mim.main;
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
	
	public static void main(String[] args) throws IOException {
		// example for running Matlab code
		Main m = new Main();
		System.out.println("Scanning image directory");
		List<Mat> descriptors = m.scanImgDirectory();
		PrintWriter descFile = new PrintWriter(Parameters.DESCRIPTOR_FILE_NAME);
		Stream<Mat> d = descriptors.stream();
		System.out.println("Creating CSV file");
//		d.forEach((mat) -> {
//				StringBuilder fileRow = new StringBuilder();
//				FloatRawIndexer idx = mat.createIndexer();
//	    		int rows = (int) idx.rows();
//	    		int cols = (int) idx.cols();
//	    		for (int j = 0; j < rows; j++) {
//	    			for (int k = 0; k < cols; k++)
//	    				fileRow.append(idx.get(j, k) + ((k < (cols - 1)) ? "," : "\n"));
//					descFile.append(fileRow.toString());
//	    		}
//			}
//		);
//		d.close();
//		descFile.close();
		System.out.println("Ended program:");
		}
	
	private void runMatlabCode (Mat descriptor) {
		MatlabEngine eng;
		
		try {
			eng = MatlabEngine.startMatlab();
			eng.putVariable("x", 7.0);
		    eng.putVariable("y", 3.0);
		    eng.eval("z = complex(x, y);");
		    double x = eng.getVariable("x");
		    System.out.println(x);
    		FloatRawIndexer descIdx = descriptor.createIndexer();
    		int rows = (int) descIdx.rows();
    		int cols = (int) descIdx.cols();
    		float[][] matlabMat = new float[rows][cols];
    		for (int j = 0; j < rows; j++)
    			for (int k = 0; k < cols; k++) matlabMat[j][k] = descIdx.get(j, k);
    		eng.putVariable("features", matlabMat);
    		eng.eval("[idx, C] = kmeans(features," + Parameters.NUM_KMEANS_CLUSTER + ");");
    		float[] idx = eng.getVariable("idx");
    		float[][] C = eng.getVariable("C");
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
		long i = 0;
		KeyPointsDetector detector = new KeyPointsDetector();		
		FeaturesExtraction extractor = new FeaturesExtraction();
		try (DirectoryStream<Path> ds = Files.newDirectoryStream(Parameters.imgDir)) {
	        for (Path directory : ds) {
	        	DirectoryStream<Path> childs = Files.newDirectoryStream(directory);
	        	for (Path file : childs) {
	        		if (!Files.isDirectory(file) && file.toString().toLowerCase().endsWith(".jpg")) {
	        			System.out.println("img: " + file.toString());
	            		Mat image = imread(file.toString());
	            		KeyPointVector keypoints = detector.detectKeypoints(image);
	            		System.out.println("keypoints = " + keypoints.size());
	            		Mat descriptor = extractor.extractDescriptor(image, keypoints);
	            		descriptors.add(descriptor);
		            	i += keypoints.size();
	        		}
	        	}
            }
		}
		System.out.println("Total keypoints: " + i);
        return descriptors;
    }
}
