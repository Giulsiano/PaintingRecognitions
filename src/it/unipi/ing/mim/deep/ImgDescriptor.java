package it.unipi.ing.mim.deep;

import java.io.Serializable;

import org.bytedeco.javacpp.indexer.FloatRawIndexer;
import org.bytedeco.opencv.opencv_core.Mat;
import static org.bytedeco.opencv.global.opencv_core.CV_32F;

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
	
	public static float[][] mat2float (Mat mat){
		FloatRawIndexer idx = mat.createIndexer();
		int rows = (int) idx.rows();
		int cols = (int) idx.cols();
		float[][] matrix = new float[rows][cols];
		for (int i = 0; i < rows; i++) {
			for (int j = 0; j < cols; ++j) {
				matrix[i][j] = idx.get(i, j);
			}
		}
		return matrix;
	}
	
	public static Mat float2Mat (float[][] mat){
		Mat matrix = new Mat(mat.length, mat[0].length, CV_32F);
		FloatRawIndexer idx = matrix.createIndexer();
		int rows = (int) idx.rows();
		int cols = (int) idx.cols();
		for (int i = 0; i < rows; i++) {
			for (int j = 0; j < cols; ++j) {
				idx.put(i, j, mat[i][j]);
			}
		}
		return matrix;
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
