package it.unipi.ing.mim.main;

public class RansacParameters {
	private int DISTANCE_THRESHOLD = 25; // from 25 to 50
	private int MIN_RANSAC_INLIERS = 12;
	private int MIN_GOOD_MATCHES = 15;
	private double RANSAC_PX_THRESHOLD = 1.0;
	
	public int getDistanceThreshold () {
		return DISTANCE_THRESHOLD;
	}
	
	public void setDistanceThreshold (int distanceThreshold) {
		if (distanceThreshold < 25)  DISTANCE_THRESHOLD = 25;
		else if (distanceThreshold > 50) DISTANCE_THRESHOLD = 50;
		else DISTANCE_THRESHOLD = distanceThreshold;
	}
	
	public int getMinRansacInliers () {
		return MIN_RANSAC_INLIERS;
	}
	
	public void setMinRansacInliers (int minRansacInliers) {
		MIN_RANSAC_INLIERS = minRansacInliers;
	}
	
	public int getMinGoodMatches () {
		return MIN_GOOD_MATCHES;
	}
	
	public void setMinGoodMatches (int minGoodMatches) {
		MIN_GOOD_MATCHES = minGoodMatches;
	}
	
	public double getRansacPixelThreshold () {
		return RANSAC_PX_THRESHOLD;
	}
	
	public void setRansacPixelThreshold (double ransacPxThreshold) {
		if (ransacPxThreshold < 1.0f) RANSAC_PX_THRESHOLD = 1.0f;
		if (ransacPxThreshold > 3.0f) RANSAC_PX_THRESHOLD = 3.0f;
		else RANSAC_PX_THRESHOLD = ransacPxThreshold;
	}
}
