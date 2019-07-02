package it.unipi.ing.mim.img.elasticsearch;

import static org.bytedeco.opencv.global.opencv_imgcodecs.imread;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.http.HttpHost;
import org.apache.lucene.queryparser.classic.ParseException;
import org.bytedeco.opencv.opencv_core.DMatchVector;
import org.bytedeco.opencv.opencv_core.KeyPointVector;
import org.bytedeco.opencv.opencv_core.Mat;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.builder.SearchSourceBuilder;

import com.github.cliftonlabs.json_simple.JsonException;

import it.unipi.ing.mim.deep.ImgDescriptor;
import it.unipi.ing.mim.deep.tools.StreamManagement;
import it.unipi.ing.mim.features.FeaturesExtraction;
import it.unipi.ing.mim.features.FeaturesMatching;
import it.unipi.ing.mim.features.FeaturesMatchingFiltered;
import it.unipi.ing.mim.features.Ransac;
import it.unipi.ing.mim.main.Centroid;
import it.unipi.ing.mim.main.Parameters;
import it.unipi.ing.mim.main.RansacParameters;
import it.unipi.ing.mim.utils.BOF;
import it.unipi.ing.mim.utils.MatConverter;
import it.unipi.ing.mim.utils.ResizeImage;

public class ElasticImgSearching implements AutoCloseable {

	private static String HOST = "localhost";
	private static int PORT = 9200;
	private static String PROTOCOL = "http";
	private RestHighLevelClient client;
	private int topKqry;
	private List<Centroid> centroidList = null;
	private String ESIndexName;
	
	private RansacParameters ransacParameters;
	private Map<String, Object> bestGoodMatch;
	
	public ElasticImgSearching (int topKSearch) throws ClassNotFoundException, IOException {
		this(new RansacParameters(), topKSearch, Parameters.INDEX_NAME);
	}
	
	public ElasticImgSearching (int topKSearch, String indexName) throws ClassNotFoundException, IOException {
        this(new RansacParameters(), topKSearch, indexName);
    }
	
	public ElasticImgSearching (RansacParameters parameters, int topKSearch, String indexName) {
		ransacParameters = parameters;
		RestClientBuilder builder = RestClient.builder(new HttpHost(HOST, PORT, PROTOCOL));
	    client = new RestHighLevelClient(builder);
	    this.topKqry = topKSearch;
	    this.ESIndexName = indexName;
	    this.bestGoodMatch = new HashMap<>();
	    this.bestGoodMatch.put("queryImg", null);
	    this.bestGoodMatch.put("queryKeypoints", null);
	    this.bestGoodMatch.put("image", null);
	    this.bestGoodMatch.put("imageKeypoints", null);
	    this.bestGoodMatch.put("matchVector", null);
	}
	
	/**
	 * 
	 * @param qryImage 
	 * @param test true to compute statistics
	 * @return
	 * @throws Exception
	 */
	public String search (String qryImageName) 
	        throws ClassNotFoundException, ParseException, IOException, JsonException {
		if (!qryImageName.toLowerCase().endsWith("jpg")) 
		    throw new IllegalArgumentException("Image " + qryImageName + " is not a .jpg file format");
		
		// Read the image to be searched and extract its feature
		Mat qryImg = imread(qryImageName);
		Mat resizedQryImg = ResizeImage.resizeImage(qryImg);
		System.out.println("Computing query features using SIFT");
		FeaturesExtraction extractor = new FeaturesExtraction(FeaturesExtraction.SIFT_FEATURES);
		Mat queryDesc = extractor.extractDescriptor(qryImg);
		if (queryDesc.empty()) {
			System.err.println("Can't extract features from " + qryImageName);
			return null;
		}
		float[][] queryFeatures = getRandomFeatures(queryDesc);
		ImgDescriptor query = new ImgDescriptor(queryFeatures, qryImageName);
		
		// Make the search by computing the bag of feature of the query
		System.out.println("Creating query feature-to-text");
		String bofQuery = BOF.features2Text(computeClusterFrequencies(query), topKqry);
		System.out.println("Ask ElasticSearch to return "  +  Parameters.KNN + " neighbours");
		List<String> neighbours = search(bofQuery, Parameters.KNN);
		
		// Compute the best good match if any
		System.out.println("Computing best good match among neighbours");
		String bestGoodMatchName= computeBestGoodMatch(neighbours, resizedQryImg);
		if (bestGoodMatchName==null) System.err.println("No good matches found for " + qryImageName);
		else {
		    System.out.println("Match found: " + bestGoodMatchName);
		    
		}
		return bestGoodMatchName;
	}
	
