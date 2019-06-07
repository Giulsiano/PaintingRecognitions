package it.unipi.ing.mim.deep;

import java.io.Serializable;

import org.bytedeco.javacpp.indexer.FloatRawIndexer;
import org.bytedeco.opencv.opencv_core.Mat;

public class ImgDescriptor implements Serializable, Comparable<ImgDescriptor> {

	private static final long serialVersionUID = 1L;
	
	private Mat features; // image feature
	
	private String id; // unique id of the image (usually file name)
	
	private double dist; // used for sorting purposes
	
	private int rows;
	private int cols;
	
	public ImgDescriptor(Mat features, String id) {
		this.features = new Mat(features);
		FloatRawIndexer idx = this.features.createIndexer();
		this.rows = (int) this.features.rows();
		this.cols = (int) this.features.cols();
		
		// Compute normalized features for this image
		float[] feat = new float[(int) cols];
		for (int i = 0; i < rows; ++i) {
			for (int j = 0; j < cols; ++j) {
				 feat[j] = idx.get(i, j);
			}
			float norm2 = evaluateNorm2(feat);
			feat = getNormalizedVector(feat, norm2);
			for (int j = 0; j < feat.length; ++j) {
				idx.put(i, j, feat[j]);
			}
		}
	}
	
	public int getRows() {
		return rows;
	}

	public int getCols() {
		return cols;
	}

	public Mat getFeatures() {
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
	
//	//evaluate Euclidian distance
//	public double distance(ImgDescriptor desc) {
//		Mat queryVector = desc.getFeatures();
//		FloatRawIndexer qryIdx = queryVector.createIndexer();
//		FloatRawIndexer featIdx = features.createIndexer();
//		
//		long rows = qryIdx.rows();
//		long cols = qryIdx.cols();
//		dist = 0;
//		for (int i = 0; i < rows; i++) {
//			for (int j = 0; j < cols; ++j) {
//				dist += (features[i][j] - queryVector[i][j]) * (features[i][j] - queryVector[i][j]);
//			}
//			dist = Math.sqrt(dist);
//		}
//		return dist;
//	}
	
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
