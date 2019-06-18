package it.unipi.ing.mim.features;

import static org.bytedeco.opencv.global.opencv_calib3d.RANSAC;
import static org.bytedeco.opencv.global.opencv_calib3d.findHomography;
import static org.bytedeco.opencv.global.opencv_core.CV_32FC2;
import static org.bytedeco.opencv.global.opencv_core.CV_8UC1;

import org.bytedeco.javacpp.indexer.FloatIndexer;
import org.bytedeco.javacpp.indexer.UByteRawIndexer;
import org.bytedeco.opencv.opencv_core.DMatchVector;
import org.bytedeco.opencv.opencv_core.KeyPointVector;
import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.opencv.opencv_core.Point2f;

import it.unipi.ing.mim.main.RansacParameters;

public class Ransac {

	private Mat homography;
	private Mat inliers;
	private double pxThreshold;
	private RansacParameters parameters;
	
	public Ransac (RansacParameters parameters) {
		this.parameters = parameters;
	}

	public void computeHomography(DMatchVector goodMatches, KeyPointVector keypointsObject,
								  KeyPointVector keypointsScene) {

		Mat obj = new Mat((int) goodMatches.size(), 1, CV_32FC2);
		Mat scene = new Mat((int) goodMatches.size(), 1, CV_32FC2);
		this.inliers = new Mat((int) parameters.getMinRansacInliers(), 1, CV_8UC1);

		FloatIndexer ptObjIdx = obj.createIndexer();
		FloatIndexer ptSceneIdx = scene.createIndexer();

		for (int i = 0; i < goodMatches.size(); i++) {
			Point2f p1 = keypointsObject.get(goodMatches.get(i).queryIdx()).pt(); 
			ptObjIdx.put(i, p1.x(), p1.y());
			Point2f p2 = keypointsScene.get(goodMatches.get(i).trainIdx()).pt(); 
			ptSceneIdx.put(i, p2.x(), p2.y());
		}
		homography = findHomography(obj, scene, inliers, RANSAC, pxThreshold);
	}


	public Mat getHomography() {
		return homography;
	}

	public Mat getInliers() {
		return inliers;
	}
	
	public int countNumInliers() {
		UByteRawIndexer index = inliers.createIndexer();
		int count = 0;
		long rows = index.rows();
		long cols = index.cols();
		for (int i = 0; i < rows; i++) {
			for (int j = 0; j < cols; j++) {
				float elem = index.get(i, j);
				if (elem == 1) {
					count++;
				}
			}
		}
		return count;
	}

}
