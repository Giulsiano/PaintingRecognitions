package it.unipi.ing.mim.deep.seq;

import it.unipi.ing.mim.deep.ImgDescriptor;
import it.unipi.ing.mim.deep.Parameters;
import it.unipi.ing.mim.deep.tools.FeaturesStorage;
import it.unipi.ing.mim.features.FeaturesExtraction;
import it.unipi.ing.mim.features.KeyPointsDetector;
import java.nio.file.Path;

import static org.bytedeco.opencv.global.opencv_imgcodecs.imread;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.bytedeco.javacpp.indexer.FloatRawIndexer;
import org.bytedeco.opencv.opencv_core.KeyPointVector;
import org.bytedeco.opencv.opencv_core.Mat;

public class SeqImageStorage {

	public static void main(String[] args) throws Exception {
				
		SeqImageStorage indexing = new SeqImageStorage();
				
		List<ImgDescriptor> descriptors = indexing.extractFeatures(Parameters.imgDir);
		
		FeaturesStorage.store(descriptors, Parameters.STORAGE_FILE);
	}
	
	public List<ImgDescriptor> extractFeatures(Path imgFolder){
		List<ImgDescriptor> imageDescs = new LinkedList<>();
		String filename = imgFolder.toString();
		KeyPointsDetector detector = new KeyPointsDetector();		
		FeaturesExtraction extractor = new FeaturesExtraction();
		try {
			for (Path dir : Files.newDirectoryStream(imgFolder)) {
				for (Path file : Files.newDirectoryStream(dir)) {
					filename = file.toString();
					if (filename.toLowerCase().endsWith(".jpg")) {
						Mat image = imread(filename);
						KeyPointVector keypoints = detector.detectKeypoints(image);
						Mat descriptor = extractor.extractDescriptor(image, keypoints);
						
						FloatRawIndexer idx = descriptor.createIndexer();
						int rows = (int) idx.rows();
						int cols = (int) idx.cols();
						float[][] feat = new float[rows][cols];
						for (int i = 0; i < rows; i++) {
							for (int j = 0; j < cols; j++) {
								feat[i][j] = idx.get(i, j);
							}
						}
						imageDescs.add(new ImgDescriptor(feat, filename));
					}
				}
			}
		}
		catch (IOException e) {
			System.out.println("IOException for " + filename);
			imageDescs.clear();
		}
		return imageDescs;	
	}		
}
