package it.unipi.ing.mim.utils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
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
	
	/**
	 * ElasticSearch index name to query for match images on
	 */
	public static String ESindexName = Parameters.INDEX_NAME;
	
    /**
	 * Output file where this class will store results, one for each run. The total number of runs is
	 * determined by the number of rows in {@link Statistics#ransacParameter}
	 */
	public static File outputFile = new File("statistic.txt");
	
	/**
	 * File that has to be a CSV with this format:<br/>
	 * a,b,c,d <br/>
	 * where a = Distance Threshold, b = Min Ransac Inliers, c = Min Good Matches, d = Px Threshold<br/>
	 * For example: 30,12,15,1.0
	 */
	public static File ransacParameterFile = new File("ransac_parameters.csv");
	
	/**
	 * This file must be a CSV with this format:<br/>
	 * name_of_image_to_match, list_of_comma_separated_images_that_matches<br/>
	 * For example:<br/>
	 * <code>a-bear.jpg,test-a-bear.jpg,test2-a-bear.jpg</code><br/>
	 * where <code>a-bear.jpg</code> is the name of the image to match, <code>test?-a-bear.jpg</code> are the images that
	 * should match with a-bear.jpg<br/>
	 */
	public static File testSetFile = new File("test_set.csv");
	
	private int TP;    // True positive: image correctly matched
	private int FP;    // False positive: image got from index but not correctly matched
	private int TN;    // True negative: image correctly not matched
	private int FN;    // False negative: indexed image but not matched
	
	private List<String> tnImages;
	private List<String> tpImages;
	private RansacParameters ransacParameter;

	private Map<String, List<String>> testset;
	
	public static Path tnImg= FileSystems.getDefault().getPath("tnImages");
	public static Path tpImg= FileSystems.getDefault().getPath("tpImages");
	
	/**
	 * Start gathering statistics by using file names passed by parameters
	 * @param fileNames A positional array that contains file names of each file needed:<br/>
	 *                  fileNames[0] = file where statistics should stored<br/>
	 *                  fileNames[1] = file where you can read different ransac parameters. The 
	 *                  format of this file is explained into {@link Statistics#ransacParameterFile}<br/>
	 *                  fileNames[2] = test set file, that is the file with the format explained on
	 *                  {@link Statistics#testSetFile}<br/>
	 *                  fileNames[3] = directory containing images to test that are in the index
	 *                  (true positive images)<br/>
	 *                  fileNames[4] = directory containing images to test that aren't in the index
	 *                  (true negative images)
	 */
	public static void run (String[] fileNames, String indexName) {
	    if (fileNames.length < 4) throw new IllegalArgumentException("fileNames is too short.");
		try {
		    outputFile = new File(fileNames[0]);
		    ransacParameterFile = new File(fileNames[1]);
		    testSetFile = new File(fileNames[2]);
		    tpImg = FileSystems.getDefault().getPath(fileNames[3]);
		    tnImg = FileSystems.getDefault().getPath(fileNames[4]);
		    ESindexName = indexName;
			System.out.println("Gathering statistics into " + outputFile.toString());
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
			
			// Collect statistics by using different parameters for RANSAC algorithm
			System.out.println("Calculating statistics");
			Statistics statistics = new Statistics(); 
			for (RansacParameters ransacParameters : parameters) {
				statistics.setRansacParameter(ransacParameters);
				statistics.computeConfusionMatrixValues();
				
				float precision= statistics.computePrecision();
				float recall= statistics.computeRecall();
				float accuracy= statistics.computeAccuracy();
				float fScore= (2 * recall * precision)/(recall + precision);
				
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
		}
		catch (ElasticsearchException e) {
			System.err.println("Elasticsearch Exception");
			System.err.println(e.getMessage());
			System.err.println(e.getDetailedMessage());
			e.printStackTrace();
		}
		catch (IOException e) {
		    System.err.println("Can't read file from disk");
		    e.printStackTrace();
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		System.out.println("End statistics program");
	}
	
	public Statistics () throws IOException {
		TP=0;
		FP=0;
		TN=0;
		FN=0;
		initializeTrueNegativeImg();
		initializeTruePostiveImg();
		initTestSet();
	}
	
	public void resetStatstics () {
		TP = TN = FP = FN = 0;
	}
	
	public Statistics (RansacParameters ransacParameter) throws IOException {
		this();
		this.ransacParameter=ransacParameter;
	}
	
	public void setRansacParameter (RansacParameters ransacParameter) {
		this.ransacParameter = ransacParameter;
	}
	
	private void initTestSet () throws IOException {
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
			        new ElasticImgSearching(this.ransacParameter, Parameters.TOP_K_QUERY, ESindexName);
			try{
				System.out.println("Searching for " + currTPImg);
				bestMatch = elasticImgSearch.search(currTPImg, true);
				elasticImgSearch.close();
				if(bestMatch == null) ++FN;
				else {
					// In case there is a best match try to compare the last part of the image's path
					String[] splitPath = bestMatch.split(Pattern.quote(File.separator));
					bestMatch = splitPath[splitPath.length - 1];
					splitPath = currTPImg.split(Pattern.quote(File.separator));
					String currTPImgName = splitPath[splitPath.length - 1];
					
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
			        new ElasticImgSearching(this.ransacParameter, Parameters.TOP_K_QUERY, ESindexName);
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
			// For each file into the directory tpImg
			for (Path dir : Files.newDirectoryStream(tpImg)) {
				if(!dir.toString().endsWith(".DS_Store")) {
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
	
   public void setIndexName(String indexName) {
        this.ESindexName = indexName;
    }

}
