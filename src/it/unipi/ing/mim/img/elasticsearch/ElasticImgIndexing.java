package it.unipi.ing.mim.img.elasticsearch;

import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.ConnectException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.AbstractMap.SimpleEntry;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.http.HttpHost;
import org.bytedeco.opencv.opencv_core.Mat;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.client.IndicesClient;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.client.indices.CreateIndexRequest;
import org.elasticsearch.client.indices.GetIndexRequest;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.settings.Settings.Builder;

import it.unipi.ing.mim.deep.ImgDescriptor;
import it.unipi.ing.mim.deep.seq.SeqImageStorage;
import it.unipi.ing.mim.deep.tools.StreamManagement;
import it.unipi.ing.mim.main.Centroid;
import it.unipi.ing.mim.main.Parameters;
import it.unipi.ing.mim.utils.BOF;
import it.unipi.ing.mim.utils.KmeansResults;
import it.unipi.ing.mim.utils.MatConverter;

public class ElasticImgIndexing implements AutoCloseable {
	
	private static String HOST = "localhost";
	private static int PORT = 9200;
	private static String PROTOCOL = "http";
	
	private int topKIdx;
	private String ESIndexName;
	
	private RestHighLevelClient client;
	private KmeansResults kmeansResults;

	public ElasticImgIndexing(int topKIdx) throws IOException, ClassNotFoundException {
		this(topKIdx, Parameters.INDEX_NAME);
	}
	
	public ElasticImgIndexing(int topKIdx, String indexName) throws IOException, ClassNotFoundException {
        this.topKIdx = topKIdx;
        this.ESIndexName = indexName;
        RestClientBuilder builder = RestClient.builder(new HttpHost(HOST, PORT, PROTOCOL));
        client = new RestHighLevelClient(builder);
    }
	
	@SuppressWarnings("unchecked")

	public void indexAll (String imgDir) throws Exception {
	    Path imgRootDir = FileSystems.getDefault().getPath(imgDir);
		SeqImageStorage indexing = new SeqImageStorage();
		System.out.println("Scanning image directory");
		File descFile = Parameters.DESCRIPTOR_FILE;
		if (!descFile.exists()) {

			indexing.extractFeatures(imgRootDir);
		}
		// Compute centroids of the database
		Mat labels = null;
		File clusterFile =  Parameters.CLUSTER_FILE;
		File labelFile = Parameters.LABEL_FILE;
		List<Centroid> centroidList = null;
		try {
			// Loading them from file for saving time and memory
		    System.out.println("Loading centroids");
			centroidList = (List<Centroid>) StreamManagement.load(clusterFile, List.class);
		}
		catch (FileNotFoundException e) {
			// Compute centroids and store them to the disk
			centroidList = computeClusterCentres(descFile);
			labels = kmeansResults.getLabels();
			System.out.println("Storing centroids to disk");
			StreamManagement.store(centroidList, clusterFile, List.class);
    		StreamManagement.store(MatConverter.mat2int(labels), labelFile, int[][].class);
		}
		// Load labels from disk
		if (!centroidList.isEmpty()) {
		    System.out.println("Loading labels");
			int[][] rawLabels = (int[][]) StreamManagement.load(labelFile, int[][].class);
			labels = MatConverter.int2Mat(rawLabels);
		}
		else {
			System.err.println("No centroids have been found. Exiting.");
			System.exit(1);
		}
		// Create posting lists by counting frequencies of cluster per image and store it to disk
		File postingListFile = Parameters.POSTING_LISTS_FILE;
		if (!postingListFile.exists()) {
			System.out.println("Computing and storing posting lists");
			BOF.getPostingLists(labels, centroidList.size(), indexing.getKeypointPerImage(), 
					indexing.getImageNames());
		}
		else {
		    // Put images to the index
		    System.out.println("Start indexing");
		    this.createIndex();
		    this.index();
		    this.close();
		    System.out.println("End indexing");
		}
	}
	
