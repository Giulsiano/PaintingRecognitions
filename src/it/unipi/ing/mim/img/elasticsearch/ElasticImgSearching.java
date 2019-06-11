package it.unipi.ing.mim.img.elasticsearch;

import static org.bytedeco.opencv.global.opencv_features2d.drawKeypoints;
import static org.bytedeco.opencv.global.opencv_features2d.drawMatches;
import static org.bytedeco.opencv.global.opencv_imgcodecs.imread;
import static org.bytedeco.opencv.global.opencv_highgui.imshow;
import static org.bytedeco.opencv.global.opencv_highgui.destroyAllWindows;
import static org.bytedeco.opencv.global.opencv_highgui.waitKey;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

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

import com.sun.org.apache.bcel.internal.generic.BASTORE;

import it.unipi.ing.mim.deep.ImgDescriptor;
import it.unipi.ing.mim.deep.Parameters;
import it.unipi.ing.mim.deep.tools.StreamManagement;
import it.unipi.ing.mim.features.FeaturesExtraction;
import it.unipi.ing.mim.features.FeaturesMatching;
import it.unipi.ing.mim.features.FeaturesMatchingFiltered;
import it.unipi.ing.mim.main.Centroid;
import it.unipi.ing.mim.utils.BOF;
import it.unipi.ing.mim.utils.MatConverter;

public class ElasticImgSearching implements AutoCloseable {

	private static String HOST = "localhost";
	private static int PORT = 9200;
	private static String PROTOCOL = "http";
	private RestHighLevelClient client;
	
	public void search(String image) throws Exception {
		// Read the image to search and extract its feature
		Mat queryImg = imread(image);
		ElasticImgSearching eis = new ElasticImgSearching(Parameters.TOP_K_QUERY);
		FeaturesExtraction extractor = new FeaturesExtraction(FeaturesExtraction.SIFT_FEATURES);
		KeyPointVector keypoints = new KeyPointVector();
		extractor.getDescExtractor().detect(queryImg, keypoints);
		
		Mat queryDesc = extractor.extractDescriptor(queryImg, keypoints);
		ImgDescriptor query = new ImgDescriptor(MatConverter.mat2float(queryDesc), image);

		// Make the search by computing the bag of feature of the query
		String bofQuery = BOF.features2Text(eis.computeClusterFrequencies(query), Parameters.K);
		List<String> neighbours = eis.search(bofQuery, Parameters.K);
		eis.close();
		
		// Compute ORB features for query
		extractor = new FeaturesExtraction(FeaturesExtraction.ORB_FEATURES);
		KeyPointVector qryKeypoints = new KeyPointVector();
		extractor.getDescExtractor().detect(queryImg, qryKeypoints);
		queryDesc = extractor.extractDescriptor(queryImg, qryKeypoints);
		
		// Compute ORB features for each neighbour and 
		FeaturesMatching matcher = new FeaturesMatching();
		FeaturesMatchingFiltered filter = new FeaturesMatchingFiltered();
		List<SimpleEntry<String, DMatchVector>> goodMatches = new LinkedList<>();
		for (String neighbourName : neighbours) {
			Mat img = imread(neighbourName);
			keypoints = new KeyPointVector();
			extractor.getDescExtractor().detect(img, keypoints);
			Mat imgFeatures = extractor.extractDescriptor(img, keypoints);
			DMatchVector matches = matcher.match(queryDesc, imgFeatures);
			DMatchVector filteredMatches = filter.filterMatches(matches, Parameters.MAX_DISTANCE_THRESHOLD);
			goodMatches.add(new SimpleEntry<String, DMatchVector>(neighbourName, filteredMatches));
 		}
		
		// Get the image with the great number of matches
		long numGoodMatches = 0;
		SimpleEntry<String, DMatchVector> bestGoodMatch = null;
		for (SimpleEntry<String, DMatchVector> goodMatch : goodMatches) {
			long size = goodMatch.getValue().size();
			if (size > numGoodMatches) {
				numGoodMatches = size;
				bestGoodMatch = goodMatch;
			}
		}
		if (bestGoodMatch != null) {
			Mat bestImg = imread(bestGoodMatch.getKey());
			keypoints = new KeyPointVector();
			extractor.getDescExtractor().detect(bestImg, keypoints);
			Mat imgFeatures = extractor.extractDescriptor(bestImg, keypoints);
			DMatchVector matches = matcher.match(queryDesc, imgFeatures);
			Mat imgMatches = new Mat();
			drawMatches(queryImg, qryKeypoints, bestImg , keypoints, bestGoodMatch.getValue(), imgMatches);
			imshow("Test matching", imgMatches);
			waitKey();
			destroyAllWindows();
		}
		else System.err.println("No good matches found for " + image);
	}
	
	public void close () throws IOException {
		//close REST client
		client.close();
	}
	
	public List<String> search (String queryString, int k) throws ParseException, IOException, ClassNotFoundException{
		List<String> res = new LinkedList<String>();

		//call composeSearch to get SearchRequest object
		SearchRequest searchReq= composeSearch(queryString, k);
		
		//perform elasticsearch search
		SearchResponse searchResponse = client.search(searchReq, RequestOptions.DEFAULT);
		SearchHit[] hits = searchResponse.getHits().getHits();
		
		res = new ArrayList<>(hits.length);	
		for (int i = 0; i < hits.length; i++) {
			Map<String, Object> metadata = hits[i].getSourceAsMap();
			String id =  (String) metadata.get(Fields.ID);
			System.out.println("img: " + id + "\n Score: " + hits[i].getScore() );
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
		SearchRequest searchRequest = new SearchRequest(Parameters.INDEX_NAME);
		searchRequest.types("doc");
		searchRequest.source(sb);
		return searchRequest;
	}
	
	public List<ImgDescriptor> reorder (ImgDescriptor queryF, List<ImgDescriptor> res) throws IOException, ClassNotFoundException {
		//for each result evaluate the distance with the query, call  setDist to set the distance, then sort the results
		for(ImgDescriptor imgDescTemp: res) {
//			imgDescTemp.distance(queryF);
		  }
		
		Collections.sort(res);
		return res;
	}
	
	@SuppressWarnings("unchecked")
	public SimpleEntry<Integer, Integer>[] computeClusterFrequencies (ImgDescriptor query) throws FileNotFoundException, ClassNotFoundException, IOException {
		// Read centroids, compute distances of query to each of them
		List<Centroid> centroidList = (List<Centroid>) StreamManagement.load(Parameters.PIVOTS_FILE, List.class);
		Float[][] distancesFromCentroids = query.distancesTo(centroidList);
		int[] qryLabel = new int[distancesFromCentroids.length];
		
		// Create the "label" of the cluster, that is an array of at which cluster the keypoint
		// considered belong to
		Arrays.fill(qryLabel, 0);
		float minValue = Float.MAX_VALUE;
		for (int i = 0; i < distancesFromCentroids.length; ++i) {
			for (int j = 0; j < distancesFromCentroids[i].length; ++j) {
				if (minValue > distancesFromCentroids[i][j]) {
					minValue = distancesFromCentroids[i][j];
					qryLabel[i] = j;	// Save the cluster index
				}
			}
		}

		// Compute frequencies of clusters the query belongs to
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
}
