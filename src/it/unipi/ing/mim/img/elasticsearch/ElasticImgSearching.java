package it.unipi.ing.mim.img.elasticsearch;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.http.HttpHost;
import org.apache.lucene.queryparser.classic.ParseException;
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

import it.unipi.ing.mim.deep.ImgDescriptor;
import it.unipi.ing.mim.deep.Parameters;
import it.unipi.ing.mim.deep.tools.FeaturesStorage;
import it.unipi.ing.mim.deep.tools.Output;

public class ElasticImgSearching implements AutoCloseable {

	private RestHighLevelClient client;
	
	private Pivots pivots;
	
	private int topKSearch;
	
	//optional
	private Map<String, ImgDescriptor> imgDescMap;
		
	//TODO
	public ElasticImgSearching(File pivotsFile, int topKSearch) throws ClassNotFoundException, IOException {
		//Initialize pivots, imgDescMap, REST
		this.pivots= new Pivots(pivotsFile);
		this.topKSearch=topKSearch;
		RestClientBuilder builder= RestClient.builder(new HttpHost("localhost", 9200, "http"));
	    client=new RestHighLevelClient(builder);
	    
	    //optional
	    List<ImgDescriptor> tempList= FeaturesStorage.load(Parameters.STORAGE_FILE);
	    this.imgDescMap= new HashMap<>();
	    for(ImgDescriptor imgDescTemp: tempList) {
	    	imgDescMap.put(imgDescTemp.getId(),imgDescTemp);
	    }
	    
	}
	
	//TODO
	public void close() throws IOException {
		//close REST client
		client.close();
	}
	
	//TODO
	public List<ImgDescriptor> search(ImgDescriptor queryF, int k) throws ParseException, IOException, ClassNotFoundException{
		List<ImgDescriptor> res = null;
		//convert queryF to text
		String queryString= pivots.features2Text(queryF, k);
		//call composeSearch to get SearchRequest object
		SearchRequest searchReq= composeSearch(queryString, k);
		//perform elasticsearch search
		SearchResponse searchResponse=client.search(searchReq, RequestOptions.DEFAULT);
		SearchHit[] hits= searchResponse.getHits().getHits();
		res=new ArrayList<>(hits.length);	
		
		//LOOP to fill res
			//for each result retrieve the ImgDescriptor from imgDescMap and call setDist to set the score
		for (int i = 0; i < hits.length; i++) {
			Map<String, Object> metadata= hits[i].getSourceAsMap();
			String id=  (String)metadata.get(Fields.ID);
			ImgDescriptor imgDescTemp= new ImgDescriptor(null, id);
			imgDescMap.get(id).setDist(hits[i].getScore());
			res.add(imgDescMap.get(id));//imgDescTemp);
			
		}
		return res;
	}
	
	//TODO
	private SearchRequest composeSearch(String query, int k) {
		//Initialize SearchRequest and set query and k
		SearchRequest searchRequest = null;
		
		QueryBuilder queryBuild=QueryBuilders.multiMatchQuery(query, Fields.IMG);
		SearchSourceBuilder sb=new SearchSourceBuilder();
		sb.size(k);
		sb.query(queryBuild);
		
		searchRequest= new SearchRequest(Parameters.INDEX_NAME);
		searchRequest.types("doc");
		searchRequest.source(sb);
		return searchRequest;
	}
	
	//TODO
	public List<ImgDescriptor> reorder(ImgDescriptor queryF, List<ImgDescriptor> res) throws IOException, ClassNotFoundException {
		//Optional Step!!!
		//LOOP
		//for each result evaluate the distance with the query, call  setDist to set the distance, then sort the results
		for(ImgDescriptor imgDescTemp: res) {
			//imgDescTemp.distance(queryF);
		  }
		
		Collections.sort(res);
		return res;
	}
}
