package it.unipi.ing.mim.main;

import java.io.File;
import java.nio.file.FileSystems;
import java.nio.file.Path;

public class Parameters {
    
    //Image Source Folder
    public static final File SRC_FOLDER = new File("./");
    
    //k-Nearest Neighbors
    public static final int KNN = 50;

    // Number of random keypoint chosen from keypoint computed by SIFT
    public static final int RANDOM_KEYPOINT_NUM = 200;

    //Top K pivots For Indexing
    public static final int TOP_K_IDX = RANDOM_KEYPOINT_NUM;

    //Top K pivots For Searching
    public static final int TOP_K_QUERY = TOP_K_IDX;
    
    //Lucene Index
    public static final String INDEX_NAME = "painting2";
    
    //HTML Output Parameters
    public static final String BASE_URI = Parameters.SRC_FOLDER.toURI().toString();
    public static final File RESULTS_HTML = new File("out/deep.seq.html");
    
    //Images to be indexed
    public static final Path imgDir = FileSystems.getDefault().getPath("wikiart");
    
    public static final Integer NUM_KMEANS_CLUSTER = 10000; // TODO Ricorda di cambiarlo
    
    // ORB feature extraction parameters
    public static final int ORB_MAX_FEATURE = 1000;
    
    // Megapixel per image
    public static final int MPX_PER_IMAGE = 1000000;
    
    // Files to store data
    public static final File KEYPOINT_PER_IMAGE_FILE = new File(RANDOM_KEYPOINT_NUM + "keyPerImage.dat");
    public static final File DESCRIPTOR_FILE = new File("descriptors" + RANDOM_KEYPOINT_NUM + ".dat");
    public static final File IMAGE_NAMES_FILE =  new File("imageNames.dat");

    //Cluster File
    public static final File CLUSTER_FILE = new File(RANDOM_KEYPOINT_NUM + "x" + NUM_KMEANS_CLUSTER + "pivot.dat");
    
    public static final File LABEL_FILE = new File("labels" + RANDOM_KEYPOINT_NUM + "x" + NUM_KMEANS_CLUSTER + ".dat");
    public static final File POSTING_LISTS_FILE =  new File("posting_lists" + RANDOM_KEYPOINT_NUM + "x" + NUM_KMEANS_CLUSTER + ".dat");
    
}