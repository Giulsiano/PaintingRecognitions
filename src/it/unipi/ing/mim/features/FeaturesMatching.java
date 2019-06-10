package it.unipi.ing.mim.features;

import org.bytedeco.opencv.opencv_core.DMatchVector;
import org.bytedeco.opencv.opencv_core.Hamming;
import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.opencv.opencv_features2d.BFMatcher;
import org.bytedeco.opencv.opencv_features2d.DescriptorMatcher;



public class FeaturesMatching {

	private DescriptorMatcher matcher;
	
	public FeaturesMatching() {
		//initialize matcher
		matcher = new BFMatcher(Hamming.normType, true);
	}

	public DMatchVector match(Mat queryDescriptors, Mat trainDescriptors) {
		DMatchVector matchVector = new DMatchVector();
		if (matcher != null) matcher.match(queryDescriptors, trainDescriptors, matchVector);
		return matchVector;
	}

}