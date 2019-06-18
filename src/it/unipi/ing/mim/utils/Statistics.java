package it.unipi.ing.mim.utils;

import java.nio.file.Path;
import java.util.LinkedList;
import java.util.List;

public class Statistics {
	private static final int IMG_NUM = 500;
	
	// True/False Positives/Negatives
	// False positive means a retrieved image with matches but it isn't the searched image
	// False negative is an existent indexed image that isn't retrieved by the system
	private int TP; 
	private int FP;
	private int TN;
	private int FN;
	
	private List<String> randomImages;
	
	public Statistics() {
		
	}
	
	public float computeAccuracy () {
		return (float) (TP + TN /(TP + FP + TN + FN));
	}
	
	private List<String> selectRandomImagesFrom (Path imgDir){
		List<String> randomList = new LinkedList<>();
		
	}
}
