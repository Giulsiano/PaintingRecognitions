package it.unipi.ing.mim.img.elasticsearch;

import static org.bytedeco.opencv.global.opencv_imgcodecs.imread;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.AbstractMap.SimpleEntry;

import org.apache.http.HttpHost;
import org.apache.lucene.queryparser.classic.ParseException;
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

import com.sun.xml.internal.bind.v2.runtime.unmarshaller.XsiNilLoader.Array;

import it.unipi.ing.mim.deep.ImgDescriptor;
import it.unipi.ing.mim.deep.Parameters;
import it.unipi.ing.mim.deep.tools.Output;
import it.unipi.ing.mim.deep.tools.StreamManagement;
import it.unipi.ing.mim.features.FeaturesExtraction;
import it.unipi.ing.mim.features.KeyPointsDetector;
import it.unipi.ing.mim.main.Centroid;
import it.unipi.ing.mim.utils.BOF;
import it.unipi.ing.mim.utils.MatConverter;

public class ElasticImgSearching implements AutoCloseable {

	private static String HOST = "localhost";
	private static int PORT = 9200;
	private static String PROTOCOL = "http";
	private RestHighLevelClient client;
	
	private int topKSearch;
	
	//optional
	private Map<String, ImgDescriptor> imgDescMap;
	
	public static void search(String image) throws Exception {
		// Read the image to search and extract its feature
		Mat img = imread(image);
		ElasticImgSearching eis = new ElasticImgSearching(Parameters.TOP_K_QUERY);
		
		FeaturesExtraction extractor = new FeaturesExtraction(FeaturesExtraction.SIFT_FEATURES);
		KeyPointVector keypoints = new KeyPointVector(); 
		extractor.getDescExtractor().detect(img, keypoints);
		
		Mat queryDesc = extractor.extractDescriptor(img, keypoints);
		ImgDescriptor query = new ImgDescriptor(MatConverter.mat2float(queryDesc), image);
		
		
		String bofQuery = BOF.features2Text(eis.computeClusterFrequencies(query), Parameters.TOP_K_QUERY);
		List<String> neighbours = eis.search(bofQuery, Parameters.K);
//		Output.toHTML(neighbours, Parameters.BASE_URI, Parameters.RESULTS_HTML_REORDERED);
	}
		
	//TODO
	public ElasticImgSearching (int topKSearch) throws ClassNotFoundException, IOException {
		//Initialize pivots, imgDescMap, REST
		this.topKSearch = topKSearch;
		RestClientBuilder builder = RestClient.builder(new HttpHost(HOST, PORT, PROTOCOL));
	    client = new RestHighLevelClient(builder);
	}
	
	//TODO
	public void close () throws IOException {
		//close REST client
		client.close();
	}
	
	//TODO
	public List<String> search (String queryString, int k) throws ParseException, IOException, ClassNotFoundException{
		List<String> res = new LinkedList<String>();

		//call composeSearch to get SearchRequest object
		SearchRequest searchReq= composeSearch(queryString, k);
		
		//perform elasticsearch search
		SearchResponse searchResponse = client.search(searchReq, RequestOptions.DEFAULT);
		SearchHit[] hits = searchResponse.getHits().getHits();
		res = new ArrayList<>(hits.length);	
		
		//for each result retrieve the ImgDescriptor from imgDescMap and call setDist to set the score
		for (int i = 0; i < hits.length; i++) {
			Map<String, Object> metadata = hits[i].getSourceAsMap();
			String id =  (String) metadata.get(Fields.ID);
			res.add(id);
		}
		return res;
	}
	
	//TODO
	private SearchRequest composeSearch (String query, int k) {
		//Initialize SearchRequest and set query and k
		SearchRequest searchRequest = null;
		
		QueryBuilder queryBuild = QueryBuilders.multiMatchQuery(query, Fields.IMG);
		SearchSourceBuilder sb = new SearchSourceBuilder();
		sb.size(k);
		sb.query(queryBuild);
		searchRequest = new SearchRequest(Parameters.INDEX_NAME);
		searchRequest.types("doc");
		searchRequest.source(sb);
		return searchRequest;
	}
	
	//TODO
	public List<ImgDescriptor> reorder (ImgDescriptor queryF, List<ImgDescriptor> res) throws IOException, ClassNotFoundException {
		//for each result evaluate the distance with the query, call  setDist to set the distance, then sort the results
		for(ImgDescriptor imgDescTemp: res) {
//			imgDescTemp.distance(queryF);
		  }
		
		Collections.sort(res);
		return res;
	}
	
	public SimpleEntry<Integer, Integer>[] computeClusterFrequencies (ImgDescriptor query) throws FileNotFoundException, ClassNotFoundException, IOException {
		// Read centroids, compute distances of query to each of them
		List<Centroid> centroidList = (List<Centroid>) StreamManagement.load(Parameters.PIVOTS_FILE, List.class);
		Float[][] distancesFromCentroids = query.distancesTo(centroidList);
		int[] qryLabel = new int[distancesFromCentroids.length];
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
		SimpleEntry<Integer, Integer>[] clusterFrequencies = (SimpleEntry<Integer, Integer>[]) new SimpleEntry[numClusters];
		for (int i = 0; i < frequencies.length; ++i) {
			clusterFrequencies[i] = new SimpleEntry<Integer, Integer>(i, frequencies[i]);
		}
		Arrays.parallelSort(clusterFrequencies, Comparator.comparing(SimpleEntry::getValue, 
				Comparator.reverseOrder())
				);

		return clusterFrequencies;
	}
}
