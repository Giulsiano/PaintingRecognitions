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

	public Centroid(Mat m, int id) {
		if (m.rows() > 1) throw new IllegalArgumentException("Mat row is not a row");
		FloatRawIndexer idx = m.createIndexer();
		coordinates = new float[m.cols()];
		for (int i = 0; i < m.cols(); ++i) {
			coordinates[i] = idx.get(i);
		}
		this.id = id;
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
