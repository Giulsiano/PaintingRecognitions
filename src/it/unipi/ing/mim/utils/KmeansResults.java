package it.unipi.ing.mim.utils;

import static org.bytedeco.opencv.global.opencv_core.CV_32F;
import static org.bytedeco.opencv.global.opencv_core.kmeans;

import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.opencv.opencv_core.TermCriteria;
import org.opencv.core.Core;

import it.unipi.ing.mim.main.Parameters;

public class KmeansResults {
	private Mat features;
	private Mat centroids;
	private Mat labels;
	
	public KmeansResults(Mat features) {
		this.features = features;
	}
	
	public void computeKmeans () {
		labels = new Mat();
		centroids = new Mat();
		TermCriteria criteria = new TermCriteria(CV_32F, 100, 1.0d);
		kmeans(features, Parameters.NUM_KMEANS_CLUSTER, labels, criteria, 10, Core.KMEANS_PP_CENTERS, centroids);
	}
	
	public Mat getCentroids() {
		return centroids;
	}

	public Mat getLabels() {
		return labels;
	}

}
