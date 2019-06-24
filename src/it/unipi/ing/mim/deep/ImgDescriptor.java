package it.unipi.ing.mim.deep;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import it.unipi.ing.mim.main.Centroid;

public class ImgDescriptor implements Serializable, Comparable<ImgDescriptor> {

	private static final long serialVersionUID = 1L;
	
	private float[][] features; // image feature
	
	private String id; // unique id of the image (usually file name)
	
	private double dist; // used for sorting purposes
	
	public ImgDescriptor(float[][] features, String id) {
		this.features = new float[features.length][];
		
		// Compute normalized features for this image
		for (int i = 0; i < features.length; ++i) {
			float[] feat = features[i];
			float norm2 = evaluateNorm2(feat);
			this.features[i] = getNormalizedVector(feat, norm2);
		}
		this.id = id;
	}
	
	public float[][] getFeatures() {
		return features;
	}
	
    public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public double getDist() {
		return dist;
	}

	public void setDist(double dist) {
		this.dist = dist;
	}

	// compare with other friends using distances
	@Override
	public int compareTo(ImgDescriptor arg0) {
		return Double.valueOf(dist).compareTo(arg0.dist);
	}
	 
	public Float[][] distancesTo (List<Centroid> centroids) {
		Float[][] distances = new Float[features.length][];
		
		int centroidNum = centroids.size();
		for (int i = 0; i < features.length; i++) {
			Iterator<Centroid> centroIt = centroids.iterator();
			distances[i] = new Float[centroidNum];
			Arrays.fill(distances[i], 0.0f);
			for (int k = 0; k < centroidNum; ++k) {
				Centroid centroid = centroIt.next();
				for (int j = 0; j < features[i].length; j++) {
					float[] coordinates = centroid.getCoordinates();
					distances[i][k] += (features[i][j] - coordinates[j]) * (features[i][j]- coordinates[j]);
				}
				distances[i][k] = (float) Math.sqrt(distances[i][k]);
			}
		}
		return distances;
	}
	
	//Normalize the vector values 
	private float[] getNormalizedVector(float[] vector, float norm) {
		if (norm != 0) {
			for (int i = 0; i < vector.length; i++) {
				vector[i] = vector[i]/norm;
			}
		}
		return vector;
	}
	
	//Norm 2
	private float evaluateNorm2(float[] vector) {
		float norm2 = 0;
		for (int i = 0; i < vector.length; i++) {
			norm2 += (vector[i]) * (vector[i]);
		}
		norm2 = (float) Math.sqrt(norm2);
		
		return norm2;
	}
    
}
