package it.unipi.ing.mim.utils;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import it.unipi.ing.mim.deep.seq.SeqImageStorage;
import it.unipi.ing.mim.img.elasticsearch.ElasticImgSearching;
import it.unipi.ing.mim.main.Parameters;

public class Statistics {
	private static final int IMG_NUM = 5;
	
	// True/False Positives/Negatives
	// False positive means a retrieved image with matches but it isn't the searched image
	// False negative is a non existent indexed image that is retrieved by the system
	private int TP; 
	private int FP;
	private int TN;
	private int FN;
	
	private List<String> tnImages;
	
	public static final Path tnImg= FileSystems.getDefault().getPath("/Users/Maria/git/PaintingRecognitions/tnImages");
	
	public static void main(String[] args) {
		Statistics statistics= new Statistics();
		try{
			statistics.initializeTrueNegativeImg();
			statistics.computeConfusionMatrixValues();
		}catch(Exception e) {
			e.printStackTrace();
		}
		
		float precision= statistics.computePrecision();
		float recall= statistics.computeRecall();
		float accuracy= statistics.computeAccuracy();
		float fScore= (2 * recall * precision)/(recall+ precision);
		
		System.out.println("Precision: " + precision + " Recall: " + recall + " Accuracy: "+ accuracy +" F-Score: " + fScore);
		
		System.out.println("TN= " + statistics.TN + " FP= " + statistics.FP + "\nFN= " + statistics.FN + " TP= " + statistics.TP);
	}
	
	public Statistics() {
		TP=0;
		FP=0;
		TN=0;
		FN=0;
		tnImages=new LinkedList<>();
	}
	
	
	private List<String> selectRandomImages () throws FileNotFoundException, ClassNotFoundException, IOException{
		List<String> imgList = new SeqImageStorage().getImageNames();

		List<String> randomList= new ArrayList<>(IMG_NUM);
		while(randomList.size()< IMG_NUM) {
			randomList.add(imgList.remove((int)(Math.random()*imgList.size())));
		}

		return randomList;
	}

	public void initializeTrueNegativeImg() throws IOException {
		for (Path tnImg : Files.newDirectoryStream(tnImg)) {
			tnImages.add(tnImg.toString());
		}
	}

	public float computeAccuracy () {
		return ((float)(TP + TN)/((float)(IMG_NUM + tnImages.size())));
	}
	
	public float computePrecision() {
		return ((float)(TP)/((float)(TP + TN)));
	}
	
	public float computeRecall () {
		return ((float)(TP)/((float)(TP + FP)));
	}



	public void computeConfusionMatrixValues() throws Exception {
		
		String bestMatch = null;
		List<String> randomImg = selectRandomImages();
		for(String currImg: randomImg) {
			ElasticImgSearching elasticImgSearch= new ElasticImgSearching(Parameters.KNN);
            bestMatch=elasticImgSearch.search(currImg); 
			//elasticImgSearch.close();
			if(bestMatch == null) ++FN; 
			else if(bestMatch.equals(currImg)) {
				++TP;
			}
			else ++FP;
		}

		for(String currTNimg : tnImages) {
			ElasticImgSearching elasticImgSearch= new ElasticImgSearching(Parameters.KNN);
            bestMatch=elasticImgSearch.search(currTNimg); 
			if(bestMatch == null) ++TN;
			else ++FP;
		}

	}

	
	
}
