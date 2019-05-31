package it.unipi.ing.mim.features;

import static org.bytedeco.opencv.global.opencv_features2d.drawKeypoints;
import static org.bytedeco.opencv.global.opencv_highgui.destroyAllWindows;
import static org.bytedeco.opencv.global.opencv_highgui.imshow;
import static org.bytedeco.opencv.global.opencv_highgui.waitKey;
import static org.bytedeco.opencv.global.opencv_imgcodecs.imread;
import static org.bytedeco.opencv.global.opencv_features2d.DRAW_RICH_KEYPOINTS;

import org.bytedeco.opencv.opencv_core.KeyPoint;
import org.bytedeco.opencv.opencv_core.KeyPointVector;
import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.opencv.opencv_core.Point2f;
import org.bytedeco.opencv.opencv_core.Scalar;
import org.bytedeco.opencv.opencv_features2d.Feature2D;
import org.bytedeco.opencv.opencv_features2d.ORB;

public class KeyPointsDetector {

	private Feature2D detector;
	public static int MAX_FEATURE = 1000;

	public static void main(String[] args) throws Exception {
		
		Mat img = imread("data/img/figure-at-a-window.jpg");
		System.out.println("image1.type="+org.opencv.core.CvType.typeToString(img.type()));

		
		KeyPointsDetector detector = new KeyPointsDetector();
		
		KeyPointVector keypoints = detector.detectKeypoints(img);
		detector.printKeyPointsValues(keypoints);

		Mat outImage = new Mat();
//		drawKeypoints(img, keypoints, outImage, new Scalar(255,0,0, 0), DrawMatchesFlags.DRAW_RICH_KEYPOINTS);
		drawKeypoints(img, keypoints, outImage);
		
		imshow("Keypoint Size and Orientation", outImage);
		waitKey();
		destroyAllWindows();
	}
	
	//
	public KeyPointsDetector() {
		//initialize detector
		detector = ORB.create();
		((ORB) detector).setMaxFeatures(MAX_FEATURE);
	}

	//
	public KeyPointVector detectKeypoints(Mat img) {
		KeyPointVector keyPoints = new KeyPointVector();
		//Detect img keypoints
		if (detector != null) detector.detect(img, keyPoints);
		return keyPoints;
	}

	//
	public void printKeyPointsValues(KeyPointVector keypoints) {
		//Print keypoint data for each keypoint: x, y, size and angle
		long numel = keypoints.size();
		for(long i = 0; i < numel; ++i) {
			KeyPoint keypoint = keypoints.get(i);
			System.out.printf("(%f, %f) size: %f, angle: %f\n", keypoint.pt().x(),
					                                           keypoint.pt().y(),
					                                           keypoint.size(),
					                                           keypoint.angle()
					         );
		}
	}

}