	public void close () throws IOException {
		//close REST client
		client.close();
	}
	
	public float[][] getRandomFeatures (Mat features){
		long descriptorRows = features.rows();
		float[][] randomFeatures = null;
		if (descriptorRows > 0) {
			if (descriptorRows <= Parameters.RANDOM_KEYPOINT_NUM) {
				randomFeatures = MatConverter.mat2float(features);
			}
			else {
				// Get unique random numbers from RNG
				Set<Integer> randomRows = new HashSet<Integer>(Parameters.RANDOM_KEYPOINT_NUM);
				long times = Parameters.RANDOM_KEYPOINT_NUM;
				for (long i = 0; i < times; ++i) {
					int randValue = (int) (Math.random() * descriptorRows);
					if (!randomRows.add(randValue)) --i;
				}
				// Make the matrix of whole features by taking random rows from the feature matrix
				Mat featMat = new Mat();
				randomRows.forEach((randRow) -> featMat.push_back(features.row(randRow)));
				randomFeatures = MatConverter.mat2float(featMat);				
			}
		}
		return randomFeatures;
	}
	
	/**
	 * search for the k-nearest neighbors to the query image
	 * @param queryString
	 * @param k 
	 * @return
	 * @throws ParseException
	 * @throws IOException
	 * @throws ClassNotFoundException
	 */
	public List<String> search (String queryString, int k) throws ParseException, IOException, ClassNotFoundException{
		List<String> res = new LinkedList<String>();

		//call composeSearch to get SearchRequest object
		SearchRequest searchReq= composeSearch(queryString, k);
		
		SearchResponse searchResponse = client.search(searchReq, RequestOptions.DEFAULT);//options.build());
		client.close();
		SearchHit[] hits = searchResponse.getHits().getHits();
		res = new ArrayList<>(hits.length);	
		for (int i = 0; i < hits.length; i++) {
			Map<String, Object> metadata = hits[i].getSourceAsMap();
			String id =  (String) metadata.get(Fields.ID);
			res.add(id);
		}
		return res;
	}
	
	private SearchRequest composeSearch (String query, int k) {
		QueryBuilder queryBuild = QueryBuilders.multiMatchQuery(query, Fields.IMG);
		SearchSourceBuilder sb = new SearchSourceBuilder();
		sb.size(k);
		sb.query(queryBuild);
		
		// Build the request
		SearchRequest searchRequest = new SearchRequest(this.ESIndexName);
		searchRequest.types("doc");
		searchRequest.source(sb);
		return searchRequest;
	}
	
	/**
	 * compute bag of features for the query image
	 * @param query
	 * @return
	 * @throws FileNotFoundException
	 * @throws ClassNotFoundException
	 * @throws IOException
	 */
	@SuppressWarnings("unchecked")
	public SimpleEntry<Integer, Integer>[] computeClusterFrequencies (ImgDescriptor query)
	        throws FileNotFoundException, ClassNotFoundException, IOException {
		// Read centroids, compute distances of query to each of them
		if (this.centroidList == null || this.centroidList.isEmpty()) {
			this.centroidList =  (List<Centroid>) StreamManagement.load(Parameters.CLUSTER_FILE, List.class);
		}
		Float[][] distancesFromCentroids = query.distancesTo(centroidList);
		int[] qryLabel = new int[distancesFromCentroids.length];
		
		// Create the "label" of the cluster, that is an array of which cluster the keypoint
		// considered belonging to
		Arrays.fill(qryLabel, 0);
		for (int i = 0; i < distancesFromCentroids.length; ++i) {
			float minValue = Float.MAX_VALUE;
			for (int j = 0; j < distancesFromCentroids[i].length; ++j) {
				if (minValue > distancesFromCentroids[i][j]) {
					minValue = distancesFromCentroids[i][j];
					qryLabel[i] = j;	// Save the cluster index
				}
			}
		}
		// Compute frequencies of clusters 
		int[] frequencies = new int[centroidList.size()];
		Arrays.fill(frequencies, 0);
		for (int i = 0; i < qryLabel.length; ++i) {
			++frequencies[qryLabel[i]];
		}
		// Create the posting list
		int numClusters = centroidList.size();
		SimpleEntry<Integer, Integer>[] clusterFrequencies = 
				(SimpleEntry<Integer, Integer>[]) new SimpleEntry[numClusters];
		for (int i = 0; i < frequencies.length; ++i) {
			clusterFrequencies[i] = new SimpleEntry<Integer, Integer>(i, frequencies[i]);
		}
		Arrays.sort(clusterFrequencies, Comparator.comparing(SimpleEntry::getValue, 
															 Comparator.reverseOrder()));
		return clusterFrequencies;
	}
	
