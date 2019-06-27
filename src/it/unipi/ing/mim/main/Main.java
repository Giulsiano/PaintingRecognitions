package it.unipi.ing.mim.main;

import static org.bytedeco.opencv.global.opencv_features2d.drawMatches;
import static org.bytedeco.opencv.global.opencv_highgui.destroyAllWindows;
import static org.bytedeco.opencv.global.opencv_highgui.waitKey;

import java.util.Map;

import org.bytedeco.opencv.opencv_core.DMatchVector;
import org.bytedeco.opencv.opencv_core.KeyPointVector;
import org.bytedeco.opencv.opencv_core.Mat;

import com.github.cliftonlabs.json_simple.JsonObject;

import it.unipi.ing.mim.deep.tools.Output;
import it.unipi.ing.mim.features.ImageBox;
import it.unipi.ing.mim.img.elasticsearch.ElasticImgIndexing;
import it.unipi.ing.mim.img.elasticsearch.ElasticImgSearching;
import it.unipi.ing.mim.utils.MetadataRetriever;

public class Main {
	private static boolean showMatchWindow = false;
	public static void main(String[] args) {
		if (args.length < 2) printHelp();
		else {
		    String indexName = "-i".equals(args[args.length - 2]) ? args[args.length - 1] :
		                                                            Parameters.INDEX_NAME;
			try {
				switch (args[0]) {
				    case "search":
				        ElasticImgSearching eis = 
				                        new ElasticImgSearching(Parameters.TOP_K_QUERY, indexName);
				        String bestGoodMatch = eis.search(args[1]);
				        eis.close();
				        
				        if (bestGoodMatch != null) {
				            JsonObject metadata = MetadataRetriever.readJsonFile(bestGoodMatch);
			                String qryImagePath = Parameters.BASE_URI + args[1];
			                String bestMatchPath = Parameters.BASE_URI + bestGoodMatch;
			                Output.toHTML(metadata, qryImagePath, bestMatchPath, Parameters.RESULTS_HTML);
				        }
				        if (showMatchWindow) {
				            Mat imgMatches = new Mat();
				            Map<String, Object> bestMatch = eis.getBestGoodMatch();
				            Mat queryImg = (Mat) bestMatch.get("queryImg");
				            KeyPointVector qryKeypoints = 
				                    (KeyPointVector) bestMatch.get("queryKeypoints");
				            Mat bestImg = (Mat) bestMatch.get("image");
				            KeyPointVector bestKeyPoints = 
                                    (KeyPointVector) bestMatch.get("imageKeypoints");
				            DMatchVector matchVector = 
				                    (DMatchVector) bestMatch.get("matchVector");
				            Mat homomography = (Mat) bestMatch.get("homomography");
				            drawMatches(queryImg, qryKeypoints, bestImg , bestKeyPoints, matchVector, imgMatches);
				            //ImageBox.addBoundingBox(imgMatches, queryImg, homomography, 0);// queryImg.cols());
				            ImageBox.imshow("RANSAC", imgMatches);
				            waitKey();
				            destroyAllWindows();
				        }
				        break;

				    case "index":
				        ElasticImgIndexing eii = 
			                            new ElasticImgIndexing(Parameters.TOP_K_IDX, indexName);
				        eii.indexAll(args[1]);
				        eii.close();
				        break;

				    default:
				        printHelp();
				        break;
				}
				System.out.println("Program ended");
				System.exit(0);
			} 
			catch (Exception e) {
				System.err.println("Program generated an exception: " + e.getClass().getName() 
						+ ":\n" + e.getMessage() + "\nExiting");
				System.exit(1);
	}
		}
	}
	
	private static void printHelp() {
		System.out.println("Available commands:");
		System.out.println("search path/to/image [-i index_name]");
		System.out.println("\t\tAsk the program to retrieve");
		System.out.println("\t\tthe most similar image present");
		System.out.println("\t\tinto the index called index_name");
		System.out.println("\t\tinstead of default one (" + Parameters.INDEX_NAME +")");
		System.out.println();
		System.out.println("index dir [-i index_name]");
		System.out.println("\t\tStart indexing each image into dir");
		System.out.println("\t\tOptionally you can tell the program");
		System.out.println("\t\tto index objects using index_name");
		System.out.println("\t\tinstead of default one (" + Parameters.INDEX_NAME +").");
		System.out.println("\t\tdir must contain subdirectories that");
		System.out.println("\t\tcontains images to index");
		System.out.println();
		System.out.println("statistics -tp TPdir -tn TNdir [-i index_name]");
		System.out.println("\t\tStart gathering statistics using");
		System.out.println("\t\timages in TPdir to compute true");
		System.out.println("\t\tpositives and images in TNdir for");
		System.out.println("\t\tcomputing true negative. Optionally");
		System.out.println("\t\tthe user can use index_name index");
		System.out.println("\t\tinstead of default one (" + Parameters.INDEX_NAME +")");
	}

	public static void test() {
		System.out.println("INDEXING FROM GUI");
	}
}
