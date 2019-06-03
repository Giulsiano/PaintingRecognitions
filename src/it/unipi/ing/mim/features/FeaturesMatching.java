package it.unipi.ing.mim.features;

import static org.bytedeco.opencv.global.opencv_features2d.drawMatches;
import static org.bytedeco.opencv.global.opencv_highgui.destroyAllWindows;
import static org.bytedeco.opencv.global.opencv_highgui.imshow;
import static org.bytedeco.opencv.global.opencv_highgui.waitKey;
import static org.bytedeco.opencv.global.opencv_imgcodecs.imread;

import org.bytedeco.opencv.opencv_core.DMatchVector;
import org.bytedeco.opencv.opencv_core.Hamming;
import org.bytedeco.opencv.opencv_core.KeyPointVector;
import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.opencv.opencv_features2d.BFMatcher;
import org.bytedeco.opencv.opencv_features2d.DescriptorMatcher;



public class FeaturesMatching {

	private DescriptorMatcher matcher;
	
	public static void main(String[] args) throws Exception {
		String image1 = "data/img/figure-at-a-window.photo.jpg";
		String image2 = "data/img/figure-at-a-window.jpg";

		Mat img1 = imread(image1);

		Mat img2 = imread(image2);

		KeyPointsDetector detector = new KeyPointsDetector();
		FeaturesExtraction extractor = new FeaturesExtraction();
		
		KeyPointVector keypoints1 = detector.detectKeypoints(img1);
		Mat descriptor1 = extractor.extractDescriptor(img1, keypoints1);

		KeyPointVector keypoints2 = detector.detectKeypoints(img2);
		Mat descriptor2 = extractor.extractDescriptor(img2, keypoints2);

		FeaturesMatching matching = new FeaturesMatching();
		DMatchVector matches1 = matching.match(descriptor1, descriptor2);

		Mat img_matches1 = new Mat();

		drawMatches(img1, keypoints1, img2, keypoints2, matches1, img_matches1);
		
		imshow("Features Matching", img_matches1);
		waitKey();
		destroyAllWindows();

	}
	
	//TODO
	public FeaturesMatching() {
		//initialize matcher
		matcher = new BFMatcher(Hamming.normType, true);
	}

	//TODO
	public DMatchVector match(Mat queryDescriptors, Mat trainDescriptors) {
		DMatchVector matchVector = new DMatchVector();
		if (matcher != null) matcher.match(queryDescriptors, trainDescriptors, matchVector);
		return matchVector;
	}

}