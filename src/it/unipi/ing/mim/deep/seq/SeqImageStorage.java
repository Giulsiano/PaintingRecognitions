package it.unipi.ing.mim.deep.seq;

import it.unipi.ing.mim.deep.ImgDescriptor;
import it.unipi.ing.mim.deep.Parameters;
import it.unipi.ing.mim.deep.tools.FeaturesStorage;
import it.unipi.ing.mim.features.FeaturesExtraction;
import it.unipi.ing.mim.features.KeyPointsDetector;
import java.nio.file.Path;

import static org.bytedeco.opencv.global.opencv_imgcodecs.imread;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.bytedeco.javacpp.indexer.FloatRawIndexer;
import org.bytedeco.opencv.opencv_core.KeyPointVector;
import org.bytedeco.opencv.opencv_core.Mat;

public class SeqImageStorage {
	
	private final File descFile = new File(Parameters.DESCRIPTOR_FILE);
	private final File featFile = new File(Parameters.FEATURE_FILE);

	public void extractFeatures(Path imgFolder) throws FileNotFoundException{
		String filename = imgFolder.toString();
		PrintWriter featFile = new PrintWriter(this.featFile);
		KeyPointsDetector detector = new KeyPointsDetector();
		StringBuilder fileLine = new StringBuilder();
		FeaturesExtraction extractor = new FeaturesExtraction();
		
		try {
			// For each directory into the main image directory
			ObjectOutputStream ois = new ObjectOutputStream(new FileOutputStream(descFile));
			for (Path dir : Files.newDirectoryStream(imgFolder)) {
				
				// For each file into the directory
				for (Path file : Files.newDirectoryStream(dir)) {
					
					filename = file.toString();
					if (filename.toLowerCase().endsWith(".jpg")) {
						// Compute the descriptors of the image
						Mat image = imread(filename);
						KeyPointVector keypoints = detector.detectKeypoints(image);
						Mat descriptor = extractor.extractDescriptor(image, keypoints);
						
						// Create the feature matrix for the image to be stored into an ImgDescriptor
						FloatRawIndexer idx = descriptor.createIndexer();
						int rows = (int) idx.rows();
						int cols = (int) idx.cols();
						float[][] feat = new float[rows][cols];
						for (int i = 0; i < rows; i++) {
							for (int j = 0; j < cols; j++) {
								feat[i][j] = idx.get(i, j);
							}
						}
						
						// Store on file each descriptor's feature normalized. ImgDescriptor normalize
						// the matrix into the constructor
						System.out.println("Saving keypoints for " + filename);
						ImgDescriptor ids = new ImgDescriptor(feat, filename);
						ids.setId(filename);
						ois.writeObject(ids);
						feat = ids.getFeatures();
						for (int i = 0; i < rows; i++) {
							for (int j = 0; j < cols; j++) {
								fileLine.append(feat[i][j] + ((j < (feat[i].length - 1)) ? "," : "\n"));
							}
							featFile.append(fileLine.toString());
							
			    			// Reset the string buffer
			    			fileLine.setLength(0);
						}
					}
				}
				// If we store everything in memory the program crashes for its memory consumption
				ois.flush();
			}
		}
		catch (IOException e) {
			System.out.println("IOException for " + filename);
		}
		featFile.close();
	}		
}