	private List<Centroid> computeClusterCentres (File descFile) throws Exception{
		System.out.println("Computing clusters for the dataset");
		List<Centroid> centroidList = new LinkedList<Centroid>();
		Mat kmeansData = createKmeansData(descFile);
		kmeansResults = new KmeansResults(kmeansData);
		kmeansResults.computeKmeans();
		Mat centroids = kmeansResults.getCentroids();
		
		// Store it for quickly access them on a second run of this program
		int rows = centroids.rows();
		for (int i = 0; i < rows; i ++) {
			Centroid c = new Centroid(centroids.row(i), i);
			centroidList.add(c);
		}
		return centroidList;
	}
	
	/*
	 * create the features Mat for kmeans
	 */
	private Mat createKmeansData (File descriptorFile) throws ClassNotFoundException {
		Mat bigmat = new Mat();
		try {
			ObjectInputStream ois = new ObjectInputStream(new FileInputStream(descriptorFile));
			while (true){
				try {
					// Read the matrix of features 
					float[][] feat = ((ImgDescriptor) ois.readObject()).getFeatures();
					bigmat.push_back(MatConverter.float2Mat(feat));
				}
				catch (EOFException e) { 
					break;
				}
			}
			ois.close();
		}
		catch (IOException | ClassNotFoundException e) {
			e.printStackTrace();
		}
		return bigmat;
	}
	
	@SuppressWarnings("unchecked")
	private void index () throws ClassNotFoundException {
		try {
			ObjectInputStream ois = new ObjectInputStream(new FileInputStream(Parameters.POSTING_LISTS_FILE));
			while (true){
				try {
					SimpleEntry<String, SimpleEntry<Integer, Integer>[]> postingList = 
							(SimpleEntry<String, SimpleEntry<Integer, Integer>[]>) ois.readObject();
					String imgId = postingList.getKey();
					SimpleEntry<Integer, Integer>[] clusterFrequencies = postingList.getValue();
					System.out.println("Elaboration of indexing request for " + imgId);
					String bof = BOF.features2Text(clusterFrequencies, topKIdx);
					IndexRequest request = composeRequest(imgId, bof);
					client.index(request, RequestOptions.DEFAULT);
				}
				catch (EOFException e) { 
					break;
				}
				catch (ConnectException e) {
					System.err.println("ElasticSearch server not running!");
					System.exit(1);
				}
				catch (IOException e) {
					System.err.println(e.getMessage());
					e.printStackTrace();
				}
			}
			ois.close();
		}
		catch (IOException | ClassNotFoundException e) {
			e.printStackTrace();
		}
	}
	
	public void close() throws IOException {
		//close REST client
		client.close();
	}
	
	/**
	 * check if the index already exist
	 */
	public boolean isESIndexExist (String idxName) throws IOException {
		GetIndexRequest requestdel = new GetIndexRequest(ESIndexName);
		return client.indices().exists(requestdel, RequestOptions.DEFAULT);
	}
	
	public void createIndex() throws IOException, ConnectException {
		try {
			// If the index already exists delete it, then rebuild it
			if(isESIndexExist(ESIndexName)) {
				System.out.println("Delete index " + ESIndexName);
				DeleteIndexRequest deleteInd = new DeleteIndexRequest(ESIndexName);
				client.indices().delete(deleteInd, RequestOptions.DEFAULT);
			}
			
			System.out.println("Create index " + ESIndexName);
			//Create the Elasticsearch index
			IndicesClient idx = client.indices();
			CreateIndexRequest request = new CreateIndexRequest(ESIndexName);
			Builder s = Settings.builder()
								.put("index.number_of_shards", 1)
					            .put("index.number_of_replicas", 0)
					            .put("analysis.analyzer.first.type", "whitespace");
			request.settings(s);
			idx.create(request, RequestOptions.DEFAULT);
		}
		catch (ConnectException e) {
			System.err.println("ElasticSearch server not running!");
			e.printStackTrace();
			System.exit(1);
		}
	}
	
	private IndexRequest composeRequest(String id, String imgTxt) {			
		//Initialize and fill IndexRequest Object with Fields.ID and Fields.IMG 
		Map<String, String> jsonMap = new HashMap<>();
		jsonMap.put(Fields.ID,id);
		jsonMap.put(Fields.IMG, imgTxt);
		
		IndexRequest request = null;
		request = new IndexRequest(ESIndexName, "doc");
		request.source(jsonMap);
		return request;
	}
}
