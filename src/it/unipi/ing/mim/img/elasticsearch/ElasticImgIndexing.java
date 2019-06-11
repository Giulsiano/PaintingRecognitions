package it.unipi.ing.mim.img.elasticsearch;

import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.AbstractMap.SimpleEntry;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.http.HttpHost;
import org.bytedeco.opencv.opencv_core.Mat;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.support.master.AcknowledgedResponse;
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
	
	private RestHighLevelClient client;
	private KmeansResults kmeansResults;

	public ElasticImgIndexing(int topKIdx) throws IOException, ClassNotFoundException {
		//Initialize pivots, imgDescDataset, REST
		this.topKIdx = topKIdx;
		RestClientBuilder builder = RestClient.builder(new HttpHost(HOST, PORT, PROTOCOL));
	    client = new RestHighLevelClient(builder);
	}
	
	@SuppressWarnings("unchecked")
	public void indexAll(String[] args) throws Exception {
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
			// Compute centroids and store them to the disk
			centroidList = computeClusterCentres(descFile);
			labels = kmeansResults.getLabels();
			StreamManagement.store(centroidList, pivotFile);
    		StreamManagement.store(MatConverter.mat2int(labels), labelFile);
		}
		// Load labels from disk
		if (!centroidList.isEmpty()) {
			System.out.println("Loaded centroids");
			int[][] rawLabels = (int[][]) StreamManagement.load(labelFile, int[][].class);
			labels = MatConverter.int2Mat(rawLabels);
			System.out.println("Loaded labels");
		}
		else {
			System.err.println("No centroids have been found. Exiting.");
			System.exit(1);
		}
		
		// Create posting lists by counting frequencies of cluster per image
		Map<String, SimpleEntry<Integer, Integer>[]> postingLists = 
				BOF.getPostingLists(labels, centroidList.size(), indexing.getKeypointPerImage(), 
									indexing.getImageNames());
		
		// Save posting lists to file
		StreamManagement.store(postingLists, Parameters.POSTING_LISTS_FILE);

		// Put images to the index
		this.createIndex();
		this.index();
		this.close();
	}
	
	private List<Centroid> computeClusterCentres (File descFile) throws Exception{
		System.out.println("Computing clusters for the dataset");
		List<Centroid> centroidList = new LinkedList<Centroid>();
		Mat kmeansData = createKmeansData(descFile);
		kmeansResults = new KmeansResults(kmeansData);
		kmeansResults.computeKmeans();
		Mat centroids = kmeansResults.getCentroids();
		
		// Store it for quickly access them on a second run of this program
		System.out.println("Storing centroids to disk");
		int rows = centroids.rows();
		for (int i = 0; i < rows; i ++) {
			Centroid c = new Centroid(centroids.row(i), i);
			centroidList.add(c);
		}
		return centroidList;
	}
	
	private Mat createKmeansData (File descriptorFile) {
		// Get features randomly from each image
		Mat bigmat = new Mat();
		try {
			ObjectInputStream ois = new ObjectInputStream(new FileInputStream(descriptorFile));
			while (true){
				try {
					// Read the matrix of features
					float[][] feat = ((ImgDescriptor) ois.readObject()).getFeatures();
					Mat featMat = MatConverter.float2Mat(feat);
					int featRows = featMat.rows();
					
					// Get unique random numbers from RNG
					Set<Integer> randomRows = new HashSet<Integer>(Parameters.RANDOM_KEYPOINT_NUM);
					int times = Math.min(featRows, Parameters.RANDOM_KEYPOINT_NUM);
					for (int i = 0; i < times; ++i) {
						int randValue = (int) (Math.random() * featRows);
						if (!randomRows.add(randValue)) --i;
					}
					// Make the matrix of whole features by taking random rows from the feature matrix
					randomRows.forEach((randRow) -> { bigmat.push_back(featMat.row(randRow)); } );
				}
				catch (EOFException e) { 
					break;
				}
			}
			ois.close();
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		
		return bigmat;
	}
	
	public void close() throws IOException {
		//close REST client
		client.close();
	}
	
	public void createIndex() throws IOException {
		try {
			GetIndexRequest requestdel = new GetIndexRequest(Parameters.INDEX_NAME);
			// If the index already exists
			if(client.indices().exists(requestdel, RequestOptions.DEFAULT)) {
				System.out.println("Delete index");
				DeleteIndexRequest deleteInd = new DeleteIndexRequest(Parameters.INDEX_NAME);
				AcknowledgedResponse deleteIndexResponse = client.indices().delete(deleteInd, RequestOptions.DEFAULT);
			}
			
			System.out.println("Create index");
			//Create the Elasticsearch index
			IndicesClient idx = client.indices();
			CreateIndexRequest request = new CreateIndexRequest(Parameters.INDEX_NAME);
			Builder s = Settings.builder()
								.put("index.number_of_shards", 1)
					            .put("index.number_of_replicas", 0)
					            .put("analysis.analyzer.first.type", "whitespace");
			request.settings(s);
			idx.create(request, RequestOptions.DEFAULT);
		}catch(Exception e) {
			System.out.println("Index \'" + Parameters.INDEX_NAME +"\' already exists");
		}
	}
	
	@SuppressWarnings("unchecked")
	public void index() throws FileNotFoundException, ClassNotFoundException, IOException {
		Map<String, SimpleEntry<Integer, Integer>[]> postingLists = 
				(Map<String, SimpleEntry<Integer, Integer>[]>) StreamManagement.load(Parameters.POSTING_LISTS_FILE, Map.class);
		postingLists.forEach((imgId, postingList) -> {
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
