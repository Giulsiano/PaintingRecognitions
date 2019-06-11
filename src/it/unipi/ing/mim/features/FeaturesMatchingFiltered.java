package it.unipi.ing.mim.features;

import org.bytedeco.opencv.opencv_core.DMatch;
import org.bytedeco.opencv.opencv_core.DMatchVector;

public class FeaturesMatchingFiltered {

	public DMatchVector filterMatches(DMatchVector matches, int threshold) {
		//return the good matches
		long nGoodMatches = 0;
		long numMatches = matches.size();
		DMatchVector goodMatches = new DMatchVector(numMatches);
		for (long i = 0; i < numMatches; ++i) {
			DMatch match = matches.get(i);
			if (match.distance() < threshold) {
				goodMatches.put(nGoodMatches, match);
				nGoodMatches++;
			}
		}
		goodMatches.resize(nGoodMatches);
		return goodMatches;
	}
}