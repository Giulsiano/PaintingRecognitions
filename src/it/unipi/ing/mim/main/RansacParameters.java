package it.unipi.ing.mim.main;

public class RansacParameters {
	private int DISTANCE_THRESHOLD = 25; // from 25 to 50
	private int MIN_RANSAC_INLIERS = 12;
	private int MIN_GOOD_MATCHES = 15;
	private double RANSAC_PX_THRESHOLD = 1.0;
	
	public int getDistanceThreshold () {
		return DISTANCE_THRESHOLD;
	}
	
	public void setDistanceThreshold (int dISTANCE_THRESHOLD) {
		if (dISTANCE_THRESHOLD < 25)  DISTANCE_THRESHOLD = 25;
		else if (dISTANCE_THRESHOLD > 50) DISTANCE_THRESHOLD = 50;
		else DISTANCE_THRESHOLD = dISTANCE_THRESHOLD;
	}
	
	public int getMinRansacInliers () {
		return MIN_RANSAC_INLIERS;
	}
	
	public void setMinRansacInliers (int mIN_RANSAC_INLIERS) {
		MIN_RANSAC_INLIERS = mIN_RANSAC_INLIERS;
	}
	
	public int getMinGoodMatches () {
		return MIN_GOOD_MATCHES;
	}
	
	public void setMinGoodMatches (int mIN_GOOD_MATCHES) {
		MIN_GOOD_MATCHES = mIN_GOOD_MATCHES;
	}
	
	public double getRansacPixelThreshold () {
		return RANSAC_PX_THRESHOLD;
	}
	
	public void setRansacPixelThreshold (double rANSAC_PX_THRESHOLD) {
		if (rANSAC_PX_THRESHOLD < 1.0f) RANSAC_PX_THRESHOLD = 1.0f;
		if (rANSAC_PX_THRESHOLD > 3.0f) RANSAC_PX_THRESHOLD = 3.0f;
		else RANSAC_PX_THRESHOLD = rANSAC_PX_THRESHOLD;
	}
}
