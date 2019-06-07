package it.unipi.ing.mim.main;

import java.io.Serializable;

import it.unipi.ing.mim.deep.ImgDescriptor;

public class Centroid implements Serializable{
	public static final int NUMBER_OF_COLS = 128;
	private float[] coordinates;
	private int id;

	public Centroid(float[] coordinates, int id) {
		if (coordinates.length == NUMBER_OF_COLS) {
			this.coordinates = coordinates;
			this.id = id;
		}
		else throw new IllegalArgumentException("Coordinates should be of " + NUMBER_OF_COLS + 
				"columns");
	}
	
	public Float[] distancesTo (ImgDescriptor img) {
		float[][] imgKeypoints = img.getFeatures();
		Float[] d = new Float[imgKeypoints.length];
		for (int i = 0; i < imgKeypoints.length; i++) {
			for (int j = 0; j < imgKeypoints[i].length; j++) {
				d[i] += (imgKeypoints[i][j] - coordinates[j]) * (imgKeypoints[i][j] - coordinates[j]);
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
