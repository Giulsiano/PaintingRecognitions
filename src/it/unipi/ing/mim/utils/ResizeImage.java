package it.unipi.ing.mim.utils;

import static org.bytedeco.opencv.global.opencv_imgcodecs.imread;
import static org.bytedeco.opencv.global.opencv_imgproc.resize;

import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.opencv.opencv_core.Size;

import it.unipi.ing.mim.main.Parameters;

public class ResizeImage {

	public static Mat resizeImage (Mat image) {
		Mat resizedImage = new Mat();
		int h = image.rows();
		int w = image.cols();
		float scaleFactor = (float) Math.sqrt(Parameters.MPX_PER_IMAGE/(w * h));
		w = Math.round(w * scaleFactor);
		h = Math.round(h * scaleFactor);
		resize(image, resizedImage, new Size(w, h));
		return resizedImage;
	}
}