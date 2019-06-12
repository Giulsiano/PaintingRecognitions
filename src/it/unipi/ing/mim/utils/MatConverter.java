package it.unipi.ing.mim.utils;

import static org.bytedeco.opencv.global.opencv_core.CV_32F;
import static org.bytedeco.opencv.global.opencv_core.CV_32S;

import org.bytedeco.javacpp.indexer.FloatRawIndexer;
import org.bytedeco.javacpp.indexer.IntRawIndexer;
import org.bytedeco.opencv.opencv_core.Mat;

public class MatConverter {

	public float[][] mat2float (Mat mat){
		FloatRawIndexer idx = mat.createIndexer();
		int rows = (int) idx.rows();
		int cols = (int) idx.cols();
		float[][] matrix = new float[rows][cols];
		for (int i = 0; i < rows; i++) {
			idx.get(i, matrix[i]);
		}
		return matrix;
	}
	
	public int[][] mat2int (Mat mat){
		IntRawIndexer idx = mat.createIndexer();
		int rows = (int) idx.rows();
		int cols = (int) idx.cols();
		int[][] matrix = new int[rows][cols];
		for (int i = 0; i < rows; i++) {
			idx.get(i, matrix[i]);
		}
		return matrix;
	}

	public Mat int2Mat (int[][] mat){
		Mat matrix = new Mat(mat.length, mat[0].length, CV_32S);
		IntRawIndexer idx = matrix.createIndexer();
		int rows = (int) idx.rows();
		for (int i = 0; i < rows; i++) {
			idx.put(i, mat[i]);
		}
		return matrix;
	}
	
	public Mat float2Mat (float[][] mat){
		Mat matrix = new Mat(mat.length, mat[0].length, CV_32F);
		FloatRawIndexer idx = matrix.createIndexer();
		int rows = (int) idx.rows();
		for (int i = 0; i < rows; i++) {
			idx.put(i, mat[i]);
		}
		return matrix;
	}
}
