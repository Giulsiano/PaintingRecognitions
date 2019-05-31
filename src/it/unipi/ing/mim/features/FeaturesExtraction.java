package it.unipi.ing.mim.features;

import static org.bytedeco.opencv.global.opencv_imgcodecs.imread;

import org.bytedeco.opencv.opencv_core.KeyPointVector;
import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.opencv.opencv_features2d.Feature2D;
import org.bytedeco.opencv.opencv_features2d.ORB;
import org.bytedeco.javacpp.indexer.UByteRawIndexer;

public class FeaturesExtraction {

	private Feature2D descExtractor;

	public static void main(String[] args) throws Exception {

		Mat img = imread("data/img/figure-at-a-window.jpg");

		KeyPointsDetector detector = new KeyPointsDetector();		
		KeyPointVector keypoints = detector.detectKeypoints(img);
		
		FeaturesExtraction extractor = new FeaturesExtraction();
		Mat descQuery = extractor.extractDescriptor(img, keypoints);

		extractor.printFeatureValues(descQuery);
	}

	//TODO
	public FeaturesExtraction() {
		//initialize descExtractor;
		descExtractor = ORB.create();
	}

	//TODO
	public Mat extractDescriptor(Mat img, KeyPointVector keypoints) {
		//extract the visual features
		Mat descriptor = new Mat();
		if (descriptor != null) descExtractor.compute(img,  keypoints, descriptor);
		return descriptor;
	}

	//TODO
	public void printFeatureValues(Mat descQuery) {
		//Print the feature data
		UByteRawIndexer indexer = descQuery.createIndexer();
		long rows = indexer.rows();
		long cols = indexer.cols();
		for (long i = 0; i < rows; ++i)
			for (long j = 0; j < cols; ++j) System.out.println("value: " + indexer.get(i, j));
	}

}