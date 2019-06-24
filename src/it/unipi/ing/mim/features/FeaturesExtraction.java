package it.unipi.ing.mim.features;

import org.bytedeco.opencv.opencv_core.KeyPointVector;
import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.opencv.opencv_features2d.Feature2D;

public class FeaturesExtraction {

	public static final int SIFT_FEATURES = 1;
	public static final int ORB_FEATURES = 2;
	
	private Feature2D extractor;

	public FeaturesExtraction (Feature2D detector) {
		this.extractor = detector;
	}
	
	public Feature2D getDescExtractor() {
		return extractor;
	}

	
	/**
	 * extract the local features
	 */
	public Mat extractDescriptor(Mat img, KeyPointVector keypoints) {
		Mat descriptor = new Mat();
		extractor.compute(img,  keypoints, descriptor);
		return descriptor;
	}
}