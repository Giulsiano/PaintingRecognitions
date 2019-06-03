package it.unipi.ing.mim.deep;

import java.io.Serializable;

public class ImgDescriptor implements Serializable, Comparable<ImgDescriptor> {

	private static final long serialVersionUID = 1L;
	
	private float[][] features; // image feature
	
	private String id; // unique id of the image (usually file name)
	
	private double dist; // used for sorting purposes
	
	public ImgDescriptor(float[][] features, String id) {
		if (features != null) {
			this.features = new float[features.length][128];
			for (int i = 0; i < features.length; ++i) {
				float norm2 = evaluateNorm2(features[i]);
				this.features[i] = getNormalizedVector(features[i], norm2);
			}
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
	
	//evaluate Euclidian distance
	public double distance(ImgDescriptor desc) {
		float[][] queryVector = desc.getFeatures();
		
		dist = 0;
		for (int i = 0; i < queryVector.length; i++) {
			for (int j = 0; j < queryVector[i].length; ++j) {
				dist += (features[i][j] - queryVector[i][j]) * (features[i][j] - queryVector[i][j]);
			}
			// TODO: questa cosa non ha senso in caso di una matrice di features
			dist = Math.sqrt(dist);
		}
		
		return dist;
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
