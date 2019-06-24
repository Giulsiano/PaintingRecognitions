package it.unipi.ing.mim.features;

import org.bytedeco.opencv.opencv_core.KeyPointVector;
import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.opencv.opencv_features2d.Feature2D;
import org.bytedeco.opencv.opencv_features2d.ORB;
import org.bytedeco.opencv.opencv_xfeatures2d.SIFT;

import it.unipi.ing.mim.main.Parameters;

public class KeyPointsDetector {

	public static final int SIFT_FEATURES = 1;
	public static final int ORB_FEATURES = 2;
	public static int MAX_FEATURE = 1000;
	
	private Feature2D detector;
	
	public Feature2D getKeypointDetector () {
		return detector;
	}
	
	/**
	 * initialize the detector with the type of keypoint detector to use
	 */
	public KeyPointsDetector(int featureType) {
		switch (featureType) {
		case SIFT_FEATURES:
			detector = SIFT.create();
			break;

		case ORB_FEATURES:
			detector = ORB.create();
			((ORB) detector).setMaxFeatures(Parameters.ORB_MAX_FEATURE);
			break;

		default:
			throw new IllegalArgumentException("Feature extractor not recognized");
		}
	}

	/**
	 * detect image keypoints
	 */
	public KeyPointVector detectKeypoints(Mat img) {
		KeyPointVector keyPoints = new KeyPointVector();
		if (detector != null) detector.detect(img, keyPoints);
		return keyPoints;
	}
}
