package it.unipi.ing.mim.features;

import static org.bytedeco.opencv.global.opencv_imgcodecs.imread;

import org.bytedeco.javacpp.indexer.FloatRawIndexer;
import org.bytedeco.opencv.opencv_core.KeyPointVector;
import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.opencv.opencv_features2d.Feature2D;
import org.bytedeco.opencv.opencv_features2d.ORB;
import org.bytedeco.opencv.opencv_xfeatures2d.SIFT;

import it.unipi.ing.mim.deep.Parameters;

public class FeaturesExtraction {

	private Feature2D descExtractor;

	public static final int SIFT_FEATURES = 1;
	public static final int ORB_FEATURES = 2;
	KeyPointsDetector detector = new KeyPointsDetector();

	public FeaturesExtraction(int featureType) {
		//initialize descExtractor;
		switch (featureType) {
			case SIFT_FEATURES:
				descExtractor = SIFT.create();
				break;
			
			case ORB_FEATURES:
				descExtractor = ORB.create();
				((ORB) descExtractor).setMaxFeatures(Parameters.ORB_MAX_FEATURE);
				break;
	
			default:
				throw new IllegalArgumentException("Feature extractor not recognized");
		}
	}
	
	public Feature2D getDescExtractor() {
		return descExtractor;
	}

	public Mat extractDescriptor(Mat img, KeyPointVector keypoints) {
		//extract the visual features
		Mat descriptor = new Mat();
		descExtractor.compute(img,  keypoints, descriptor);
		return descriptor;
	}

	public void printFeatureValues(Mat descQuery) {
		//Print the feature data
		FloatRawIndexer indexer = descQuery.createIndexer();
		long rows = indexer.rows();
		long cols = indexer.cols();
		for (long i = 0; i < rows; ++i)
			for (long j = 0; j < cols; ++j) System.out.println("value: " + indexer.get(i, j));
	}
}