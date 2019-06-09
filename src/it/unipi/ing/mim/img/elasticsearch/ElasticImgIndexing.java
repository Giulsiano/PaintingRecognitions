package it.unipi.ing.mim.img.elasticsearch;

import static org.bytedeco.opencv.global.opencv_core.CV_32F;
import static org.bytedeco.opencv.global.opencv_core.kmeans;

import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.AbstractMap.SimpleEntry;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.http.HttpHost;
import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.opencv.opencv_core.TermCriteria;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.client.IndicesClient;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.client.indices.CreateIndexRequest;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.settings.Settings.Builder;
import org.opencv.core.Core;

import it.unipi.ing.mim.deep.ImgDescriptor;
import it.unipi.ing.mim.deep.Parameters;
import it.unipi.ing.mim.deep.seq.SeqImageStorage;
import it.unipi.ing.mim.deep.tools.StreamManagement;
import it.unipi.ing.mim.main.Centroid;
import it.unipi.ing.mim.main.Main;
import it.unipi.ing.mim.utils.BOF;
import it.unipi.ing.mim.utils.MatConverter;

public class ElasticImgIndexing implements AutoCloseable {
	
	private static String HOST = "localhost";
	private static int PORT = 9200;
	private static String PROTOCOL = "http";
	
	private Map<String, SimpleEntry<Integer, Integer>[]> postingListDataset;
	private int topKIdx;
	
	private RestHighLevelClient client;

	//TODO
	public ElasticImgIndexing(File postingListFile, int topKIdx) throws IOException, ClassNotFoundException {
		//Initialize pivots, imgDescDataset, REST
		this.postingListDataset = (Map<String, SimpleEntry<Integer, Integer>[]>) StreamManagement.load(postingListFile, Map.class);
		this.topKIdx = topKIdx;
		RestClientBuilder builder = RestClient.builder(new HttpHost(HOST, PORT, PROTOCOL));
	    client = new RestHighLevelClient(builder);
	}
	
	public static void indexAll(String[] args) throws Exception {
		SeqImageStorage indexing = new SeqImageStorage();
		System.out.println("Scanning image directory");
		File descFile = Parameters.DESCRIPTOR_FILE;
		if (!descFile.exists()) {
			indexing.extractFeatures(Parameters.imgDir);
		}
		// Compute centroids of the database
		Mat labels = null;
		File pivotFile =  Parameters.PIVOTS_FILE;
		File labelFile = Parameters.LABEL_FILE;
		List<Centroid> centroidList = null;
		try {
			// Loading them from file for saving time and memory
			centroidList = (List<Centroid>) StreamManagement.load(pivotFile, List.class);
		}
		catch (FileNotFoundException e) {
			// Compute centroids with kmeans
			centroidList = new LinkedList<Centroid>();
			System.out.println("Running kmeans by using OpenCV");
			Mat[] kmeansResults = computeKMeans(descFile);
			Mat centroids = kmeansResults[0];
			labels = kmeansResults[1];
			
			// Store it for quickly access them on a second run of this program
			System.out.println("Storing centroids to disk");
			int rows = centroids.rows();
			for (int i = 0; i < rows; i ++) {
				Centroid c = new Centroid(centroids.row(i), i);
				centroidList.add(c);
			}
			StreamManagement.store(centroidList, pivotFile);
    		StreamManagement.store(MatConverter.mat2int(labels), labelFile);
		}
		// Load labels with centroids also
		if (centroidList.isEmpty()) {
			System.err.println("No centroids have been found. Exiting.");
			System.exit(1);
		}
		else {
			System.out.println("Loaded centroids");
			int[][] rawLabels = (int[][]) StreamManagement.load(labelFile, int[][].class);
			labels = MatConverter.int2Mat(rawLabels);
			System.out.println("Loaded labels");
		}
		
		// Read all image names from disk
		List<String> imgIds = getImagesName(descFile);
		
		// Create posting lists by counting frequencies of cluster per image
		Map<String, SimpleEntry<Integer, Integer>[]> postingLists = 
				BOF.getPostingLists(labels, centroidList.size(), imgIds);
		
		// Save posting lists to file
		StreamManagement.store(postingLists, Parameters.POSTING_LISTS_FILE);

		// Make ElasticSearch to index images
		try(ElasticImgIndexing esIndex = new ElasticImgIndexing(Parameters.POSTING_LISTS_FILE, 
																Parameters.TOP_K_IDX)){
			esIndex.createIndex();
			esIndex.index();
		}
	}
	
	private static List<String> getImagesName (File file) throws FileNotFoundException, IOException, ClassNotFoundException{
		List<String> imgIds = new LinkedList<String>();
		try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file))){
			while (true) {
				try  {
					ImgDescriptor c = (ImgDescriptor) ois.readObject();
					imgIds.add(c.getId());
				}
				catch (EOFException e) {
					break;
				}
			}
		}
		return imgIds;
	}
	
	private static Mat[] computeKMeans (File descriptorFile) throws Exception {
		// Get features randomly from each image
		Mat bigmat = new Mat();
		try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(descriptorFile))){
			
			while (true){
				try {
					float[][] feat = ((ImgDescriptor) ois.readObject()).getFeatures();
					
					Mat featMat = MatConverter.float2Mat(feat);
					int featRows = featMat.rows();
					for (int i = 0; i < Parameters.RANDOM_KEYPOINT_NUM; ++i) {
						bigmat.push_back(featMat.row((int)(Math.random() * featRows)));
					}
				}
				catch (EOFException e) { 
					break;
				}
			}
		}
		
		// Compute kmeans' centroids
		Mat labels = new Mat();
		Mat centroids = new Mat();
		TermCriteria criteria = new TermCriteria(CV_32F, 100, 1.0d);
		kmeans(bigmat, Parameters.NUM_KMEANS_CLUSTER, labels, criteria, 1, Core.KMEANS_PP_CENTERS, centroids);
		
		// Put results of kmea-s into an array and return it
		Mat[] results = new Mat[2];
		results[0] = centroids;
		results[1] = labels;
		return results;
	}
	
	public void close() throws IOException {
		//close REST client
		client.close();
	}
	
	//TODO
	public void createIndex() throws IOException {
		//Create the Elasticsearch index
		IndicesClient idx = client.indices();
		CreateIndexRequest request = new CreateIndexRequest(Parameters.INDEX_NAME);
		Builder s = Settings.builder()
							.put("index.number_of_shards", 1)
				            .put("index.number_of_replicas", 0)
				            .put("analysis.analyzer.first.type", "whitespace");
		request.settings(s);
		idx.create(request, RequestOptions.DEFAULT);
	}
	
	//TODO
	public void index() {
		postingListDataset.forEach((imgId, postingList) -> {
				String temp = BOF.features2Text(postingList, topKIdx);
				IndexRequest request = composeRequest(imgId, temp);
				try {
					client.index(request, RequestOptions.DEFAULT);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		);
	}
	
	private IndexRequest composeRequest(String id, String imgTxt) {			
		//Initialize and fill IndexRequest Object with Fields.ID and Fields.IMG txt
		Map<String, String> jsonMap = new HashMap<>();
		jsonMap.put(Fields.ID,id);
		jsonMap.put(Fields.IMG, imgTxt);
		
		IndexRequest request = null;
		request = new IndexRequest(Parameters.INDEX_NAME, "doc");
		request.source(jsonMap);
		return request;
	}
}