	/**
	 * compute the one nearest neighbor to the query using RANSAC
	 * @param neighbours
	 * @param queryImg
	 * @param qryImage
	 * @param test
	 * @return
	 * @throws FileNotFoundException
	 * @throws IOException
	 * @throws JsonException
	 */
	public String computeBestGoodMatch(List<String> neighbours, Mat queryImg) 
	        throws FileNotFoundException, IOException, JsonException {
		
		FeaturesMatching matcher = new FeaturesMatching();
		FeaturesMatchingFiltered filter = new FeaturesMatchingFiltered();
		List<SimpleEntry<String, DMatchVector>> goodMatches = new LinkedList<>();
		
		// Compute ORB features for query and save its keypoints to avoid loosing them
		FeaturesExtraction extractor = new FeaturesExtraction(FeaturesExtraction.ORB_FEATURES);
		Mat queryDesc = extractor.extractDescriptor(queryImg);
		KeyPointVector qryKeypoints = extractor.getKeyPointVector();
		
		// Compute ORB features for each returned neighbour and try to match them against query
		for (String neighbourName : neighbours) {
			Mat neighbourDesc = extractor.extractDescriptor(imread(neighbourName));
			if (neighbourDesc.empty()) {
				System.err.println("Can't compute ORB features for " + neighbourName);
				continue;
			}
			DMatchVector matches = matcher.match(queryDesc, neighbourDesc);
			DMatchVector filteredMatches = filter.filterMatches(matches, ransacParameters.getDistanceThreshold());
			if (!filteredMatches.empty())
			    goodMatches.add(new SimpleEntry<String, DMatchVector>(neighbourName, filteredMatches));
 		}
		// Get the image with the best number of matches using RANSAC (RANdom SAmple Consensus)
		long minInliers = (ransacParameters.getMinRansacInliers()-1);
		Ransac ransac = new Ransac(ransacParameters);
		Mat bestImg=null;
		KeyPointVector bestKeypoints=null;
		SimpleEntry<String, DMatchVector> bestGoodMatch = null;
		for (SimpleEntry<String, DMatchVector> goodMatch : goodMatches) {
			DMatchVector matches = goodMatch.getValue();
			if (matches.size() > 0) {
				Mat img = ResizeImage.resizeImage(imread(goodMatch.getKey()));
				KeyPointVector keypoints = extractor.getDetector().detectKeypoints(img);
				ransac.computeHomography(goodMatch.getValue(), qryKeypoints, keypoints);
				int inliers = ransac.countNumInliers();
				if (inliers > minInliers) {
					minInliers = inliers;
					bestGoodMatch = goodMatch;
					bestImg= img;
					bestKeypoints= keypoints;
				}
			}
		}
		// Put results into the map to retrieve information about best match
		if (bestGoodMatch != null) {
		    this.bestGoodMatch.put("queryImg", queryImg);
	        this.bestGoodMatch.put("queryKeypoints", qryKeypoints);
	        this.bestGoodMatch.put("image", bestImg);
	        this.bestGoodMatch.put("imageKeypoints", bestKeypoints);
	        this.bestGoodMatch.put("matchVector", bestGoodMatch.getValue());
	        return bestGoodMatch.getKey();
		}
	    return null;
	}

	public RansacParameters getRansacParameters() {
		return ransacParameters;
	}

	public void setRansacParameters(RansacParameters ransacParameters) {
		this.ransacParameters = ransacParameters;
	}
	
	public Map<String, Object> getBestGoodMatch (){
	    return bestGoodMatch;
	}
}