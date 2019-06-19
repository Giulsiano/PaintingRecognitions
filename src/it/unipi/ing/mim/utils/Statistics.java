package it.unipi.ing.mim.utils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedList;
import java.util.List;

import it.unipi.ing.mim.img.elasticsearch.ElasticImgSearching;
import it.unipi.ing.mim.main.Parameters;
import it.unipi.ing.mim.main.RansacParameters;

public class Statistics {
	private static final String DELIMITER = ",";
	private static final String COMMENT = "#";
//	private static final int IMG_NUM = 5;
	public static final File outputFile = new File("statistic.txt");
	public static final File ransacParameterFile = new File("ransac_parameters.csv");
	
	// True/False Positives/Negatives
	// False positive means a retrieved image with matches but it isn't the searched image
	// False negative is a non existent indexed image that is retrieved by the system
	private int TP; 
	private int FP;
	private int TN;
	private int FN;
	
	private List<String> tnImages;
	private List<String> tpImages;
	private RansacParameters ransacParameter;
	
	public static final Path tnImg= FileSystems.getDefault().getPath("tnImages");
	public static final Path tpImg= FileSystems.getDefault().getPath("tpImages");
	
	public static void main(String[] args) {
		try{
			// Read RANSAC algorithm parameters from file and put them into a list 
			List<RansacParameters> parameters = new LinkedList<>();
			BufferedReader parameterReader = new BufferedReader(new FileReader(ransacParameterFile));
			String line = null; 
			while ((line = parameterReader.readLine()) != null) {
				if (!line.startsWith(COMMENT) && !line.contentEquals("")) {
					String[] lineParameters = line.split(DELIMITER);
					RansacParameters rp = new RansacParameters();
					rp.setDistanceThreshold(Integer.parseInt(lineParameters[0]));
					rp.setMinRansacInliers(Integer.parseInt(lineParameters[1]));
					rp.setMinGoodMatches(Integer.parseInt(lineParameters[2]));
					rp.setRansacPixelThreshold(Double.parseDouble(lineParameters[3]));
					parameters.add(rp);
				}
			}
			parameterReader.close();
			
			// Collect statistics by using different parameters for RANSAC algorithm
			for (RansacParameters ransacParameters : parameters) {
				Statistics statistics= new Statistics(ransacParameters);
				statistics.initializeTrueNegativeImg();
				statistics.initializeTruePostiveImg();
				statistics.computeConfusionMatrixValues();
				
				float precision= statistics.computePrecision();
				float recall= statistics.computeRecall();
				float accuracy= statistics.computeAccuracy();
				float fScore= (2 * recall * precision)/(recall+ precision);
				
				PrintWriter printFile = new PrintWriter(new BufferedWriter(new FileWriter(outputFile, true)));
				printFile.printf("Run's Parameter:\n");
				printFile.printf("Distance Threshold: %d\nMin Ransac Inliers: %d\nMin Good Matches: %d\nPX threshold: %f\n", 
						ransacParameters.getDistanceThreshold(), 
						ransacParameters.getMinRansacInliers(),
						ransacParameters.getMinGoodMatches(), 
						ransacParameters.getRansacPixelThreshold()
						);
				printFile.printf("Precision: %f\nRecall: %f \nAccuracy: %f\nF-Score: %f\n", 
						precision, 
						recall, 
						accuracy, 
						fScore);
				printFile.println("TN= " + statistics.TN + " FP= " + statistics.FP + "\nFN= " + statistics.FN + " TP= " + statistics.TP);
				printFile.println();
				printFile.println();
				printFile.close();
			}
		} catch(Exception e) {
			e.printStackTrace();
		}
		
	}
	
	public Statistics(RansacParameters ransacParameter) {
		TP=0;
		FP=0;
		TN=0;
		FN=0;
		tnImages=new LinkedList<>();
		tpImages=new LinkedList<>();
		this.ransacParameter=ransacParameter;
	}
	
	
//	private List<String> selectRandomImages () throws FileNotFoundException, ClassNotFoundException, IOException{
//		List<String> imgList = new SeqImageStorage().getImageNames();
//
//		List<String> randomList= new ArrayList<>(IMG_NUM);
//		while(randomList.size()< IMG_NUM) {
//			randomList.add(imgList.remove((int)(Math.random()*imgList.size())));
//		}
//
//		return randomList;
//	}

	public void initializeTrueNegativeImg() throws IOException {
		for (Path tnImg : Files.newDirectoryStream(tnImg)) {
			tnImages.add(tnImg.toString());
		}
	}

	public void initializeTruePostiveImg() throws IOException {
		for (Path tpImg : Files.newDirectoryStream(tpImg)) {
			tpImages.add(tpImg.toString());
		}
	}

	public float computeAccuracy () {
		return ((float)(TP + TN)/((float)(tpImages.size() + tnImages.size())));
	}
	
	public float computePrecision() {
		return ((float)(TP)/((float)(TP + TN)));
	}
	
	public float computeRecall () {
		return ((float)(TP)/((float)(TP + FP)));
	}

	public void computeConfusionMatrixValues() throws Exception {
		
		String bestMatch = null;
		for(String currTPImg: tpImages) {
			ElasticImgSearching elasticImgSearch= new ElasticImgSearching(this.ransacParameter, Parameters.KNN);
			bestMatch = elasticImgSearch.search(currTPImg);
			if(bestMatch == null) ++FN;
			else {
				// In case there is a best match try to compare the last part of the image's path
				String[] splitPath = bestMatch.split(File.separator);
				bestMatch = splitPath[splitPath.length - 1];
				splitPath = currTPImg.split(File.separator);
				String currTPImgName = splitPath[splitPath.length - 1];
				//elasticImgSearch.close();
				if(bestMatch.equals(currTPImgName)) {
					++TP;
				}
				else ++FP;
			}
		}

		for(String currTNImg : tnImages) {
			ElasticImgSearching elasticImgSearch= new ElasticImgSearching(this.ransacParameter, Parameters.KNN);
            bestMatch=elasticImgSearch.search(currTNImg);
			if(bestMatch == null) ++TN;
			else ++FP;
		}
	}
}
