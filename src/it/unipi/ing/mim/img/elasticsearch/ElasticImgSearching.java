package it.unipi.ing.mim.img.elasticsearch;

import static org.bytedeco.opencv.global.opencv_features2d.drawMatches;
import static org.bytedeco.opencv.global.opencv_highgui.destroyAllWindows;
import static org.bytedeco.opencv.global.opencv_highgui.waitKey;
import static org.bytedeco.opencv.global.opencv_imgcodecs.imread;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.http.Header;
import org.apache.http.HttpHost;
import org.apache.lucene.queryparser.classic.ParseException;
import org.bytedeco.opencv.opencv_core.DMatchVector;
import org.bytedeco.opencv.opencv_core.KeyPointVector;
import org.bytedeco.opencv.opencv_core.Mat;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.HttpAsyncResponseConsumerFactory;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RequestOptions.Builder;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.builder.SearchSourceBuilder;

import com.github.cliftonlabs.json_simple.JsonException;
import com.github.cliftonlabs.json_simple.JsonObject;

import it.unipi.ing.mim.deep.ImgDescriptor;
import it.unipi.ing.mim.deep.tools.Output;
import it.unipi.ing.mim.deep.tools.StreamManagement;
import it.unipi.ing.mim.features.BoundingBox;
import it.unipi.ing.mim.features.FeaturesExtraction;
import it.unipi.ing.mim.features.FeaturesMatching;
import it.unipi.ing.mim.features.FeaturesMatchingFiltered;
import it.unipi.ing.mim.features.KeyPointsDetector;
import it.unipi.ing.mim.features.Ransac;
import it.unipi.ing.mim.main.Centroid;
import it.unipi.ing.mim.main.Parameters;
import it.unipi.ing.mim.main.RansacParameters;
import it.unipi.ing.mim.utils.BOF;
import it.unipi.ing.mim.utils.MatConverter;
import it.unipi.ing.mim.utils.MetadataRetriever;
import it.unipi.ing.mim.utils.ResizeImage;

public class ElasticImgSearching implements AutoCloseable {

	private static String HOST = "localhost";
	private static int PORT = 9200;
	private static String PROTOCOL = "http";
	private RestHighLevelClient client;
	private int topKqry;
	private List<Centroid> centroidList = null;
	
	private RansacParameters ransacParameters;
	
	public ElasticImgSearching (int topKSearch) throws ClassNotFoundException, IOException {
		//Initialize pivots, imgDescMap, REST
		this(new RansacParameters(), topKSearch);
	}
	
	public ElasticImgSearching (RansacParameters parameters, int topKSearch) throws ClassNotFoundException, IOException {
		//Initialize pivots, imgDescMap, REST
		ransacParameters = parameters;
		RestClientBuilder builder = RestClient.builder(new HttpHost(HOST, PORT, PROTOCOL));
	    client = new RestHighLevelClient(builder);
	    this.topKqry = topKSearch; 
	}
	
