package it.unipi.ing.mim.features;


import org.bytedeco.javacv.CanvasFrame;
import org.bytedeco.javacv.OpenCVFrameConverter;
import org.bytedeco.opencv.opencv_core.Mat;

public class ImageBox {
	
	private static OpenCVFrameConverter.ToMat toMat = new OpenCVFrameConverter.ToMat();

	public static void imshow(String title, Mat img) {
		CanvasFrame canvas = new CanvasFrame(title);
		canvas.showImage(toMat.convert(img));
	}
}
