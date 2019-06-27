package it.unipi.ing.mim.utils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.DirectoryStream;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.elasticsearch.ElasticsearchException;

import it.unipi.ing.mim.img.elasticsearch.ElasticImgSearching;
import it.unipi.ing.mim.main.Parameters;
import it.unipi.ing.mim.main.RansacParameters;

public class Statistics {
	private static final String DELIMITER = ",";
	private static final String COMMENT = "#";
	public static final File outputFile = new File("statistic.txt");
	public static final File ransacParameterFile = new File("ransac_parameters.csv");
	public static final File testSetFile = new File("test_set.csv");
	
	//	True positive: image correctly matched
	//	True negative: image correctly not matched
	//	False positive: image got from index but not correctly matched
	//	False negative: indexed image but not matched
	private int TP; 
	private int FP;
	private int TN;
	private int FN;
	
	private List<String> tnImages;
	private List<String> tpImages;
	private RansacParameters ransacParameter;

	private Map<String, List<String>> testset;
	
	public static final Path tnImg= FileSystems.getDefault().getPath("tnImages");
	public static final Path tpImg= FileSystems.getDefault().getPath("tpImages");
	
	public static void main(String[] args) {
		try{
			System.out.println("Start statistics program");
			System.out.println("Read RANSAC parameters");
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
			
			System.out.println("Calculating statistics");
			Statistics statistics = new Statistics(); 
			// Collect statistics by using different parameters for RANSAC algorithm
			for (RansacParameters ransacParameters : parameters) {
				statistics.setRansacParameter(ransacParameters);
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
				statistics.resetStatstics();
			}
		} catch(ElasticsearchException e) {
			System.err.println("Elasticsearch Exception");
			System.err.println(e.getMessage());
			System.err.println(e.getDetailedMessage());
			e.printStackTrace();
		} catch(IOException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		System.out.println("End statistics program");
	}
	
	public Statistics () {
		TP=0;
		FP=0;
		TN=0;
		FN=0;
		try {
			initializeTrueNegativeImg();
			initializeTruePostiveImg();
			initTestSet();
		} 
		catch (IOException e) {
			System.err.println("Can't read file from disk");
			e.printStackTrace();
			System.err.println("Exiting");
			System.exit(1);
		}
	}
	
	public void resetStatstics () {
		TP = TN = FP = FN = 0;
	}
	
	public Statistics(RansacParameters ransacParameter) {
		this();
		this.ransacParameter=ransacParameter;
	}
	
	public void setRansacParameter(RansacParameters ransacParameter) {
		this.ransacParameter = ransacParameter;
	}
	
	private void initTestSet() throws IOException {
		testset = new HashMap<>();
		BufferedReader testSetReader = new BufferedReader(new FileReader(testSetFile));
		String line = null; 
		System.out.println("Initializing test set from file " + testSetFile.toString());
		while ((line = testSetReader.readLine()) != null) {
				String[] lineName = line.split(DELIMITER);
				List<String> matchImg = new ArrayList<String>(lineName.length-1);
				Arrays.stream(Arrays.copyOfRange(lineName, 1, lineName.length))
					  .forEach((imgName) -> {
						  matchImg.add(imgName);
					  });
				testset.put(lineName[0], matchImg);
		}
		testSetReader.close();
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
		tnImages = initializeImgList(tnImg);
	}
	
	private List<String> initializeImgList (Path imgDirectory) throws IOException {
		System.out.println("Initializing image list from directory " + imgDirectory.toString());
		List<String> imgList = new LinkedList<String>();
		DirectoryStream<Path> imgDirectories = Files.newDirectoryStream(imgDirectory);
		for (Path img : imgDirectories) {
			imgList.add(img.toString());
		}
		imgDirectories.close();
		return imgList;
	}
	
	public void initializeTruePostiveImg() throws IOException {
		tpImages = initializeImgList(tpImg);
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
		
		System.out.println("Generating Confusion matrix");
		String bestMatch = null;
		for(String currTPImg: tpImages) {
			ElasticImgSearching elasticImgSearch= 
			        new ElasticImgSearching(this.ransacParameter, Parameters.TOP_K_QUERY, Parameters.INDEX_NAME);
			try{
				System.out.println("Searching for " + currTPImg);
				bestMatch = elasticImgSearch.search(currTPImg);
				elasticImgSearch.close();
				if(bestMatch == null) ++FN;
				else {
					// In case there is a best match try to compare the last part of the image's path
					String[] splitPath = bestMatch.split(Pattern.quote(File.separator));
					bestMatch = splitPath[splitPath.length - 1];
					splitPath = currTPImg.split(Pattern.quote(File.separator));
					String currTPImgName = splitPath[splitPath.length - 1];
					//elasticImgSearch.close();
					
					List<String> expectedImgs = testset.get(bestMatch); //search the name
					if(expectedImgs == null) ++FP; //if null, not present
					else if(expectedImgs.contains(currTPImgName)) //if not null search
						++TP;
					else ++FP;
				}
			}
			catch (IllegalArgumentException e) {
				System.err.println(e.getMessage());
			}
			System.out.println();
		}

		for(String currTNImg : tnImages) {
			try{
			ElasticImgSearching elasticImgSearch=
			        new ElasticImgSearching(this.ransacParameter, Parameters.TOP_K_QUERY, Parameters.INDEX_NAME);
	            bestMatch=elasticImgSearch.search(currTNImg);
	            elasticImgSearch.close();
				if(bestMatch == null) ++TN;
				else ++FP;
			}catch(IllegalArgumentException e) {
				System.err.println(e.getMessage());
			}
		}
	}
	
	public static void createCsvFile() {
		String filepath = "";
		try {
			BufferedWriter bw = new BufferedWriter(new FileWriter(testSetFile));
			for (Path dir : Files.newDirectoryStream(tpImg)) {
				if(!dir.toString().endsWith(".DS_Store")) {
					// For each file into the directory
					filepath = dir.toString();
					String[] splitPath = filepath.split(File.separator);
					String filename = splitPath[splitPath.length - 1];
					bw.write(filename +","+filename+"\n");
				}
			}
			bw.close();
		}catch (IOException e) {

			System.err.println("IOException file " + filepath);
			e.printStackTrace();

		}
	}
}
