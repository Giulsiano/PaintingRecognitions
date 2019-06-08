package it.unipi.ing.mim.deep.seq;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

import it.unipi.ing.mim.deep.ImgDescriptor;
import it.unipi.ing.mim.deep.tools.FeaturesStorage;

public class SeqImageSearch {

	private List<ImgDescriptor> descriptors;
		
	public void open(File storageFile) throws ClassNotFoundException, IOException {
		descriptors = FeaturesStorage.load(storageFile );
	}
	
	public List<ImgDescriptor> search(ImgDescriptor queryF, int k) {
		long time = -System.currentTimeMillis();
		for (int i=0;i<descriptors.size();i++){
			//descriptors.get(i).distance(queryF);
		}
		time += System.currentTimeMillis();
		System.out.println(time + " ms");

		Collections.sort(descriptors);
		
		return descriptors.subList(0, k);
	}

}
