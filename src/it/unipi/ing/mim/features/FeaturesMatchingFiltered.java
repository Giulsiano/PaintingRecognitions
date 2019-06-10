package it.unipi.ing.mim.features;

import static org.bytedeco.opencv.global.opencv_features2d.drawMatches;
import static org.bytedeco.opencv.global.opencv_highgui.destroyAllWindows;
import static org.bytedeco.opencv.global.opencv_highgui.imshow;
import static org.bytedeco.opencv.global.opencv_highgui.waitKey;
import static org.bytedeco.opencv.global.opencv_imgcodecs.imread;

import org.bytedeco.opencv.opencv_core.DMatch;
import org.bytedeco.opencv.opencv_core.DMatchVector;
import org.bytedeco.opencv.opencv_core.KeyPointVector;
import org.bytedeco.opencv.opencv_core.Mat;

import it.unipi.ing.mim.deep.Parameters;


public class FeaturesMatchingFiltered {
	
	public static void main(String[] args) throws Exception {

		Mat img1 = imread("data/img/figure-at-a-window.photo.jpg");
		Mat img2 = imread("data/img/figure-at-a-window.jpg");
		
		KeyPointsDetector detector = new KeyPointsDetector();
		FeaturesExtraction extractor = new FeaturesExtraction();

		KeyPointVector keypoints1 = detector.detectKeypoints(img1);
		Mat descriptor1 = extractor.extractDescriptor(img1, keypoints1);

		KeyPointVector keypoints2 = detector.detectKeypoints(img2);
		Mat descriptor2 = extractor.extractDescriptor(img2, keypoints2);
		
		FeaturesMatching matching = new FeaturesMatching();
		DMatchVector matches = matching.match(descriptor1, descriptor2);
		
		FeaturesMatchingFiltered filter = new FeaturesMatchingFiltered();
		DMatchVector matches2 = filter.matchWithFiltering(matches, Parameters.MAX_DISTANCE_THRESHOLD);
		System.out.println("Good Matches: " + matches2.size());

		Mat filteredMatches = new Mat();

		drawMatches(img1, keypoints1, img2, keypoints2, matches2, filteredMatches);
		
		imshow("Filtered Matching", filteredMatches);
		waitKey();
		destroyAllWindows();
	}

	public DMatchVector matchWithFiltering(DMatchVector matches, int threshold) {
		//return the good matches
		long nGoodMatches = 0;
		long numMatches = matches.size();
		DMatchVector goodMatches = new DMatchVector(numMatches);
		for (long i = 0; i < numMatches; ++i) {
			DMatch match = matches.get(i);
			if (match.distance() < threshold) {
				goodMatches.put(nGoodMatches, match);
				nGoodMatches++;
			}
		}
		goodMatches.resize(nGoodMatches);
		return goodMatches;
	}

}