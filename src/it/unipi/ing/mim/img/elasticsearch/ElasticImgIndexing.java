package it.unipi.ing.mim.img.elasticsearch;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.AbstractMap.SimpleEntry;

import org.apache.http.HttpHost;
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
import it.unipi.ing.mim.deep.Parameters;
import it.unipi.ing.mim.deep.tools.FeaturesStorage;
import it.unipi.ing.mim.main.Centroid;
import it.unipi.ing.mim.utils.BOF;

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
		this.postingListDataset = FeaturesStorage.load(postingListFile);
		this.topKIdx = topKIdx;
		RestClientBuilder builder= RestClient.builder(new HttpHost(HOST, PORT, PROTOCOL));
	    client = new RestHighLevelClient(builder);
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
					client.index(request,RequestOptions.DEFAULT);
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
