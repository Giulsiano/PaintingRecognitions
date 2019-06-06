package it.unipi.ing.mim.main;

import java.io.Serializable;

import org.bytedeco.javacpp.indexer.FloatRawIndexer;
import org.bytedeco.opencv.opencv_core.Mat;

import it.unipi.ing.mim.deep.ImgDescriptor;

public class Centroid implements Serializable{

	private static final long serialVersionUID = 1L;
	public static final int NUMBER_OF_COLS = 128;
	private float[] coordinates;
	private int id;

	public Centroid(Mat row, int id) {
		if (row.rows() > 1) throw new IllegalArgumentException("Mat row is not a row");
		FloatRawIndexer idx = row.createIndexer();
		for (int i = 0; i < row.cols(); ++i) {
			coordinates[i] = idx.get(i);
		}
	}
	
	public Float[] distancesTo (ImgDescriptor img) {
		Mat imgKeypoints = img.getFeatures();
		FloatRawIndexer keypointIdx = imgKeypoints.createIndexer();
		
		int rows = img.getRows();
		int cols = img.getCols();
		Float[] d = new Float[(int) rows];
		for (int i = 0; i < rows; i++) {
			for (int j = 0; j < cols; j++) {
				float value = keypointIdx.get(i, j);
				d[i] += (value - coordinates[j]) * (value- coordinates[j]);
			}
			d[i] = (float) Math.sqrt(d[i]);
		}
		return d;
	}
	
	public float[] getCoordinates() {
		return coordinates;
	}

	public void setCoordinates(float[] coordinates) {
		this.coordinates = coordinates;
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

}
