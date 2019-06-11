package it.unipi.ing.mim.deep.seq;

import java.util.Collections;
import java.util.List;

import it.unipi.ing.mim.deep.ImgDescriptor;

public class SeqImageSearch {

	private List<ImgDescriptor> descriptors;

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
