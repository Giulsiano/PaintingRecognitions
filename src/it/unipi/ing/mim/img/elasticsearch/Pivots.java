package it.unipi.ing.mim.img.elasticsearch;

import it.unipi.ing.mim.deep.ImgDescriptor;
import it.unipi.ing.mim.deep.Parameters;
import it.unipi.ing.mim.deep.seq.SeqImageSearch;
import it.unipi.ing.mim.deep.tools.FeaturesStorage;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Pivots {
	
	private SeqImageSearch seqPivots = new SeqImageSearch();
	
	//TODO
	public Pivots(File pivotsFile) throws ClassNotFoundException, IOException {
		//Load the pivots file
		seqPivots.open(pivotsFile);
	}

	public static void main(String[] args) throws ClassNotFoundException, IOException {
		List<ImgDescriptor> ids = FeaturesStorage.load(Parameters.STORAGE_FILE);
		List<ImgDescriptor> pivs = Pivots.makeRandomPivots(ids, Parameters.NUM_PIVOTS);
		FeaturesStorage.store(pivs, Parameters.PIVOTS_FILE);
	}
	
	//TODO
	public static List<ImgDescriptor> makeRandomPivots(List<ImgDescriptor> ids, int nPivs) {
		ArrayList<ImgDescriptor> pivots = null;
		pivots= new ArrayList<>();
		Collections.shuffle(ids);
		//LOOP
		//Create nPivs random pivots and add them in the pivots List
		for(int i =0; i<nPivs; i++) {
			pivots.add(ids.get(i));
			pivots.get(i).setId(String.valueOf(""+i));
		}
		return pivots;
	}
	
	
	//TODO
	public String features2Text(ImgDescriptor imgF, int topK) {
		StringBuilder sb = null;
		sb=new StringBuilder();
		//perform a sequential search to get the topK most similar pivots
		//LOOP
			//compose the text string using pivot ids
		ArrayList<ImgDescriptor> topKpivot = new ArrayList<>( seqPivots.search(imgF, topK) );
		int topkTemp=topK;
		for(int i=0; i<topKpivot.size(); i++) {
			String temp="";
			String id=topKpivot.get(i).getId();
			
			for(int j=topkTemp; j>0; j--) {
				temp+= id+ " ";
				}
			topkTemp--;
			sb.append(temp+'\n');
			}
		//sb.append(""+'\n');
		System.out.println("created:" + sb.toString());
		return sb.toString();
	}
	
}