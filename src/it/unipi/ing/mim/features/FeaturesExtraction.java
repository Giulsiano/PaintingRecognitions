package it.unipi.ing.mim.features;

import org.bytedeco.opencv.opencv_core.KeyPointVector;
import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.opencv.opencv_features2d.Feature2D;

import it.unipi.ing.mim.utils.ResizeImage;

public class FeaturesExtraction {

	public static final int SIFT_FEATURES = 1;
	public static final int ORB_FEATURES = 2;
	
	private Feature2D extractor;
	private KeyPointsDetector detector;
	private KeyPointVector keypoints;
	
	public FeaturesExtraction (int detectorType) {
	    switch (detectorType) {
	        case SIFT_FEATURES:
	            detector = new KeyPointsDetector(KeyPointsDetector.SIFT_FEATURES);
	            break;
	            
	        case ORB_FEATURES:
	            detector = new KeyPointsDetector(KeyPointsDetector.ORB_FEATURES);
	            break;
	           
	        default:
	            throw new IllegalArgumentException("detector type not supported");
	    }
	    extractor = detector.getKeypointDetector();
	}
	
	public FeaturesExtraction (KeyPointsDetector detector) {
	    this.detector = detector;
        this.extractor = detector.getKeypointDetector();
    }
    
	public FeaturesExtraction (Feature2D extractor) {
		this.extractor = extractor;
	}
	
	public Feature2D getDescExtractor() {
		return extractor;
	}
	
	public Mat extractDescriptor(Mat img) {
	    Mat resizedImage = ResizeImage.resizeImage(img);
	    keypoints = detector.detectKeypoints(resizedImage);
	    Mat descriptor = new Mat();
	    extractor.compute(img,  keypoints, descriptor);
	    return descriptor;
	}
	
	public KeyPointsDetector getDetector () {
	    return detector;
	}
	
	public KeyPointVector getKeyPointVector() {
	    return keypoints;
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