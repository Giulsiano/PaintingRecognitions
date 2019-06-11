package it.unipi.ing.mim.deep;

import java.io.File;
import java.nio.file.FileSystems;
import java.nio.file.Path;

public class Parameters {
	
	//DEEP parameters
	public static final double[] MEAN_VALUES = {104, 117, 123, 0};
	
	public static final String DEEP_LAYER = "fc7";
	public static final int IMG_WIDTH = 227;
	public static final int IMG_HEIGHT = 227;
	
	//Image Source Folder
	public static final File SRC_FOLDER = new File("data/img");
	
	//k-Nearest Neighbors
	public static final int K = 10;
	
	//Pivots File
	public static final File PIVOTS_FILE = new File("pivot.dat");
	
	//Number Of Pivots
	//public static final int NUM_PIVOTS = 100;

	//Top K pivots For Indexing
	public static final int TOP_K_IDX = 10;
	
	//Top K pivots For Searching
	public static final int TOP_K_QUERY = 30;
	
	//Lucene Index
	public static final String INDEX_NAME = "painting";
	
	//HTML Output Parameters
	public static final  String BASE_URI = "file:///" + Parameters.SRC_FOLDER.getAbsolutePath() + "/";
	public static final File RESULTS_HTML = new File("out/deep.seq.html");
	public static final File RESULTS_HTML_ELASTIC = new File("out/deep.elastic.html");
	public static final File RESULTS_HTML_REORDERED = new File("out/deep.reordered.html");
	
	// Our parameters
	// Files we need to do things
	public static final File DB_PATH = new File("data/database.db");
	public static final File CLUSTER_FILE = new File("clusters.dat");
	public static final File LABEL_FILE = new File("labels.dat");
	public static final File DESCRIPTOR_FILE = new File("descriptors.dat");
	public static final File POSTING_LISTS_FILE =  new File("posting_lists.dat");
	
	// Where are images to be indexed
	public static final Path imgDir = FileSystems.getDefault().getPath("/Users/Maria/git/PaintingRecognitions/wikiart");
	
	// Algorithm parameters
	public static final int MAX_DISTANCE_THRESHOLD = 35;
	public static final Integer NUM_KMEANS_CLUSTER = 10;	// TODO Ricorda di cambiarlo

	// Number of random keypoint chosen from keypoint computed by SIFT
	public static final int RANDOM_KEYPOINT_NUM = 100;
	
	// ORB feature extraction parameters
	public static final int ORB_MAX_FEATURE = 1000;
}