	public String search (String qryImage, boolean test) throws Exception {
		if(!qryImage.endsWith("jpg")) throw new IllegalArgumentException("Image " + qryImage +" is not a .jpg file format");
		
		// Read the image to search and extract its feature
		MatConverter matConverter = new MatConverter();
		
		Mat queryImg = ResizeImage.resizeImage(imread(qryImage));
		KeyPointsDetector detector = new KeyPointsDetector(KeyPointsDetector.SIFT_FEATURES);
		FeaturesExtraction extractor = new FeaturesExtraction(detector.getKeypointDetector());
		KeyPointVector keypoints = detector.detectKeypoints(queryImg);
		Mat queryDesc = extractor.extractDescriptor(queryImg, keypoints);
		if (queryDesc.empty()) {
			System.err.println("Query image is not a valid image for extracting features");
			System.exit(1);
		}
		ImgDescriptor query = new ImgDescriptor(matConverter.mat2float(queryDesc), qryImage);

		// Make the search by computing the bag of feature of the query
		String bofQuery = BOF.features2Text(computeClusterFrequencies(query), Parameters.NUM_BOF_CLUSTERS); //50);//
		List<String> neighbours = search(bofQuery, Parameters.KNN);
		
		String bestGoodMatchName= computeBestGoodMatch(neighbours, queryImg, qryImage, test);
		
		if(bestGoodMatchName==null) {
			System.err.println("No good matches found for " + qryImage);
		}
		return bestGoodMatchName;
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
		Builder options = RequestOptions.DEFAULT.toBuilder();
		options.setHttpAsyncResponseConsumerFactory(
				new HttpAsyncResponseConsumerFactory.HeapBufferedResponseConsumerFactory(1000000000)
				);
		SearchResponse searchResponse = client.search(searchReq, options.build());
		client.close();
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
	
	@SuppressWarnings("unchecked")
	public SimpleEntry<Integer, Integer>[] computeClusterFrequencies (ImgDescriptor query) throws FileNotFoundException, ClassNotFoundException, IOException {
		// Read centroids, compute distances of query to each of them
		if (this.centroidList == null || this.centroidList.isEmpty()) {
			this.centroidList =  (List<Centroid>) StreamManagement.load(Parameters.PIVOTS_FILE, List.class);
		}
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
	
	public String computeBestGoodMatch(List<String> neighbours, Mat queryImg, String qryImage, boolean test) throws FileNotFoundException, IOException, JsonException {
		
		FeaturesMatching matcher = new FeaturesMatching();
		FeaturesMatchingFiltered filter = new FeaturesMatchingFiltered();
		List<SimpleEntry<String, DMatchVector>> goodMatches = new LinkedList<>();
		
		// Compute ORB features for query
		KeyPointsDetector detector = new KeyPointsDetector(KeyPointsDetector.ORB_FEATURES);
		FeaturesExtraction extractor = new FeaturesExtraction(detector.getKeypointDetector());
		KeyPointVector qryKeypoints = detector.detectKeypoints(queryImg);
		Mat queryDesc = extractor.extractDescriptor(queryImg, qryKeypoints);
		
		KeyPointVector keypoints= null;
		
		for (String neighbourName : neighbours) {
			Mat img = ResizeImage.resizeImage(imread(neighbourName));
			keypoints = detector.detectKeypoints(img);
			Mat imgFeatures = extractor.extractDescriptor(img, keypoints);
			DMatchVector matches = matcher.match(queryDesc, imgFeatures);
			DMatchVector filteredMatches = filter.filterMatches(matches, ransacParameters.getDistanceThreshold());
			goodMatches.add(new SimpleEntry<String, DMatchVector>(neighbourName, filteredMatches));
 		}
		
		// Get the image with the best number of matches using RANSAC (RANdom SAmple Consensus)
		long maxInliers = 0;
		Ransac ransac = new Ransac(ransacParameters);
		Mat bestHomography = null;
		Mat bestImg=null;
		KeyPointVector bestKeypoints=null;
		SimpleEntry<String, DMatchVector> bestGoodMatch = null;
		for (SimpleEntry<String, DMatchVector> goodMatch : goodMatches) {
			DMatchVector matches = goodMatch.getValue();
			if (matches.size() > 0) {
				Mat img = ResizeImage.resizeImage(imread(goodMatch.getKey()));
				keypoints = detector.detectKeypoints(img);
				ransac.computeHomography(goodMatch.getValue(), qryKeypoints, keypoints);
				int inliers = ransac.countNumInliers();
				if (inliers > maxInliers) {
					maxInliers = inliers;
					bestGoodMatch = goodMatch;
					bestHomography = ransac.getHomography();
					bestImg= img;
					bestKeypoints= keypoints;
				}
			}
		}
		if (bestGoodMatch != null) {
			if (test == false) {
				JsonObject metadata = MetadataRetriever.readJsonFile(bestGoodMatch.getKey());
				String qryImagePath = Parameters.BASE_URI + qryImage;
				String bestMatchPath = Parameters.BASE_URI + bestGoodMatch.getKey();
				Output.toHTML(metadata, qryImagePath, bestMatchPath, Parameters.RESULTS_HTML);
				
				Mat imgMatches = new Mat();
				drawMatches(  queryImg, qryKeypoints, bestImg , bestKeypoints, bestGoodMatch.getValue(), imgMatches);
				BoundingBox.addBoundingBox(imgMatches, queryImg, bestHomography,0);// queryImg.cols());
				BoundingBox.imshow("RANSAC", imgMatches);
				waitKey();
				destroyAllWindows();
			}
			return bestGoodMatch.getKey();
		}
		else {
			return null;
		}
	}

	public RansacParameters getRansacParameters() {
		return ransacParameters;
	}

	public void setRansacParameters(RansacParameters ransacParameters) {
		this.ransacParameters = ransacParameters;
	}
}
